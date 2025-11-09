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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

// finds resources by signature
public class ResourceExtract {
  /**
   * Reads the source file and writes the found chunks with signatures to the same folder.
   * @param path the source file path
   * @param output the output file format file%04d.png
   * @param signatures signatures
   * @param offsets signature offsets
   */
  public ResourceExtract(Path path, String output, byte[][] signatures, int[] offsets) {
    Path folder = path.getParent();
    int length = signatures.length;
    if (length <= 0 || length != offsets.length) throw new IllegalStateException();
    try {
      byte[] bytes = Files.readAllBytes(path);
      int fileCount = 0;
      for (int i = 0; i < bytes.length; i++) if (valid(bytes, i, signatures, offsets))
        Files.write(folder.resolve(String.format(output, fileCount++)), Arrays.copyOfRange(bytes, i, bytes.length));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static boolean valid(byte[] content, int pos, byte[][] signatures, int[] offsets) {
    int length = signatures.length;
    for (int i = 0; i < length; i++) if (!valid(content, pos + offsets[i], signatures[i])) return false;
    return true;
  }

  public static boolean valid(byte[] content, int pos, byte[] signature) {
    int length = signature.length;
    if (pos < 0 || pos + length > content.length) return false;
    for (byte b : signature) if (content[pos++] != b) return false;
    return true;
  }

  public static void main(String[] args) throws IOException {
    Path path = Files.list(Paths.get("assets/resource")).filter(p -> p.toString().endsWith(".exe")).findFirst().get();
    new ResourceExtract(path, "png%04d.png", new byte[][]{{0x49, 0x48, 0x44, 0x52},
        {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}}, new int[]{12, 0});
    new ResourceExtract(path, "jpg%04d.jpg", new byte[][]{{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}}, new int[]{0});
    //new ResourceExtract(path, "bmp%04d.bmp", new byte[][]{{0x42, 0x4D}}, new int[]{0});
  }
}
