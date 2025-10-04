package ab.nbsnk;

import ab.jnc3.Screen;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class Sketch3 {

  private static final int UPDATE_PERIOD_NS = 10_000_000;
  public static final double MOUSE_SENSITIVITY = 1 / 10000.0;
  private Screen screen;
  private Engine3d engine3d;
  private boolean systemExit;
  public static final double WALKING_SPEED = 0.2;

  private void run() {
    Obj teapot = Sketch2.obj("assets/teapot.obj");
    Obj cow = Sketch2.obj("assets/cow.obj");
    Obj.fixNormal(cow);
    cow.image = Sketch2.img("assets/cow.png");

    screen = new Screen();
//    screen.image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
//    screen.preferredSize = new Dimension(1920, 1080);
    screen.image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
    screen.preferredSize = new Dimension(640, 360);
    engine3d = new EngineFx().open(screen.image);
    for (int y = -100; y < 100; y += 20) {
      for (int x = -100; x < 100; x += 20) {
        engine3d.shape(teapot).translation(x, 0, y);
      }
    }

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
        engine3d.camera().translation(world.px, 1.8, world.pz).rotation(world.pry, world.prp, 0); // 6 feet tall
        engine3d.update();
        screen.update();
      }
    }
  }

}
