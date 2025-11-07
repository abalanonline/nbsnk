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
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

// model Toyota CHR by Metos CC BY 4.0 https://skfb.ly/o6KEY
// photo ONroute West Lorne Canadian Tire Gas+
public class Demo1 {
  public static void main(String[] args) {
    // FIXME: 2025-11-06 windows and mirrors specular and reflection
    double[] lights = new double[]{
        0.7607163762779245, 0.490930452410943, 0.4246149853198427,
        -0.9873558794765415, 0.1293659769811316, 0.09161228772835331,
        -0.4731850641032841, 0.54659537127593, 0.6908902917318339,
        -0.002003860949269666, 0.2603913478172573, 0.9655010774324428,
        0.8015124880587781, 0.2791403225906539, 0.5288273931915903,
        0.9224854104185654, 0.3548753619380186, 0.15193467364051944,
        0.8065999816217034, 0.3372205202522469, -0.48546759971049724,
        0.6108367943242081, 0.26398612578844394, -0.746451429157271,
//        0.9408104859521214, 0.1777877066356513, 0.28856049780205273, // 2
//        0.9803445516663639, 0.19353944725934205, 0.03830198914709537, // 2
//        0.9303834967853071, 0.19322654400057493, -0.3115285726913948, // 2
//        0.8372170492288421, 0.17372932789041015, -0.5185419299452037, // 2
        -0.4002789506127817, 0.4078173981276859, -0.8206471418829746,
        -0.5598609099317171, 0.1914850113381522, -0.8061570888873071,
        -0.7012092236547449, 0.14991312396248707, -0.697016269483947,
        -0.9296693273161385, 0.19248714069344278, 0.3141076925438493,
    };
    boolean debug = false;
    boolean hdtv = false;
//    debug = true;
    double far = 100_000.0;
    Engine3d engine3d = new EngineNbs().setFarClip(far);
    FpsMeter fpsMeter = new FpsMeter();
    if (!hdtv) engine3d.textSupplier(() -> String.format("fps: %.0f", fpsMeter.getFps()));
    SceneViewer sceneViewer = new SceneViewer(engine3d, new Dimension(hdtv ? 1280 : 640, hdtv ? 720 : 360));
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
    for (int i = 0; i < lights.length / 3; i++) {
      engine3d.light().setColor(new Col(i < 2 ? 0xFFFF6600 : -1).mul(0.3).argb())
          .translation(lights[3 * i] * far, lights[3 * i + 1] * far, lights[3 * i + 2] * far);
    }

    //System.out.println(Fbx.fromPath(Paths.get("assets/demo1"))[0].root.debug());
    Geometry[] geometry = Geometry.load(Paths.get("assets/demo1"));
    for (Geometry g : geometry) {
      Obj obj = debug ? new Shapes.Cube().scale(100) : g.getObj();
      String modelName = (String) g.model.property[1];
      String materialName = (String) g.material.property[1];
      if (modelName.equals("logo_front\u0000\u0001Model")) obj.translate(-7, -42, -5); // logo fix
      Optional.ofNullable(g.getDiffuseMap()).ifPresent(image -> obj.image = image);
      Engine3d.Shape shape = engine3d.shape(obj);
      shape.setColor(g.getDiffuseColor());
      if (g.specularColor != null) {
        shape.setSpecular(g.specularColor.argb(), Optional.ofNullable(g.shininessExponent).orElse(32.0));
      }
      if (g.reflectionFactor != null) shape.setReflectionMap(photosphere, g.reflectionFactor, sceneViewer.sky);
      Optional.ofNullable(g.getBumpMap()).ifPresent(shape::setBumpMap);
      if (materialName.equals("interior\u0000\u0001Material")) {
        shape.setSpecular(0, 0);
        shape.setColor(0xFF111111);
      }
      if (materialName.equals("tire\u0000\u0001Material")) shape.setSpecular(0, 0); // specular rubber fix
      if (materialName.equals("mirror\u0000\u0001Material")) {
        shape.setColor(0xFF000000);
        shape.setReflectionMap(photosphere, 0.2, sceneViewer.sky);
      }
      if (materialName.equals("windowglass\u0000\u0001Material")) shape.setSpecular(-1, 200);
      if (materialName.equals("rim\u0000\u0001Material")) shape.setSpecular(-1, 100);
      if (materialName.equals("carpaint\u0000\u0001Material")) {
        shape.setReflectionMap(photosphere, 0.4, sceneViewer.sky);
        shape.setSpecular(-1, 100);
      }
      shape.connect(sceneViewer.node);
      if (debug) {
        shape.setColor(0xFF000000);
        shape.setReflectionMap(photosphere, 0.4, sceneViewer.sky);
        shape.setSpecular(-1, 320.0);
        break;
      }
    }
    AtomicLong nanoTime = new AtomicLong();
    if (!debug) sceneViewer.rotation = () -> {
      if (hdtv) {
        if (nanoTime.get() == 0L) nanoTime.set(System.nanoTime());
        long n = nanoTime.addAndGet(1_000_000_000) - System.nanoTime();
        if (n < 0) throw new IllegalStateException();
        try { Thread.sleep(n / 1_000_000); } catch (InterruptedException e) {}
      }
      double v = System.nanoTime() / 20_000_000_000.0 / (hdtv ? 10 : 1);
      return new double[]{0, 0, 0, v, -0.01};
    };
    sceneViewer.run();
  }
}
