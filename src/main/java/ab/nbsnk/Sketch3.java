package ab.nbsnk;

import ab.jnc3.Screen;
import ab.nbsnk.nodes.FractalLandscape;
import ab.nbsnk.nodes.Shapes;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class Sketch3 {

  private static final int UPDATE_PERIOD_NS = 10_000_000;
  public static final double MOUSE_SENSITIVITY = 1 / 10000.0;
  public static final int BOX_SIZE = 512;
  public static final int BOX_WIDTH = 500;
  public static final int BOX_HEIGHT = 100;
  public static final int FAR_CLIP = BOX_WIDTH / 2;
  private Screen screen;
  private Engine3d engine3d;
  private boolean systemExit;
  public static final double WALKING_SPEED = 7.0 * UPDATE_PERIOD_NS / 1_000_000_000; // 7 m/s
  private Engine3d.Group horizon;
  private Engine3d.Group moon;
  private double[][] box;
  public static final int TILE_DIV = 32; // 1k
  private Engine3d.Shape[] tiles;
  private double[] tilexz;
  private int tick;

  public static BufferedImage scale(BufferedImage image, int v) {
    int width = image.getWidth();
    int height = image.getHeight();
    BufferedImage scaled = new BufferedImage(width * v, height * v, BufferedImage.TYPE_INT_RGB);
    scaled.getGraphics().drawImage(image, 0, 0, width * v, height * v, null);
    return scaled;
  }

  private void run() {
    boolean fullHd = false;
//    fullHd = true;
    box = FractalLandscape.diamondSquare(BOX_SIZE, 3);
    BufferedImage boxTexture = FractalLandscape.diamondSquareTexture(64, 1, 15);
    //boxTexture = Sketch2.img("assets/maptest.png");
    Obj[] tileObjs = FractalLandscape.generate(this.box, TILE_DIV);
    tiles = new Engine3d.Shape[TILE_DIV * TILE_DIV];
    tilexz = new double[TILE_DIV * TILE_DIV * 2];
    for (int i = 0; i < TILE_DIV * TILE_DIV; i++) {
      Obj tileObj = tileObjs[i];
      Obj.scale(tileObj, BOX_WIDTH, BOX_HEIGHT, BOX_WIDTH);
      Obj.flatNormal(tileObj);
      Obj.interpolateNormal(tileObj);
      tileObj.image = boxTexture;
    }
    Obj gridShape = new Shapes.Cube();

    // cattle
    //gridShape = Sketch2.obj("assets/pig1/pig1.obj");
    //gridShape.image = scale(Sketch2.img("assets/pig1/pig1.png"), 16);
    //gridShape = Sketch2.obj("assets/sheep2/sheep2.obj");
    //gridShape.image = scale(Sketch2.img("assets/sheep2/sheep2.png"), 16);

    screen = new Screen();
    int screenWidth = fullHd ? 1920 : 640;
    int screenHeight = fullHd ? 1080 : 360;
    screen.image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
    screen.preferredSize = new Dimension(screenWidth, screenHeight);
    engine3d = new EngineFx().open(screen.image).setFarClip(FAR_CLIP).showFps();
    horizon = engine3d.group();
    moon = (Engine3d.Group) engine3d.group().connect(horizon);
    for (int y = -100; y <= 100; y += 40) {
      for (int x = -100; x <= 100; x += 40) {
        engine3d.shape(gridShape)
//            .selfIllumination()
            .translation(x, 0, y).rotation(0, 0, 0);
      }
    }
    for (int z = 0, i = 0; z < TILE_DIV; z++) {
      for (int x = 0; x < TILE_DIV; x++, i++) {
        tiles[i] = engine3d.shape(tileObjs[i]);
        tilexz[2 * i] = (x + 0.5) * BOX_WIDTH / TILE_DIV;
        tilexz[2 * i + 1] = (z + 0.5) * BOX_WIDTH / TILE_DIV;
      }
    }
    Obj skyObj = Animals.sky().scale(FAR_CLIP * 0.99);
    //skyObj.image = Sketch2.img("assets/sky_test.png");
    //skyObj.image = Sketch2.img("assets/pano2.png");
    engine3d.shape(skyObj).selfIllumination().connect(horizon);
    engine3d.shape(new Shapes.Icosphere().scale(3)).selfIllumination().translation(0, 0, -FAR_CLIP * 0.95).connect(moon);
    engine3d.light().translation(0, 0, -FAR_CLIP * 0.98).connect(moon);
    //engine3d.shape(gridShape).selfIllumination().translation(0, 35, 50);

    World world = new World();
    world.tilexz = new int[TILE_DIV * TILE_DIV * 2];
    world.tilebr = new double[TILE_DIV * TILE_DIV];
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
      double yLimit = 0.12; // half max
//      yLimit = 0.25;
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

      for (int i = 0; i < tiles.length; i++) {
        // px = tx + i * BW
        int xi = (int) Math.round((world.px - tilexz[2 * i]) / BOX_WIDTH);
        int zi = (int) Math.round((world.pz - tilexz[2 * i + 1]) / BOX_WIDTH);
        world.tilexz[2 * i] = xi;
        world.tilexz[2 * i + 1] = zi;
        double xd = tilexz[2 * i] + BOX_WIDTH * xi - world.px;
        double zd = tilexz[2 * i + 1] + BOX_WIDTH * zi - world.pz;
        double d = 1 - Math.sqrt(xd * xd + zd * zd) / BOX_WIDTH * 2.1; // 2.0 - 2.5
        world.tilebr[i] = Math.max(0, d);
      }
      world.mny = tick / 6000.0; // 1 spin per 60 sec
      world.mnp = Math.sin(tick / 11000.0 * 2 * Math.PI) / 20 + 0.06; // 1 spin per 60 sec
      tick++;

      // done
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
    double bx = x * BOX_SIZE / BOX_WIDTH;
    double bz = z * BOX_SIZE / BOX_WIDTH;
    double bxf = Math.floor(bx);
    double bzf = Math.floor(bz);
    int ix = Math.floorMod((int) bxf, BOX_SIZE);
    int iz = Math.floorMod((int) bzf, BOX_SIZE);
    double py =
        box[iz][ix] * (1 - bz + bzf) * (1 - bx + bxf) +
        box[iz][(ix + 1) % BOX_SIZE] * (1 - bz + bzf) * (bx - bxf) +
        box[(iz + 1) % BOX_SIZE][ix] * (bz - bzf) * (1 - bx + bxf) +
        box[(iz + 1) % BOX_SIZE][(ix + 1) % BOX_SIZE] * (bz - bzf) * (bx - bxf);
    return py * BOX_HEIGHT;
  }

  /**
   * The world contains modifiable variables to be double buffered to the render loop.
   */
  private static class World {
    double px; // player xz
    double pz;
    double pry; // player yaw pitch
    double prp;
    int[] tilexz; // tile xz adjustment
    double[] tilebr; // tile brightness
    double mny; // moon yaw pitch
    double mnp;

    @Override
    protected World clone() {
      World world = new World();
      world.px = this.px;
      world.pz = this.pz;
      world.pry = this.pry;
      world.prp = this.prp;
      world.tilexz = Arrays.copyOf(this.tilexz, this.tilexz.length);
      world.tilebr = Arrays.copyOf(this.tilebr, this.tilebr.length);
      world.mny = this.mny;
      world.mnp = this.mnp;
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
        horizon.translation(world.px, py, world.pz);
        moon.rotation(world.mny, world.mnp, 0);
        for (int i = 0; i < tiles.length; i++) {
          tiles[i].setColor((int) (world.tilebr[i] * 0xFF) * 0x010101 | 0xFF000000)
              .translation(world.tilexz[2 * i] * BOX_WIDTH, 0, world.tilexz[2 * i + 1] * BOX_WIDTH);
        }
        engine3d.update();
        screen.update();
      }
    }
  }

}
