package ab.nbsnk;

import ab.jnc3.Screen;
import ab.nbsnk.nodes.FractalLandscape;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class Sketch3 {

  private static final int UPDATE_PERIOD_NS = 10_000_000;
  public static final double MOUSE_SENSITIVITY = 1 / 10000.0;
  public static final int BOX_SIZE = 500;
  public static final int BOX_HEIGHT = 100;
  private Screen screen;
  private Engine3d engine3d;
  private boolean systemExit;
  public static final double WALKING_SPEED = 0.2;
  private Engine3d.Shape sphere;
  private double[][] box;

  private void run() {
    box = FractalLandscape.diamondSquare(BOX_SIZE, 3);
    BufferedImage boxTexture = FractalLandscape.diamondSquareTexture(256, 1, 15);
    Obj[] landscapes = FractalLandscape.generate(this.box, 0, 0, BOX_SIZE, BOX_SIZE);
    for (Obj landscape : landscapes) {
      Obj.scale(landscape, 1, BOX_HEIGHT, 1);
      landscape.image = Sketch2.img("assets/maptest.png");
      landscape.image = boxTexture;
//      Obj.translate(landscape, 0, -10, 0);
    }
    Obj teapot = Sketch2.obj("assets/blender_cube.obj");
//    Obj.scale(teapot, 0.5, 0.5, 0.5);
    Obj cow = Sketch2.obj("assets/cow.obj");
    Obj.fixNormal(cow);
    cow.image = Sketch2.img("assets/cow.png");

    screen = new Screen();
//    screen.image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
//    screen.preferredSize = new Dimension(1920, 1080);
    screen.image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
    screen.preferredSize = new Dimension(640, 360);
    engine3d = new EngineFx().open(screen.image);
    for (int y = -100; y <= 100; y += 40) {
      for (int x = -100; x <= 100; x += 40) {
        engine3d.shape(teapot).translation(x, 1, y);
      }
    }
    for (Obj landscape : landscapes) engine3d.shape(landscape);
//    for (Obj landscape : landscapes) engine3d.shape(landscape).translation(0, 0, -BOX_SIZE);
//    for (Obj landscape : landscapes) engine3d.shape(landscape).translation(-BOX_SIZE, 0, 0);
//    for (Obj landscape : landscapes) engine3d.shape(landscape).translation(-BOX_SIZE, 0, -BOX_SIZE);
    sphere = (Engine3d.Shape) engine3d.shape(Sketch2.photosphere()).selfIllumination().rotation(0.25, 0, 0);
    engine3d.light().translation(700, 300, 0);

    World world = new World();
    screen.gameController = true;
    boolean[] mouseButton = new boolean[10];
    Queue<String> keyListener = new LinkedBlockingQueue<>();
    screen.keyListener = keyListener::add;

    RenderLoop renderLoop = new RenderLoop();
    renderLoop.world = world.clone();
    Thread renderLoopThread = new Thread(renderLoop);
    renderLoopThread.start();

    long updateNs = System.nanoTime();
    boolean[] gamepadButton = new boolean[4];
    int[] gamepadAxis = new int[2];
    // -------------------------------- physics loop --------------------------------
    while (!systemExit) {
      while (!keyListener.isEmpty()) {
        String key = keyListener.remove();
        switch (key) {
          case "Esc": systemExit = true; break;
          case "+W": gamepadButton[2] = true; break;
          case "-W": gamepadButton[2] = false; break;
          case "+A": gamepadButton[0] = true; break;
          case "-A": gamepadButton[0] = false; break;
          case "+S": gamepadButton[1] = true; break;
          case "-S": gamepadButton[1] = false; break;
          case "+D": gamepadButton[3] = true; break;
          case "-D": gamepadButton[3] = false; break;
        }
        if (key.startsWith("Mouse")) {
          char bw = key.charAt(6);
          int button = key.charAt(7) - '0';
          boolean buttonOn = key.charAt(5) == '+';
          if (bw == 'B') {
            mouseButton[button] = buttonOn;
          }
          if (bw <= '9') {
            String[] xys = key.substring(5).split(",");
            gamepadAxis[0] += Integer.parseInt(xys[0]);
            gamepadAxis[1] -= Integer.parseInt(xys[1]);
          }
        }
      }
      double yLimit = 0.125; // half max
      if (gamepadAxis[1] > yLimit / MOUSE_SENSITIVITY) gamepadAxis[1] = (int) (yLimit / MOUSE_SENSITIVITY);
      if (gamepadAxis[1] < -yLimit / MOUSE_SENSITIVITY) gamepadAxis[1] = (int) (-yLimit / MOUSE_SENSITIVITY);
      world.pry = gamepadAxis[0] * MOUSE_SENSITIVITY;
      world.prp = gamepadAxis[1] * MOUSE_SENSITIVITY;
      double s = Math.sin(world.pry * 2 * Math.PI) * WALKING_SPEED;
      double c = Math.cos(world.pry * 2 * Math.PI) * WALKING_SPEED;
      if (gamepadButton[0]) { world.px -= c; world.pz -= s; }
      if (gamepadButton[1]) { world.px -= s; world.pz += c; }
      if (gamepadButton[2]) { world.px += s; world.pz -= c; }
      if (gamepadButton[3]) { world.px += c; world.pz += s; }
      renderLoop.world = world.clone();

      updateNs += UPDATE_PERIOD_NS;
      long ns = System.nanoTime();
      if (ns < updateNs) {
        try { Thread.sleep((updateNs - ns) / 1_000_000); } catch (InterruptedException ignore) {}
      } else updateNs = ns;
    }
    try { renderLoopThread.join(); } catch (InterruptedException ignore) {}
    engine3d.close();
    screen.close();
  }

  public static void main(String[] args) {
    new Sketch3().run();
  }

  public double surfaceY(double x, double z) {
    double bx = x % BOX_SIZE; if (bx < 0) bx += BOX_SIZE;
    double bz = z % BOX_SIZE; if (bz < 0) bz += BOX_SIZE;
    int ix = (int) bx;
    int iz = (int) bz;
    double py =
        box[iz][ix] * (1 - bz + iz) * (1 - bx + ix) +
        box[iz][(ix + 1) % BOX_SIZE] * (1 - bz + iz) * (bx - ix) +
        box[(iz + 1) % BOX_SIZE][ix] * (bz - iz) * (1 - bx + ix) +
        box[(iz + 1) % BOX_SIZE][(ix + 1) % BOX_SIZE] * (bz - iz) * (bx - ix);
    return py * BOX_HEIGHT;
  }

  /**
   * The world contains modifiable variables to be double buffered to the render loop.
   */
  private static class World {
    double px;
    double pz;
    double pry;
    double prp;

    @Override
    protected World clone() {
      World world = new World();
      world.px = this.px;
      world.pz = this.pz;
      world.pry = this.pry;
      world.prp = this.prp;
      return world;
    }
  }

  private class RenderLoop implements Runnable {
    World world;
    @Override
    public void run() {
      while (!systemExit) {
        World world = this.world;
        double py = surfaceY(world.px, world.pz) + 1.8;
        engine3d.camera().translation(world.px, py, world.pz).rotation(world.pry, world.prp, 0);
        if (sphere != null) sphere.translation(world.px, py, world.pz);
        engine3d.update();
        screen.update();
      }
    }
  }

}
