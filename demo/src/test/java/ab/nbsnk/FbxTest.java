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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

class FbxTest {

  public static void testNode(ByteBuffer buffer, int indent) {
    while (true) {
      Fbx.NodeRecord nodeRecord = Fbx.NodeRecord.getRecordInt(buffer);
      if (nodeRecord.isNullRecord()) return;
      for (int i = 0; i < indent; i++) System.out.print("  ");
      System.out.print(nodeRecord.name);
      int endOffset = buffer.position() + nodeRecord.propertyListLen;
      for (int i = 0; i < nodeRecord.numProperties; i++) testProperty(buffer);
      System.out.println();
      if (buffer.position() != endOffset) throw new IllegalStateException();
      if (nodeRecord.endOffset != endOffset) testNode(buffer, indent + 1);
    }
  }

  public static int testProperty(ByteBuffer buffer) {
    char type = (char) buffer.get();
    switch (type) {
      case 'I': System.out.print(" I: " + buffer.getInt()); return 5;
      case 'D': System.out.print(" D: " + buffer.getDouble()); return 9;
      case 'L': System.out.print(" L: " + buffer.getLong()); return 9;
      case 'C': System.out.print(" C: " + Fbx.getString(buffer, 1)); return 2;
      case 'S':
        int lengthS = buffer.getInt();
        System.out.print(" S: " + Fbx.getString(buffer, lengthS)); return lengthS + 5;
      case 'R':
        int lengthR = buffer.getInt();
        Fbx.getString(buffer, lengthR);
        System.out.print(" R: byte[" + lengthR + "]");
        return lengthR + 5;
      case 'd':
        int sized = buffer.getInt();
        int encodingd = buffer.getInt();
        int byteSized = buffer.getInt();
        Fbx.getString(buffer, byteSized);
        System.out.print(" d: double[" + sized + "]"); return byteSized + 13;
      case 'i':
        int sizei = buffer.getInt();
        int encodingi = buffer.getInt();
        int byteSizei = buffer.getInt();
        Fbx.getString(buffer, byteSizei);
        System.out.print(" i: int[" + sizei + "]"); return byteSizei + 13;
      default: throw new IllegalStateException();
    }
  }

  @Disabled
  @Test
  void printFbx() throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get("../assets/pig1/source/pig.fbx"));
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    System.out.println(Fbx.getString(buffer, 23) + " v." + buffer.getInt());
    testNode(buffer, 0);
  }

  @Disabled
  @Test
  void fromFile() throws IOException {
    List<Path> pathList = Files.find(Paths.get("../assets/"), 5, (path, attributes) ->
        attributes.isRegularFile() && path.toString().toLowerCase().endsWith(".fbx")).collect(Collectors.toList());
    pathList.forEach(Fbx::fromFile);
    Fbx.fromFile(Paths.get("../assets/pig1/source/pig.fbx"));
  }

}
