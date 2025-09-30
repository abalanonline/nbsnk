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
  private final EngineNbs engineNbs;
  private final EngineFx engineFx;
  private int imageWidth;
  private int imageHeight;
  private BufferedImage image;
  private BufferedImage imageNbs;
  private BufferedImage imageFx;

  public EngineDual() {
    this.engineNbs = new EngineNbs();
    this.engineFx = new EngineFx();
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
    this.imageNbs = new BufferedImage(imageWidth / 2, height, BufferedImage.TYPE_INT_RGB);
    this.imageFx = new BufferedImage(imageWidth / 2, height, BufferedImage.TYPE_INT_RGB);
    engineNbs.open(imageNbs);
    engineFx.open(imageFx);
    return this;
  }

  @Override
  public void background(BufferedImage image) {
    int height = imageHeight - imageHeight / IMGDIV;
    BufferedImage crop = new BufferedImage(imageWidth / 2, height, BufferedImage.TYPE_INT_RGB);
    crop.getGraphics().drawImage(image, 0, 0, null);
    engineNbs.background(crop);
    engineFx.background(crop);
  }

  @Override
  public Shape shape(Obj obj) {
    return new ShapeDual(engineNbs.shape(obj), engineFx.shape(obj));
  }

  @Override
  public void update() {
    engineNbs.update();
    engineFx.update();
    Graphics2D graphics = image.createGraphics();
    int width = imageWidth / 2;
    int height = imageHeight - imageHeight / IMGDIV;
    graphics.drawImage(imageNbs, 0, 0, null);
    graphics.drawImage(imageFx, width, 0, null);
    // comparison takes about 7%
    BufferedImage compare = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    int[] dataNbs = new int[width * height];
    imageNbs.getRaster().getDataElements(0, 0, width, height, dataNbs);
    int[] dataFx = new int[width * height];
    imageFx.getRaster().getDataElements(0, 0, width, height, dataFx);
    for (int i = 0; i < width * height; i++) dataNbs[i] = (dataNbs[i] >> 1 & 0x7F7F7F | 0xFF808080) - (dataFx[i] >> 1 & 0x7F7F7F);
    compare.getRaster().setDataElements(0, 0, width, height, dataNbs);
    int thumbHeight = imageHeight / IMGDIV;
    int thumbWidth = width * thumbHeight / height;
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    //graphics.drawImage(imageNbs, imageWidth - thumbWidth * 3, height, thumbWidth, thumbHeight, null);
    //graphics.drawImage(imageFx, imageWidth - thumbWidth * 2, height, thumbWidth, thumbHeight, null);
    graphics.drawImage(compare, imageWidth - thumbWidth, height, thumbWidth, thumbHeight, null);
  }

  @Override
  public void close() {
    engineFx.close();
    engineNbs.close();
  }

  private static class ShapeDual implements Shape {

    private final Shape shapeNbs;
    private final Shape shapeFx;

    public ShapeDual(Shape shapeNbs, Shape shapeFx) {
      this.shapeNbs = shapeNbs;
      this.shapeFx = shapeFx;
    }

    @Override
    public void translation(double x, double y, double z) {
      shapeNbs.translation(x, y, z);
      shapeFx.translation(x, y, z);
    }

    @Override
    public void rotation(double z) {
      shapeNbs.rotation(z);
      shapeFx.rotation(z);
    }

    @Override
    public void connect(Shape shape) {
      ShapeDual shapeDual = (ShapeDual) shape;
      shapeNbs.connect(shapeDual.shapeNbs);
      shapeFx.connect(shapeDual.shapeFx);
    }
  }
}
