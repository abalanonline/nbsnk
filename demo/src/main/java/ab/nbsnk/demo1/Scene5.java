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

package ab.nbsnk.demo1;

import ab.nbsnk.Engine3d;
import ab.nbsnk.EngineFx;
import ab.nbsnk.EngineNbs;
import ab.nbsnk.Obj;
import ab.nbsnk.SceneViewer;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;

public class Scene5 {
  public static void main(String[] args) {
    Obj obj = Obj.load(Scene5.class.getResourceAsStream("/ab/teapot.obj")).scale(1/2.6).translate(0, -0.6, 0);
    BufferedImage background = new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB);
    background.getGraphics().clearRect(0, 0, 1280, 720);
    Engine3d engine3d = new EngineNbs().background(background);
    SceneViewer sceneViewer = new SceneViewer(engine3d, new Dimension(1280, 720));
    engine3d.light().translation(-100, 100, 100);
    engine3d.setAmbient(0xFF222222);
    Engine3d.Node teapot = engine3d.shape(obj).setColor(0xFFDDDDDD).setSpecular(-1, 50).connect(sceneViewer.node);
    sceneViewer.cameraRig.translation(0, 0, 4.7);

    sceneViewer.rotation = nanoTime -> {
      double v = nanoTime / 60_000_000_000.0;
      teapot.rotation(-v * 9, 0, 0);
      return new double[]{-v, -23.4 / 360, 0, 0, 0};
    };
    sceneViewer.engine3d.textSupplier(null);
    sceneViewer.outputLimit = 60 * 50;
    sceneViewer.outputFolder = Paths.get("assets/output");
    sceneViewer.run();
  }
}
