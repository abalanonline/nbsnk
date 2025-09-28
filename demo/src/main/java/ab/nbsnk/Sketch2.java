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

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.time.Instant;

public class Sketch2 {

  private static final Obj cube = Obj.load(("" +
      "# Blender 4.5.3 LTS\n" +
      "# www.blender.org\n" +
      "mtllib Untitled.mtl\n" +
      "o Cube\n" +
      "v 1.000000 1.000000 -1.000000\n" +
      "v 1.000000 -1.000000 -1.000000\n" +
      "v 1.000000 1.000000 1.000000\n" +
      "v 1.000000 -1.000000 1.000000\n" +
      "v -1.000000 1.000000 -1.000000\n" +
      "v -1.000000 -1.000000 -1.000000\n" +
      "v -1.000000 1.000000 1.000000\n" +
      "v -1.000000 -1.000000 1.000000\n" +
      "vn -0.0000 1.0000 -0.0000\n" +
      "vn -0.0000 -0.0000 1.0000\n" +
      "vn -1.0000 -0.0000 -0.0000\n" +
      "vn -0.0000 -1.0000 -0.0000\n" +
      "vn 1.0000 -0.0000 -0.0000\n" +
      "vn -0.0000 -0.0000 -1.0000\n" +
      "vt 0.625000 0.500000\n" +
      "vt 0.875000 0.500000\n" +
      "vt 0.875000 0.750000\n" +
      "vt 0.625000 0.750000\n" +
      "vt 0.375000 0.750000\n" +
      "vt 0.625000 1.000000\n" +
      "vt 0.375000 1.000000\n" +
      "vt 0.375000 0.000000\n" +
      "vt 0.625000 0.000000\n" +
      "vt 0.625000 0.250000\n" +
      "vt 0.375000 0.250000\n" +
      "vt 0.125000 0.500000\n" +
      "vt 0.375000 0.500000\n" +
      "vt 0.125000 0.750000\n" +
      "s 0\n" +
      "usemtl Material\n" +
      "f 1/1/1 5/2/1 7/3/1 3/4/1\n" +
      "f 4/5/2 3/4/2 7/6/2 8/7/2\n" +
      "f 8/8/3 7/9/3 5/10/3 6/11/3\n" +
      "f 6/12/4 2/13/4 4/5/4 8/14/4\n" +
      "f 2/13/5 1/1/5 3/4/5 4/5/5\n" +
      "f 6/11/6 5/10/6 1/1/6 2/13/6\n").getBytes());

  public static void renderNoise(BufferedImage image) {
    int w = image.getWidth();
    int h = image.getHeight();
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int i = Math.max(0, 0x7F - x / 5 - y);
        int r = (int) (0x1F * (Math.sin((x + y) / 40.0) + 1));
        int g = (int) (0x1F * (Math.sin((x - y) / 40.0) + 1));
        int b = 0x3F - r;
        image.setRGB(x, y, (r << 16 | g << 8 | b | 0x404040) + i * 0x010101);
      }
    }

  }

  public static void main(String[] args) {
    Screen screen = new Screen();
//    screen.image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
//    screen.preferredSize = new Dimension(1920, 1080);
    BufferedImage background = new BufferedImage(screen.image.getWidth(), screen.image.getHeight(), BufferedImage.TYPE_INT_RGB);
    renderNoise(background);
    try (Engine3d engine3d = new EngineFx().open(screen.image)) {
      engine3d.background(background);
      Engine3d.Shape c0 = engine3d.shape(cube);
      c0.translation(-4, 0, -20);
      c0.rotation(10);

      Engine3d.Shape g0 = engine3d.shape(null);
      g0.translation(4, 0, -20);
      Engine3d.Shape c1 = engine3d.shape(cube);
      c1.translation(0, 1.5, 0);
      c1.connect(g0);
      Engine3d.Shape c2 = engine3d.shape(cube);
      c2.translation(0, -1.5, 0);
      c2.connect(g0);
      g0.rotation(10);

      Graphics graphics = screen.image.createGraphics();
      graphics.setColor(java.awt.Color.DARK_GRAY);
      boolean[] open = {true};
      screen.keyListener = key -> {
        if (key.equals("Esc")) open[0] = false;
      };
      FpsMeter fpsMeter = new FpsMeter();
      while (open[0]) {
        long m = Instant.now().toEpochMilli();
        g0.rotation(m % 3600 / 10.0);
        c0.translation(-4, Math.sin(m % 10000 / 5000.0 * Math.PI), -20);
        engine3d.update();
        graphics.drawString(String.format("fps: %.0f", fpsMeter.getFps()), 20, 20);
        screen.update();
        try { Thread.sleep(40); } catch (InterruptedException ignore) {}
      }
    }
  }

}
