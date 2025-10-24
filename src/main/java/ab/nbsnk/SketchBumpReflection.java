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
    double[] gamepadAxis = new double[3];
    boolean systemExit = false;

    // shapes
    Obj obj0 = obj1;
    BufferedImage obj0bump = obj1bump;
//    obj0 = obj2; obj0bump = obj2bump;
    Engine3d.Node node = engine3d.shape(obj0).setBumpMap(obj0bump).setSpecular(-1, 100).translation(0, 0, -5);
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
            gamepadAxis[0] -= Integer.parseInt(xys[0]);
            gamepadAxis[1] += Integer.parseInt(xys[1]);
          }
          if (bw == 'W') gamepadAxis[2] += (key.charAt(5) == '+') ? 1 : -1;
        }
      }
      node.rotation(gamepadAxis[0] / 1000.0, gamepadAxis[1] / 1000.0, gamepadAxis[2] / 12.0);
      engine3d.update();
      screen.update();
      try { Thread.sleep(20); } catch (InterruptedException ignore) {}
    }
    screen.close();
    engine3d.close();
  }
}
