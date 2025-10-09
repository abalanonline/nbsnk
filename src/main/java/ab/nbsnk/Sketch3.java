package ab.nbsnk;

import ab.jnc3.Screen;
import ab.nbsnk.nodes.FractalLandscape;

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
  private Engine3d.Shape sphere;
  private double[][] box;
  public static final int TILE_DIV = 32; // 1k
  private Engine3d.Shape[] tiles;
  private double[] tilexz;

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
    Obj gridShape = Sketch2.obj("assets/blender_cube.obj");
    //gridShape.image = Sketch2.img("assets/cow.png");
//    Obj.scale(gridShape, 0.5, 0.5, 0.5);
    Obj cow = Sketch2.obj("assets/cow.obj");
    Obj.fixNormal(cow);
    cow.image = Sketch2.img("assets/cow.png");

    screen = new Screen();
    int screenWidth = fullHd ? 1920 : 640;
    int screenHeight = fullHd ? 1080 : 360;
    screen.image = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
    screen.preferredSize = new Dimension(screenWidth, screenHeight);
    engine3d = new EngineFx().open(screen.image).setFarClip(FAR_CLIP).showFps();
//    for (int y = -100; y <= 100; y += 40) {
//      for (int x = -100; x <= 100; x += 40) {
//        engine3d.shape(gridShape).translation(x, 1, y);
//      }
//    }
    for (int z = 0, i = 0; z < TILE_DIV; z++) {
      for (int x = 0; x < TILE_DIV; x++, i++) {
        tiles[i] = engine3d.shape(tileObjs[i]);
        tilexz[2 * i] = (x + 0.5) * BOX_WIDTH / TILE_DIV;
        tilexz[2 * i + 1] = (z + 0.5) * BOX_WIDTH / TILE_DIV;
      }
    }
//    for (int i = 0; i < TILE_DIV * TILE_DIV; i++) engine3d.shape(gridShape).translation(tilexz[2 * i], 1, tilexz[2 * i + 1]);
    //for (Obj tileObj : tileObjs) engine3d.shape(tileObj).translation(0, 0, -BOX_WIDTH);
    //for (Obj tileObj : tileObjs) engine3d.shape(tileObj).translation(-BOX_WIDTH, 0, 0);
    //for (Obj tileObj : tileObjs) engine3d.shape(tileObj).translation(-BOX_WIDTH, 0, -BOX_WIDTH);
    Obj photosphere = Sketch2.photosphere(fullHd ? "assets/pano2.png" : "assets/sky_test.png", FAR_CLIP * 0.99);
    for (int i = 0; i < photosphere.texture.length; i++) {
      double y = photosphere.texture[++i]; // y = y0 * 0.3 + 0.5
      photosphere.texture[i] = Math.min(Math.max(0, (y - 0.4) / 0.4), 1);
    }
    sphere = (Engine3d.Shape) engine3d.shape(photosphere).selfIllumination();//.rotation(0.25, 0, 0);
    engine3d.light().translation(0, 3500, 5000);
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
    double bx = x * BOX_SIZE / BOX_WIDTH % BOX_SIZE; if (bx < 0) bx += BOX_SIZE;
    double bz = z * BOX_SIZE / BOX_WIDTH % BOX_SIZE; if (bz < 0) bz += BOX_SIZE;
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
    int[] tilexz;
    double[] tilebr;

    @Override
    protected World clone() {
      World world = new World();
      world.px = this.px;
      world.pz = this.pz;
      world.pry = this.pry;
      world.prp = this.prp;
      world.tilexz = Arrays.copyOf(this.tilexz, this.tilexz.length);
      world.tilebr = Arrays.copyOf(this.tilebr, this.tilebr.length);
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
