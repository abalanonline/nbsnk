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

import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.IntStream;

public class Shader {

  public static final double FOCAL_LENGTH = 50;
  public static final double FILM_HEIGHT = 24;

  public final int[] imageRaster;
  public final int imageWidth;
  public final int imageHeight;
  public final double[] zbuffer;
  public final double[] viewer;
  public int[] textureRaster;
  public int textureWidth;
  public int textureHeight;
  public int[] face; // current obj
  public double[] vertex;
  public double[] normal;
  public double[] texture;

  public Shader(int imageWidth, int imageHeight) {
    this.imageRaster = new int[imageWidth * imageHeight];
    this.imageWidth = imageWidth;
    this.imageHeight = imageHeight;
    this.zbuffer = new double[imageWidth * imageHeight];
    this.viewer = new double[imageWidth * imageHeight * 3];
    double[] v = new double[3];
    final double viewerZ = FOCAL_LENGTH / FILM_HEIGHT * imageHeight;
    double imageHeight2 = imageHeight / -2.0 + 0.5;
    double imageWidth2 = imageWidth / 2.0 - 0.5;
    for (int y = 0, j = 0; y < imageHeight; y++) {
      for (int x = 0; x < imageWidth; x++) {
        v[0] = imageWidth2 - x;
        v[1] = imageHeight2 + y;
        v[2] = viewerZ;
        normalize(v);
        for (int i = 0; i < 3; i++) viewer[j++] = v[i];
      }
    }
  }

  /**
   * Vertex coordinates are in screen units (left handed), no further transformation required.
   * Z from 0 (very) far to 1 near.
   */
  public void rasterization() {
//    drawVertex();
//    drawEdge();
    drawFace();
  }

  public void drawVertex() {
    IntStream.range(0, vertex.length / 3).boxed()
        .sorted(Comparator.comparingDouble(i -> vertex[i * 3 + 2])).forEach(i -> {
      double x = vertex[i * 3];
      double y = vertex[i * 3 + 1];
      double z = vertex[i * 3 + 2];
      if (z < 0 || z > 1) throw new IllegalArgumentException();
      plot((int) Math.round(x), (int) Math.round(y), (int) (z * 255) * 0x010101);
    });
  }

  public void plot(int x, int y, int rgb) {
    imageRaster[y * imageWidth + x] = rgb;
  }

  public void drawLine(double x1, double y1, double x2, double y2, int rgb) {
    // https://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm modified for double coordinates
    double dx = Math.abs(x2 - x1);
    int sx = x1 < x2 ? 1 : -1;
    double dy = Math.abs(y2 - y1);
    int sy = y1 < y2 ? 1 : -1;
    double error = dx - dy;
    int x = (int) x1;
    int y = (int) y1;
    int x0 = (int) x2;
    int y0 = (int) y2;
    while (true) {
      plot(x, y, rgb);
      double e2 = 2 * error;
      if (e2 >= -dy) {
        if (x == x0) break;
        error -= dy;
        x += sx;
      }
      if (e2 <= dx) {
        if (y == y0) break;
        error += dx;
        y += sy;
      }
    }
  }

  public void drawEdge() {
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
      double n = ax * by - ay * bx; // normal of the triangle
      if (n > 0) continue; // left
      drawLine(x0, y0, x1, y1, (int) ((z0 + z1) * 127) * 0x010101);
      drawLine(x1, y1, x2, y2, (int) ((z1 + z2) * 127) * 0x010101);
      drawLine(x2, y2, x0, y0, (int) ((z2 + z0) * 127) * 0x010101);
    }
  }

  /**
   * @return true if the point p is inside the triangle abc
   */
  public static boolean barycentric(double px, double py,
      double ax, double ay, double bx, double by, double cx, double cy, double[] r) {
    if (r[3] == 0) r[3] = 1 / ((ax - cx) * (by - cy) - (bx - cx) * (ay - cy));
    r[0] = ((px - cx) * (by - cy) - (py - cy) * (bx - cx)) * r[3];
    r[1] = ((py - cy) * (ax - cx) - (px - cx) * (ay - cy)) * r[3];
    r[2] = 1.0 - r[0] - r[1];
    return r[0] >= 0 && r[1] >= 0 && r[2] >= 0;
  }

  public static double barycentricValue(double a, double b, double c, double[] r) {
    return a * r[0] + b * r[1] + c * r[2];
  }

  public static void barycentricValue(double[] a, int ia, double[] b, int ib, double[] c, int ic,
      double[] r, double[] d) {
    for (int i = 0; i < d.length; i++) d[i] = a[ia++] * r[0] + b[ib++] * r[1] + c[ic++] * r[2];
  }

  public static double length(double[] vector, int i, int size) {
    double length = 0;
    for (int j = 0; j < size; j++) {
      double v = vector[i++];
      length += v * v;
    }
    return Math.sqrt(length);
  }

  public static void normalize(double[] vector) {
    double length = length(vector, 0, vector.length);
    if (length == 0) return;
    for (int i = 0; i < vector.length; i++) vector[i] /= length;
  }

  public static double dotProduct(double[] a, int ia, double[] b, int ib) {
    return a[ia] * b[ib] + a[ia + 1] * b[ib + 1] + a[ia + 2] * b[ib + 2];
  }

  public void drawFace() {
    double[] light = {-5, 3, 5}; // DisplayStand
    double[] barycentricNormal = new double[3];
    normalize(light);
    double ambient = 0.4;
    double diffuse = 0.6;
    double specular = 0.8;
    double shininess = 100;

    int imageMaxY = imageHeight - 1;
    int imageMaxX = imageWidth - 1;
    for (int i = 0; i < face.length; i += 9) {
      // FIXME: make a method for the back-face culling boilerplate
      int v0 = face[i] * 3;
      int n0 = face[i + 1] * 3;
      int t0 = face[i + 2] * 2;
      int v1 = face[i + 3] * 3;
      int n1 = face[i + 4] * 3;
      int t1 = face[i + 5] * 2;
      int v2 = face[i + 6] * 3;
      int n2 = face[i + 7] * 3;
      int t2 = face[i + 8] * 2;
//      double t0x = texture[t0];
//      double t0y = texture[t0 + 1];
//      double t1x = texture[t1];
//      double t1y = texture[t1 + 1];
//      double t2x = texture[t2];
//      double t2y = texture[t2 + 1];

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
      double nTr = ax * by - ay * bx; // normal of the triangle
      if (nTr > 0) continue; // left

      double[] r = new double[4];
      int minX = Math.max((int) Math.floor(Math.min(x0, Math.min(x1, x2))), 0);
      int maxX = Math.min((int) Math.ceil(Math.max(x0, Math.max(x1, x2))), imageMaxX);
      int minY = Math.max((int) Math.floor(Math.min(y0, Math.min(y1, y2))), 0);
      int maxY = Math.min((int) Math.ceil(Math.max(y0, Math.max(y1, y2))), imageMaxY);
      for (int y = minY; y <= maxY; y++) {
        for (int x = minX; x <= maxX; x++) {
          int xy = y * imageWidth + x;
          if (!barycentric(x, y, x0, y0, x1, y1, x2, y2, r)) continue;
          double z = barycentricValue(z0, z1, z2, r);
          if (zbuffer[xy] > z) continue;
          zbuffer[xy] = z;
          imageRaster[xy] = (int) (z * 255) * 0x010101;
          barycentricValue(normal, n0, normal, n1, normal, n2, r, barycentricNormal);
          normalize(barycentricNormal);
          double dotLN = Math.max(0, dotProduct(barycentricNormal, 0, light, 0));
          double[] R = {
              2 * dotLN * barycentricNormal[0] - light[0],
              2 * dotLN * barycentricNormal[1] - light[1],
              2 * dotLN * barycentricNormal[2] - light[2]
          };
          normalize(R);
          int textureColor = 0xCC9999;
          if (textureHeight > 0) {
            double txy0 = barycentricValue(texture[t0], texture[t1], texture[t2], r);
            double txy1 = barycentricValue(texture[t0 + 1], texture[t1 + 1], texture[t2 + 1], r);
            //double[] txy = barycentricValue(texture, t0, texture, t1, texture, t2, r, 2);
            int tx = Math.min(Math.max(0, (int) (txy0 * textureWidth)), textureWidth - 1);
            int ty = Math.min(Math.max(0, (int) (txy1 * textureHeight)), textureHeight - 1);
            textureColor = textureRaster[(textureHeight - 1 - ty) * textureWidth + tx];
          }
          double dotRV = Math.max(0, dotProduct(R, 0, viewer, xy * 3));
          double v = ambient + diffuse * dotLN + specular * Math.pow(dotRV, shininess);
          int rcol = (textureColor >> 16 & 0xFF);
          int gcol = (textureColor >> 8 & 0xFF);
          int bcol = (textureColor & 0xFF);
          if (v > 1) {
            int vcol = (int) ((v - 1) * 0xFF);
            rcol = Math.min(rcol + vcol, 0xFF);
            gcol = Math.min(gcol + vcol, 0xFF);
            bcol = Math.min(bcol + vcol, 0xFF);
          } else {
            rcol *= v;
            gcol *= v;
            bcol *= v;
          }
          imageRaster[xy] = rcol << 16 | gcol << 8 | bcol;
        }
      }
    }
  }

  /**
   * In place rotation, angle 0-1, axis 0x,1y,2z
   */
  public static void rotate(double[] vertex, double angle, int axis) {
    // TODO: do not optimize rotations until phong texture fully optimized
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

  public void cls() {
    Arrays.fill(imageRaster, 0);
    Arrays.fill(zbuffer, 0);
  }

  public int[] run(int[] textureRaster, int textureWidth, int textureHeight,
      Obj obj, double year) {
    this.textureRaster = textureRaster;
    this.textureWidth = textureWidth;
    this.textureHeight = textureHeight;
    this.face = obj.face;
    this.texture = obj.texture == null ? new double[2] : obj.texture;
    this.vertex = Arrays.copyOf(obj.vertex, obj.vertex.length);
    this.normal = Arrays.copyOf(obj.normal, obj.normal.length);
    double xyzmax = 0;
    for (int i = 0; i < vertex.length; i += 3) xyzmax = Math.max(xyzmax, length(vertex, i, 3));
    xyzmax *= 1.2;
    for (int i = 0; i < vertex.length; i++) vertex[i] /= xyzmax;
    int w2 = imageWidth / 2;
    int h2 = imageHeight / 2;

    rotate(vertex, year * 9, 1);
    rotate(vertex, -23.44 / 360, 0);
    rotate(vertex, year, 1);
    rotate(normal, year * 9, 1);
    rotate(normal, -23.44 / 360, 0);
    rotate(normal, year, 1);
    for (int i = 0; i < vertex.length / 3; i++) {
      double x = vertex[i * 3];
      double y = vertex[i * 3 + 1];
      double z = vertex[i * 3 + 2];
      double d = h2 * 5 / (5 - z);
      vertex[i * 3] = w2 + x * d;
      vertex[i * 3 + 1] = h2 - y * d; // left
      vertex[i * 3 + 2] = z / 2 + 0.5;
    };
    rasterization();
    return imageRaster;
  }

}
