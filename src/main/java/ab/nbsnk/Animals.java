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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Random;

// FIXME: 2025-10-10 create a package for the game
public class Animals {

  public static String fileBase64(String file) {
    try {
      return Base64.getMimeEncoder().encodeToString(Files.readAllBytes(Paths.get(file)));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static BufferedImage imageBase64(String s) {
    try {
      return ImageIO.read(new ByteArrayInputStream(Base64.getMimeDecoder().decode(s)));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static BufferedImage scaleImage(int v, BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    BufferedImage scaled = new BufferedImage(width * v, height * v, BufferedImage.TYPE_INT_RGB);
    scaled.getGraphics().drawImage(image, 0, 0, width * v, height * v, null);
    return scaled;
  }

  public static class Pig extends Obj { // 3D Pig
    public Pig() {
      //System.out.println(Sketch2.obj("assets/pig1/pig1.obj").scale(4/0.1016).ry90().ry90().translate(0, 8, 0).toCode());
      //System.out.println(fileBase64("assets/pig1/pig1.png"));
      this.face = new int[]{0, 0, 0, 2, 0, 1, 3, 0, 2, 0, 0, 0, 3, 0, 2, 1, 0, 3, 4, 1, 4, 5, 1, 0, 7, 1, 1,
          4, 1, 4, 7, 1, 1, 6, 1, 5, 0, 2, 6, 1, 2, 7, 5, 2, 0, 0, 2, 6, 5, 2, 0, 4, 2, 4,
          1, 3, 7, 3, 3, 8, 7, 3, 3, 1, 3, 7, 7, 3, 3, 5, 3, 0, 3, 4, 8, 2, 4, 9, 6, 4, 10,
          3, 4, 8, 6, 4, 10, 7, 4, 3, 2, 5, 11, 0, 5, 6, 4, 5, 4, 2, 5, 11, 4, 5, 4, 6, 5, 12,
          8, 0, 13, 10, 0, 14, 11, 0, 15, 8, 0, 13, 11, 0, 15, 9, 0, 16, 12, 1, 17, 13, 1, 18, 15, 1, 19,
          12, 1, 17, 15, 1, 19, 14, 1, 20, 8, 2, 13, 9, 2, 16, 13, 2, 21, 8, 2, 13, 13, 2, 21, 12, 2, 22,
          9, 3, 16, 11, 3, 15, 15, 3, 19, 9, 3, 16, 15, 3, 19, 13, 3, 18, 11, 4, 23, 10, 4, 24, 14, 4, 25,
          11, 4, 23, 14, 4, 25, 15, 4, 26, 10, 5, 14, 8, 5, 13, 12, 5, 27, 10, 5, 14, 12, 5, 27, 14, 5, 28,
          20, 2, 29, 21, 2, 30, 23, 2, 31, 20, 2, 29, 23, 2, 31, 22, 2, 32, 16, 0, 33, 17, 0, 34, 21, 0, 35,
          16, 0, 33, 21, 0, 35, 20, 0, 31, 17, 3, 36, 19, 3, 37, 23, 3, 31, 17, 3, 36, 23, 3, 31, 21, 3, 30,
          19, 1, 33, 18, 1, 38, 22, 1, 32, 19, 1, 33, 22, 1, 32, 23, 1, 31, 18, 5, 39, 16, 5, 40, 20, 5, 29,
          18, 5, 39, 20, 5, 29, 22, 5, 32, 24, 4, 41, 26, 4, 40, 27, 4, 42, 24, 4, 41, 27, 4, 42, 25, 4, 43,
          28, 2, 44, 29, 2, 45, 31, 2, 46, 28, 2, 44, 31, 2, 46, 30, 2, 47, 24, 0, 48, 25, 0, 49, 29, 0, 50,
          24, 0, 48, 29, 0, 50, 28, 0, 51, 25, 3, 43, 27, 3, 42, 31, 3, 46, 25, 3, 43, 31, 3, 46, 29, 3, 45,
          27, 1, 48, 26, 1, 52, 30, 1, 47, 27, 1, 48, 30, 1, 47, 31, 1, 46, 26, 5, 53, 24, 5, 54, 28, 5, 44,
          26, 5, 53, 28, 5, 44, 30, 5, 47, 32, 4, 41, 34, 4, 40, 35, 4, 42, 32, 4, 41, 35, 4, 42, 33, 4, 43,
          36, 2, 44, 37, 2, 45, 39, 2, 46, 36, 2, 44, 39, 2, 46, 38, 2, 47, 32, 0, 48, 33, 0, 49, 37, 0, 50,
          32, 0, 48, 37, 0, 50, 36, 0, 51, 33, 3, 43, 35, 3, 42, 39, 3, 46, 33, 3, 43, 39, 3, 46, 37, 3, 45,
          35, 1, 48, 34, 1, 52, 38, 1, 47, 35, 1, 48, 38, 1, 47, 39, 1, 46, 34, 5, 53, 32, 5, 54, 36, 5, 44,
          34, 5, 53, 36, 5, 44, 38, 5, 47, 40, 4, 41, 42, 4, 40, 43, 4, 42, 40, 4, 41, 43, 4, 42, 41, 4, 43,
          44, 2, 44, 45, 2, 45, 47, 2, 46, 44, 2, 44, 47, 2, 46, 46, 2, 47, 40, 0, 48, 41, 0, 49, 45, 0, 50,
          40, 0, 48, 45, 0, 50, 44, 0, 51, 41, 3, 43, 43, 3, 42, 47, 3, 46, 41, 3, 43, 47, 3, 46, 45, 3, 45,
          43, 1, 48, 42, 1, 52, 46, 1, 47, 43, 1, 48, 46, 1, 47, 47, 1, 46, 42, 5, 53, 40, 5, 54, 44, 5, 44,
          42, 5, 53, 44, 5, 44, 46, 5, 47, 48, 4, 41, 50, 4, 40, 51, 4, 42, 48, 4, 41, 51, 4, 42, 49, 4, 43,
          52, 2, 44, 53, 2, 45, 55, 2, 46, 52, 2, 44, 55, 2, 46, 54, 2, 47, 48, 0, 48, 49, 0, 49, 53, 0, 50,
          48, 0, 48, 53, 0, 50, 52, 0, 51, 49, 3, 43, 51, 3, 42, 55, 3, 46, 49, 3, 43, 55, 3, 46, 53, 3, 45,
          51, 1, 48, 50, 1, 52, 54, 1, 47, 51, 1, 48, 54, 1, 47, 55, 1, 46, 50, 5, 53, 48, 5, 54, 52, 5, 44,
          50, 5, 53, 52, 5, 44, 54, 5, 47, };
      this.vertex = new double[]{
          4, 8, -11, -4, 8, -11, 4, 8, -3, -4, 8, -3, 4, 16, -11, -4, 16, -11, 4, 16, -3, -4, 16, -3,
          5, 6, -5, -5, 6, -5, 5, 6, 11, -5, 6, 11, 5, 14, -5, -5, 14, -5, 5, 14, 11, -5, 14, 11,
          2, 9, -11, -2, 9, -11, 2, 12, -11, -2, 12, -11, 2, 9, -12, -2, 9, -12, 2, 12, -12, -2, 12, -12,
          5, 0, 0, 1, 0, 0, 5, 6, 0, 1, 6, 0, 5, 0, -4, 1, 0, -4, 5, 6, -4, 1, 6, -4,
          -1, 0, 0, -5, 0, 0, -1, 6, 0, -5, 6, 0, -1, 0, -4, -5, 0, -4, -1, 6, -4, -5, 6, -4,
          5, 0, 12, 1, 0, 12, 5, 6, 12, 1, 6, 12, 5, 0, 8, 1, 0, 8, 5, 6, 8, 1, 6, 8,
          -1, 0, 12, -5, 0, 12, -1, 6, 12, -5, 6, 12, -1, 0, 8, -5, 0, 8, -1, 6, 8, -5, 6, 8, };
      this.normal = new double[]{0, -1, 0, 0, 1, 0, 0, 0, -1, -1, 0, 0, 0, 0, 1, 1, 0, 0, };
      this.texture = new double[]{
          0.250000, 0.875000, 0.250000, 1.000000, 0.375000, 1.000000, 0.375000, 0.875000,
          0.125000, 0.875000, 0.125000, 1.000000, 0.125000, 0.750000, 0.250000, 0.750000,
          0.375000, 0.750000, 0.500000, 0.750000, 0.500000, 0.875000, 0.000000, 0.750000,
          0.000000, 0.875000, 0.562500, 0.750000, 0.562500, 0.500000, 0.725000, 0.500000,
          0.725000, 0.750000, 1.000000, 0.750000, 0.850000, 0.750000, 0.850000, 0.500000,
          1.000000, 0.500000, 0.725000, 0.875000, 0.562500, 0.875000, 0.876033, 0.750000,
          0.726033, 0.750000, 0.726044, 0.875000, 0.876033, 0.875000, 0.437491, 0.750000,
          0.437500, 0.500000, 0.265943, 0.687500, 0.328000, 0.687500, 0.328000, 0.734000,
          0.265943, 0.733957, 0.328000, 0.750000, 0.387500, 0.750000, 0.387500, 0.734000,
          0.343000, 0.687500, 0.343000, 0.734000, 0.265943, 0.750000, 0.250000, 0.733957,
          0.250000, 0.687500, 0.250000, 0.600000, 0.187500, 0.687500, 0.187500, 0.600000,
          0.062500, 0.600000, 0.125000, 0.600000, 0.125000, 0.687500, 0.062500, 0.687500,
          0.125000, 0.750008, 0.187500, 0.750008, 0.187500, 0.687508, 0.125000, 0.687508,
          0.062500, 0.750000, 0.000000, 0.687500, 0.000000, 0.600000, };
      this.image = scaleImage(16, imageBase64(
          "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAIAAAAlC+aJAAAAAXNSR0IB2cksfwAAAARnQU1BAACx\n" +
          "jwv8YQUAAAAgY0hSTQAAeiYAAICEAAD6AAAAgOgAAHUwAADqYAAAOpgAABdwnLpRPAAAC0JJREFU\n" +
          "aN7t2MvrZVdWB/DvWmvvfc659/5u/R71SKqqkzaPFrEnkUb8JwTbZKYoKCi2gtDg2EbEidAgSNMD\n" +
          "J9LSaOPAx8BBgzoT09iKDxLUrlTFpCr1/D3uveec/VhrOYgdUk2qDTQxEX5res5lnQ9r373W3nT7\n" +
          "S7+JD4p47Vl2cipQJ4kQcnDoOvUMEWIR42IWRKwU9jq9cQsfR/CTHzQOigqouzZEcmu7s3eoMWb3\n" +
          "MQMQZsBZWCs+rngiwPtQpyqRpnkDFs9KrVluFlwGohSaKpGgNRZGoE8cACfz/OiOUlOrMoT54UMI\n" +
          "sUIccFLm0AVYBYmpscknrwIdeBiw7MVdp1ZLkTgo1Nx0Us8VTizs3qqoW/nEAcSk1erHRXdZSGlI\n" +
          "dGFYLi9jdrgnDgZDDEKcPEoXPy5AePITkkUX+tAf7GER4k50s3OqFlLoeloENmpQGGDmVT95ABaY\n" +
          "65y7xRoeLAVU42HJQoDb2Ewbh8CrTk9nCd3HBuAuSp+Iw8mbd4aDRb/atyBQWCuaG4agRqToVheo\n" +
          "giKD3EuzjkVVd9nTgOitofv0s57VrSIFgZnJ9OBed3TQbt99L9nw/HN6tlU32hZLIZTcLIcu6qxe\n" +
          "VIZB2yhxKGcbTkTeS4pIqrtZ+q5O8wcDdBonndrZPKyH2O9vjx+gmfE6XdkTJemSBfiU096S9nqQ\n" +
          "w3zcPGxno4/NStkLV9CIhLm17d3b3cXD6cbb/NRhOz3zqkPdf3+yVgs5kQdaC+VMIdA4a3JJSbmo\n" +
          "TYLYTs54FQNFNTNXz27OIvbECmimOo4Xjg6zLP7h1lsS5HMvPgOJRbV/9joiw4iH6I28FF4kJ+/6\n" +
          "9Y27Z6S7z1y8vL19Wyj2+xcVPrv2Bq11z7uxerxylYf4+I4RrIOZdQyXhc4jHa55VmU1ddTS6iQh\n" +
          "am1/f+umpPTSi8+IEZgx+xNXeuoT58ouy0EAvPqv3/7H228BuPqFXz38mZ/zXaWpUBIKvvz8ywc/\n" +
          "+Qo2+bVbb33r7/42z0Dg5cWn+6tPy1J4HTsWiND+UgfxPsaU8vH4/mSmJa57kLcKK1U4oSoNIkZk\n" +
          "Fha9KdDh1Ru3PvfSD790/fq3X3+jbU3Ym7cnAkrx4alLas3n6m5gxty06L2vfPXkD79GRB5JTzcA\n" +
          "pr/6y7tf/xoCWykEiLiVAm3Y7fRsxkmbdwUCq4pNkRh0O7E9tjuF1QLN+/Uq7C0cjOQggrqWWsnQ\n" +
          "dxwjALj7o923btz4sWeuo2vtdAp99+QKpLC7f//knQeF1VRzrS995jlZsKwSRLEICJGHhTdyo24Y\n" +
          "EOVHP/u8uku0EJI3dYKLzzwdXL+Era+vPlU8z6dbiMYrq/cn09Nd3Yw6FyWnBekMMNWxWgwpJr13\n" +
          "BiHdZiIypJ/4kee9VGwqhtAejk8C0L//2hdCHxfrI+wF2jQalkQke3tWGkS4DwBoOZAZ9vZcdffm\n" +
          "DZ11feWpxhpmfXTr1v61S7UYVwZqE4pggLVm7qOnoHfuv5esDovl4YEQaVXPs/WJ5slLDRw8ejve\n" +
          "emkShPrw6o033d1a+/EXnpMkTcnLBzd7eue3f4dUZdGrVsREDiQhsOwt1FhSpBS8S8jKkYk4n+xC\n" +
          "R14rKtiqaqOs6irs2kfs1Khao7iMTISi+fjkvWTd4b41R0ggEoZy80lRtdWJnAJQFDF1Fme0aHPh\n" +
          "yepaIkHMqn7w/zhYywB0twXQDb3mBkq+Lb//F3/8Kz/9yh98409+6Rd+fvqnf3nsN888SwDtDWTd\n" +
          "a6+9EVLqntnn+5vLdx78z7ocFu6EkOrsYbV0UzfKOctmRxJqswgtXdLWYiCvjc3Rmq6TlhyzWVay\n" +
          "ShSwDH3X6bYpMnU9mknqZGBVFQRVoNXHOnFVZXUbG/fi7gRyIi35/e8Mn37erl6Nw6DuIPps1unm\n" +
          "d3Bj93h3JBj8ZBcPF/qgGFNcDGER22bUOqW0qrVKQyCzXbNeuJmDcTb1Im3cwYGYRGqtBLfqPKwX\n" +
          "8zan5UoEejLLeqG5IgnF8NgwR8WdHKwCcwCkcBd7fNwQ/aPf+7K+cYPu3P7TL/8u5AOmoLKdt2+f\n" +
          "Pnz7rXrnuOUS1B7953+0RxuUd9dM9c1W286lQpxLk04QTDhu37kLJmO456pIexcQADQgDJf2w9Bh\n" +
          "lSxisuLJtnfvYlg+DkhE647ZDTEwWzEA/j1dUOVnf/2L8kPP+dNXX/nib0A/4CQQBf2lvo1Vho57\n" +
          "gMy06Tx7NWb17TZyR82kRjRlEq867SbVolVBxJ0c379H1GCTsAz7K225bnfalCrJMCRmGjosOsxz\n" +
          "iOuFsnB2RJG+x7Sr1birv/zyT2Fuv/jy51Fb9+ynAMFcPMp08zu4+Z38fQcsVeB0jgdL3c0nD+8d\n" +
          "XbtWZ6cj6PFJZe9WA7JCzUmUrD06SUO3lIieKzWrFSIJbJmwJs0IK4IJ+mF89HC4dJGi7B4+XOII\n" +
          "aEKRdbR2OiICy6B5VA/sQHNUlQvLVhQK3c2eM6/T9zSmJwbVCSWUwoFC0zbPXVLJHfWcNOhpPblz\n" +
          "06qXcR7/600O0cnApg9yqGAK6WCJLkjPAEkETUQeYzO1FqRzJTcmAKN7p4wyByYNhLFwYC+jk8gi\n" +
          "ALDaggRlxPVKFh2qWmkf5vs9yFKSTpO3ec7Zc66b0X3r5M5gtqMXnrc2RbH+4BLKpEwN0MG3OY/j\n" +
          "FgXIJkOad1WFi6pEybUGDWi5MYckANCRTc581BmYtqVtxradsrvHojt1Nx1Lg6Mpmuk46Zw/7OFd\n" +
          "0fJssDYWatVJXC3vZimpaJXFen77odfqwumgNyRsaxgbn07rRb9gasejByqjhoMkzBR9znMcklNt\n" +
          "mymm5rmamqhz51webeZHj8pu5636difbye9vcptEG5MlZptz3mykjxDxD1cB5Eou0oBK3tTPiqDJ\n" +
          "AmW3a+7t7OTs3tuo1tr44J9fj7E192zwGLVWj5xjK6Wy5rBpus1m5mZcspuBoLkhMkvsn37alDga\n" +
          "9eshkEb1L33zm+T4rb/56ziVlqs31HmkFITpq1//Blv4yp//2YcCuDHzbpo8l6OLl308LZvsU+OF\n" +
          "DQPEfH35EvciQYbDa3muwlWkmSpigKRFiEliEMLAJpCGOLAWqm5gjk7u6lNlVTSiJ93M/SBBc67c\n" +
          "xptvrY4OkBZC9f6ttw9W63BhOe5yQkCybL48GLR1GHdhGIhZHdvTB6v1IZxOzk4Prl+nuVgI6eJe\n" +
          "nhr6WB89WAwXakeRSLCwVs2ZP4pzqjx3kUoQCeNUdXtWmh9eu0xX9lTD8tIFg9+9+WZXrZ3p8ev/\n" +
          "hmZtym03jZvRJFSJ4cpht0ghQPaHuN/pbu4WMYxV54pBeurmCbQkxMCL73Oo/wFC3zhOKY5JlosV\n" +
          "eYOzL5akhoXTSLIMV154EVl1M+6/8LwMrKcWYgTp4cUjrWbjuLxwWZuiACzUs55mRGc2zobEfWg6\n" +
          "Fek7GD6SCria1nn/U1fjXheePkCUGCWsVhiLs/pUQ1HLWQbG8abdm0GkxMthoaVZYieRLkjoIA4v\n" +
          "EokDydD360P15jwjRM/atjuN/JFUwAZhI92orINuZiKUkyJJ0aeWjZiVHInIkxwlbTMcEBQtoe9l\n" +
          "6CWEerxjgg3gjDq7No1b4sCAY1RIArQtut4/mgqIBYKjIy0c9lYS++6p/W2rhC70jJDgFLoVUiBn\n" +
          "XFi7kSmlLqX1QizUuaW9Hm5NA1IksX6I4mbZUUwNVguSiGF2+0gq4OJAggBAK4YUUWy1vw/A0PHi\n" +
          "u5sVYO9ebh71AByo2wxkACUDTKHWdwfJgvLdDgmgArAy/W+30/9P4hxwDjgHnAPOAeeAc8A54Bxw\n" +
          "DjgHnAPOAeeAc8A54BxwDjgHnAPOAeeA//v4b+bUPCrJ+fMEAAAAAElFTkSuQmCC\n"));
      scale(1.0 / 16.0);
    }
  }

  public static class Sheep extends Obj { // static classes help overcome the size limit
    public Sheep() {
      //System.out.println(Sketch2.obj("assets/sheep2/sheep2.obj").scale(6 / 0.152400).ry90().ry90().translate(0, 12.575, 1.0238976).toCode());
      //System.out.println(fileBase64("assets/sheep2/sheep2.png"));
      this.face = new int[]{
          0, 0, 0, 2, 0, 1, 3, 0, 2, 0, 0, 0, 3, 0, 2, 1, 0, 3, 4, 1, 4, 5, 1, 3, 7, 1, 2,
          4, 1, 4, 7, 1, 2, 6, 1, 5, 0, 2, 6, 1, 2, 7, 5, 2, 8, 0, 2, 6, 5, 2, 8, 4, 2, 4,
          1, 3, 7, 3, 3, 9, 7, 3, 10, 1, 3, 7, 7, 3, 10, 5, 3, 8, 3, 4, 9, 2, 4, 11, 6, 4, 12,
          3, 4, 9, 6, 4, 12, 7, 4, 10, 2, 5, 13, 0, 5, 6, 4, 5, 4, 2, 5, 13, 4, 5, 4, 6, 5, 14,
          8, 0, 15, 10, 0, 16, 11, 0, 17, 8, 0, 15, 11, 0, 17, 9, 0, 18, 12, 1, 19, 13, 1, 20, 15, 1, 21,
          12, 1, 19, 15, 1, 21, 14, 1, 22, 8, 2, 23, 9, 2, 24, 13, 2, 25, 8, 2, 23, 13, 2, 25, 12, 2, 26,
          9, 3, 18, 11, 3, 17, 15, 3, 26, 9, 3, 18, 15, 3, 26, 13, 3, 27, 11, 4, 23, 10, 4, 28, 14, 4, 22,
          11, 4, 23, 14, 4, 22, 15, 4, 26, 10, 5, 29, 8, 5, 30, 12, 5, 19, 10, 5, 29, 12, 5, 19, 14, 5, 22,
          16, 0, 31, 18, 0, 32, 19, 0, 33, 16, 0, 31, 19, 0, 33, 17, 0, 34, 20, 1, 35, 21, 1, 36, 23, 1, 37,
          20, 1, 35, 23, 1, 37, 22, 1, 38, 16, 2, 39, 17, 2, 40, 21, 2, 33, 16, 2, 39, 21, 2, 33, 20, 2, 37,
          17, 3, 34, 19, 3, 33, 23, 3, 37, 17, 3, 34, 23, 3, 37, 21, 3, 36, 19, 4, 39, 18, 4, 41, 22, 4, 38,
          19, 4, 39, 22, 4, 38, 23, 4, 37, 18, 5, 42, 16, 5, 43, 20, 5, 35, 18, 5, 42, 20, 5, 35, 22, 5, 38,
          24, 0, 31, 26, 0, 32, 27, 0, 33, 24, 0, 31, 27, 0, 33, 25, 0, 34, 28, 1, 35, 29, 1, 36, 31, 1, 37,
          28, 1, 35, 31, 1, 37, 30, 1, 38, 24, 2, 39, 25, 2, 40, 29, 2, 33, 24, 2, 39, 29, 2, 33, 28, 2, 37,
          25, 3, 34, 27, 3, 33, 31, 3, 37, 25, 3, 34, 31, 3, 37, 29, 3, 36, 27, 4, 39, 26, 4, 41, 30, 4, 38,
          27, 4, 39, 30, 4, 38, 31, 4, 37, 26, 5, 42, 24, 5, 43, 28, 5, 35, 26, 5, 42, 28, 5, 35, 30, 5, 38,
          32, 0, 31, 34, 0, 32, 35, 0, 33, 32, 0, 31, 35, 0, 33, 33, 0, 34, 36, 1, 35, 37, 1, 36, 39, 1, 37,
          36, 1, 35, 39, 1, 37, 38, 1, 38, 32, 2, 39, 33, 2, 40, 37, 2, 33, 32, 2, 39, 37, 2, 33, 36, 2, 37,
          33, 3, 34, 35, 3, 33, 39, 3, 37, 33, 3, 34, 39, 3, 37, 37, 3, 36, 35, 4, 39, 34, 4, 41, 38, 4, 38,
          35, 4, 39, 38, 4, 38, 39, 4, 37, 34, 5, 42, 32, 5, 43, 36, 5, 35, 34, 5, 42, 36, 5, 35, 38, 5, 38,
          40, 0, 31, 42, 0, 32, 43, 0, 33, 40, 0, 31, 43, 0, 33, 41, 0, 34, 44, 1, 35, 45, 1, 36, 47, 1, 37,
          44, 1, 35, 47, 1, 37, 46, 1, 38, 40, 2, 39, 41, 2, 40, 45, 2, 33, 40, 2, 39, 45, 2, 33, 44, 2, 37,
          41, 3, 34, 43, 3, 33, 47, 3, 37, 41, 3, 34, 47, 3, 37, 45, 3, 36, 43, 4, 39, 42, 4, 41, 46, 4, 38,
          43, 4, 39, 46, 4, 38, 47, 4, 37, 42, 5, 42, 40, 5, 43, 44, 5, 35, 42, 5, 42, 44, 5, 35, 46, 5, 38,
          48, 6, 44, 50, 6, 45, 51, 6, 46, 48, 6, 44, 51, 6, 46, 49, 6, 47, 52, 7, 48, 53, 7, 47, 55, 7, 46,
          52, 7, 48, 55, 7, 46, 54, 7, 49, 48, 8, 50, 49, 8, 51, 53, 8, 52, 48, 8, 50, 53, 8, 52, 52, 8, 48,
          49, 9, 51, 51, 9, 53, 55, 9, 54, 49, 9, 51, 55, 9, 54, 53, 9, 52, 51, 10, 53, 50, 10, 55, 54, 10, 56,
          51, 10, 53, 54, 10, 56, 55, 10, 54, 50, 11, 57, 48, 11, 50, 52, 11, 48, 50, 11, 57, 52, 11, 48, 54, 11, 58,
          56, 6, 59, 58, 6, 60, 59, 6, 61, 56, 6, 59, 59, 6, 61, 57, 6, 62, 60, 7, 63, 61, 7, 64, 63, 7, 65,
          60, 7, 63, 63, 7, 65, 62, 7, 66, 56, 8, 67, 57, 8, 68, 61, 8, 61, 56, 8, 67, 61, 8, 61, 60, 8, 65,
          57, 9, 62, 59, 9, 61, 63, 9, 65, 57, 9, 62, 63, 9, 65, 61, 9, 64, 59, 10, 67, 58, 10, 69, 62, 10, 66,
          59, 10, 67, 62, 10, 66, 63, 10, 65, 58, 11, 70, 56, 11, 71, 60, 11, 63, 58, 11, 70, 60, 11, 63, 62, 11, 66,
          64, 6, 72, 66, 6, 73, 67, 6, 74, 64, 6, 72, 67, 6, 74, 65, 6, 75, 68, 7, 76, 69, 7, 77, 71, 7, 78,
          68, 7, 76, 71, 7, 78, 70, 7, 79, 64, 8, 80, 65, 8, 81, 69, 8, 74, 64, 8, 80, 69, 8, 74, 68, 8, 78,
          65, 9, 75, 67, 9, 74, 71, 9, 78, 65, 9, 75, 71, 9, 78, 69, 9, 77, 67, 10, 80, 66, 10, 82, 70, 10, 79,
          67, 10, 80, 70, 10, 79, 71, 10, 78, 66, 11, 83, 64, 11, 84, 68, 11, 76, 66, 11, 83, 68, 11, 76, 70, 11, 79,
          72, 6, 72, 74, 6, 73, 75, 6, 74, 72, 6, 72, 75, 6, 74, 73, 6, 75, 76, 7, 76, 77, 7, 77, 79, 7, 78,
          76, 7, 76, 79, 7, 78, 78, 7, 79, 72, 8, 80, 73, 8, 81, 77, 8, 74, 72, 8, 80, 77, 8, 74, 76, 8, 78,
          73, 9, 75, 75, 9, 74, 79, 9, 78, 73, 9, 75, 79, 9, 78, 77, 9, 77, 75, 10, 80, 74, 10, 82, 78, 10, 79,
          75, 10, 80, 78, 10, 79, 79, 10, 78, 74, 11, 83, 72, 11, 84, 76, 11, 76, 74, 11, 83, 76, 11, 76, 78, 11, 79,
          80, 6, 72, 82, 6, 73, 83, 6, 74, 80, 6, 72, 83, 6, 74, 81, 6, 75, 84, 7, 76, 85, 7, 77, 87, 7, 78,
          84, 7, 76, 87, 7, 78, 86, 7, 79, 80, 8, 80, 81, 8, 81, 85, 8, 74, 80, 8, 80, 85, 8, 74, 84, 8, 78,
          81, 9, 75, 83, 9, 74, 87, 9, 78, 81, 9, 75, 87, 9, 78, 85, 9, 77, 83, 10, 80, 82, 10, 82, 86, 10, 79,
          83, 10, 80, 86, 10, 79, 87, 10, 78, 82, 11, 83, 80, 11, 84, 84, 11, 76, 82, 11, 83, 84, 11, 76, 86, 11, 79,
          88, 6, 72, 90, 6, 73, 91, 6, 74, 88, 6, 72, 91, 6, 74, 89, 6, 75, 92, 7, 76, 93, 7, 77, 95, 7, 78,
          92, 7, 76, 95, 7, 78, 94, 7, 79, 88, 8, 80, 89, 8, 81, 93, 8, 74, 88, 8, 80, 93, 8, 74, 92, 8, 78,
          89, 9, 75, 91, 9, 74, 95, 9, 78, 89, 9, 75, 95, 9, 78, 93, 9, 77, 91, 10, 80, 90, 10, 82, 94, 10, 79,
          91, 10, 80, 94, 10, 79, 95, 10, 78, 90, 11, 83, 88, 11, 84, 92, 11, 76, 90, 11, 83, 92, 11, 76, 94, 11, 79,
      };
      this.vertex = new double[]{
          4, 12, 11, -4, 12, 11, 4, 18, 11, -4, 18, 11, 4, 12, -5, -4, 12, -5, 4, 18, -5, -4, 18, -5,
          3, 16, -3, -3, 16, -3, 3, 22, -3, -3, 22, -3, 3, 16, -11, -3, 16, -11, 3, 22, -11, -3, 22, -11,
          5, 0, 0, 1, 0, 0, 5, 12, 0, 1, 12, 0, 5, 0, -4, 1, 0, -4, 5, 12, -4, 1, 12, -4,
          -1, 0, 0, -5, 0, 0, -1, 12, 0, -5, 12, 0, -1, 0, -4, -5, 0, -4, -1, 12, -4, -5, 12, -4,
          5, 0, 12, 1, 0, 12, 5, 12, 12, 1, 12, 12, 5, 0, 8, 1, 0, 8, 5, 12, 8, 1, 12, 8,
          -1, 0, 12, -5, 0, 12, -1, 12, 12, -5, 12, 12, -1, 0, 8, -5, 0, 8, -1, 12, 8, -5, 12, 8,
          6, 10.5, 12.643622, -6, 10.5, 12.643622, 6, 19.5, 12.643622, -6, 19.5, 12.643622,
          6, 10.5, -6.643661, -6, 10.5, -6.643661, 6, 19.5, -6.643661, -6, 19.5, -6.643661,
          3.3, 15.7, -2.948032, -3.3, 15.7, -2.948032, 3.3, 22.3, -2.948032, -3.3, 22.3, -2.948032,
          3.3, 15.7, -9.548032, -3.3, 15.7, -9.548032, 3.3, 22.3, -9.548032, -3.3, 22.3, -9.548032,
          5.5, 6, 0.5, 0.5, 6, 0.5, 5.5, 13.5, 0.5, 0.5, 13.5, 0.5,
          5.5, 6, -4.5, 0.5, 6, -4.5, 5.5, 13.5, -4.5, 0.5, 13.5, -4.5,
          -0.5, 6, 0.5, -5.5, 6, 0.5, -0.5, 13.5, 0.5, -5.5, 13.5, 0.5,
          -0.5, 6, -4.5, -5.5, 6, -4.5, -0.5, 13.5, -4.5, -5.5, 13.5, -4.5,
          5.5, 6, 12.5, 0.5, 6, 12.5, 5.5, 13.5, 12.5, 0.5, 13.5, 12.5,
          5.5, 6, 7.5, 0.5, 6, 7.5, 5.5, 13.5, 7.5, 0.5, 13.5, 7.5,
          -0.5, 6, 12.5, -5.5, 6, 12.5, -0.5, 13.5, 12.5, -5.5, 13.5, 12.5,
          -0.5, 6, 7.5, -5.5, 6, 7.5, -0.5, 13.5, 7.5, -5.5, 13.5, 7.5, };
      this.normal = new double[]{
          0, 0, 1, 0, 0, -1, 0, -1, 0, -1, 0, 0, 0, 1, 0, 1, 0, 0,
          0, 0, 1, 0, 0, -1, 0, -1, 0, -1, 0, 0, 0, 1, 0, 1, 0, 0, };
      this.texture = new double[]{
          0.812500, 0.781437, 0.812500, 0.875000, 0.671628, 0.875000, 0.671720, 0.781437,
          0.531204, 0.781332, 0.531204, 0.875000, 0.530951, 0.531736, 0.656110, 0.531689,
          0.656363, 0.781286, 0.750000, 0.531689, 0.750253, 0.781286, 0.875000, 0.531689,
          0.875000, 0.781286, 0.437500, 0.531689, 0.437500, 0.781286, 0.437479, 0.781426,
          0.437479, 0.875000, 0.343760, 0.875000, 0.343760, 0.781426, 0.125000, 0.781426,
          0.218760, 0.781426, 0.218760, 0.875000, 0.125000, 0.875000, 0.218760, 1.000000,
          0.312576, 1.000000, 0.312691, 0.875000, 0.218760, 0.875000, 0.218760, 0.781426,
          0.125000, 1.000000, 0.000000, 0.875000, 0.000000, 0.781426, 0.250000, 0.500000,
          0.250000, 0.687500, 0.187500, 0.687500, 0.187500, 0.500000, 0.062500, 0.500000,
          0.125000, 0.500000, 0.125000, 0.687500, 0.062500, 0.687500, 0.125000, 0.750000,
          0.187500, 0.750000, 0.062500, 0.750000, 0.000000, 0.687500, 0.000000, 0.500000,
          0.812500, 0.281437, 0.812500, 0.375000, 0.671628, 0.375000, 0.671720, 0.281437,
          0.531204, 0.281332, 0.531204, 0.375000, 0.530951, 0.031736, 0.656110, 0.031689,
          0.656363, 0.281286, 0.750000, 0.031689, 0.750253, 0.281286, 0.875000, 0.031689,
          0.875000, 0.281286, 0.437500, 0.031689, 0.437500, 0.281286, 0.375356, 0.312102,
          0.375356, 0.405676, 0.281447, 0.405676, 0.281447, 0.312102, 0.093947, 0.312102,
          0.187707, 0.312102, 0.187707, 0.405676, 0.093947, 0.405676, 0.187631, 0.500000,
          0.281447, 0.500000, 0.093871, 0.500000, 0.000342, 0.405676, 0.000342, 0.312102,
          0.250000, 0.081087, 0.250000, 0.187500, 0.187500, 0.187500, 0.187500, 0.081087,
          0.062500, 0.081087, 0.125000, 0.081087, 0.125000, 0.187500, 0.062500, 0.187500,
          0.125000, 0.250000, 0.187500, 0.250000, 0.062500, 0.250000, 0.000000, 0.187500,
          0.000000, 0.081087, };
      this.image = scaleImage(16, imageBase64(
          "iVBORw0KGgoAAAANSUhEUgAAAEAAAABACAIAAAAlC+aJAAAAAXNSR0IB2cksfwAAAARnQU1BAACx\n" +
              "jwv8YQUAAAAgY0hSTQAAeiYAAICEAAD6AAAAgOgAAHUwAADqYAAAOpgAABdwnLpRPAAAEP1JREFU\n" +
              "aN7tWmuwnWV1fu/fbX97n733yTmEgAYE5BIhyiVAI2QIkYigYixMa1BnvJTOWOt0qtUZ22mt7Z+O\n" +
              "t5nCVCyjODpWFNAo5SomiOESuQQhRCEXSDi57Pve3+29fv2x7C6NOScbHCWd6fvrnL2/2e+73rXW\n" +
              "s571rA+32210uAWfK6UwxpTSNE2ttZTSbrdbr9er1SohpNFooNd6sfm+4Jz7vo8QStM0z/MgCIbD\n" +
              "YZIkcRxXKhUhBMYYHQWLoP/ja14DwjD0PG84HFar1SAI8jzPsowQwjlHCGmt54u9o8WAXq8npZya\n" +
              "msqybDAYMMaCIGg0GkEQTE1NSSkrlcr/h9Dv04AoiqSU1lqEkOd51WoV/maMtdttz/MopUeDAfOi\n" +
              "EGOMUso57/V6zrlut0sppZR6nocQKsuSkKPCe2zXrl1hGBJCarWa1hqi3/O8siwRQqPRyDkHjxZF\n" +
              "4Xmec873/UajceDAgYMHD3LOIUMgxSmlQoglS5Ycss3c3Fy++wnu+QihfufgGauv3vHgBq1VdNKK\n" +
              "9Nebe71eFEUyT51zXHilszJPT1v7wenp6SMbUKvVnHNwx6ft2rV1586kqafOfNvuTd9ljBlrGaWY\n" +
              "eVmazJ5z+dzDPzhZvO6UE07Ymuecc8/zkiRhjDHG6vW6lFJKKYT47W2G239OuLB55rQ64YIrdmz+\n" +
              "sdbKGNPdem/jrDX66Z86q4+/8N2tJ+8LAr/faVHGJgxRopSCO6aUfuWn3+Ocj4ZDzvnVn/zC26/7\n" +
              "e6dVMhys/fBn1n3inxljuiistf/yo68HQRBFkbV2enp6ZmYG4k1K2Wg0IMYOWVqrTutguPTNzjmM\n" +
              "sdPquHPXLjnnsiQr+s9s4n6IKVdKcSGccyXC3AufvesbkxhAP/axjxljRqOR7/snvWUlWbo02bu9\n" +
              "ctypS1l/79YHtMyN1r984I7z33TKCWddkO59duriK04//5IgCBBCzrlKpaK1ds4xxoqigOrB2KGp\n" +
              "9dK2LYHv1V93WtHa3dq1DWOc7X8+2bezMb1IeEFcm0KEytYLhFJZFL7vJ8Oe0Xrp8pVH9kBZllJK\n" +
              "cEKtVqvVapUzVpdlufbDnz7YS65Z/o4/PfvKA/3sPR//XBRFJ615f57nSZJYa7Ms45xba40x1to0\n" +
              "TcGk3z49QujMyz+gZWGMQaWbqlUJIc0zV5dl6S09W2vd67SrJ6/gjDrn8iwd9trHnv/uw/7OYXLA\n" +
              "WosxjuO4KIputwuHyLJMa62UevqFFyAkMMacc+fc3Nzc9PT0/v37nXNlWVprtdbGmCRJMMbGmMNu\n" +
              "nGUZ5d7+LXcgTBpvuqT/i7sOPPkTzw/yHY8QSmqnXtR6aiMqLaFs6cqrXnp4w9xDtwkvmMQA/Ptg\n" +
              "BPff9Pk/Wv/Jh7/zRc/z33DpesjvnQ/+QMuCECKlrNabzrk8L4479zJjDEKo89T9ZVmevOq9Ozbe\n" +
              "IqV0RqVpGtfqYRgmwz5CaMXVH39ldeB3Wc7oO2/424s+8GnP856565svCt8aI4RApTO2rFRrTHiU\n" +
              "0mPPftveLXdba6zRgtGiyJ++8+YiGZZlGVRihHHltIsJIWrLhixJJ6rEu3fvbrfbc3NzrVbruvXr\n" +
              "Hv/h1x7+3vXXrV/3Sg249M/+oV5vWGsPHjxojBFCMEqSQde6kjGulSrSROb5S7+4hxA8ddpK7Mxo\n" +
              "0JNSnrH2/dYaxtio30NG79t869xDt5162Qc5oxMZAAUhz/PRaASf5Hn+Kjzw+A+/NrP4WGvt4JmN\n" +
              "i96ylhJSPf3i+llvgwZjyXmXE4IxKktn3rhqXfuJu5df+aHS2RMv/uNf3PqvqCyVkkYVF67/pGBU\n" +
              "5sWzP77RWTORAcaYwWAghACU5F7w6ijn8is/NBz05x75EcJkuP3nbvGbBts25c9txhgzgvZs/sHi\n" +
              "sy875i1rEELPP3A7E/5D//Hl5qLZPM+58E+/4iMlxqUz1tqlq69lBC1eefXS1ddOxIWq1SrnfMxz\n" +
              "rv/W7a8uB7bddTNhQghRKMOXnmP3bvWD0DmHlpxlXnisvmjxwSd/gjCZPvOSwfbNxhjhh1rmJx5/\n" +
              "fPsxsu2Om0qjzl//6c03f54xVuQ5QujZDdef9FdfOjwK7dy5czAYzM7ODgYDgEuMca1Ws9YWRREE\n" +
              "gZQyjuOxiyaB5507d7Ye+0+MkbFlrTmTpUkYhggTZ01j2ap01+OMUmO0lgVG6PRLr9ly27+duuZ9\n" +
              "2+66efk7P7Lxpn+URSbTxA/DZe/5y63f/1KJcKVSWfPn/3R4A7Zt2wYkDJZzDrrhoigIIYQQxtho\n" +
              "NAqCIMsya229Xj+iAVs33KgWvzmKIt/38x2P5FmGEPI8sWj5mrIsu888QAjFGGutOedFlrzugiu3\n" +
              "3/NNBFVFFpd86LMIoR9/+a9lOkSIYEo933/HJ75w+BzAGEM96nQ6lNIgCKByQcJBhY7jGFhDGIaT\n" +
              "hJDUFiFECDnw+D2NZasqcRxXq57nD559sCgKhFBz2UUIoePPW5slQ2fUrk23nH/1x5M0Jahcee2n\n" +
              "7vvq3919w2e5EF5U5Z4n/ADheak7SZLEGEMIqVarvu8bY0Bu6Pf7/X6/0+nAc0IIeGyi6ohxFEWD\n" +
              "X9533Ip3tJ+6/5RV7x32e60D+/I8y3Y8arRK05QS/NKWO5ecdzmhzJXoiQ3/XqtWl1350Udu+Qph\n" +
              "gnGBEEGlO/tPPqWKnB+O4f5mr29+9gN5UUSVOKhMEUatdVFc417gjCoRpoxrmVeqU8mwTwhxJTr3\n" +
              "qo9OAqMIocZZa1pP3LN05VXQGAVB8PTd3yaEVE97q9z9mDHGWYMxXnLu23f/7NYkSS685i82f+eL\n" +
              "Qniz571z7qHbPc5nVrwriiJK6b03fOa9n7lhnhAihDPGuIAKIgR3RlFKCWUYY0oJpdSoAmFSFIXT\n" +
              "xaRIhEln632odDs3fveFB28Lw9Bae9LF6ygX6XMPVd94IULo9Rdcybj30qN3WFcC0aKMI0wYY4zR\n" +
              "mRXveu6em5/+0Y1Pb/hqXJtXQSNBGIXxFOYe97yyRFYbpWQ27OZZao0urbGuhNJGGZ/w8Fx4Wskz\n" +
              "LltPGT/l0vcZYx7/4dd+df8twBEby1YVReGM2vPoncBlw0oM4h8lZPbcK3Zt/E6/05n7+ffPuOIj\n" +
              "COHWgmyNpGmaZZkQglDGvYB5XhxXuRDOyCxNpJRGFYN+1xfUGcnYRF1SNuoTQp7bdKvW+oXNG4Qf\n" +
              "NmePff2F7wRK237q/sEzG611s8tX6yLjnq+kXPrWdb+691sIkz0Pfs8ZKzxPGfPru76ep4OV137K\n" +
              "WT2vAYx70zOzuEQY4ywdGWO67RbkGcGo12k5Z6vVGmXC80Njy8m0DhZGFedcWIm5ENNnrUaEJju2\n" +
              "dJ55cMk5lxHKECaUsfYvN5140brAE2e+/do9D204cdU1YRgsPv8q7vtlWVJK37Dm/VophNCK9/3N\n" +
              "vFtxRga9rtUyG42QNah0CKF0lMRxlVJar9cZ97wgCqOKtdZNdv6lb13nSqSVsq6kTHS3/cwYY6yb\n" +
              "OfPiIAgY435YoUxIJfc8emd92SqllDXa8zzr0HDbpiCseGGkpUIIceE/edv1P/vG5+fNtW9/7jo/\n" +
              "CKWUiFKPCy/wgTMSjIxDnudZa7kXcIpBJjr3PdcdVcIWu33zs5M8d901lzvnbrrt3qPNgHk7svFk\n" +
              "ABgEpbTX601NTcVxTAgZi0VHozIHgg80snme+74/Go2SJKlUKlEUwWRASnn0aqNAh0ajURzHQRAU\n" +
              "RXGIsD7mF0epAf1+X0pZq9XyPB8Oh8Dw6vW67/u1Wk1KGUXRUW1AFEVKKdCihRAv16U7nc7Ro0vP\n" +
              "mwOUUgiYfr/vnOv1ekenLv0bA5IkCYIA6LQxptfrlWVZlmWSJKBw1Wo1yN0oitI0he4nTVNKaZ7n\n" +
              "8O84718DGO12u+6/F2PMOQe8P89zEM1BUgfBHZq1JEnKshwLukqpOI5fq6ElU0pBSFBK4eic8zRN\n" +
              "gyCADrgsSyEEIUQI4XmeUqrZbGKMR6MRIUQpVa/XKaVKqQW2UUoVRVGtVqMoAqcNh0OY9IyHKSDW\n" +
              "h2EohCiKYsIbIYQQY8xwOIR/IGWbzSaEDSEEmuPx2BhjDD5hjHHOxwmz8Da9Xm8wGCilsixTSsGl\n" +
              "nHzyyWNoNsYAKYbRFtTQiTxQluV4RFCtVl988cU4jvM8N8aEYViWJbTzWmu4GGhNYEGDO58i/fJV\n" +
              "q9XKsoThAMYYgrPT6TDGgGNba6FodrtdrbXneZByk6rTlUqlKAoYh+V5rrWemZkZK9L79u1rNptg\n" +
              "58sV6TRNF1CkD5k6h2GYJAmMqowxkEX9fh8cyxjzPA8yqt/vwyR3koKD/zDz6lqthjFut9twU/Du\n" +
              "AoAYyAVwWZBI1lrIzEl0wT8Qoud53u/3IZAAzZRSjDGtNcQhADHINkIIkKdes0F3vV6vVCqe5+V5\n" +
              "XpZlrVYDyazf77fbbZjoEEJ83/d9H2NsrZVSpmkKw8Jmszk9PQ1RCupys9l8xXPi32XBgay1jUYD\n" +
              "wloIAa9X+L4PA1zAbiEE5PE4DYqigKFbpVJpt9sgDbZarfnK///6FLIeYI4xBvPgBayfV5XgHNhU\n" +
              "q9UCVICxGsYYwoNSijEeF/VGozGGNagYjDHQZMGAPXv2TOQBqMdFUQACQGl7Fb3LaDSC0uZ5Xr/f\n" +
              "J+Q3o0QhRL/fr9frcRxDjaeUArxGUQSFAlooSmmj0eh0OsaYVqu1wMT70PnAcDiEmgV1CtT2V2oA\n" +
              "GO+ck1JqrfM873Q6jUZDaw2WQIMxGo2At8P0vygK5xxAEFgFzgSYmsgDcRyP5wNwGVrrSqUySUE5\n" +
              "pKPQWmOMnXNaayiI7XYbYzzO2nEBAekbIbRkyRKlVJIkQFWklL7va62BzM9bB4bD4WAwmJmZATYB\n" +
              "lRIIBfyEUmqMxzDCmSSEoOgSQmB7pZQxBi4IwhpjHAQBJHGWZUAigYBFUQQbQeEDIFq0aNHhPQBT\n" +
              "jHFEAi9gjBljfN+HapIkie/7eZ4vfBnjlSSJUioMQ9/3hRBwAkAFKOTOOeBdEO5KKa018Dl41YIx\n" +
              "Fsdxs9ncu3dvtVpdIAT+Zz7Q7XYBm397PlCpVCDPwNdHXFBQIXcB2qvVKjhQSmmMgewCYgfOmZ6e\n" +
              "DoIAQq4sSxA8W62WlBKueCFtFFh0HMcvnw8MBoPBYACz+1c6HyjLElgx1NdGoxFFUb1eHx8RpBpw\n" +
              "BVDUwWDAOU+SJMsyGN4opaSU4zCbNwf2798PqAxRq5Q65phjwCpwdJqms7OzAClAwo5oQK/Xy/Mc\n" +
              "iAMgEiDbcDgcx6Hv+/BykbUWTgneHg6HgMKj0QiEHErp/v375ytHDDIdGhSgjYBiUO2hiAKWAwmF\n" +
              "0DoijAZBAL8M3gPfQlsHkhnAUaVS2bNnz9TUFDwJQUsptdZWKhVQRrrd7gLdEiuKAnCzKApoc4En\n" +
              "c86B00opwzCE3mXCJB6NRhjjZrMJNkspoe8BtAGIBCAaDofT09NpmkINhYgviiKKIs45ZP/4Ig6f\n" +
              "A57nwbsm0CUZY/bt2wcv4IBJsHcQBFrrCV+BieMYJAIATSgL0CVCZgshIBTLsoQmBOITprpCCFAy\n" +
              "ATl831+AmeLnn39+DEQIIaBTnPMwDOECoIkJwzDLMgiMI7/s4Rz0hFEUZVn2ctYAHSzEfRzHELdR\n" +
              "FHU6HchAiAjYNI5jQNUF1Fgy7ugppdVqFRKaMQaQCqNiqPNwmglRCIjNaDSy1sI7XOA9KAWQmsC4\n" +
              "gDsAfIPZxhg4D/DLhfkY3r59O9AHACLOOWwmpYRunTEGPJFSmiTJJG8S/kGlRXAcdA9Q5CFlgdMC\n" +
              "EMG3ZVm+VurVAuu/ABvQO8dLS+zPAAAAAElFTkSuQmCC\n"));
      scale(1.0 / 16.0);
    }
  }

  public static Obj sky() {
    // texture starts at +z, texture center is the north
    Obj obj = Obj.load(Animals.class.getResourceAsStream("blender_uv_sphere.obj")).ry90().ry90().ry90(); // HD sphere
    // invert normals
    for (int i = 0; i < obj.face.length; i += 9) {
      int v = obj.face[i + 3];
      obj.face[i + 3] = obj.face[i + 6];
      obj.face[i + 6] = v;
      int t = obj.face[i + 5];
      obj.face[i + 5] = obj.face[i + 8];
      obj.face[i + 8] = t;
    }
    // invert texture
    for (int i = 0; i < obj.texture.length; i += 2) obj.texture[i] = 1 - obj.texture[i];
    // generate default texture
    int w = 4096;
    int h = 1024;
    BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    Random random = new Random(0);
    for (int brightness = 1; brightness < 0x100; brightness++) {
      int rgb = brightness * 0x010101 | 0xFF000000;
      for (int i = 0; i < 16; i++) {
        int x = random.nextInt(w);
        int y = random.nextInt(h - 1);
        image.setRGB(x, y, rgb);
        image.setRGB(x, y + 1, rgb);
        x = (x + 1) % w;
        image.setRGB(x, y, rgb);
        image.setRGB(x, y + 1, rgb);
      }
    }
    obj.image = image;
    // texture size correction for 4096 x 1024 image
    for (int i = 0; i < obj.texture.length; i++) {
      double y = obj.texture[++i]; // y = y0 * 0.3 + 0.5
      obj.texture[i] = Math.min(Math.max(0, (y - 0.4) / 0.4), 1);
    }
    return obj.flatNormal().interpolateNormal();
  }

  public static Obj apple() {
    Obj obj = Sketch2.obj("assets/apple.obj"); // width 1.15
    obj.image = Sketch2.img("assets/apple_texture.png");
//    double x0 = 0;
//    double x1 = 0;
//    double yx0 = 0;
//    double yx1 = 0;
//    double z0 = 0;
//    double z1 = 0;
//    double yz0 = 0;
//    double yz1 = 0;
//    for (int i = 0; i < obj.vertex.length;) {
//      double x = obj.vertex[i++];
//      double y = obj.vertex[i++];
//      double z = obj.vertex[i++];
//      if (x < -5) continue;
//      if (x0 > x) { x0 = x; yx0 = y; }
//      if (x1 < x) { x1 = x; yx1 = y; }
//      if (z0 > z) { z0 = z; yz0 = y; }
//      if (z1 < z) { z1 = z; yz1 = y; }
//    }
    return obj.translate(0, -0.972396, 0);
  }

}
