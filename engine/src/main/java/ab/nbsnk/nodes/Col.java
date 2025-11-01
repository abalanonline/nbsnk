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

public class Col {
  public double r;
  public double g;
  public double b;
  public double a; // FIXME: 2025-10-04 transparent texture mapping

  public Col() {
  }

  public Col(double r, double g, double b, double a) {
    this.r = r;
    this.g = g;
    this.b = b;
    this.a = a;
  }

  public Col(int argb) {
    this((argb >> 16 & 0xFF) / 255.0, (argb >> 8 & 0xFF) / 255.0, (argb & 0xFF) / 255.0, (argb >> 24 & 0xFF) / 255.0);
  }

  public Col add(Col col, double alpha) {
    this.r += col.r * alpha;
    this.g += col.g * alpha;
    this.b += col.b * alpha;
    return this;
  }

  public Col mul(double v) {
    this.r *= v;
    this.g *= v;
    this.b *= v;
    return this;
  }

  public Col mul(Col col) {
    this.r *= col.r;
    this.g *= col.g;
    this.b *= col.b;
    return this;
  }

  @Deprecated
  public int rgb() {
    return 0xFF000000 | Math.min(0xFF, (int) (r * 0xFF)) << 16 |
        Math.min(0xFF, (int) (g * 0xFF)) << 8 | Math.min(0xFF, (int) (b * 0xFF));
  }

  public int argb() {
    return Math.min(0xFF, (int) (a * 0xFF)) << 24 | Math.min(0xFF, (int) (r * 0xFF)) << 16 |
        Math.min(0xFF, (int) (g * 0xFF)) << 8 | Math.min(0xFF, (int) (b * 0xFF));
  }

  @Override
  public Col clone() {
    return new Col(r, g, b, a);
  }

  public static Col barycentric(Col a, Col b, Col c, double[] v) {
    Col col = new Col();
    col.r = a.r * v[0] + b.r * v[1] + c.r * v[2];
    col.g = a.g * v[0] + b.g * v[1] + c.g * v[2];
    col.b = a.b * v[0] + b.b * v[1] + c.b * v[2];
    return col;
  }

}
