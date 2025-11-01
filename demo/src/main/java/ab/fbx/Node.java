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

import java.util.Arrays;

public class Node {
  // high cohesion class, do not modify
  public String name;
  public Object[] property = new Object[0];
  public Node[] nested = new Node[0];

  public Node get(String s) {
    Node result = null;
    for (Node node : nested) if (s.equals(node.name)) {
      if (result != null) throw new IllegalStateException();
      result = node;
    }
    return result;
  }

  public Node[] getAll(String s) {
    return Arrays.stream(nested).filter(node -> s.equals(node.name)).toArray(Node[]::new);
  }

  public String getString(String s) {
    Node node = get(s);
    if (node == null) return null;
    if (node.nested.length != 0 || node.property.length != 1) throw new IllegalStateException();
    return (String) node.property[0];
  }

  private String toString(Object object) {
    if (object instanceof byte[]) return "byte[" + ((byte[]) object).length + "]";
    if (object instanceof double[]) return "double[" + ((double[]) object).length + "]";
    if (object instanceof int[]) return "int[" + ((int[]) object).length + "]";
    if (object instanceof String) return "\"" + object + "\"";
    return String.valueOf(object);
  }

  private String propertiesToString() {
    StringBuilder s = new StringBuilder();
    s.append(name);
    for (Object o : property) s.append(" ").append(toString(o));
    return s.toString();
  }

  @Override
  public String toString() {
    int p = property.length;
    int n = nested.length;
    if (n == 0 && p < 8) return propertiesToString();
    return name + (p == 0 ? "" : " " + p) + (n == 0 ? "" : " " + n + "->");
  }

  public String debug() {
    StringBuilder s = new StringBuilder();
    s.append(propertiesToString() + "\n");
    for (Node node : nested) Arrays.stream(node.debug().split("\n")).forEach(d -> s.append("  " + d + "\n"));
    return s.toString();
  }
}
