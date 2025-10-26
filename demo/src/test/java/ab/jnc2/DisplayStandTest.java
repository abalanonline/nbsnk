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

package ab.jnc2;

import ab.nbsnk.Obj;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class DisplayStandTest {

  @Disabled
  @Test
  void main() throws IOException {
    DisplayStand displayStand = new DisplayStand();
    Obj obj = Obj.load(Files.newInputStream(Paths.get("assets/teapot.obj")));
    DisplayStand.Obj dobj = new DisplayStand.Obj();
    dobj.face = obj.face;
    dobj.vertex = obj.vertex;
    dobj.normal = obj.normal;
    dobj.texture = obj.texture;
    dobj.image = ImageIO.read(Files.newInputStream(Paths.get("assets/maptest.png")));
    displayStand.setObj(dobj);
    displayStand.open();
  }
}
