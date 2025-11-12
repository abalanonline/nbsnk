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

import ab.Collada;
import ab.nbsnk.Engine3d;
import ab.nbsnk.EngineFx;
import ab.nbsnk.EngineNbs;
import ab.nbsnk.Obj;
import ab.nbsnk.SceneViewer;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;

// model Egyptian Cat by Ankledot CC BY 4.0 https://skfb.ly/6ACLr
public class Scene4 {
  public static void main(String[] args) {
    Obj obj = Collada.read(Paths.get("assets/demo1/cat.dae"));
    obj.image = Obj.image(Paths.get("assets/demo1/cat.jpg"));
    boolean hdtv = true;
    Engine3d engine3d = new EngineFx().background(Obj.image(Paths.get("assets/demo1/wallpaper.png")));
    SceneViewer sceneViewer = new SceneViewer(engine3d, new Dimension(1280, 720));
    engine3d.light().translation(-100, 100, 100);
    engine3d.shape(obj).setBumpMap(Obj.image(Paths.get("assets/demo1/cat_n.png")))
        .setColor(-1).setSpecular(-1, 32).connect(sceneViewer.node);
    sceneViewer.cameraRig.translation(0, 0, 3.9);

    sceneViewer.rotation = nanoTime -> {
      double v = nanoTime / 20_000_000_000.0;
      return new double[]{-v, 0, 0, 0, 0};
    };
    if (hdtv) {
      sceneViewer.engine3d.textSupplier(null);
      sceneViewer.outputFolder = Paths.get("assets/output");
      sceneViewer.outputLimit = 1000;
    }
    sceneViewer.run();
  }
}
