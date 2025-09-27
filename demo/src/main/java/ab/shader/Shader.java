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
import java.util.function.IntSupplier;

public class Shader {

  public static final double FOCAL_LENGTH = 50;
  public static final double FILM_HEIGHT = 24;

  public final int[] imageRaster;
  public final int imageWidth;
  public final int imageHeight;
  public final double[] zbuffer;
  public final double[] light;
  public final double[] viewer;
  public int[] textureRaster;
  public int textureWidth;
  public int textureHeight;
  public int textureColor = 0xCC9999;

  public double ambient = 0.4;
  public double diffuse = 0.6;
  public double specular = 0.8;
  public double shininess = 100;

  public int[] face; // current obj
  public double[] vertex;
  public double[] normal;
  public double[] texture;
  public int fv0;
  public int fn0;
  public int ft0;
  public int fv1;
  public int fn1;
  public int ft1;
  public int fv2;
  public int fn2;
  public int ft2;
  public double v0x;
  public double v0y;
  public double v0z;
  public double v1x;
  public double v1y;
  public double v1z;
  public double v2x;
  public double v2y;
  public double v2z;
  public final double[] gouraudIllumination = new double[3];
  public boolean gouraudIlluminationCreated;
  public final double[] barycentricCoordinates = new double[4];
  public int imageRasterXY;

  Runnable visibleFaceMethod;
  IntSupplier visiblePixelMethod;

  public Shader(int imageWidth, int imageHeight) {
    this.imageRaster = new int[imageWidth * imageHeight];
    this.imageWidth = imageWidth;
    this.imageHeight = imageHeight;
    this.zbuffer = new double[imageWidth * imageHeight];
    this.light = new double[3];
    this.viewer = new double[imageWidth * imageHeight * 3];
    double[] v = new double[3];
    final double viewerZ = FOCAL_LENGTH / FILM_HEIGHT * imageHeight;
    double imageHeight2 = imageHeight / 2.0 - 0.5;
    double imageWidth2 = imageWidth / 2.0 - 0.5;
    for (int y = 0, j = 0; y < imageHeight; y++) {
      for (int x = 0; x < imageWidth; x++) {
        v[0] = imageWidth2 - x;
        v[1] = y - imageHeight2;
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
    visibleFaceMethod = this::drawVertex;
    visibleFaceMethod = this::drawEdge;
    visibleFaceMethod = this::drawFace;
    visiblePixelMethod = this::pixelZbuffer;
    visiblePixelMethod = this::getTextureColor;
    visiblePixelMethod = this::lambertTexture;
    visiblePixelMethod = this::gouraudShading;
    visiblePixelMethod = this::phongShading;
    iterateVisibleFace();
  }

  public int phongShading() {
    final double[] barycentricNormal = new double[3];
    barycentricValue(normal, fn0, normal, fn1, normal, fn2, barycentricCoordinates, barycentricNormal);
    normalize(barycentricNormal);
    int textureColor = getTextureColor();
    double illumination = phongReflection(light, 0, barycentricNormal, 0, viewer, imageRasterXY * 3);
    return colorBrightness(textureColor, illumination);
  }

  public double phongReflection(double[] light, int il, double[] normal, int in, double[] viewer, int iv) {
    double dotLN = Math.max(0, dotProduct(normal, in, light, il));
    double[] R = {
        2 * dotLN * normal[in] - light[il],
        2 * dotLN * normal[in + 1] - light[il + 1],
        2 * dotLN * normal[in + 2] - light[il + 2]
    };
    normalize(R);
    double dotRV = Math.max(0, dotProduct(R, 0, viewer, iv));
    return ambient + diffuse * dotLN + specular * Math.pow(dotRV, shininess);
  }

  public int gouraudShading() {
    double illumination = barycentricValue(
        gouraudIllumination[0], gouraudIllumination[1], gouraudIllumination[2], barycentricCoordinates);
    gouraudIlluminationCreated = false;
    int textureColor = getTextureColor();
    return colorBrightness(textureColor, illumination);
  }

  public int lambertTexture() {
    // flat shading requires face normals propagated in obj
    final double[] normal = new double[3];
    for (int i = 0; i < 3; i++) {
      normal[i] += this.normal[fn0 + i];
      normal[i] += this.normal[fn1 + i];
      normal[i] += this.normal[fn2 + i];
    }
    normalize(normal);
    double dotLN = Math.max(0, dotProduct(normal, 0, light, 0));
    int textureColor = getTextureColor();
    double v = ambient + diffuse * dotLN;
    return colorBrightness(textureColor, v);
  }

  /**
   * brightness 0-2+, brightness=1 returns the same color
   */
  public int colorBrightness(int color, double brightness) {
    int rcol = (color >> 16 & 0xFF);
    int gcol = (color >> 8 & 0xFF);
    int bcol = (color & 0xFF);
    if (brightness > 1) {
      int vcol = (int) ((brightness - 1) * 0xFF);
      rcol = Math.min(rcol + vcol, 0xFF);
      gcol = Math.min(gcol + vcol, 0xFF);
      bcol = Math.min(bcol + vcol, 0xFF);
    } else {
      rcol *= brightness;
      gcol *= brightness;
      bcol *= brightness;
    }
    return rcol << 16 | gcol << 8 | bcol;
  }

  public int pixelZbuffer() {
    double z = barycentricValue(v0z, v1z, v2z, barycentricCoordinates);
    return (int) (z * 0xFF) * 0x010101;
  }

  public void drawVertex() {
    if (v0z < 0 || v0z > 1 || v1z < 0 || v1z > 1 || v2z < 0 || v2z > 1) throw new IllegalArgumentException();
    plot((int) Math.round(v0x), (int) Math.round(v0y), (int) (v0z * 255) * 0x010101);
    plot((int) Math.round(v1x), (int) Math.round(v1y), (int) (v1z * 255) * 0x010101);
    plot((int) Math.round(v2x), (int) Math.round(v2y), (int) (v2z * 255) * 0x010101);
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

  public void iterateVisibleFace() {
    for (int i = 0; i < face.length;) {
      fv0 = face[i++] * 3;
      fn0 = face[i++] * 3;
      ft0 = face[i++] * 2;
      fv1 = face[i++] * 3;
      fn1 = face[i++] * 3;
      ft1 = face[i++] * 2;
      fv2 = face[i++] * 3;
      fn2 = face[i++] * 3;
      ft2 = face[i++] * 2;
      v0x = vertex[fv0];
      v0y = vertex[fv0 + 1];
      v0z = vertex[fv0 + 2];
      v1x = vertex[fv1];
      v1y = vertex[fv1 + 1];
      v1z = vertex[fv1 + 2];
      v2x = vertex[fv2];
      v2y = vertex[fv2 + 1];
      v2z = vertex[fv2 + 2];
      double ax = v1x - v0x;
      double ay = v1y - v0y;
      double bx = v2x - v0x;
      double by = v2y - v0y;
      double n = ax * by - ay * bx; // normal of the triangle
      if (n > 0) continue; // left
      visibleFaceMethod.run();
    }
  }

  public void drawEdge() {
    drawLine(v0x, v0y, v1x, v1y, (int) ((v0z + v1z) * 127) * 0x010101);
    drawLine(v1x, v1y, v2x, v2y, (int) ((v1z + v2z) * 127) * 0x010101);
    drawLine(v2x, v2y, v0x, v0y, (int) ((v2z + v0z) * 127) * 0x010101);
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

  public int getTextureColor() {
    if (textureHeight == 0) return this.textureColor;
    double txy0 = barycentricValue(texture[ft0], texture[ft1], texture[ft2], barycentricCoordinates);
    double txy1 = barycentricValue(texture[ft0 + 1], texture[ft1 + 1], texture[ft2 + 1], barycentricCoordinates);
    //double[] txy = barycentricValue(texture, t0, texture, t1, texture, t2, r, 2);
    int tx = Math.min(Math.max(0, (int) (txy0 * textureWidth)), textureWidth - 1);
    int ty = Math.min(Math.max(0, (int) (txy1 * textureHeight)), textureHeight - 1);
    return textureRaster[(textureHeight - 1 - ty) * textureWidth + tx];
  }

  public void createGouraudIllumination() {
    if (gouraudIlluminationCreated) return;
    final double viewerZ = FOCAL_LENGTH / FILM_HEIGHT * imageHeight;
    int[] vi = {fn0, fn1, fn2};
    double[] xy = {v0x, v0y, v1x, v1y, v2x, v2y};
    double[] viewer = new double[3];
    for (int i = 0; i < 3; i++) {
      viewer[0] = imageWidth / 2.0 - xy[i * 2];
      viewer[1] = xy[i * 2 + 1] - imageHeight / 2.0;
      viewer[2] = viewerZ;
      normalize(viewer);
      double v = phongReflection(light, 0, normal, vi[i], viewer, 0);
      gouraudIllumination[i] = v;
    }
    gouraudIlluminationCreated = true;
  }

  public void drawFace() {
    barycentricCoordinates[3] = 0; // clear cache
    int minX = Math.max((int) Math.floor(Math.min(v0x, Math.min(v1x, v2x))), 0);
    int maxX = Math.min((int) Math.ceil(Math.max(v0x, Math.max(v1x, v2x))), imageWidth - 1);
    int minY = Math.max((int) Math.floor(Math.min(v0y, Math.min(v1y, v2y))), 0);
    int maxY = Math.min((int) Math.ceil(Math.max(v0y, Math.max(v1y, v2y))), imageHeight - 1);
    boolean createGouraudIllumination = true;

    for (int y = minY; y <= maxY; y++) {
      for (int x = minX; x <= maxX; x++) {
        imageRasterXY = y * imageWidth + x;
        if (!barycentric(x, y, v0x, v0y, v1x, v1y, v2x, v2y, barycentricCoordinates)) continue;
        double z = barycentricValue(v0z, v1z, v2z, barycentricCoordinates);
        if (zbuffer[imageRasterXY] > z) continue;
        zbuffer[imageRasterXY] = z;
        if (createGouraudIllumination) {
          createGouraudIllumination();
          createGouraudIllumination = false;
        }
        imageRaster[imageRasterXY] = visiblePixelMethod.getAsInt();
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
    light[0] = -5; // DisplayStand
    light[1] = 3;
    light[2] = 5;
    normalize(light);
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
