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
import ab.nbsnk.nodes.Shapes;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class SketchBumpReflection {
  public static void main(String[] args) {
    // https://commons.wikimedia.org/wiki/File:Normal_map_example_with_scene_and_result.png
    BufferedImage normalMapExampleWithSceneAndResult = Obj.image(
        Paths.get("assets/Normal_map_example_with_scene_and_result.png"));
    BufferedImage obj1bump = new BufferedImage(2048, 2048, BufferedImage.TYPE_INT_ARGB);
    obj1bump.getGraphics().drawImage(normalMapExampleWithSceneAndResult, -2048, 0, null);
    //obj1bump.getGraphics().drawImage(normalMapExampleWithSceneAndResult, -2048, 2048, 2048 * 3, -2048, null);
    try { ImageIO.write(obj1bump, "png", new File("assets/test.png")); } catch (IOException ignore) {}
    Obj obj1 = new Shapes.Square();
    //obj1.texture = new double[]{1, 0.5, 0.5, 1, 0, 0.5, 0.5, 0,};

    // https://commons.wikimedia.org/wiki/File:NormalMaps.png
    BufferedImage normalMaps = Obj.image(Paths.get("assets/NormalMaps.png"));
    BufferedImage obj2bump = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
    obj2bump.getGraphics().drawImage(normalMaps, -512, 0, null);
    obj2bump.getGraphics().drawImage(normalMaps, 2 * 512, 0, -3 * 512, 512, null);
    // certainly the sphere in wiki example has an x inverted texture coordinates
    BufferedImage obj2image = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
    obj2image.getGraphics().drawImage(normalMaps, 512, 0, -3 * 512, 512, null);
    Obj obj2 = Obj.load(Engine3d.class.getResourceAsStream("blender_uv_sphere.obj")).interpolateNormal(); // HD sphere
    obj2.image = obj2image;

    Screen screen = new Screen();
    screen.gameController = true;
    screen.image = new BufferedImage(640, 360, BufferedImage.TYPE_INT_RGB);
    screen.preferredSize = new Dimension(640, 360);
    int screenHeight = screen.image.getHeight();
    int screenWidth = screen.image.getWidth();
    BufferedImage background = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_ARGB);
    Sketch2.renderNoise(background);
    Engine3d engine3d = new EngineDual().open(screen.image).showFps().background(background);
    Queue<String> keyListener = new LinkedBlockingQueue<>();
    screen.keyListener = keyListener::add;
    boolean[] gamepadButton = new boolean[10];
    double[] gamepadAxis = new double[9];
    boolean systemExit = false;
    BufferedImage photosphere = Obj.image(Paths.get("assets/reflection_sphere.jpg"));
    Engine3d.Group cameraRails = engine3d.group();
    Engine3d.Group cameraRig = (Engine3d.Group) engine3d.group().translation(0, 0, 5).connect(cameraRails);
    Engine3d.Node camera = engine3d.camera().connect(cameraRig);
    Engine3d.Node sky = engine3d.shape(Obj.load(Engine3d.class.getResourceAsStream("blender_uv_sphere.obj"))
        .interpolateNormal().scale(95).ry90().inverted()
        .withImage(photosphere)).selfIllumination(-1).connect(cameraRig);

    // shapes
//    Engine3d.Node node = engine3d.shape(obj1).setBumpMap(obj1bump).setSpecular(-1, 100);
    Obj obj = Obj.load(Engine3d.class.getResourceAsStream("blender_uv_sphere.obj")).interpolateNormal();
    Engine3d.Node node = engine3d.shape(obj)
        .setSpecular(-1, 100).setReflectionMap(photosphere, 0.3, sky);
    engine3d.light().setColor(0xFFBF7F).translation(-100, 100, 100);
    //engine3d.camera().translation(5, 0, -5).rotation(-0.25, 0, 0);

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
      node.rotation(gamepadAxis[0] / 1000.0, gamepadAxis[1] / 1000.0, gamepadAxis[8] / 12.0);
      double cameraYaw = gamepadAxis[2] / 1000.0;
      double cameraPitch = gamepadAxis[3] / 1000.0;
      cameraRails.rotation(cameraYaw, cameraPitch, 0);
      cameraRig.rotation(0, -cameraPitch, 0);
      sky.rotation(-cameraYaw, 0, 0);
      camera.rotation(0, cameraPitch, 0);
      engine3d.update();
      screen.update();
      try { Thread.sleep(20); } catch (InterruptedException ignore) {}
    }
    screen.close();
    engine3d.close();
  }
}
