package ab.nbsnk.nodes;

public class Pnt {
  public double x;
  public double y;
  public double z;

  public Pnt() {
  }

  public Pnt(double x, double y, double z) {
    this.x = x;
    this.y = y;
    this.z = z;
  }

  @Override
  public Pnt clone() {
    return new Pnt(x, y, z);
  }

  public Pnt add(Pnt p, double alpha) {
    this.x += p.x * alpha;
    this.y += p.y * alpha;
    this.z += p.z * alpha;
    return this;
  }

}
