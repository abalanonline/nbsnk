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
import ab.nbsnk.nodes.Col;
import ab.nbsnk.nodes.Shapes;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

// model Toyota CHR by Metos CC BY 4.0 https://skfb.ly/o6KEY
// photo ONroute West Lorne Canadian Tire Gas+
public class Demo1 {
  public static void main(String[] args) {
    // FIXME: 2025-11-06 windows and mirrors specular and reflection
    boolean debug = false;
//    debug = true;
    double far = 100_000.0;
    Engine3d engine3d = new EngineNbs().setFarClip(far);
    SceneViewer sceneViewer = new SceneViewer(engine3d, new Dimension(640, 360));
    sceneViewer.cameraRig.translation(0, 0, 600);

    BufferedImage photosphere = Obj.image(Paths.get("assets/demo1/photosphere.jpg"));
    Obj sphereObj = Obj.load(Engine3d.class.getResourceAsStream("blender_uv_sphere.obj"))
        .interpolateNormal().scale(far * 0.3).ry90().inverted()
        .withImage(photosphere);
    double sphereFloor = -0.093 * far;
    for (int i = 1; i < sphereObj.vertex.length; i += 3) if (sphereObj.vertex[i] < sphereFloor) sphereObj.vertex[i] = sphereFloor;
    engine3d.shape(sphereObj).selfIllumination(-1).connect(sceneViewer.sky);
    sceneViewer.sky.translation(0, debug ? -far : far * 0.005, -far * 0.4);
    sceneViewer.node.translation(0, debug ? 0 : -100, 0);
    engine3d.light().setColor(-1).translation(-far, far / 4, far);
    engine3d.light().setColor(-1).translation(-far, far / 4, -far);
    engine3d.light().setColor(-1).translation(far, far / 4, far);
    engine3d.light().setColor(-1).translation(far, far / 4, -far);

    //System.out.println(Fbx.fromPath(Paths.get("assets/demo1"))[0].root.debug());
    Geometry[] geometry = Geometry.load(Paths.get("assets/demo1"));
    for (int i = 0; i < geometry.length; i++) {
//      if (i < 58) continue;
      Geometry g = geometry[i];
      Obj obj = debug ? new Shapes.Cube().scale(100) : g.getObj();
      String modelName = (String) g.model.property[1];
      boolean cockpit = modelName.startsWith("cockpit");
//      System.out.println(modelName);
      if (modelName.equals("logo_front\u0000\u0001Model")) obj.translate(-7, -42, -5); // logo fix
      //Obj.normalizeNormal(obj);
      Optional.ofNullable(g.getDiffuseMap()).ifPresent(image -> obj.image = image);
      Engine3d.Shape shape = engine3d.shape(obj);
      shape.setColor(cockpit ? 0xFF111111 : g.getDiffuseColor());
      if (g.specularColor != null && !cockpit) {
        shape.setSpecular(g.specularColor.argb(), Optional.ofNullable(g.shininessExponent).orElse(32.0));
      }
      if (g.reflectionFactor != null) shape.setReflectionMap(photosphere, g.reflectionFactor, sceneViewer.sky);
      if (modelName.equals("door_handle\u0000\u0001Model")) shape.setReflectionMap(photosphere, 0.4, sceneViewer.sky);
      Optional.ofNullable(g.getBumpMap()).ifPresent(shape::setBumpMap);
      shape.connect(sceneViewer.node);
      if (debug) {
        shape.setColor(0xFF000000);
        shape.setReflectionMap(photosphere, 0.4, sceneViewer.sky);
        shape.setSpecular(-1, 32.0);
        break;
      }
    }
    if (!debug) sceneViewer.rotation = () -> {
      double v = System.nanoTime() / 20_000_000_000.0;
      return new double[]{0, 0, 0, v, -0.01};
    };
    sceneViewer.run();
  }
}
