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
import ab.nbsnk.nodes.Col;
import ab.nbsnk.nodes.Pnt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class Shader {

  public static final double FOCAL_LENGTH = 50;
  public static final double FILM_HEIGHT = 24;

  public boolean enableZbuffer = true;
  public boolean enableTexture = true;
  public enum Illumination { NONE, LAMBERT, GOURAUD, PHONG, BLINNPHONG }
  public Illumination enableIllumination = Illumination.PHONG; // 0 None, 1 Lambert, 2 Gouraud, 3 Phong
  public int enableDimension = 2; // 0 point cloud, 1 wire-frame, 2 polygon mesh
  public double nearClip = 0.1;
  public double farClip = 100.0; // javaFx camera defaults

  public static class Light { Col color; Pnt xyz; }
  public List<Light> lights;
  public Col[] lightColor;
  public Pnt[] lightPoint;

  public int[] imageRaster;
  public int imageWidth;
  public int imageHeight;
  public double focalLength; // in pixels
  public double[] zbuffer;
  public int[] textureRaster;
  public int textureWidth;
  public int textureHeight;
  public int[] bumpRaster;
  public int bumpWidth;
  public int bumpHeight;
  public int[] reflectionRaster;
  public int reflectionWidth;
  public int reflectionHeight;
  public double reflectionAlpha;
  public Matrix reflectionMatrix;

  public Col ambientColor = new Col();
  public Col diffuseColor = new Col(-1);
  public Col specularColor = new Col();
  public double specularPower = 32; // shininess

  public int[] face; // current obj
  public Pnt[] vertex;
  public Pnt[] vertexTrue; // because vertex is a projection
  public Pnt[] normal;
  public Pnt[] tangentBitangent;
  public double[] texture;
  public int iface;
  public int fv0;
  public int fn0;
  public int ft0;
  public int fv1;
  public int fn1;
  public int ft1;
  public int fv2;
  public int fn2;
  public int ft2;
  public Pnt v0;
  public Pnt v1;
  public Pnt v2;
  public double ttx;
  public double tty;
  public Col[] gouraudIllumination = new Col[6];
  public double[] barycentricCoordinates = new double[4];
  public int imageRasterX;
  public int imageRasterY;
  public int imageRasterXY;

  Runnable visibleFaceMethod;
  Supplier<Col[]> visiblePixelMethod;

  public static Pnt viewerVector(double x, double y, int imageWidth, int imageHeight, double focalLength) {
    return new Pnt(
        imageWidth / 2.0 - 0.5 - x,
        imageHeight / 2.0 - 0.5 - y,
        focalLength
    ).normalize();
  }

  /**
   * Vertex coordinates are in screen units (y up), no further transformation required.
   * Z from 0 (very) far to 1 near.
   */
  public void rasterization() {
    switch (enableDimension) {
      case 1: visibleFaceMethod = this::drawEdge; break;
      case 2: visibleFaceMethod = this::drawFace; break;
      default: visibleFaceMethod = this::drawVertex;
    }
    switch (enableIllumination) {
      case LAMBERT: visiblePixelMethod = this::lambertTexture; break;
      case GOURAUD: visiblePixelMethod = this::gouraudShading; break;
      case PHONG: visiblePixelMethod = this::phongShading; break;
      default: visiblePixelMethod = this::pixelZbuffer;
    }
    if (!enableTexture) textureHeight = 0;
    //visiblePixelMethod = this::getTextureColor;
    iterateVisibleFace();
  }

  public Col[] phongShading() {
    Pnt barycentricNormal = Pnt.barycentric(
        normal[fn0],
        normal[fn1],
        normal[fn2],
        barycentricCoordinates
    ).normalize(); // yes, it must be normalized
    if (bumpHeight > 0) {
      int btx = Math.min(Math.max(0, (int) (ttx * bumpWidth)), bumpWidth - 1); // bump texture x
      int bty = Math.min(Math.max(0, (int) (tty * bumpHeight)), bumpHeight - 1);
      Col bumpCol = new Col(bumpRaster[(bumpHeight - 1 - bty) * bumpWidth + btx]);
      double vt = bumpCol.r - 0.5;
      double vb = bumpCol.g - 0.5;
      double vn = 2 * (bumpCol.b - 0.5); // TODO: 2025-10-25 confirm that 2* is not a bug of javafx
      int i = iface * 2;
      Pnt t = tangentBitangent[i++];
      Pnt b = tangentBitangent[i++];
      barycentricNormal = new Pnt().add(t, vt).add(b, vb).add(barycentricNormal, vn).normalize();
    }
    Pnt barycentricPosition = Pnt.barycentric(
        vertexTrue[fv0],
        vertexTrue[fv1],
        vertexTrue[fv2],
        barycentricCoordinates);
    Pnt viewer = viewerVector(imageRasterX, imageRasterY, imageWidth, imageHeight, focalLength);
    Col[] diffuseSpecular = illuminationRgb(barycentricPosition, barycentricNormal, viewer);
    diffuseSpecular[0].mul(getTextureColor());
    if (reflectionAlpha > 0) {
      double dotVN = viewer.dot(barycentricNormal);
      Pnt R = new Pnt(
          2 * dotVN * barycentricNormal.x - viewer.x,
          2 * dotVN * barycentricNormal.y - viewer.y,
          2 * dotVN * barycentricNormal.z - viewer.z
      ).normalize();
      Matrix r1 = reflectionMatrix.times(R.toMatrix(0));
      int reflectionY = Math.min(Math.max(0, (int) Math.round((0.5 - Math.asin(r1.get(1, 0)) / Math.PI) * reflectionHeight)), reflectionHeight - 1);
      int reflectionX = (int) Math.round((1 - Math.atan2(r1.get(0, 0), r1.get(2, 0)) / Math.PI) / 2 * reflectionWidth);
      reflectionX = (reflectionX % reflectionWidth + reflectionWidth) % reflectionWidth;
      // retain diffuse alpha
      diffuseSpecular[0] = diffuseSpecular[0].clone().add(diffuseSpecular[0], -reflectionAlpha)
          .add(new Col(reflectionRaster[reflectionY * reflectionWidth + reflectionX]), reflectionAlpha);
    }
    return diffuseSpecular;
  }

  public Col[] illuminationRgb(Pnt xyz, Pnt normal, Pnt viewer) {
    Col diffuseRgb = new Col().opaque();
    Col specularRgb = new Col().opaque();
    for (int i = 0; i < lightPoint.length; i++) {
      Pnt light = lightPoint[i].clone().add(xyz, -1).normalize();
      double dotLN = light.dot(normal);
      Pnt R = new Pnt(
          2 * dotLN * normal.x - light.x,
          2 * dotLN * normal.y - light.y,
          2 * dotLN * normal.z - light.z
      ).normalize();
      double dotRV = R.dot(viewer);
      //double illumination = ambient + diffuse * dotLN + specular * Math.pow(dotRV, specularPower);
      diffuseRgb.add(lightColor[i], Math.max(0, dotLN)); // diffuse
      specularRgb.add(lightColor[i], Math.pow(Math.max(0, dotRV), specularPower)); // specular
    }
    // full opaque Col.add() in this method
    return new Col[]{diffuseRgb.add(ambientColor, 1).mul(diffuseColor), specularRgb.mul(specularColor)};
  }

  public Col[] gouraudShading() {
//    double illumination = barycentricValue(
//        gouraudIllumination[0], gouraudIllumination[1], gouraudIllumination[2], barycentricCoordinates);
//    Col col = new Col(illumination, illumination, illumination, 1);
    Col diffuse = Col.barycentric(gouraudIllumination[0], gouraudIllumination[2], gouraudIllumination[4], barycentricCoordinates);
    Col specular = Col.barycentric(gouraudIllumination[1], gouraudIllumination[3], gouraudIllumination[5], barycentricCoordinates);
    return new Col[]{getTextureColor().mul(diffuse), specular};
  }

  public Col[] lambertTexture() {
//    double illumination = gouraudIllumination[0];
//    Col col = new Col(illumination, illumination, illumination, 1);
    return new Col[]{getTextureColor().mul(gouraudIllumination[0]), gouraudIllumination[1]};
  }

  public Col[] pixelZbuffer() {
    if (textureHeight > 0) return new Col[]{getTextureColor(), new Col()};
    double z = barycentricValue(v0.z, v1.z, v2.z, barycentricCoordinates);
    return new Col[]{new Col((int) (z * 0xFF) * 0x010101 + 0xFF000000), new Col()};
  }

  public void drawVertex() {
    if (v0.z < 0 || v0.z > 1 || v1.z < 0 || v1.z > 1 || v2.z < 0 || v2.z > 1) throw new IllegalArgumentException();
    plot((int) Math.round(v0.x), (int) Math.round(v0.y), (int) (v0.z * 255) * 0x010101);
    plot((int) Math.round(v1.x), (int) Math.round(v1.y), (int) (v1.z * 255) * 0x010101);
    plot((int) Math.round(v2.x), (int) Math.round(v2.y), (int) (v2.z * 255) * 0x010101);
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

  // Nbsnk. made in NZ

  public void iterateVisibleFace() {
    for (int i = 0; i < face.length;) {
      iface = i / 9;
      fv0 = face[i++];
      fn0 = face[i++];
      ft0 = face[i++] * 2;
      fv1 = face[i++];
      fn1 = face[i++];
      ft1 = face[i++] * 2;
      fv2 = face[i++];
      fn2 = face[i++];
      ft2 = face[i++] * 2;
      v0 = vertex[fv0];
      v1 = vertex[fv1];
      v2 = vertex[fv2];
      Pnt va = v1.clone().add(v0, -1);
      Pnt vb = v2.clone().add(v0, -1);
      double n = va.x * vb.y - va.y * vb.x; // normal of the triangle
      if (n < 0) continue;
      visibleFaceMethod.run();
    }
  }

  public void drawEdge() {
    drawLine(v0.x, v0.y, v1.x, v1.y, (int) ((v0.z + v1.z) * 127) * 0x010101);
    drawLine(v1.x, v1.y, v2.x, v2.y, (int) ((v1.z + v2.z) * 127) * 0x010101);
    drawLine(v2.x, v2.y, v0.x, v0.y, (int) ((v2.z + v0.z) * 127) * 0x010101);
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

  public Col getTextureColor() {
    if (textureHeight == 0) return new Col(1, 1, 1, 1);
    int tx = Math.min(Math.max(0, (int) (ttx * textureWidth)), textureWidth - 1);
    int ty = Math.min(Math.max(0, (int) (tty * textureHeight)), textureHeight - 1);
    return new Col(textureRaster[(textureHeight - 1 - ty) * textureWidth + tx]);
  }

  public void createLambertIllumination() {
    //gouraudIllumination[0] = phongReflection(light, 0, normal, fn0, new double[3], 0);
    Pnt vertex = new Pnt()
        .add(vertexTrue[fv0], 1.0 / 3)
        .add(vertexTrue[fv1], 1.0 / 3)
        .add(vertexTrue[fv2], 1.0 / 3);
    Pnt vs = new Pnt().add(v0, 1.0 / 3).add(v1, 1.0 / 3).add(v2, 1.0 / 3);
    Pnt viewer = viewerVector(vs.x, vs.y, imageWidth, imageHeight, focalLength);
    Col[] illuminationRgb = illuminationRgb(vertex, normal[fn0], viewer);
    gouraudIllumination[0] = illuminationRgb[0];
    gouraudIllumination[1] = illuminationRgb[1];
  }

  public void createGouraudIllumination() {
    int[] fni = {fn0, fn1, fn2};
    int[] fvi = {fv0, fv1, fv2};
    Pnt[] xy = {v0, v1, v2};
    for (int i = 0; i < 3; i++) {
      Pnt viewer = viewerVector(xy[i].x, xy[i].y, imageWidth, imageHeight, focalLength);
      //double v = phongReflection(light, 0, normal, fni[i], viewer, 0);
      int fv = fvi[i];
      Col[] illuminationRgb = illuminationRgb(vertexTrue[fv], normal[fni[i]], viewer);
      gouraudIllumination[2 * i] = illuminationRgb[0];
      gouraudIllumination[2 * i + 1] = illuminationRgb[1];
    }
  }

  public void drawFace() {
    if (v0.z < 0 && v1.z < 0 && v2.z < 0) return;
    double nearClip = 1 - this.nearClip / this.farClip;
    if (v0.z > nearClip || v1.z > nearClip || v2.z > nearClip) return;
    barycentricCoordinates[3] = 0; // clear cache
    int minX = Math.max((int) Math.floor(Math.min(v0.x, Math.min(v1.x, v2.x))), 0);
    int maxX = Math.min((int) Math.ceil(Math.max(v0.x, Math.max(v1.x, v2.x))), imageWidth - 1);
    int minY = Math.max((int) Math.floor(Math.min(v0.y, Math.min(v1.y, v2.y))), 0);
    int maxY = Math.min((int) Math.ceil(Math.max(v0.y, Math.max(v1.y, v2.y))), imageHeight - 1);
    boolean createIllumination = true;

    for (imageRasterY = minY; imageRasterY <= maxY; imageRasterY++) {
      for (imageRasterX = minX; imageRasterX <= maxX; imageRasterX++) {
        imageRasterXY = (imageHeight - 1 - imageRasterY) * imageWidth + imageRasterX;
        if (!barycentric(imageRasterX, imageRasterY, v0.x, v0.y, v1.x, v1.y, v2.x, v2.y, barycentricCoordinates)) continue;
        double z = barycentricValue(v0.z, v1.z, v2.z, barycentricCoordinates);
        if (zbuffer[imageRasterXY] > z) continue;
        zbuffer[imageRasterXY] = z;
        if (createIllumination) {
          if (enableIllumination == Illumination.LAMBERT) createLambertIllumination();
          if (enableIllumination == Illumination.GOURAUD) createGouraudIllumination();
          createIllumination = false;
        }
        // barycentric perspective correction, texture and normals
        barycentricCoordinates[0] *= (1 - z) / (1 - v0.z);
        barycentricCoordinates[1] *= (1 - z) / (1 - v1.z);
        barycentricCoordinates[2] *= (1 - z) / (1 - v2.z);
        ttx = barycentricValue(texture[ft0], texture[ft1], texture[ft2], barycentricCoordinates);
        tty = barycentricValue(texture[ft0 + 1], texture[ft1 + 1], texture[ft2 + 1], barycentricCoordinates);
        Col[] col = visiblePixelMethod.get();
        if (col[0].a < 1) {
          // full opaque add
          col[0] = new Col().opaque().add(new Col(imageRaster[imageRasterXY]), 1 - col[0].a).add(col[0], col[0].a);
        }
        imageRaster[imageRasterXY] = col[0].add(col[1], 1).argb();
      }
    }
  }

  // cls(), addLight(), add()
  public void cls(int width, int height) {
    imageWidth = width;
    imageHeight = height;
    focalLength = FOCAL_LENGTH / FILM_HEIGHT * imageHeight;
    if (imageRaster == null || imageRaster.length != width * height) imageRaster = new int[width * height];
    if (zbuffer == null || zbuffer.length != width * height) zbuffer = new double[width * height];
    Arrays.fill(imageRaster, 0);
    Arrays.fill(zbuffer, 0);
    lights = new ArrayList<>();
    lightColor = null;
    lightPoint = null;
  }

  public void addLight(Pnt xyz, Col color) {
    if (color.a != 1) throw new IllegalStateException();
    Light light = new Light();
    light.xyz = xyz.clone();
    light.color = color.clone();
    lights.add(light);
  }

  public void add(Obj obj, Matrix tm) {
    if (lights != null) {
      int size = lights.size();
      lightColor = new Col[size];
      lightPoint = new Pnt[size];
      for (int i = 0; i < size; i++) {
        Light light = lights.get(i);
        lightColor[i] = light.color;
        lightPoint[i] = light.xyz;
      }
      lights = null;
    }
    // TODO: 2025-09-30 review this method, it's a mess
    this.face = obj.face;
    this.texture = obj.texture == null ? new double[2] : obj.texture;
    this.vertex = new Pnt[obj.vertex.length / 3];
    this.vertexTrue = new Pnt[obj.vertex.length / 3];
    this.normal = new Pnt[obj.normal.length / 3];
//    rotate(vertex, transformation[3], 2);
//    rotate(normal, transformation[3], 2);
    for (int i = 0; i < normal.length; i++) {
      Matrix xyz = tm.times(new Pnt(obj.normal, i * 3).toMatrix(0));
      normal[i] = Pnt.fromMatrix(xyz);
    }
    tangentBitangent = tangentBitangent == null ? new Pnt[0] : Arrays.copyOf(tangentBitangent, tangentBitangent.length);
    for (int i = 0; i < tangentBitangent.length; i++) {
      Matrix xyz = tm.times(tangentBitangent[i].toMatrix(0));
      tangentBitangent[i] = Pnt.fromMatrix(xyz);
    }
    double w2 = imageWidth / 2.0 - 0.5;
    double h2 = imageHeight / 2.0 - 0.5;
    for (int i = 0; i < vertex.length; i++) {
      Matrix xyz = tm.times(new Pnt(obj.vertex, i * 3).toMatrix(1));
      Pnt vt = Pnt.fromMatrix(xyz);
      vertexTrue[i] = vt;
      vertex[i] = new Pnt(w2 - focalLength * vt.x / vt.z, h2 - focalLength * vt.y / vt.z, (farClip + vt.z) / farClip);
    };
    rasterization();
  }

  public static Pnt[] computeTangentBitangent(
      double x1,double y1,double z1, double u1,double v1,
      double x2,double y2,double z2, double u2,double v2,
      double x3,double y3,double z3, double u3,double v3) {
    // edge vectors
    double ex1 = x2 - x1, ey1 = y2 - y1, ez1 = z2 - z1;
    double ex2 = x3 - x1, ey2 = y3 - y1, ez2 = z3 - z1;

    // uv deltas
    double du1 = u2 - u1, dv1 = v2 - v1;
    double du2 = u3 - u1, dv2 = v3 - v1;

    double f = 1.0 / (du1 * dv2 - du2 * dv1);

    // T = f * ( dv2 * edge1 - dv1 * edge2 )
    double tx = f * (dv2 * ex1 - dv1 * ex2);
    double ty = f * (dv2 * ey1 - dv1 * ey2);
    double tz = f * (dv2 * ez1 - dv1 * ez2);

    // B = f * (-du2 * edge1 + du1 * edge2)
    double bx = f * (-du2 * ex1 + du1 * ex2);
    double by = f * (-du2 * ey1 + du1 * ey2);
    double bz = f * (-du2 * ez1 + du1 * ez2);

    return new Pnt[]{new Pnt(tx, ty, tz), new Pnt(bx, by, bz)};
  }

  public static Pnt[] computeTangentBitangent(Obj obj) {
    int[] face = obj.face;
    double[] vertex = obj.vertex;
    double[] texture = obj.texture;
    Pnt[] tangentBitangent = new Pnt[2 * face.length / 9];
    for (int i = 0, tbi = 0; i < face.length; tbi += 2) {
      int fv0 = face[i++] * 3;
      int fn0 = face[i++] * 3;
      int ft0 = face[i++] * 2;
      int fv1 = face[i++] * 3;
      int fn1 = face[i++] * 3;
      int ft1 = face[i++] * 2;
      int fv2 = face[i++] * 3;
      int fn2 = face[i++] * 3;
      int ft2 = face[i++] * 2;
      double v0x = vertex[fv0];
      double v0y = vertex[fv0 + 1];
      double v0z = vertex[fv0 + 2];
      double v1x = vertex[fv1];
      double v1y = vertex[fv1 + 1];
      double v1z = vertex[fv1 + 2];
      double v2x = vertex[fv2];
      double v2y = vertex[fv2 + 1];
      double v2z = vertex[fv2 + 2];
      double t0u = texture[ft0];
      double t0v = texture[ft0 + 1];
      double t1u = texture[ft1];
      double t1v = texture[ft1 + 1];
      double t2u = texture[ft2];
      double t2v = texture[ft2 + 1];
      Pnt[] tb = Shader
          .computeTangentBitangent(v0x, v0y, v0z, t0u, t0v, v1x, v1y, v1z, t1u, t1v, v2x, v2y, v2z, t2u, t2v);
      System.arraycopy(tb, 0, tangentBitangent, tbi, 2);
    }
    return tangentBitangent;
  }

}
