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

import ab.jnc3.Screen;
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
    BufferedImage texture = null;
    Obj obj = Obj.load(getClass()
//        .getResourceAsStream("blender_cube.obj").readAllBytes());
//        .getResourceAsStream("teapot.obj").readAllBytes());
        .getResourceAsStream("spot_triangulated.obj").readAllBytes()); // 66 fps with texture
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
//    obj = Obj.load(Files.readAllBytes(Paths.get("../assets/hornet_sphere.obj")));
    Obj.flatNormal(obj);
    Obj.fixNormal(obj);
    Obj.verify(obj);
    Screen screen = new Screen();
    runShader(screen, obj, null);
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
    Obj flat = obj.clone();
    Obj.flatNormal(flat);
    Obj[] o1 = {obj};
    int width = screen.image.getWidth();
    int height = screen.image.getHeight();
    Shader shader = new Shader(width, height);
    screen.keyListener = key -> {
      switch (key) {
        case "Esc": open = false; break;
        case "1": shader.enableDimension = 0; break;
        case "2": shader.enableDimension = 1; break;
        case "3": shader.enableDimension = 2; break;
        case "4": shader.enableIllumination = 0; o1[0] = obj; break;
        case "5": shader.enableIllumination = 1; o1[0] = flat; break;
        case "6": shader.enableIllumination = 2; o1[0] = obj; break;
        case "7": shader.enableIllumination = 3; o1[0] = obj; break;
        case "=": shader.enableTexture = !shader.enableTexture; break;
      }
    };
    Graphics graphics = screen.image.createGraphics();
    graphics.setColor(Color.DARK_GRAY);
    open = true;
    FpsMeter fpsMeter = new FpsMeter();
    while (open) {
      shader.cls();
      int[] buffer = shader.run(textureRaster, textureWidth, textureHeight,
          o1[0], Instant.now().toEpochMilli() / 60_000.0);
      screen.image.getRaster().setDataElements(0, 0, width, height, buffer);
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
    Shader shader = new Shader(2, 2);
    double vxy = Math.abs(shader.viewer[0]);
    double vz = shader.viewer[2];
    assertEquals(vxy, Math.abs(shader.viewer[1]));
    assertEquals(vxy, Math.abs(shader.viewer[3]));
    assertEquals(vxy, Math.abs(shader.viewer[4]));
    assertEquals(vz, shader.viewer[5]);
    assertEquals(vxy, Math.abs(shader.viewer[6]));
    assertEquals(vxy, Math.abs(shader.viewer[7]));
    assertEquals(vz, shader.viewer[8]);
    assertEquals(vxy, Math.abs(shader.viewer[9]));
    assertEquals(vxy, Math.abs(shader.viewer[10]));
    assertEquals(vz, shader.viewer[11]);
  }

}
