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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Obj {
  public int[] face; // vertex, normal, texture
  public double[] vertex;
  public double[] normal;
  public double[] texture; // 0 <= (x, y) <= 1, Y-up, as in .obj
  public BufferedImage image;
  public String id;

  @Override
  protected Obj clone() {
    Obj obj = new Obj();
    obj.face = Arrays.copyOf(face, face.length);
    obj.vertex = Arrays.copyOf(vertex, vertex.length);
    obj.normal = Arrays.copyOf(normal, normal.length);
    obj.texture = Arrays.copyOf(texture, texture.length);
    obj.image = image;
    obj.id = id;
    return obj;
  }

  public static final Obj TETRAHEDRON = createTetrahedron();
  private static Obj createTetrahedron() {
    Obj obj = new Obj();
    obj.id = "tetrahedron";
    obj.vertex = new double[]{1, 1, 1, -1, -1, 1, -1, 1, -1, 1, -1, -1};
    int[] face = new int[]{0, 2, 1, 0, 1, 3, 0, 3, 2, 1, 2, 3};
    obj.face = new int[face.length * 3];
    for (int i = 0; i < face.length; i++) obj.face[i * 3] = face[i];
    return obj;
  }

  public static Obj load(byte[] fileBytes) {
    List<Double> v = new ArrayList<>();
    List<Double> vt = new ArrayList<>();
    List<Double> vn = new ArrayList<>();
    List<Integer> f = new ArrayList<>();
    for (String line : new String(fileBytes).split("\r?\n")) {
      String[] s = line.split("\\s+");
      switch (s[0]) {
        case "v": for (int i = 1; i < 4; i++) v.add(Double.parseDouble(s[i])); break;
        case "vt": for (int i = 1; i < 3; i++) vt.add(Double.parseDouble(s[i])); break;
        case "vn": for (int i = 1; i < 4; i++) vn.add(Double.parseDouble(s[i])); break;
        case "f":
          for (int t = 3; t < s.length; t++) // triangle
            for (int tv : new int[]{1, t - 1, t}) { // vertex
              String[] f123 = s[tv].split("/");
              for (int i = 0; i < 3; i++) f.add(f123.length > i && !f123[i].isEmpty() ? Integer.parseInt(f123[i]) : 0);
            }
          break;
        //default:
        //  System.out.println(s);
      }
    }
    double[] vertex = v.stream().mapToDouble(a -> a).toArray();
    double[] normal = vn.stream().mapToDouble(a -> a).toArray();
    double[] texture = vt.stream().mapToDouble(a -> a).toArray();
    int fs = f.size() / 3;
    int[] face = new int[fs * 3];
    for (int i = 0; i < face.length; i += 3) {
      face[i] = f.get(i) - 1; // v
      face[i + 1] = normal.length > 0 ? f.get(i + 2) - 1 : 0; // n
      face[i + 2] = texture.length > 0 ? f.get(i + 1) - 1 : 0; // t
    }
    Obj obj = new Obj();
    obj.id = UUID.nameUUIDFromBytes(fileBytes).toString();
    obj.face = face;
    obj.vertex = vertex;
    if (normal.length > 0) obj.normal = normal;
    if (texture.length > 0) obj.texture = texture;
    return obj;
  }

  public static byte[] save(Obj obj) {
    StringBuilder s = new StringBuilder();
    for (int v = 0; v < obj.vertex.length / 3; v++) s.append(String.format("v %f %f %f\n",
        obj.vertex[v * 3], obj.vertex[v * 3 + 1], obj.vertex[v * 3 + 2]));
    for (int f = 0; f < obj.face.length / 9; f++) {
      s.append(String.format("f %d %d %d\n", obj.face[f * 9] + 1, obj.face[f * 9 + 3] + 1, obj.face[f * 9 + 6] + 1));
    }
    return s.toString().getBytes();
  }

  private static double angle(double[] vertex, int a, int o, int b) {
    double ox = vertex[o++];
    double oy = vertex[o++];
    double oz = vertex[o];
    double ax = vertex[a++] - ox;
    double ay = vertex[a++] - oy;
    double az = vertex[a] - oz;
    double bx = vertex[b++] - ox;
    double by = vertex[b++] - oy;
    double bz = vertex[b] - oz;
    double dot = ax * bx + ay * by + az * bz; // dot
    double am = Math.sqrt(ax * ax + ay * ay + az * az); // magnitude
    double bm = Math.sqrt(bx * bx + by * by + bz * bz);
    return Math.acos(dot / (am * bm));
  }

  public static void flatNormal(Obj obj) {
    double[] faceNormal = new double[obj.face.length / 3];
    for (int i = 0; i < obj.face.length / 9; i++) {
      int v0 = obj.face[i * 9] * 3;
      int v1 = obj.face[i * 9 + 3] * 3;
      int v2 = obj.face[i * 9 + 6] * 3;
      double x0 = obj.vertex[v0];
      double y0 = obj.vertex[v0 + 1];
      double z0 = obj.vertex[v0 + 2];
      double x1 = obj.vertex[v1];
      double y1 = obj.vertex[v1 + 1];
      double z1 = obj.vertex[v1 + 2];
      double x2 = obj.vertex[v2];
      double y2 = obj.vertex[v2 + 1];
      double z2 = obj.vertex[v2 + 2];
      double ax = x1 - x0;
      double ay = y1 - y0;
      double az = z1 - z0;
      double bx = x2 - x0;
      double by = y2 - y0;
      double bz = z2 - z0;
      // cross product
      double cx = ay * bz - az * by;
      double cy = az * bx - ax * bz;
      double cz = ax * by - ay * bx;
      double l = Math.sqrt(cx * cx + cy * cy + cz * cz);
      faceNormal[i * 3] = cx / l;
      faceNormal[i * 3 + 1] = cy / l;
      faceNormal[i * 3 + 2] = cz / l;
      obj.face[i * 9 + 1] = i;
      obj.face[i * 9 + 4] = i;
      obj.face[i * 9 + 7] = i;
    }
    obj.normal = faceNormal;
  }

  public static void interpolateNormal(Obj obj) {
    double[] vertexNormal = new double[obj.vertex.length];
    for (int f = 0; f < obj.face.length / 9; f++) {
      for (int v = 0; v < 3; v++) {
        int va = obj.face[f * 9 + (v + 2) % 3 * 3] * 3;
        int vo = obj.face[f * 9 + v * 3] * 3;
        int vb = obj.face[f * 9 + (v + 1) % 3 * 3] * 3;
        double a = angle(obj.vertex, va, vo, vb);
        int n = obj.face[f * 9 + v * 3 + 1] * 3;
        obj.face[f * 9 + v * 3 + 1] = vo / 3;
        for (int i = 0; i < 3; i++) vertexNormal[vo++] += obj.normal[n++] * a;
      }
    }
    for (int i = 0; i < vertexNormal.length / 3; i++) {
      double x = vertexNormal[i * 3];
      double y = vertexNormal[i * 3 + 1];
      double z = vertexNormal[i * 3 + 2];
      double m = Math.sqrt(x * x + y * y + z * z); // magnitude
      vertexNormal[i * 3] /= m;
      vertexNormal[i * 3 + 1] /= m;
      vertexNormal[i * 3 + 2] /= m;
    }
    obj.normal = vertexNormal;
  }

  @Deprecated
  public static void normalToTexture(Obj obj) {
    // this is wrong
    int fsize = obj.face.length / 3;
    obj.texture = new double[fsize * 2];
    for (int f = 0; f < fsize; f++) {
      int n = obj.face[f * 3 + 1] * 3;
      obj.texture[f * 2] = Math.atan2(obj.normal[n], obj.normal[n + 2]) / Math.PI / 2 + 0.5;
      obj.texture[f * 2 + 1] = Math.asin(obj.normal[n + 1]) / Math.PI + 0.5;
      obj.face[f * 3 + 2] = f;
    }
  }

  public static void fixNormal(Obj obj) {
    if (obj.normal != null) return;
    flatNormal(obj);
    interpolateNormal(obj);
  }

  public static void verify(Obj obj) {
    for (int i = 0; i < obj.normal.length;) {
      double x = obj.normal[i++];
      double y = obj.normal[i++];
      double z = obj.normal[i++];
      double l = Math.sqrt(x * x + y * y + z * z);
      if (0.9999 > l || l > 1.0001) throw new IllegalStateException();
    }
  }

}
