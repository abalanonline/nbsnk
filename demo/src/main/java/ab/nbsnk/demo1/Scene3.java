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
import ab.nbsnk.EngineNbs;
import ab.nbsnk.Obj;
import ab.nbsnk.SceneViewer;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;

public class Scene3 {
  public static void main(String[] args) {
    boolean hdtv = false;
    Engine3d engine3d = new EngineNbs().setFarClip(2000000).setFocalLength(33);
    SceneViewer sceneViewer = new SceneViewer(engine3d, new Dimension(hdtv ? 1280 : 640, hdtv ? 720 : 360));
    engine3d.light().translation(33, 100, -24);

    BufferedImage photosphere = Obj.image(Paths.get("assets/demo1/bubble.png"));
    Obj sphereObj = Obj.load(Engine3d.class.getResourceAsStream("blender_uv_sphere.obj"))
        .interpolateNormal().scale(900000).ry90().inverted().withImage(photosphere);
    engine3d.shape(sphereObj).selfIllumination(-1);

    Obj obj = Obj.load(Engine3d.class.getResourceAsStream("blender_uv_sphere.obj")).interpolateNormal();
    //obj = new Shapes.Icosahedron().scale(1.1);
    engine3d.shape(obj).setColor(-1).setSpecular(0, 10000).setReflectionMap(photosphere, 0.9, sceneViewer.sky)
        .connect(sceneViewer.node);
    sceneViewer.cameraRig.translation(0, 0, 3.9);

    sceneViewer.rotation = nanoTime -> {
      double v = nanoTime / 20_000_000_000.0;
      return new double[]{0, 0, 0, v, 0};
    };
    if (hdtv) {
      sceneViewer.engine3d.textSupplier(null);
      sceneViewer.outputFolder = Paths.get("assets/output");
      sceneViewer.outputLimit = 1000;
    }
    sceneViewer.run();
  }
}
