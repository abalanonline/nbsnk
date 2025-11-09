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

package ab.nbsnk.demo1;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Cube2Sphere {
  public static BufferedImage imageRead(String s) throws IOException {
    return ImageIO.read(Files.newInputStream(Paths.get("assets/resource/" + s + ".png")));
  }

  public static void main(String[] args) throws IOException {
    BufferedImage top = imageRead("top");
    BufferedImage bottom = imageRead("bottom");
    BufferedImage left = imageRead("left");
    BufferedImage right = imageRead("right");
    BufferedImage back = imageRead("back");
    BufferedImage front = imageRead("front");
    int width = 2048;
    int height = 1024;
    BufferedImage sphere = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        double pitch = (height / 2.0 - y - 0.5) / (height * 2.0);
        double yaw = (x + 0.5) / width;
        double ps = Math.sin(2 * Math.PI * pitch);
        double pc = Math.cos(2 * Math.PI * pitch);
        double ys = Math.sin(2 * Math.PI * yaw);
        double yc = Math.cos(2 * Math.PI * yaw);
        double x1 = pc * yc;
        double y1 = ps;
        double z1 = pc * ys;
        double xa = Math.abs(x1);
        double ya = Math.abs(y1);
        double za = Math.abs(z1);
//        int vx = (int) (yaw * w) % w * 255 / w;
//        int vy = (int) (pitch * h + h) % h * 255 / h;
//        color = vx << 16 | vy;
        int color;
        if (xa > ya && xa > za) {
          BufferedImage image = x1 > 0 ? back : front;
          int w = image.getWidth();
          int h = image.getHeight();
          color = image.getRGB(
              Math.min(Math.max(0, (int) Math.round((1 + z1 / x1) * w / 2)), w - 1),
              Math.min(Math.max(0, (int) Math.round((1 - y1 / xa) * h / 2)), h - 1));
        } else if (ya > xa && ya > za) {
          BufferedImage image = y1 > 0 ? top : bottom;
          int w = image.getWidth();
          int h = image.getHeight();
          color = image.getRGB(
              Math.min(Math.max(0, (int) Math.round((1 - z1 / ya) * w / 2)), w - 1),
              Math.min(Math.max(0, (int) Math.round((1 - x1 / y1) * h / 2)), h - 1));
        } else if (za > ya && za > xa) {
          BufferedImage image = z1 > 0 ? left : right;
          int w = image.getWidth();
          int h = image.getHeight();
          color = image.getRGB(
              Math.min(Math.max(0, (int) Math.round((1 - x1 / z1) * w / 2)), w - 1),
              Math.min(Math.max(0, (int) Math.round((1 - y1 / za) * h / 2)), h - 1));
        } else throw new IllegalStateException(); // it can happen tho
        sphere.setRGB(x, y, color);
      }
    }
    ImageIO.write(sphere, "png", Files.newOutputStream(Paths.get("assets/resource/sphere.png")));
  }
}
