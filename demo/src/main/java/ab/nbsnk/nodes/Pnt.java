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

}
