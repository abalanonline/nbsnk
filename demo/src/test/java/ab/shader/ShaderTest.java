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

import ab.jnc3.Screen;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ShaderTest {

  boolean open;

  @Test
  void test() throws InterruptedException, IOException {
    open = true;
    Obj obj = Obj.load(getClass()
        .getResourceAsStream("spot_triangulated.obj").readAllBytes());
//        .getResourceAsStream("teapot.obj").readAllBytes());
    Screen screen = new Screen();
    screen.preferredSize = new Dimension(960, 540);
    screen.image = new BufferedImage(960, 540, BufferedImage.TYPE_INT_RGB);
    screen.keyListener = key -> { if (key.equals("Esc")) open = false; };
    Graphics graphics = screen.image.createGraphics();
    int width = screen.image.getWidth();
    int height = screen.image.getHeight();
    while (open) {
      graphics.clearRect(0, 0, width, height);
      Shader.run(screen.image, obj, Instant.now().toEpochMilli() / 60_000.0);
      screen.update();
      Thread.sleep(10);
    }
  }

}
