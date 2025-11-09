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
import ab.nbsnk.Obj;
import ab.nbsnk.SceneViewer;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;

public class Scene2 {
  public static void main(String[] args) {
    boolean hdtv = false;
    Engine3d engine3d = new EngineFx();
    SceneViewer sceneViewer = new SceneViewer(engine3d, new Dimension(hdtv ? 1280 : 640, hdtv ? 720 : 360));
    Obj max = Obj.load(Paths.get("assets/demo1/max.obj"));
    max.translate(-128, -140, -60);
    Obj.scale(max, 0.04 * 7 / 6, 0.04, 0.04);
    max.flatNormal().interpolateNormal();
    max.texture = new double[2];
    BufferedImage photosphere = Obj.image(Paths.get("assets/demo1/max.png"));
    BufferedImage p2 = Obj.image(Paths.get("assets/demo1/max3.png"));
    Obj sphereObj = Obj.load(Engine3d.class.getResourceAsStream("blender_uv_sphere.obj"))
        .interpolateNormal().scale(10).ry90().inverted().withImage(photosphere);
    Engine3d.Shape back = engine3d.shape(sphereObj).selfIllumination(-1);
    engine3d.shape(max).setColor(0xFFCCCCCC).setSpecular(-1, 32)//.setReflectionMap(p2, 0.3, sceneViewer.sky)
        .rotation(0, 0.75, 0).translation(0, 0, -1.5).connect(sceneViewer.node);

    engine3d.light().translation(-30, 30, 100);
    sceneViewer.rotation = nanoTime -> {
      double v = nanoTime / 20_000_000_000.0 / (hdtv ? 50 : 1);
      double temperature = 0.07;
      back.rotation(0, 0, 0.25 + Math.sin(2 * Math.PI * 2 * v) * 0.1);
      return new double[]{0, 0, 0, Math.sin(2 * Math.PI * v) * temperature, Math.cos(2 * Math.PI * v) * temperature};
    };
    if (hdtv) {
      sceneViewer.fps = 1;
      sceneViewer.engine3d.textSupplier(null);
    }
    sceneViewer.run();
  }
}
