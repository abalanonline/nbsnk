package ab.nbsnk.nodes;

import ab.nbsnk.Obj;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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

  public static Obj[] generate(double[][] doubles, int x1, int y1, int x2, int y2) {
    int h = doubles.length;
    int w = doubles[0].length;
    int h0 = y2 - y1;
    int w0 = x2 - x1;
    int w1 = w0 + 1;
    int h1 = h0 + 1;
    Obj obj = new Obj();
    double[] v = new double[(h1 * w1 + h0 * w0) * 3];
    int vi = 0;
    for (int y = y1; y <= y2; y++) for (int x = x1; x <= x2; x++) {
      v[vi++] = x;
      v[vi++] = doubles[y % h][x % w];
      v[vi++] = y;
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
      t[ti++] = v[i++] / w; i++;
      t[ti++] = 1 - v[i++] / h;
    }
    for (int i = 0; i < f.length;) {
      int fn = f[i++]; i++; f[i++] = fn;
    }
    assert vi == v.length;
    assert fi == f.length;
    obj.vertex = v;
    obj.face = f;
    obj.texture = t;
    Obj.flatNormal(obj);
    return new Obj[]{obj};
  }

  /**
   * 50, 5, 1
   */
  public static Obj[] generate(int n) {
    Random random = new Random(0);
    double[][] d = new double[n][n];
    for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) d[y][x] = random.nextDouble();
    V[][] vs = new V[n + 1][n + 1];
    for (int y = 0; y < n + 1; y++) for (int x = 0; x < n + 1; x++) {
      V v = new V();
      v.x = x;
      v.z = y;
      v.y = d[y % n][x % n];
      vs[y][x] = v;
    }
    List<S> so;
    List<S> sn = new ArrayList<>();
    for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
      sn.add(new S(vs[y][x], vs[y][x + 1], vs[y + 1][x + 1], vs[y + 1][x]));
    }
    so = sn;
    sn = new ArrayList<>();
    for (S s : so) {
      V v = new V();
      for (int i = 0; i < 4; i++) {
        v.x += s.vs[i].x / 4;
        v.y += s.vs[i].y / 4;
        v.z += s.vs[i].z / 4;
      }
      for (int i = 0; i < 4; i++) sn.add(new S(s.vs[i], v, s.vs[(i + 1) % 4]));
    }
    so = sn;
    Set<V> set = new HashSet<>();
    for (S s : so) set.addAll(Arrays.asList(s.vs));
    List<V> vn = new ArrayList<>(set);
    Collections.shuffle(vn);
    Collections.shuffle(sn);
    double[] ov = new double[vn.size() * 3];
    for (int i = 0, j = 0; i < vn.size(); i++) {
      V v = vn.get(i);
      v.id = i;
      ov[j++] = v.x;
      ov[j++] = v.y;
      ov[j++] = v.z;
    }
    int[] of = new int[sn.size() * 9];
    for (int i = 0; i < sn.size(); i++) {
      S s = sn.get(i);
      of[i * 9] = s.vs[0].id;
      of[i * 9 + 3] = s.vs[1].id;
      of[i * 9 + 6] = s.vs[2].id;
    }
    Obj obj = new Obj();
    obj.face = of;
    obj.vertex = ov;
    Obj.flatNormal(obj);
    return new Obj[]{obj};
  }

  public static Obj[] generate1(int n) {
    Random random = ThreadLocalRandom.current();
    double[][][][] d = new double[n][n][2][2];
    for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) d[y][x][0][0] = random.nextDouble();
    for (int y = 0; y < n; y++) for (int x = 0; x < n; x++) {
      d[y][x][0][1] = d[y][(x + 1) % n][0][0];
      d[y][x][1][0] = d[(y + 1) % n][x][0][0];
      d[y][x][1][1] = d[(y + 1) % n][(x + 1) % n][0][0];
    }

    double[] v = new double[15];
    v[0] = 0; v[2] = 0; v[1] = d[0][0][0][0];
    v[3] = 0; v[5] = 20; v[4] = d[0][0][0][1];
    v[6] = 20; v[8] = 0; v[7] = d[0][0][1][0];
    v[9] = 20; v[11] = 20; v[10] = d[0][0][1][1];
    v[12] = 10; v[14] = 10; v[13] = (d[0][0][0][0] + d[0][0][0][1] + d[0][0][1][0] + d[0][0][1][1]) / 4;
    int[] f = new int[36];
    f[0] = 0; f[3] = 1; f[6] = 4;
    f[9] = 1; f[12] = 3; f[15] = 4;
    f[18] = 3; f[21] = 2; f[24] = 4;
    f[27] = 2; f[30] = 0; f[33] = 4;
    Obj obj = new Obj();
    obj.face = f;
    obj.vertex = v;
    Obj.flatNormal(obj);
    return new Obj[]{obj};
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
