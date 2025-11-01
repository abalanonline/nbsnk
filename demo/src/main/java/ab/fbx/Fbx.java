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

package ab.fbx;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Fbx {
  Node root;
  Path path;
  Map<Long, Node> idNode = new HashMap<>();
  Map<Node, Map<String, List<Node>>> oo = new LinkedHashMap<>();
  Map<Node, Map<String, List<Node>>> or = new LinkedHashMap<>();
  Map<Node, Map<String, List<Node>>> op = new LinkedHashMap<>();

  public Fbx(Node root, Path path) {
    System.out.println(root.debug());
    this.root = root;
    this.path = path;
    idNode.put(0L, root);
    Node objects = root.get("Objects");
//    List.of("Model", "Geometry", "Material", "NodeAttribute", "Video", "Texture", "CollectionExclusive")
//        .stream().map(objects::getAll).flatMap(Arrays::stream).forEach(n -> idNode.put((Long) n.property[0], n));
    Arrays.stream(objects.nested).forEach(n -> idNode.put((Long) n.property[0], n));
    for (Node connection : root.get("Connections").getAll("C")) {
      Long p1 = (Long) connection.property[1];
      Long p2 = (Long) connection.property[2];
      Node n1 = idNode.get(p1);
      Node n2 = idNode.get(p2);
      Objects.requireNonNull(n1, p1.toString());
      Objects.requireNonNull(n2, p2.toString());
      if ("OO".equals(connection.property[0])) {
        oo.computeIfAbsent(n2, a -> new LinkedHashMap<>())
            .computeIfAbsent(n1.name, a -> new ArrayList<>()).add(n1);
        or.computeIfAbsent(n1, a -> new LinkedHashMap<>())
            .computeIfAbsent(n2.name, a -> new ArrayList<>()).add(n2);
      }
      if ("OP".equals(connection.property[0])) op.computeIfAbsent(n2, a -> new LinkedHashMap<>())
          .computeIfAbsent((String) connection.property[3], a -> new ArrayList<>()).add(n1);
    }

  }

  public Geometry[] geometry() {
    return Arrays.stream(root.get("Objects").getAll("Geometry")).map(n -> new Geometry(this, n)).toArray(Geometry[]::new);
  }

  public static Fbx[] fromPath(Path path) {
    List<Path> pathList;
    try {
      pathList = Files.find(path, 2, (p, attributes) ->
          attributes.isRegularFile() && p.toString().toLowerCase().endsWith(".fbx"))
          .collect(Collectors.toList());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return pathList.stream().map(p -> new Fbx(Container.readNode(p), p.getParent())).toArray(Fbx[]::new);
  }

}
