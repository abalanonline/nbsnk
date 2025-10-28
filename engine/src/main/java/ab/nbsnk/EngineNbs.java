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
import ab.nbsnk.nodes.Col;
import ab.nbsnk.nodes.Pnt;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class EngineNbs implements Engine3d {

  public static final Matrix IDENTITY = new Matrix(new double[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1}, 4);
  private int imageWidth;
  private int imageHeight;
  private int[] imageRaster;
  private Shader shader;
  private BufferedImage image;
  private int[] background;
  private BufferedImage backgroundImage;
  private Set<NodeNbs> root = new HashSet<>();
  private NodeNbs camera;
  private Supplier<String> textSupplier;
  private Map<BufferedImage, int[]> imageCache = new HashMap<>();
  private Col ambientColor = new Col();

  public static Matrix multiply(Matrix matrix, double tx, double ty, double tz, double rx, double ry, double rz) {
    Matrix t = new Matrix(new double[][]{
        {1, 0, 0, tx},
        {0, 1, 0, ty},
        {0, 0, 1, tz},
        {0, 0, 0, 1},
    });
    double s = Math.sin(2 * Math.PI * rz);
    double c = Math.cos(2 * Math.PI * rz);
    Matrix mrz = new Matrix(new double[][]{
        {c,-s, 0, 0},
        {s, c, 0, 0},
        {0, 0, 1, 0},
        {0, 0, 0, 1},
    });
    s = Math.sin(2 * Math.PI * rx);
    c = Math.cos(2 * Math.PI * rx);
    Matrix mrx = new Matrix(new double[][]{
        {1, 0, 0, 0},
        {0, c,-s, 0},
        {0, s, c, 0},
        {0, 0, 0, 1},
    });
    s = Math.sin(2 * Math.PI * ry);
    c = Math.cos(2 * Math.PI * ry);
    Matrix mry = new Matrix(new double[][]{
        {c, 0, s, 0},
        {0, 1, 0, 0},
        {-s,0, c, 0},
        {0, 0, 0, 1},
    });
    return matrix.times(t).times(mry).times(mrx).times(mrz);
  }

  public EngineNbs() {
    this.camera = new NodeNbs();
  }

  @Override
  public EngineNbs open(BufferedImage image) {
    this.imageWidth = image.getWidth();
    this.imageHeight = image.getHeight();
    this.imageRaster = new int[imageWidth * imageHeight];
    this.image = image;
    this.shader = new Shader(imageWidth, imageHeight);
    applyBackground();
    return this;
  }

  private void applyBackground() {
    background = null;
    if (imageWidth == 0 || imageHeight == 0 || backgroundImage == null) return;
    BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
    image.getGraphics().drawImage(backgroundImage, 0, 0, null);
    background = new int[imageWidth * imageHeight];
    image.getRaster().getDataElements(0, 0, imageWidth, imageHeight, background);
  }

  @Override
  public EngineNbs background(BufferedImage image) {
    backgroundImage = image;
    applyBackground();
    return this;
  }

  @Override
  public ShapeNbs shape(Obj obj) {
    Obj.verify(obj);
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
  public EngineNbs setAmbient(int color) {
    this.ambientColor = new Col(color);
    return this;
  }

  @Override
  public NodeNbs camera() {
    return this.camera;
  }

  @Override
  public EngineNbs setFarClip(double value) {
    shader.farClip = value;
    return this;
  }

  private static void dfs(Set<NodeNbs> nodes, Matrix tm, Map<NodeNbs, Matrix> map) {
    for (NodeNbs node : nodes) {
      if (!node.visible) continue;
      Matrix t = node.multiply(tm);
      if (node instanceof GroupNbs) {
        dfs(((GroupNbs) node).groupNode, t, map);
        continue;
      }
      map.put(node, t);
    }
  }

  @Override
  public void update() {
    shader.cls();
    if (background == null) {
      Arrays.fill(imageRaster, -1);
    } else {
      System.arraycopy(background, 0, imageRaster, 0, imageWidth * imageHeight);
    }
    shader.imageRaster = this.imageRaster;
    Map<NodeNbs, Matrix> map = new LinkedHashMap<>();
    dfs(root, IDENTITY, map);
    final Matrix cameraMatrix = map.get(this.camera).inverse();
    map.entrySet().stream().filter(e -> e.getKey() instanceof LightNbs)
        .forEach(e -> {
          Matrix xyz = cameraMatrix.times(e.getValue()).times(new Matrix(new double[]{0, 0, 0, 1}, 4));
          Pnt pnt = new Pnt(xyz.get(0, 0), xyz.get(1, 0), xyz.get(2, 0));
          int color = ((LightNbs) e.getKey()).color;
          shader.addLight(pnt, new Col(color));
          //shader.addLight(((LightNbs) e.getKey()).color, e.getValue()); // deprecated
        });
    // if no lights, add the light from the camera, Javafx default
    if (shader.lights.isEmpty()) shader.addLight(new Pnt(), new Col(-1));
    for (Map.Entry<NodeNbs, Matrix> entry : map.entrySet()) {
      NodeNbs node = entry.getKey();
      if (node instanceof LightNbs || node == this.camera) continue;
      if (node instanceof ShapeNbs) {
        ShapeNbs shape = (ShapeNbs) node;
        Shader.Illumination enableIllumination = shader.enableIllumination;
        if (shape.selfIllumination) shader.enableIllumination = Shader.Illumination.NONE;
        shader.ambientColor = this.ambientColor;
        shader.diffuseColor = shape.diffuseColor;
        shader.specularColor = shape.specularColor;
        shader.specularPower = shape.specularPower;
        shader.textureRaster = shape.textureRaster;
        shader.textureWidth = shape.textureWidth;
        shader.textureHeight = shape.textureHeight;
        shader.bumpRaster = shape.bumpRaster;
        shader.bumpWidth = shape.bumpWidth;
        shader.bumpHeight = shape.bumpHeight;
        shader.tangentBitangent = shape.tangentBitangent;
        shader.reflectionRaster = shape.reflectionRaster;
        shader.reflectionWidth = shape.reflectionWidth;
        shader.reflectionHeight = shape.reflectionHeight;
        shader.reflectionAlpha = shape.reflectionAlpha;
        shader.reflectionMatrix = cameraMatrix.times(map.getOrDefault(shape.reflectionSkybox, IDENTITY)).inverse();
        shader.add(shape.obj, cameraMatrix.times(entry.getValue()));
        if (shape.selfIllumination) shader.enableIllumination = enableIllumination;
        continue;
      }
      throw new IllegalStateException();
    }
    image.getRaster().setDataElements(0, 0, imageWidth, imageHeight, imageRaster);
    if (textSupplier != null) {
      Graphics graphics = image.createGraphics();
      graphics.setColor(java.awt.Color.DARK_GRAY);
      graphics.drawString(textSupplier.get(), 2, imageHeight - 4);
    }
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
  public EngineNbs textSupplier(Supplier<String> supplier) {
    textSupplier = supplier;
    return this;
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
    private boolean visible = true;

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
      return EngineNbs.multiply(matrix, this.tx, this.ty, this.tz, this.rx, this.ry, this.rz).times(this.pivot);
    }

    @Override
    public NodeNbs connect(Group node) {
      Set<NodeNbs> group = ((GroupNbs) node).groupNode;
      this.group.remove(this);
      this.group = group;
      this.group.add(this);
      return this;
    }

    @Override
    public NodeNbs setVisible(boolean value) {
      visible = value;
      return this;
    }
  }

  private static int[] loadImg(BufferedImage image) {
    int textureWidth = image.getWidth();
    int textureHeight = image.getHeight();
    int[] textureRaster = new int[textureWidth * textureHeight];
    //image.getRaster().getDataElements(0, 0, textureWidth, textureHeight, textureRaster);
    for (int y = 0; y < textureHeight; y++) {
      for (int x = 0; x < textureWidth; x++) textureRaster[y * textureWidth + x] = image.getRGB(x, y);
    }
    return textureRaster;
  }

  private class ShapeNbs extends NodeNbs implements Shape {

    private final Obj obj;
    private int[] textureRaster;
    private int textureWidth;
    private int textureHeight;
    private int[] bumpRaster;
    private int bumpWidth;
    private int bumpHeight;
    private Col diffuseColor = new Col(-1);
    private Col specularColor = new Col();
    private double specularPower = 32;
    private boolean selfIllumination;
    private double[] tangentBitangent;
    private int[] reflectionRaster;
    private int reflectionWidth;
    private int reflectionHeight;
    private double reflectionAlpha;
    private NodeNbs reflectionSkybox;

    public ShapeNbs(Obj obj) {
      this.obj = obj.clone();
      if (obj.image != null) {
        this.textureWidth = obj.image.getWidth();
        this.textureHeight = obj.image.getHeight();
        this.textureRaster = imageCache.computeIfAbsent(obj.image, EngineNbs::loadImg);
      }
      this.tangentBitangent = Shader.computeTangentBitangent(obj);
    }

    @Override
    public ShapeNbs setColor(int color) {
      this.diffuseColor = new Col(color);
      return this;
    }

    @Override
    public ShapeNbs setSpecular(int color, double power) {
      this.specularColor = new Col(color);
      this.specularPower = power;
      return this;
    }

    @Override
    public ShapeNbs selfIllumination(int color) {
      if ((color & 0xFFFFFF) != 0xFFFFFF) {
        Col mul = new Col(color);
        int n = textureRaster.length;
        int[] tr = new int[n];
        for (int i = 0; i < n; i++) tr[i] = new Col(textureRaster[i]).mul(mul).rgb();
        textureRaster = tr;
      }
      selfIllumination = true;
      return this;
    }

    @Override
    public ShapeNbs setBumpMap(BufferedImage image) {
      this.bumpWidth = image.getWidth();
      this.bumpHeight = image.getHeight();
      this.bumpRaster = imageCache.computeIfAbsent(image, EngineNbs::loadImg);
      return this;
    }

    @Override
    public ShapeNbs setReflectionMap(BufferedImage image, double alpha, Node skybox) {
      this.reflectionWidth = image.getWidth();
      this.reflectionHeight = image.getHeight();
      this.reflectionRaster = imageCache.computeIfAbsent(image, EngineNbs::loadImg);
      this.reflectionAlpha = alpha;
      this.reflectionSkybox = (NodeNbs) skybox;
      return this;
    }
  }

  private class GroupNbs extends NodeNbs implements Group {

    private Set<NodeNbs> groupNode = new HashSet<>();

  }

  private class LightNbs extends NodeNbs implements Light {

    private int color = -1;

    @Override
    public LightNbs setColor(int color) {
      this.color = color;
      return this;
    }
  }

}
