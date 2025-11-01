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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

class FbxTest {

  @Disabled
  @Test
  void printFbx() {
    System.out.println(Container.readNode(Paths.get("../assets/pig1/source/pig.fbx")).debug());
  }

  @Disabled
  @Test
  void fromPath() throws IOException {
    List<Path> pathList = Files.find(Paths.get("../assets/"), 5, (path, attributes) ->
        attributes.isRegularFile() && path.toString().toLowerCase().endsWith(".fbx")).collect(Collectors.toList());
    pathList.forEach(Fbx::fromPath);
    Fbx.fromPath(Paths.get("../assets/pig1/source/pig.fbx"));
  }

}
