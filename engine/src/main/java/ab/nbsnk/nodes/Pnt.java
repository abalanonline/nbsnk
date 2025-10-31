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

package ab.nbsnk.nodes;

public class Pnt {
  public double x;
  public double y;
  public double z;

  public Pnt() {
  }

  public Pnt(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @Override
  public Pnt clone() {
    return new Pnt(x, y, z);
  }

  public Pnt add(Pnt p, double alpha) {
    this.x += p.x * alpha;
    this.y += p.y * alpha;
    this.z += p.z * alpha;
    return this;
  }

  public Pnt normalize() {
    double length = Math.sqrt(x * x + y * y + z * z);
    x /= length;
    y /= length;
    z /= length;
    return this;
  }

  public double dot(Pnt pnt) {
    return this.x * pnt.x + this.y * pnt.y + this.z * pnt.z;
  }

  public static Pnt barycentric(Pnt a, Pnt b, Pnt c, double[] v) {
    return new Pnt(
      a.x * v[0] + b.x * v[1] + c.x * v[2],
      a.y * v[0] + b.y * v[1] + c.y * v[2],
      a.z * v[0] + b.z * v[1] + c.z * v[2]);
  }

  // TODO: 2025-10-31 getMatrix

}
