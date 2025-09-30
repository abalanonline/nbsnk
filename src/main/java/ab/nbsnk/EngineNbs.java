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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EngineNbs implements Engine3d {

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
    private double tx;
    private double ty;
    private double tz;
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
    public void translation(double x, double y, double z) {
      tx = x; ty = y; tz = z;
    }

    @Override
    public void rotation(double z) {
      rz = z;
    }

    @Override
    public void connect(Shape shape) {
      Set<ShapeNbs> group = ((ShapeNbs) shape).groupShape;
      this.group.remove(this);
      this.group = group;
      this.group.add(this);
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

  private void dfs(Set<ShapeNbs> shapes, double[] transformation) {
    for (ShapeNbs shape : shapes) {
      double[] t = Arrays.copyOf(transformation, transformation.length);
      t[0] += shape.tx;
      t[1] += shape.ty;
      t[2] += shape.tz;
      t[3] += shape.rz;
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
    dfs(root, new double[4]);
    image.getRaster().setDataElements(0, 0, imageWidth, imageHeight, imageRaster);
  }

  @Override
  public void close() {
    this.image = null;
  }
}
