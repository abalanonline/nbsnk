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

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

public class SceneViewer implements Runnable {
  public Engine3d engine3d;
  public Dimension screenSize;
  public Engine3d.Group cameraRails;
  public Engine3d.Group cameraRig;
  public Engine3d.Node camera;
  public Engine3d.Group sky;
  public Engine3d.Group node;
  public Supplier<double[]> rotation;

  public SceneViewer(Engine3d engine3d, Dimension screenSize) {
    this.engine3d = engine3d;
    this.screenSize = screenSize;
    cameraRails = engine3d.group();
    cameraRig = engine3d.group();
    cameraRig.translation(0, 0, 5).connect(cameraRails);
    camera = engine3d.camera().connect(cameraRig);
    sky = engine3d.group();
    sky.connect(cameraRig);
    node = engine3d.group();
    FpsMeter fpsMeter = new FpsMeter();
    engine3d.textSupplier(() -> String.format("fps: %.0f", fpsMeter.getFps()));
  }

  @Override
  public void run() {
    Queue<String> keyListener = new LinkedBlockingQueue<>();
    boolean[] gamepadButton = new boolean[10];
    double[] gamepadAxis = new double[9];
    boolean systemExit = false;
    Screen screen = new Screen();
    screen.gameController = true;
    screen.image = new BufferedImage(screenSize.width, screenSize.height, BufferedImage.TYPE_INT_RGB);
    screen.preferredSize = screenSize;
    screen.keyListener = keyListener::add;
    engine3d.open(screen.image);
    while (!systemExit) {
      while (!keyListener.isEmpty()) {
        String key = keyListener.remove();
        if ("Esc".equals(key)) systemExit = true;
        if (key.startsWith("Mouse")) {
          char bw = key.charAt(6);
          if (bw <= '9') {
            String[] xys = key.substring(5).split(",");
            int axis = (gamepadButton[1] ? 2 : 0) + (gamepadButton[3] ? 4 : 0);
            gamepadAxis[axis] -= Integer.parseInt(xys[0]);
            gamepadAxis[axis + 1] += Integer.parseInt(xys[1]);
          }
          if (bw == 'W') gamepadAxis[8] += (key.charAt(5) == '+') ? 1 : -1;
          if (bw == 'B') {
            int button = key.charAt(7) - '0';
            boolean buttonOn = key.charAt(5) == '+';
            gamepadButton[button] = buttonOn;
          }
        }
      }
      gamepadAxis[4] += gamepadAxis[6]; gamepadAxis[2] += gamepadAxis[6]; gamepadAxis[6] = 0;
      gamepadAxis[5] += gamepadAxis[7]; gamepadAxis[3] += gamepadAxis[7]; gamepadAxis[7] = 0;
      double[] rotation = this.rotation != null ? this.rotation.get() : new double[]{
          gamepadAxis[4] / 1000.0, gamepadAxis[5] / 1000.0, gamepadAxis[8] / 12.0,
          gamepadAxis[2] / 1000.0, gamepadAxis[3] / 1000.0};
      node.rotation(rotation[0], rotation[1], rotation[2]);
      cameraRails.rotation(rotation[3], rotation[4], 0);
      cameraRig.rotation(0, -rotation[4], 0);
      sky.rotation(-rotation[3], 0, 0);
      camera.rotation(0, rotation[4], 0);
      engine3d.update();
      screen.update();
      try { Thread.sleep(20); } catch (InterruptedException ignore) {}
    }
    screen.close();
    engine3d.close();

  }
}
