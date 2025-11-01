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

import ab.fbx.Geometry;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.nio.file.Paths;
import java.util.Optional;

public class Demo1 {
  public static void main(String[] args) {
    double far = 100_000.0;
    Engine3d engine3d = new EngineNbs().setFarClip(far);
    SceneViewer sceneViewer = new SceneViewer(engine3d, new Dimension(640, 360));
    sceneViewer.cameraRig.translation(0, 0, 1000);

    BufferedImage photosphere = Obj.image(Paths.get("assets/photosphere.jpg"));
    engine3d.shape(Obj.load(Engine3d.class.getResourceAsStream("blender_uv_sphere.obj"))
        .interpolateNormal().scale(far * 0.95).ry90().inverted()
        .withImage(photosphere)).selfIllumination(-1).connect(sceneViewer.sky);
    engine3d.light().setColor(-1).translation(-far, far / 4, far);

    Geometry[] geometry = Geometry.load(Paths.get("assets/demo1"));
    for (Geometry g : geometry) {
      Obj obj = g.getObj();
      Optional.ofNullable(g.getDiffuseMap(g)).ifPresent(image -> obj.image = image);
      Engine3d.Shape shape = engine3d.shape(obj);
      shape.setColor(g.getDiffuseColor());
      shape.setSpecular(-1, 100).setReflectionMap(photosphere, 0.3, sceneViewer.sky)
          .connect(sceneViewer.node);
    }
    sceneViewer.run();
  }
}
