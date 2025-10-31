/*
 * Copyright (C) 2025 Aleksei Balan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ab.nbsnk;

import Jama.Matrix;
import ab.jnc3.Screen;
import ab.nbsnk.nodes.Col;
import ab.nbsnk.nodes.Pnt;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ShaderTest {

  boolean open;

  @Disabled
  @Test
  void testPhongTexture() throws IOException {
    // Spot performance:
    // 66 fps with texture
    // 180 fps after replacing BufferedImage with int array
    // 180 fps 12MiB pre-calculated normalized viewer angle
    // during the shader improvements, it went down
    // 115 fps multiple light color and specular color
    // 2025-10-31 Pnt refactoring 105 fps -> 95 fps
    BufferedImage texture = null;
    Obj obj = Obj.load(getClass()
//        .getResourceAsStream("blender_cube.obj").readAllBytes());
//        .getResourceAsStream("teapot.obj").readAllBytes());
        .getResourceAsStream("spot_triangulated.obj").readAllBytes());
    texture = ImageIO.read(getClass().getResourceAsStream("spot_texture.png"));

//    Obj.flatNormal(obj);
    Obj.fixNormal(obj);
    Obj.verify(obj);
    Screen screen = new Screen();
    screen.preferredSize = new Dimension(960, 540);
    screen.image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB);
    runShader(screen, obj, texture);
  }

  @Disabled
  @Test
  void testSphere() throws IOException {
    Obj obj = Obj.load(Files.readAllBytes(Paths.get("../assets/blender_uv_sphere.obj")));
    BufferedImage texture = ImageIO.read(Files.newInputStream(Paths.get("../assets/photosphere.jpg")));
    Obj.interpolateNormal(obj);
    Screen screen = new Screen();
    screen.preferredSize = new Dimension(960, 540);
    screen.image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB);
    runShader(screen, obj, texture);
  }

  @Disabled
  @Test
  void polyhedron() throws IOException {
    Obj obj = Obj.load(Files.readAllBytes(Paths.get("../assets/polyhedron3.obj")));
//    obj = Obj.load(Files.readAllBytes(Paths.get("assets/hornet_sphere.obj")));
    Obj.flatNormal(obj);
    Obj.fixNormal(obj);
    Obj.verify(obj);
    Screen screen = new Screen();
    runShader(screen, obj, null);
  }

  public static double length(double[] vector, int i, int size) {
    double length = 0;
    for (int j = 0; j < size; j++) {
      double v = vector[i++];
      length += v * v;
    }
    return Math.sqrt(length);
  }

  void runShader(Screen screen, Obj obj, BufferedImage texture) {
    int[] textureRaster = null;
    int textureWidth = 0;
    int textureHeight = 0;
    if (texture != null) {
      textureWidth = texture.getWidth();
      textureHeight = texture.getHeight();
      textureRaster = new int[textureWidth * textureHeight];
      for (int y = 0; y < textureHeight; y++) {
        for (int x = 0; x < textureWidth; x++) textureRaster[y * textureWidth + x] = texture.getRGB(x, y);
      }
    }
    double xyzmax = 0;
    for (int i = 0; i < obj.vertex.length; i += 3) xyzmax = Math.max(xyzmax, length(obj.vertex, i, 3));
    xyzmax *= 1.2;
    for (int i = 0; i < obj.vertex.length; i++) obj.vertex[i] /= xyzmax;

    Obj flat = obj.clone();
    Obj.flatNormal(flat);
    Obj smooth = obj;
    int width = screen.image.getWidth();
    int height = screen.image.getHeight();
    Shader shader = new Shader();
    shader.ambientColor = new Col(0xFF222222);
    shader.diffuseColor = new Col(0xFFDDDDDD);
    shader.specularColor = new Col(-1);
    shader.specularPower = 100;
    AtomicInteger mode = new AtomicInteger();
    screen.keyListener = key -> {
      if ("Esc".equals(key)) open = false;
      if (key.length() == 1) mode.set(key.charAt(0));
    };
    Graphics graphics = screen.image.createGraphics();
    graphics.setColor(Color.DARK_GRAY);
    open = true;
    FpsMeter fpsMeter = new FpsMeter();
    while (open) {
      switch (mode.get()) {
        case '1': shader.enableDimension = 0; break;
        case '2': shader.enableDimension = 1; break;
        case '3': shader.enableDimension = 2; break;
        case '4': shader.enableIllumination = Shader.Illumination.NONE; obj = smooth; break;
        case '5': shader.enableIllumination = Shader.Illumination.LAMBERT; obj = flat; break;
        case '6': shader.enableIllumination = Shader.Illumination.GOURAUD; obj = smooth; break;
        case '7': shader.enableIllumination = Shader.Illumination.PHONG; obj = smooth; break;
        case '=': shader.enableTexture = !shader.enableTexture; break;
      }
      mode.set(0);
      shader.cls(width, height);
      shader.textureRaster = textureRaster;
      shader.textureWidth = textureWidth;
      shader.textureHeight = textureHeight;
      shader.addLight(new Pnt(-5000, 3000, 5000), new Col(-1));
      double year = Instant.now().toEpochMilli() / 60_000.0;
      Matrix matrix = EngineNbs.IDENTITY;
      matrix = EngineNbs.multiply(matrix, 0, 0, -4, 0, 0, 0);
      matrix = EngineNbs.multiply(matrix, 0, 0, 0, 0, year, 0);
      matrix = EngineNbs.multiply(matrix, 0, 0, 0, -23.44 / 360, 0, 0);
      matrix = EngineNbs.multiply(matrix, 0, 0, 0, 0, year * 9, 0);
      shader.add(obj, matrix);
      //int[] buffer = shader.run(obj, year);
      screen.image.getRaster().setDataElements(0, 0, width, height, shader.imageRaster);
      graphics.drawString(String.format("fps: %.0f", fpsMeter.getFps()), 20, 20);
      screen.update();
    }
  }

  @Disabled
  @Test
  void barycentric() throws InterruptedException {
    open = true;
    Screen screen = new Screen();
    screen.keyListener = key -> { if (key.equals("Esc")) open = false; };
    double[] r = new double[4];
    for (int y = 0; y < 240; y++) {
      for (int x = 0; x < 320; x++) {
        if (!Shader.barycentric(x, y, 60, 1, 299, 120, 60, 239, r)) continue;
        //double cr = Shader.barycentricValue(1, 1, 0, r); // r[0] + r[1];
        //double cg = Shader.barycentricValue(1, 0.5, 0, r); // r[0] + r[1] / 2;
        //double cb = Shader.barycentricValue(1, 0, 0, r); // r[0];
        double[] rgb = new double[3];
        Shader.barycentricValue(
            new double[]{1, 1, 1}, 0, new double[]{1, 0.5, 0}, 0, new double[]{0, 0, 0}, 0, r, rgb);
        int cd7 = (int) (rgb[0] * 0xFF);
        int cd4 = (int) (rgb[1] * 0xFF);
        int i2 = (int) (rgb[2] * 0xFF);
        screen.image.setRGB(x, y, cd7 << 16 | cd4 << 8 | i2);
      }
    }
    while (open) {
      screen.update();
      Thread.sleep(100);
    }
  }

  @Test
  void viewerCenter() {
    Pnt v00 = Shader.viewerVector(0, 0, 2, 2, 5);
    Pnt v01 = Shader.viewerVector(0, 1, 2, 2, 5);
    Pnt v10 = Shader.viewerVector(1, 0, 2, 2, 5);
    Pnt v11 = Shader.viewerVector(1, 1, 2, 2, 5);
    double vxy = Math.abs(v00.x);
    double vz = v00.z;
    assertEquals(vxy, Math.abs(v00.y));
    assertEquals(vxy, Math.abs(v01.x));
    assertEquals(vxy, Math.abs(v01.y));
    assertEquals(vz, v01.z);
    assertEquals(vxy, Math.abs(v10.x));
    assertEquals(vxy, Math.abs(v10.y));
    assertEquals(vz, v10.z);
    assertEquals(vxy, Math.abs(v11.x));
    assertEquals(vxy, Math.abs(v11.y));
    assertEquals(vz, v11.z);
  }

}
