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

import java.awt.image.BufferedImage;

/**
 * Conventions: right-handed system, Y up, distances in meters, angles in turns [0,1).
 * Rotations in order yaw, pitch, roll, the object must look towards -Z, the same as user.
 */
public interface Engine3d extends AutoCloseable {
  /**
   * Starts the engine using image for rendering.
   */
  Engine3d open(BufferedImage image);

  void background(BufferedImage image);

  Shape shape(Obj obj);

  Shape camera();

  void update();

  @Override
  void close();

  interface Shape {

    Shape translation(double x, double y, double z);

    /**
     * @param y, p, r an angle, in turns [0,1).
     */
    Shape rotation(double y, double p, double r);

    /**
     * Use the current transformation as a pivot and reset the values.
     */
    Shape setPivot();

    Shape connect(Shape shape);

  }

}
