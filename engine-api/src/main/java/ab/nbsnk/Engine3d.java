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
 * Yaw 0 direction toward north.
 */
public interface Engine3d extends AutoCloseable {
  /**
   * Starts the engine using image for rendering.
   */
  Engine3d open(BufferedImage image);

  Engine3d background(BufferedImage image);

  Shape shape(Obj obj);

  Group group();

  Light light();

  Engine3d setAmbient(int color);

  Node camera();

  Engine3d setFarClip(double value);

  void update();

  /**
   * System Exclusive message.
   */
  void sysex(int i);

  Engine3d showFps();

  @Override
  void close();

  interface Node {

    Node translation(double x, double y, double z);

    /**
     * @param yaw, pitch, roll an angle, in turns [0,1).
     */
    Node rotation(double yaw, double pitch, double roll);

    /**
     * Use the current transformation as a pivot and reset the values.
     */
    Node setPivot();

    Node connect(Group node);

    Node setVisible(boolean value);

  }

  interface Shape extends Node {

    // default color is the diffuse one
    Shape setColor(int color);

    Shape setSpecular(int color, double power);

    Shape selfIllumination(int color);

    // although it is a normal map, the word bump makes it instantly understood
    Shape setBumpMap(BufferedImage image);

    Shape setReflectionMap(BufferedImage image, double alpha, Node skybox);
  }

  interface Group extends Node {}

  interface Light extends Node {

    Light setColor(int color);

  }

}
