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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

// https://code.blender.org/2013/08/fbx-binary-file-format-specification/
public class Fbx {

  Node root;
  int version;

  public class Node {
    String name;
    List<Object> propertyList = new ArrayList<>();
    List<Node> nestedList = new ArrayList<>();

    public void getNested(ByteBuffer buffer) {
      while (true) {
        NodeRecord nodeRecord = version < 7500 ? NodeRecord.getRecordInt(buffer) : NodeRecord.getRecordLong(buffer);
        if (nodeRecord.isNullRecord()) return;
        Node node = new Node();
        nestedList.add(node);
        node.name = nodeRecord.name;
        int endOffset = buffer.position() + nodeRecord.propertyListLen;
        for (int i = 0; i < nodeRecord.numProperties; i++) propertyList.add(getProperty(buffer));
        if (buffer.position() != endOffset) throw new IllegalStateException();
        if (nodeRecord.endOffset != endOffset) node.getNested(buffer);
      }
    }
  }

  public static class NodeRecord {
    int endOffset;
    int numProperties;
    int propertyListLen;
    String name;
    public boolean isNullRecord() {
      return endOffset == 0 && numProperties == 0 && propertyListLen == 0 && name.isEmpty();
    }
    public static NodeRecord getRecordInt(ByteBuffer buffer) {
      NodeRecord nodeRecord = new NodeRecord();
      nodeRecord.endOffset = buffer.getInt();
      nodeRecord.numProperties = buffer.getInt();
      nodeRecord.propertyListLen = buffer.getInt();
      nodeRecord.name = getString(buffer, buffer.get() & 0xFF);
      return nodeRecord;
    }
    public static NodeRecord getRecordLong(ByteBuffer buffer) {
      NodeRecord nodeRecord = new NodeRecord();
      nodeRecord.endOffset = (int) buffer.getLong();
      nodeRecord.numProperties = (int) buffer.getLong();
      nodeRecord.propertyListLen = (int) buffer.getLong();
      nodeRecord.name = getString(buffer, buffer.get() & 0xFF);
      return nodeRecord;
    }
  }

  public static String getString(ByteBuffer buffer, int length) {
    byte[] bytes = new byte[length];
    buffer.get(bytes);
    return new String(bytes);
  }

  public static byte[] getEncoded(ByteBuffer buffer, int arrayLength, int encoding, int compressedLength) {
    byte[] bytes = new byte[compressedLength];
    buffer.get(bytes);
    if (encoding != 0) {
      if (encoding != 1) throw new IllegalStateException();
      Inflater inflater = new Inflater();
      inflater.setInput(bytes);
      byte[] result = new byte[arrayLength + 1024];
      int resultLength;
      try {
        resultLength = inflater.inflate(result);
      } catch (DataFormatException e) {
        throw new IllegalStateException(e);
      }
      inflater.end();
      if (resultLength != arrayLength) throw new IllegalStateException();
      bytes = Arrays.copyOf(result, resultLength);
    }
    return bytes;
  }

  public static Object getProperty(ByteBuffer buffer) {
    char type = (char) buffer.get();
    switch (type) {
      case 'I': return buffer.getInt();
      case 'D': return buffer.getDouble();
      case 'L': return buffer.getLong();
      case 'C': return (char) buffer.get();
      case 'S': return getString(buffer, buffer.getInt());
      case 'R': return getEncoded(buffer, 0, 0, buffer.getInt());
      case 'd':
        byte[] doubleBytes = getEncoded(buffer, buffer.getInt() * 8, buffer.getInt(), buffer.getInt());
        ByteBuffer doubleBuffer = ByteBuffer.wrap(doubleBytes).order(ByteOrder.LITTLE_ENDIAN);
        double[] doubles = new double[doubleBytes.length / 8];
        for (int i = 0; i < doubles.length; i++) doubles[i] = doubleBuffer.getDouble();
        return doubles;
      case 'i':
        byte[] intBytes = getEncoded(buffer, buffer.getInt() * 4, buffer.getInt(), buffer.getInt());
        ByteBuffer intBuffer = ByteBuffer.wrap(intBytes).order(ByteOrder.LITTLE_ENDIAN);
        int[] ints = new int[intBytes.length / 4];
        for (int i = 0; i < ints.length; i++) ints[i] = intBuffer.getInt();
        return ints;
      case 'b':
        byte[] booleanBytes = getEncoded(buffer, buffer.getInt(), buffer.getInt(), buffer.getInt());
        boolean[] booleans = new boolean[booleanBytes.length];
        for (int i = 0; i < booleans.length; i++) {
          byte booleanByte = booleanBytes[i];
          if ((booleanByte | 1) != 1) throw new IllegalStateException();
          booleans[i] = booleanByte > 0;
        }
        return booleans;
      default: throw new IllegalStateException();
    }
  }

  public static Fbx fromBytes(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
    String signature = getString(buffer, 23);
    if (!"Kaydara FBX Binary  \u0000\u001A\u0000".equals(signature)) throw new IllegalStateException("signature");
    Fbx fbx = new Fbx();
    fbx.version = buffer.getInt();
    fbx.root = fbx.new Node();
    fbx.root.getNested(buffer);
    int remaining = buffer.remaining();
    if (161 > remaining || remaining > 176) throw new IllegalStateException("remaining");
    return fbx;
  }

  public static Fbx fromFile(Path path) {
    try {
      return fromBytes(Files.readAllBytes(path));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

}
