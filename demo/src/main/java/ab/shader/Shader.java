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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

public class Shader {

  /**
   * Vertex coordinates are in screen units, no further transformation required.
   * Z from 0 far to 1 near.
   */
  public static void rasterization(BufferedImage image, double[] vertex, int[] face) {
    // vertex
    IntStream.range(0, vertex.length / 3).boxed()
        .sorted(Comparator.comparingDouble(i -> vertex[i * 3 + 2])).forEach(i -> {
      double x = vertex[i * 3];
      double y = vertex[i * 3 + 1];
      double z = vertex[i * 3 + 2];
      if (z < 0 || z > 1) throw new IllegalArgumentException();
      image.setRGB((int) Math.round(x), (int) Math.round(y), (int) (z * 255) * 0x010101);
    });
    // edge
    Graphics graphics = image.getGraphics();
    for (int i = 0; i < face.length; i += 9) {
      int v0 = face[i] * 3;
      int v1 = face[i + 3] * 3;
      int v2 = face[i + 6] * 3;
      double x0 = vertex[v0];
      double x1 = vertex[v1];
      double x2 = vertex[v2];
      double y0 = vertex[v0 + 1];
      double y1 = vertex[v1 + 1];
      double y2 = vertex[v2 + 1];
      double z0 = vertex[v0 + 2];
      double z1 = vertex[v1 + 2];
      double z2 = vertex[v2 + 2];
      double ax = x1 - x0;
      double ay = y1 - y0;
      double bx = x2 - x0;
      double by = y2 - y0;
      double nz = ax * by - ay * bx; // normal of the triangle
      if (nz > 0) continue;
      graphics.setColor(new Color((int) ((z0 + z1) * 127) * 0x010101));
      graphics.drawLine((int) Math.round(x0), (int) Math.round(y0), (int) Math.round(x1), (int) Math.round(y1));
      graphics.setColor(new Color((int) ((z1 + z2) * 127) * 0x010101));
      graphics.drawLine((int) Math.round(x1), (int) Math.round(y1), (int) Math.round(x2), (int) Math.round(y2));
      graphics.setColor(new Color((int) ((z2 + z0) * 127) * 0x010101));
      graphics.drawLine((int) Math.round(x2), (int) Math.round(y2), (int) Math.round(x0), (int) Math.round(y0));
    }
  }

  /**
   * In place rotation, angle 0-1, axis 0x,1y,2z
   */
  public static void rotate(double[] vertex, double angle, int axis) {
    angle = 2 * Math.PI * angle;
    double s = Math.sin(angle);
    double c = Math.cos(angle);
    int ax = (axis + 1) % 3;
    int ay = (axis + 2) % 3;
    for (int i = 0; i < vertex.length; i += 3) {
      double x = vertex[i + ax];
      double y = vertex[i + ay];
      vertex[i + ax] = x * c - y * s;
      vertex[i + ay] = x * s + y * c;
    }
  }

  public static void run(BufferedImage image, Obj obj, double year) {
    double[] vertex = Arrays.copyOf(obj.vertex, obj.vertex.length);
    double xyzmax = 0;
    for (double v : vertex) xyzmax = Math.max(xyzmax, Math.abs(v));
    xyzmax *= 1.3;
    for (int i = 0; i < vertex.length; i++) vertex[i] /= xyzmax;
    int w2 = image.getWidth() / 2;
    int h2 = image.getHeight() / 2;

    rotate(vertex, year * 9, 1);
    rotate(vertex, -23.44 / 360, 0);
    rotate(vertex, year, 1);
    for (int i = 0; i < vertex.length / 3; i++) {
      double x = vertex[i * 3];
      double y = vertex[i * 3 + 1];
      double z = vertex[i * 3 + 2];
      double d = h2 * 5 / (5 - z);
      vertex[i * 3] = w2 + x * d;
      vertex[i * 3 + 1] = h2 - y * d;
      vertex[i * 3 + 2] = z / 2 + 0.5;
    };
    rasterization(image, vertex, obj.face);

  }

}
