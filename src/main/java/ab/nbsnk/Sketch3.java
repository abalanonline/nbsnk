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

import ab.jnc3.Screen;
import ab.nbsnk.nodes.Col;
import ab.nbsnk.nodes.FractalLandscape;
import ab.nbsnk.nodes.Pnt;
import ab.nbsnk.nodes.Shapes;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class Sketch3 {

  private static final int UPDATE_PERIOD_NS = 10_000_000;
  private static final double UPDATE_PERIOD_S = UPDATE_PERIOD_NS / 1_000_000_000.0;
  public static final double MOUSE_SENSITIVITY = 1 / 10000.0;
  public static final int BOX_SIZE = 512;
  public static final int BOX_WIDTH = 500;
  public static final int BOX_HEIGHT = 100;
  public static final int FAR_CLIP = BOX_WIDTH / 2;
  public static final double GRAVITY = 1.625;
  private Screen screen;
  private Engine3d engine3d;
  private boolean systemExit;
  public static final double WALKING_SPEED = 7.0; // 7 m/s
  private Engine3d.Group horizon;
  private Engine3d.Group moon;
  private double[][] box;
  public static final int TILE_DIV = 32; // 1k
  private Engine3d.Shape[] tiles;
  private double[] tilexz;
  private int tick;

  private void run() {
    long nanoTime = System.nanoTime();
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
    engine3d.shape(skyObj).selfIllumination(-1).connect(horizon);
    engine3d.shape(new Shapes.Icosphere().scale(3)).selfIllumination(-1).translation(0, 0, -FAR_CLIP * 0.95).connect(moon);
    engine3d.light().translation(0, 0, -FAR_CLIP * 0.98).connect(moon);
    //engine3d.shape(gridShape).selfIllumination().translation(0, 35, 50);

    Projectile apple = new Projectile(5, a -> engine3d.shape(new Shapes.Icosphere().scale(0.07 * a))
        .selfIllumination(new Col(0xFFCA4E21).mul(a).rgb()));
    apple.light = engine3d.light().setColor(0xFFBF3720);
    apple.node = engine3d.shape(Animals.apple().scale(0.08)).selfIllumination(-1);

    screen.gameController = true;
    boolean[] mouseButton = new boolean[10];
    Queue<String> keyListener = new LinkedBlockingQueue<>();
    screen.keyListener = keyListener::add;

    RenderLoop renderLoop = new RenderLoop();
    Thread renderLoopThread = new Thread(renderLoop);
    renderLoopThread.start();

    long updateNs = System.nanoTime();
    boolean[] gamepadButton = new boolean[4];
    int[] gamepadAxis = new int[2];
    // -------------------------------- physics loop --------------------------------
    double playerX = 0;
    double playerZ = 0;
    System.out.println((System.nanoTime() - nanoTime) / 1_000_000);
    while (!systemExit) {
      LinkedHashMap<Engine3d.Node, Tr> world = new LinkedHashMap<>();
      boolean[] mouseClick = new boolean[10];
      boolean[] mouseRelease = new boolean[10];
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
            (buttonOn ? mouseClick : mouseRelease)[button] = true;
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
      double playerYaw = gamepadAxis[0] * MOUSE_SENSITIVITY;
      double playerPitch = gamepadAxis[1] * MOUSE_SENSITIVITY;
      double ws = Math.sin(playerYaw * 2 * Math.PI) * WALKING_SPEED * UPDATE_PERIOD_S;
      double wc = Math.cos(playerYaw * 2 * Math.PI) * WALKING_SPEED * UPDATE_PERIOD_S;
      if (gamepadButton[0]) { playerX -= wc; playerZ -= ws; }
      if (gamepadButton[1]) { playerX -= ws; playerZ += wc; }
      if (gamepadButton[2]) { playerX += ws; playerZ -= wc; }
      if (gamepadButton[3]) { playerX += wc; playerZ += ws; }
      double playerY = surfaceY(playerX, playerZ) + 1.8;
      apple.run(UPDATE_PERIOD_S);
      if (mouseButton[1]) {
        double s = Math.sin(playerYaw * 2 * Math.PI);
        double c = Math.cos(playerYaw * 2 * Math.PI);
        double forward = 1.1;
        double right = 0.4;
        apple.launch(
            playerX + s * forward + c * right, playerY + 0.1,
            playerZ - c * forward + s * right, playerYaw, playerPitch, 20); // 15-20 m/s is pretty average
      }

      for (int i = 0; i < tiles.length; i++) {
        // px = tx + i * BW
        int xi = (int) Math.round((playerX - tilexz[2 * i]) / BOX_WIDTH);
        int zi = (int) Math.round((playerZ - tilexz[2 * i + 1]) / BOX_WIDTH);
        double xd = tilexz[2 * i] + BOX_WIDTH * xi - playerX;
        double zd = tilexz[2 * i + 1] + BOX_WIDTH * zi - playerZ;
        double d = 1 - Math.sqrt(xd * xd + zd * zd) / BOX_WIDTH * 2.1; // 2.0 - 2.5
        double br = Math.max(0, d);
        world.put(tiles[i], new Tr(xi * BOX_WIDTH, 0, zi * BOX_WIDTH, (int) (br * 0xFF) * 0x010101 | 0xFF000000));
      }
      double mny = tick / 6000.0; // 1 spin per 60 sec
      double mnp = Math.sin(tick / 11000.0 * 2 * Math.PI) / 20 + 0.06; // 1 spin per 60 sec
      tick++;

      // done
      world.put(engine3d.camera(), new Tr(playerX, playerY, playerZ, playerYaw, playerPitch, 0));
      world.put(horizon, new Tr(playerX, playerY, playerZ, 0, 0, 0));
      world.put(moon, new Tr(0, 0, 0, mny, mnp, 0));
      apple.worldUpdate(world);

      renderLoop.world = world;
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
    //try { Thread.sleep(20000); } catch (InterruptedException ignore) {}
    new Sketch3().run();
  }

  private class Particle {
    Pnt p = new Pnt();
    Pnt v = new Pnt();
    Pnt a = new Pnt();
    boolean visible;
    Engine3d.Node node;
    public Particle run(double time) {
      v.add(a, time);
      p.add(v, time);
      return this;
    }
  }

  private class Projectile extends Particle {
    private final static double TRAIL_PERIOD = 0.2;
    Engine3d.Node[] trail;
    Pnt[] trailPnt;
    Engine3d.Node light;
    double time;
    int timeInt;
    public Projectile() {
      this(0, a -> null);
    }
    public Projectile(int trail, Function<Double, Engine3d.Node> supplier) {
      this.trail = new Engine3d.Node[trail];
      this.trailPnt = new Pnt[trail];
      for (int i = 0; i < trail; i++) {
        this.trail[i] = supplier.apply((trail + 1 - i) / (double) (trail + 1));
        this.trailPnt[i] = new Pnt();
      }
    }
    void launch(double x, double y, double z, double yaw, double pitch, double speed) {
      p = new Pnt(x, y, z);
      Arrays.fill(trailPnt, new Pnt(0, -1000, 0));
      double c = Math.cos(pitch * 2 * Math.PI) * speed;
      v = new Pnt(Math.sin(yaw * 2 * Math.PI) * c, Math.sin(pitch * 2 * Math.PI) * speed, -Math.cos(yaw * 2 * Math.PI) * c);
      a = new Pnt(0, -GRAVITY, 0);
      time = 0;
      timeInt = 0;
    }

    @Override
    public Particle run(double time) {
      this.time += time;
      int t = (int) Math.floor(this.time / TRAIL_PERIOD);
      if (t > timeInt) {
        timeInt = t;
        for (int i = trailPnt.length - 1; i > 0; i--) trailPnt[i] = trailPnt[i - 1];
        trailPnt[0] = p.clone();
      }
      super.run(time);
      return this;
    }

    void worldUpdate(LinkedHashMap<Engine3d.Node, Tr> world) {
      double a = System.nanoTime() / 1_000_000_000.0 * 4;
      Tr tr = new Tr(p.x, p.y, p.z, 0, a, 0);
      world.put(node, tr);
      if (light != null) world.put(light, tr);
      for (int i = 0; i < trail.length; i++) world.put(trail[i], new Tr(trailPnt[i].x, trailPnt[i].y, trailPnt[i].z));
    }
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

  private static class Tr implements Consumer<Engine3d.Node> {
    double x;
    double y;
    double z;
    double yaw;
    double pitch;
    double roll;
    Integer color;

    public Tr(double x, double y, double z, double yaw, double pitch, double roll) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.yaw = yaw;
      this.pitch = pitch;
      this.roll = roll;
    }

    public Tr(double x, double y, double z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }

    public Tr(double x, double y, double z, int color) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.color = color;
    }

    @Override
    public void accept(Engine3d.Node node) {
      if (color != null && node instanceof Engine3d.Light) ((Engine3d.Light) node).setColor(color);
      if (color != null && node instanceof Engine3d.Shape) ((Engine3d.Shape) node).setColor(color);
      node.translation(x, y, z).rotation(yaw, pitch, roll);
    }
  }

  private class RenderLoop implements Runnable {
    Map<Engine3d.Node, Tr> world = Collections.emptyMap();
    @Override
    public void run() {
      while (!systemExit) {
        Map<Engine3d.Node, Tr> world = this.world;
        if (world.isEmpty()) {
          try { Thread.sleep(1); } catch (InterruptedException e) { break; }
          continue;
        }
        world.forEach((key, value) -> value.accept(key));
        engine3d.update();
        screen.update();
      }
    }
  }

}
