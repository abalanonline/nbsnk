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

package ab.shader;

import ab.jnc3.Screen;
import org.junit.jupiter.api.Test;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ShaderTest {

  boolean open;

  @Test
  void test() throws InterruptedException, IOException {
    open = true;
    Obj obj = Obj.load(getClass()
        .getResourceAsStream("spot_triangulated.obj").readAllBytes());
//        .getResourceAsStream("teapot.obj").readAllBytes());
//        .getResourceAsStream("blender_cube.obj").readAllBytes());
    Obj.fixNormal(obj);
    Obj.verify(obj);
    Screen screen = new Screen();
    screen.preferredSize = new Dimension(960, 540);
    screen.image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB);
    screen.keyListener = key -> { if (key.equals("Esc")) open = false; };

    int width = screen.image.getWidth();
    int height = screen.image.getHeight();
    BufferedImage doubleBufferedImage = new BufferedImage(
        screen.image.getWidth(), screen.image.getHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics graphics = doubleBufferedImage.createGraphics();
    while (open) {
      graphics.clearRect(0, 0, width, height);
      Shader.run(doubleBufferedImage, obj, Instant.now().toEpochMilli() / 60_000.0);
      screen.image.getRaster().setDataElements(0, 0, doubleBufferedImage.getRaster());
      screen.update();
      Thread.sleep(10);
    }
  }

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
        double[] rgb = Shader.barycentricValue(
            new double[]{1, 1, 1}, 0, new double[]{1, 0.5, 0}, 0, new double[]{0, 0, 0}, 0, r, 3);
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

}
