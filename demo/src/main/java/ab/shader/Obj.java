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

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Obj {
  public int[] face; // vertex, normal, texture
  public double[] vertex;
  public double[] normal;
  public double[] texture; // 0 <= (x, y) <= 1, Y-up, as in .obj
  public BufferedImage image;
  public String id;

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
