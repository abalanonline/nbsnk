package ab.nbsnk.nodes;

import ab.nbsnk.Obj;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;

public class FractalLandscape {

  public static double[][] diamondSquare(int size, long seed) {
    Random random = new Random(seed);
    int size2 = 1 << (31 - Integer.numberOfLeadingZeros(size));
    if (size2 < size) size2 *= 2;
    double[][] d = new double[size2][size2];
    double distanceSquare1 = 0.747 / size2; // 0.747 will keep the output in the interval (-0.5, 0.5)
    double distanceDiamond1 = distanceSquare1 * Math.sqrt(2);
    for (int s2 = size2; s2 > 1; s2 /= 2) {
      int s1 = s2 / 2;
      double distanceDiamond = distanceDiamond1 * s1;
      for (int y = 0; y < size2; y += s2) {
        for (int x = 0; x < size2; x += s2) {
          double v = 0;
          for (int y1 = 0; y1 <= s2; y1 += s2) {
            for (int x1 = 0; x1 <= s2; x1 += s2) {
              v += d[(y + y1) % size2][(x + x1) % size2];
            }
          }
          double r = random.nextDouble() - 0.5; //r = 0.5;
          d[y + s1][x + s1] = v / 4 + distanceDiamond * r;
        }
      }
      double distanceSquare = distanceSquare1 * s1;
      for (int y = 0; y < size2; y += s1) {
        for (int x = 0; x < size2; x += s1) {
          if ((x + y) % s2 == 0) continue;
          double v = 0;
          for (int y1 = -s1; y1 <= s1; y1 += s1) {
            for (int x1 = -s1; x1 <= s1; x1 += s1) {
              if ((x1 + y1) % s2 == 0) continue;
              v += d[(y + y1 + size2) % size2][(x + x1 + size2) % size2];
            }
          }
          double r = random.nextDouble() - 0.5; //r = 0.5;
          d[y][x] = v / 4 + distanceSquare * r;
        }
      }
    }
    double[][] resize = new double[size][size];
    for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) resize[y][x] = d[y * size2 / size][x * size2 / size];
    return resize;
  }

  private static boolean test(double d) {
    d %= 1;
    if (d < 0) d += 1;
    return d > 0.5;
  }

  public static BufferedImage diamondSquareTexture(int size, long seed, double temperature) {
    int[] colors = {0xF9EC8C /* yellow */, 0x015102 /* dark green */, 0x959696 /* bright gray */, 0x7E4A40 /* brown */,
        0x6CDA3C /* bright green */, 0xD6843B /* orange */, 0x0464D5 /* blue */, 0x676566 /* dark gray */,};
    double[][] texture1 = FractalLandscape.diamondSquare(size, seed + 1000000000L);
    double[][] texture2 = FractalLandscape.diamondSquare(size, seed + 2000000000L);
    double[][] texture3 = FractalLandscape.diamondSquare(size, seed + 3000000000L);
    BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) {
      int color = (test(texture1[y][x] * temperature) ? 1 : 0)
          | (test(texture2[y][x] * temperature) ? 2 : 0) | (test(texture3[y][x] * temperature) ? 4 : 0);
      image.setRGB(x, y, colors[color]);
    }
    return image;
  }

  public static Obj[] generate(double[][] doubles, int div) {
    int wh = doubles.length;
    ArrayList<Obj> list = new ArrayList<>();
    for (int y = 0; y < div; y++) for (int x = 0; x < div; x++) {
      list.add(generate(doubles, wh * x / div, wh * y / div, wh * (x + 1) / div, wh * (y + 1) / div));
    }
    return list.toArray(new Obj[0]);
  }

  public static Obj generate(double[][] doubles, int x1, int y1, int x2, int y2) {
    final int h = doubles.length;
    final int w = doubles[0].length;
    int h0 = y2 - y1;
    int w0 = x2 - x1;
    int w1 = w0 + 1;
    int h1 = h0 + 1;
    Obj obj = new Obj();
    double[] v = new double[(h1 * w1 + h0 * w0) * 3];
    int vi = 0;
    for (int y = y1; y <= y2; y++) for (int x = x1; x <= x2; x++) {
      v[vi++] = (double) x / (double) w;
      v[vi++] = doubles[y % h][x % w];
      v[vi++] = (double) y / (double) h;
    }
    int[] f = new int[h0 * w0 * 4 * 3 * 3];
    int fi = 0;
    for (int y = 0; y < h0; y++) for (int x = 0; x < w0; x++) {
      int[] fs = {y * h1 + x, y * h1 + x + 1, (y + 1) * h1 + x + 1, (y + 1) * h1 + x, vi / 3};
      double[] xyz = new double[3];
      for (int ifs = 0; ifs < 4; ifs++) {
        for (int i = 0, j = fs[ifs] * 3; i < 3; i++) xyz[i] += v[j++];
        f[fi] = fs[ifs]; fi += 3;
        f[fi] = fs[4]; fi += 3;
        f[fi] = fs[(ifs + 1) % 4]; fi += 3;
      }
      v[vi++] = xyz[0] / 4;
      v[vi++] = xyz[1] / 4;
      v[vi++] = xyz[2] / 4;
    }
    double[] t = new double[v.length * 2 / 3];
    for (int i = 0, ti = 0; i < v.length;) {
      t[ti++] = v[i++]; i++;
      t[ti++] = 1 - v[i++];
    }
    for (int i = 0; i < f.length;) {
      int fn = f[i++]; i++; f[i++] = fn;
    }
    assert vi == v.length;
    assert fi == f.length;
    obj.vertex = v;
    obj.face = f;
    obj.texture = t;
    //Obj.flatNormal(obj); // let the app decide which normals do they wish
    return obj;
  }

  public static class V {
    int id;
    double x;
    double y;
    double z;
  }

  public static class S {
    V[] vs;
    public S(V... vs) {
      this.vs = vs;
    }
  }

}
