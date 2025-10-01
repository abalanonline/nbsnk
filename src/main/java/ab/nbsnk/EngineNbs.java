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

import Jama.Matrix;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EngineNbs implements Engine3d {

  private static final Matrix IDENTITY = new Matrix(new double[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}, 4);
  private int imageWidth;
  private int imageHeight;
  private int[] imageRaster;
  private Shader shader;
  private BufferedImage image;
  private int[] background;
  private Set<ShapeNbs> root = new HashSet<>();

  private static class ShapeNbs implements Shape {
    private final Obj obj;
    private final Set<ShapeNbs> groupShape;
    private Set<ShapeNbs> group;
    private Matrix pivot = IDENTITY;
    private double tx;
    private double ty;
    private double tz;
    private double rx;
    private double ry;
    private double rz;

    private ShapeNbs(Obj obj, Set<ShapeNbs> root) {
      if (obj == null) {
        this.obj = null;
        this.groupShape = new HashSet<>();
      } else {
        this.obj = obj.clone();
        this.groupShape = null;
      }
      this.group = root;
      this.group.add(this);
    }

    @Override
    public ShapeNbs translation(double x, double y, double z) {
      tx = x; ty = y; tz = z;
      return this;
    }

    @Override
    public ShapeNbs rotation(double y, double p, double r) {
      ry = -y; // negative, the yaw axis directed towards the bottom
      rx = p;
      rz = -r; // negative, the longitudinal axis directed forward
      return this;
    }

    @Override
    public ShapeNbs setPivot() {
      pivot = this.multiply(IDENTITY);
      return this;
    }

    private Matrix multiply(Matrix matrix) {
      Matrix t = new Matrix(new double[][]{
          {1, 0, 0, this.tx},
          {0, 1, 0, this.ty},
          {0, 0, 1, this.tz},
          {0, 0, 0, 1},
      });
      double s = Math.sin(2 * Math.PI * this.rz);
      double c = Math.cos(2 * Math.PI * this.rz);
      Matrix rz = new Matrix(new double[][]{
          {c,-s, 0, 0},
          {s, c, 0, 0},
          {0, 0, 1, 0},
          {0, 0, 0, 1},
      });
      s = Math.sin(2 * Math.PI * this.rx);
      c = Math.cos(2 * Math.PI * this.rx);
      Matrix rx = new Matrix(new double[][]{
          {1, 0, 0, 0},
          {0, c,-s, 0},
          {0, s, c, 0},
          {0, 0, 0, 1},
      });
      s = Math.sin(2 * Math.PI * this.ry);
      c = Math.cos(2 * Math.PI * this.ry);
      Matrix ry = new Matrix(new double[][]{
          {c, 0, s, 0},
          {0, 1, 0, 0},
          {-s,0, c, 0},
          {0, 0, 0, 1},
      });
      return matrix.times(t).times(ry).times(rx).times(rz).times(this.pivot);
    }

    @Override
    public ShapeNbs connect(Shape shape) {
      Set<ShapeNbs> group = ((ShapeNbs) shape).groupShape;
      this.group.remove(this);
      this.group = group;
      this.group.add(this);
      return this;
    }
  }

  @Override
  public EngineNbs open(BufferedImage image) {
    this.imageWidth = image.getWidth();
    this.imageHeight = image.getHeight();
    this.imageRaster = new int[imageWidth * imageHeight];
    this.image = image;
    this.shader = new Shader(imageWidth, imageHeight);
    return this;
  }

  @Override
  public void background(BufferedImage image) {
    if (image == null) {
      background = null;
      return;
    }
    if (image.getWidth() != imageWidth || image.getHeight() != imageHeight) throw new IllegalArgumentException();
    background = new int[imageWidth * imageHeight];
    image.getRaster().getDataElements(0, 0, imageWidth, imageHeight, background);
  }

  @Override
  public Shape shape(Obj obj) {
    return new ShapeNbs(obj, root);
  }

  private void dfs(Set<ShapeNbs> shapes, Matrix tm) {
    for (ShapeNbs shape : shapes) {
      Matrix t = shape.multiply(tm);
      if (shape.obj == null) {
        dfs(shape.groupShape, t);
      } else {
        shader.add(shape.obj, t);
      }
    }
  }

  @Override
  public void update() {
    if (background == null) {
      Arrays.fill(imageRaster, -1);
    } else {
      System.arraycopy(background, 0, imageRaster, 0, imageWidth * imageHeight);
    }
    System.out.println();
    Arrays.fill(shader.zbuffer, 0);
    shader.imageRaster = this.imageRaster;
    dfs(root, IDENTITY);
    image.getRaster().setDataElements(0, 0, imageWidth, imageHeight, imageRaster);
  }

  @Override
  public void close() {
    this.image = null;
  }
}
