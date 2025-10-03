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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Both engines, side by side.
 */
public class EngineDual implements Engine3d {
  public static final int IMGDIV = 4;
  private final Engine3d engineLeft;
  private final Engine3d engineRight;
  private int imageWidth;
  private int imageHeight;
  private BufferedImage image;
  private BufferedImage imageLeft;
  private BufferedImage imageRight;

  public EngineDual() {
    this.engineLeft = new EngineNbs();
    this.engineRight = new EngineFx();
  }

  @Override
  public EngineDual open(BufferedImage image) {
    Set<Integer> resolution = new HashSet<>();
    int fhd = 1080;
    int hd = 720;
    for (int i = 0, p = 1; i < 3; i++, p *= 2) {
      resolution.add(fhd / p);
      resolution.add(hd / p);
    }
    StringBuilder s = new StringBuilder();
    for (int i : resolution.stream().sorted(Comparator.comparingInt(a -> -a)).collect(Collectors.toList())) {
      s.append(String.format("%4dx%4d", i * 16 / 9, i));
      if (fhd % i == 0) s.append(" FHD" + (i == fhd ? "" : "/" + fhd / i));
      if (hd % i == 0) s.append(" HD" + (i == hd ? "" : "/" + hd / i));
      s.append('\n');
    }
    // HDTV compatible low res
    // 1920x1080 FHD
    // 1280x 720 HD
    //  960x 540 FHD/2
    //  640x 360 FHD/3 HD/2
    //  480x 270 FHD/4
    //  320x 180 FHD/6 HD/4

    this.image = image;
    this.imageWidth = image.getWidth();
    this.imageHeight = image.getHeight();
    int height = imageHeight - imageHeight / IMGDIV;
    this.imageLeft = new BufferedImage(imageWidth / 2, height, BufferedImage.TYPE_INT_RGB);
    this.imageRight = new BufferedImage(imageWidth / 2, height, BufferedImage.TYPE_INT_RGB);
    engineLeft.open(imageLeft);
    engineRight.open(imageRight);
    return this;
  }

  @Override
  public void background(BufferedImage image) {
    int height = imageHeight - imageHeight / IMGDIV;
    BufferedImage crop = new BufferedImage(imageWidth / 2, height, BufferedImage.TYPE_INT_ARGB);
    crop.getGraphics().drawImage(image, 0, 0, null);
    engineLeft.background(crop);
    engineRight.background(crop);
  }

  @Override
  public ShapeDual shape(Obj obj) {
    return new ShapeDual(engineLeft.shape(obj), engineRight.shape(obj));
  }

  @Override
  public GroupDual group() {
    return new GroupDual(engineLeft.group(), engineRight.group());
  }

  @Override
  public LightDual light() {
    return new LightDual(engineLeft.light(), engineRight.light());
  }

  @Override
  public NodeDual camera() {
    return new NodeDual(engineLeft.camera(), engineRight.camera());
  }

  @Override
  public void update() {
    engineLeft.update();
    engineRight.update();
    Graphics2D graphics = image.createGraphics();
    int width = imageWidth / 2;
    int height = imageHeight - imageHeight / IMGDIV;
    graphics.drawImage(imageLeft, 0, 0, null);
    graphics.drawImage(imageRight, width, 0, null);
    // comparison takes about 7%
    BufferedImage compare = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    int[] dataLeft = new int[width * height];
    imageLeft.getRaster().getDataElements(0, 0, width, height, dataLeft);
    int[] dataRight = new int[width * height];
    imageRight.getRaster().getDataElements(0, 0, width, height, dataRight);
    for (int i = 0; i < width * height; i++) dataLeft[i] = (dataLeft[i] >> 1 & 0x7F7F7F | 0xFF808080) - (dataRight[i] >> 1 & 0x7F7F7F);
    compare.getRaster().setDataElements(0, 0, width, height, dataLeft);
    int thumbHeight = imageHeight / IMGDIV;
    int thumbWidth = width * thumbHeight / height;
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    //graphics.drawImage(imageLeft, imageWidth - thumbWidth * 3, height, thumbWidth, thumbHeight, null);
    //graphics.drawImage(imageRight, imageWidth - thumbWidth * 2, height, thumbWidth, thumbHeight, null);
    graphics.drawImage(compare, imageWidth - thumbWidth, height, thumbWidth, thumbHeight, null);
  }

  @Override
  public void close() {
    engineRight.close();
    engineLeft.close();
  }

  private static class NodeDual implements Node {

    protected final Node nodeLeft;
    protected final Node nodeRight;

    public NodeDual(Node nodeLeft, Node nodeRight) {
      this.nodeLeft = nodeLeft;
      this.nodeRight = nodeRight;
    }

    @Override
    public NodeDual translation(double x, double y, double z) {
      nodeLeft.translation(x, y, z);
      nodeRight.translation(x, y, z);
      return this;
    }

    @Override
    public NodeDual rotation(double y, double p, double r) {
      nodeLeft.rotation(y, p, r);
      nodeRight.rotation(y, p, r);
      return this;
    }

    @Override
    public NodeDual setPivot() {
      nodeLeft.setPivot();
      nodeRight.setPivot();
      return this;
    }

    @Override
    public NodeDual connect(Node node) {
      NodeDual nodeDual = (NodeDual) node;
      nodeLeft.connect(nodeDual.nodeLeft);
      nodeRight.connect(nodeDual.nodeRight);
      return this;
    }
  }

  private static class ShapeDual extends NodeDual implements Shape {

    public ShapeDual(Shape shapeLeft, Shape shapeRight) {
      super(shapeLeft, shapeRight);
    }

  }

  private static class GroupDual extends NodeDual implements Group {

    public GroupDual(Group groupLeft, Group groupRight) {
      super(groupLeft, groupRight);
    }

  }

  private static class LightDual extends NodeDual implements Light {
    public LightDual(Light lightLeft, Light lightRight) {
      super(lightLeft, lightRight);
    }

    @Override
    public LightDual setColor(int color) {
      ((Light) this.nodeLeft).setColor(color);
      ((Light) this.nodeRight).setColor(color);
      return this;
    }
  }

}
