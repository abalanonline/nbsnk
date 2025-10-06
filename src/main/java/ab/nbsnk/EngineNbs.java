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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class EngineNbs implements Engine3d {

  private static final Matrix IDENTITY = new Matrix(new double[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}, 4);
  private int imageWidth;
  private int imageHeight;
  private int[] imageRaster;
  private Shader shader;
  private BufferedImage image;
  private int[] background;
  private Set<NodeNbs> root = new HashSet<>();
  private NodeNbs camera;

  @Override
  public EngineNbs open(BufferedImage image) {
    this.imageWidth = image.getWidth();
    this.imageHeight = image.getHeight();
    this.imageRaster = new int[imageWidth * imageHeight];
    this.image = image;
    this.shader = new Shader(imageWidth, imageHeight);
    this.camera = new NodeNbs();
    root.remove(this.camera); // disconnect from the scene
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
  public ShapeNbs shape(Obj obj) {
    return new ShapeNbs(obj);
  }

  @Override
  public GroupNbs group() {
    return new GroupNbs();
  }

  @Override
  public LightNbs light() {
    return new LightNbs();
  }

  @Override
  public NodeNbs camera() {
    return this.camera;
  }

  private static void dfs(Set<NodeNbs> nodes, Matrix tm, Map<NodeNbs, Matrix> map) {
    for (NodeNbs node : nodes) {
      Matrix t = node.multiply(tm);
      if (node instanceof GroupNbs) {
        dfs(((GroupNbs) node).groupNode, t, map);
        continue;
      }
      if (node instanceof ShapeNbs || node instanceof LightNbs) {
        map.put(node, t);
        continue;
      }
      throw new IllegalStateException();
    }
  }

  @Override
  public void update() {
    if (background == null) {
      Arrays.fill(imageRaster, -1);
    } else {
      System.arraycopy(background, 0, imageRaster, 0, imageWidth * imageHeight);
    }
    Arrays.fill(shader.zbuffer, 0);
    shader.imageRaster = this.imageRaster;
    Map<NodeNbs, Matrix> map = new LinkedHashMap<>();
    dfs(root, this.camera.multiply(IDENTITY).inverse(), map);
    map.entrySet().stream().filter(e -> e.getKey() instanceof LightNbs)
        .forEach(e -> shader.addLight(((LightNbs) e.getKey()).color, e.getValue()));
    for (Map.Entry<NodeNbs, Matrix> entry : map.entrySet()) {
      NodeNbs node = entry.getKey();
      if (node instanceof LightNbs) {
        //System.out.println(node);
        continue;
      }
      if (node instanceof ShapeNbs) {
        ShapeNbs shape = (ShapeNbs) node;
        shader.add(shape.obj, entry.getValue(), shape.textureRaster, shape.textureWidth, shape.textureHeight, shape.color);
        continue;
      }
      throw new IllegalStateException();
    }
    image.getRaster().setDataElements(0, 0, imageWidth, imageHeight, imageRaster);
  }

  @Override
  public void sysex(int i) {
    switch (i) {
//      case '1': shader.enableDimension = 0; break;
//      case '2': shader.enableDimension = 1; break;
//      case '3': shader.enableDimension = 2; break;
      case '4': shader.enableIllumination = Shader.Illumination.NONE; break;
      case '5': shader.enableIllumination = Shader.Illumination.LAMBERT; break;
      case '6': shader.enableIllumination = Shader.Illumination.GOURAUD; break;
      case '7': shader.enableIllumination = Shader.Illumination.PHONG; break;
      case '=': shader.enableTexture = !shader.enableTexture; break;
    }
  }

  @Override
  public void close() {
    this.image = null;
  }

  private class NodeNbs implements Node {
    private Set<NodeNbs> group;
    private Matrix pivot = IDENTITY;
    private double tx;
    private double ty;
    private double tz;
    private double rx;
    private double ry;
    private double rz;

    private NodeNbs() {
      this.group = root;
      this.group.add(this);
    }

    @Override
    public NodeNbs translation(double x, double y, double z) {
      tx = x; ty = y; tz = z;
      return this;
    }

    @Override
    public NodeNbs rotation(double y, double p, double r) {
      ry = -y; // negative, the yaw axis directed towards the bottom
      rx = p;
      rz = -r; // negative, the longitudinal axis directed forward
      return this;
    }

    @Override
    public NodeNbs setPivot() {
      pivot = this.multiply(IDENTITY);
      tx = 0; ty = 0; tz = 0;
      rx = 0; ry = 0; rz = 0;
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
    public NodeNbs connect(Node node) {
      Set<NodeNbs> group = ((GroupNbs) node).groupNode;
      this.group.remove(this);
      this.group = group;
      this.group.add(this);
      return this;
    }
  }

  private class ShapeNbs extends NodeNbs implements Shape {

    private final Obj obj;
    private int[] textureRaster;
    private int textureWidth;
    private int textureHeight;
    private int color = -1;

    public ShapeNbs(Obj obj) {
      this.obj = obj.clone();
      if (obj.image != null) {
        int textureWidth = obj.image.getWidth();
        int textureHeight = obj.image.getHeight();
        int[] textureRaster = new int[textureWidth * textureHeight];
        //obj.image.getRaster().getDataElements(0, 0, textureWidth, textureHeight, textureRaster);
        for (int y = 0; y < textureHeight; y++) {
          for (int x = 0; x < textureWidth; x++) textureRaster[y * textureWidth + x] = obj.image.getRGB(x, y);
        }
        this.textureWidth = textureWidth;
        this.textureHeight = textureHeight;
        this.textureRaster = textureRaster;
      }
    }

    @Override
    public ShapeNbs setColor(int color) {
      this.color = color;
      return this;
    }

    @Override
    public ShapeNbs selfIllumination() {
      return this;
    }
  }

  private class GroupNbs extends NodeNbs implements Group {

    private Set<NodeNbs> groupNode = new HashSet<>();

  }

  private class LightNbs extends NodeNbs implements Light {

    private int color;

    @Override
    public LightNbs setColor(int color) {
      this.color = color;
      return this;
    }
  }

}
