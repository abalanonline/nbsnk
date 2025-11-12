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

package ab;

import ab.nbsnk.Obj;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;

public class Collada {
  public static Obj read(Path path) {
    XmlElement root = XmlElement.read(path);
    if (!"COLLADA".equals(root.name)) throw new IllegalStateException();
    XmlElement mesh = root.get("library_geometries").get("geometry").get("mesh");

    // mesh floats
    LinkedHashMap<String, double[]> floatArrayById = new LinkedHashMap<>();
    for (XmlElement element : mesh.contentElement) {
      if (!"source".equals(element.name)) continue;
      String id = element.attributes.get("id");
      if (!id.equals(element.attributes.get("name"))) throw new IllegalStateException();
      XmlElement floatArray = element.get("float_array");
      String[] floatString = floatArray.text.trim().split("\\s");
      int length = floatString.length;
      if (length != Integer.parseInt(floatArray.attributes.get("count"))) throw new IllegalStateException();
      double[] doubles = new double[length];
      for (int i = 0; i < length; i++) doubles[i] = Double.parseDouble(floatString[i]);
      floatArrayById.put(id, doubles);
    }

    // mesh faces
    XmlElement polylist = mesh.get("polylist");
    boolean vcountAllMatch3 = Arrays.stream(polylist.get("vcount").text.split("\\s"))
        .mapToInt(Integer::parseInt).allMatch(i -> i == 3);
    if (!vcountAllMatch3) throw new IllegalStateException("not implemented");
    int[] faces = Arrays.stream(polylist.get("p").text.split("\\s")).mapToInt(Integer::parseInt).toArray();

    // make obj
    Obj obj = new Obj();
    obj.face = new int[faces.length * 3];
    for (int i = 0; i < faces.length; i++) {
      obj.face[3 * i] = faces[i];
      obj.face[3 * i + 1] = faces[i];
      obj.face[3 * i + 2] = faces[i];
    }
    obj.vertex = floatArrayById.get("meshId0-positions");
    obj.normal = floatArrayById.get("meshId0-normals");
    obj.texture = floatArrayById.get("meshId0-tex0");
    return obj;
  }
}
