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

package ab;

import ab.nbsnk.EngineFx;
import ab.nbsnk.Obj;
import com.sun.javafx.perf.PerformanceTracker;
import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Point3D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.Random;

public class TeapotFxMark {
  // https://en.wikipedia.org/wiki/Utah_teapot#OBJ_conversion
  // https://www.cs.utah.edu/~natevm/newell_teaset/newell_teaset.zip

  public static class JavaFx extends Application {
    private static Scene scene;
    @Override
    public void start(Stage stage) {
      stage.setScene(scene);
      stage.setFullScreen(true);
      stage.show();
    }
    public static void launch(Scene scene) {
      JavaFx.scene = scene;
      launch();
    }
  }

  private final Obj teapot;
  public TeapotFxMark() {
    teapot = Obj.load(getClass().getResourceAsStream("teapot.obj"));
  }

  private MeshView getMeshView() {
    MeshView meshView = new MeshView();
    meshView.setMesh(EngineFx.loadObj(teapot));
    PhongMaterial material = new PhongMaterial();
    material.setDiffuseColor(Color.DARKGRAY);
    material.setSpecularColor(Color.WHITE);
    meshView.setMaterial(material);
    return meshView;
  }

  public void run() {
    Text text = new Text();
    text.setTranslateX(-90);
    text.setTranslateY(50);
    text.setTranslateZ(200);
    Group root = new Group(text);
    Scene scene = new Scene(root, 192, 108, true, SceneAntialiasing.DISABLED);
    PerspectiveCamera camera = new PerspectiveCamera(true);
    camera.setFarClip(1000);
    scene.setCamera(camera);
    scene.setFill(Color.GRAY);
    PerformanceTracker sceneTracker = PerformanceTracker.getSceneTracker(scene);
    AnimationTimer timer = new AnimationTimer() {
      @Override
      public void handle(long now) {
        text.setText(String.format("%03.0f", sceneTracker.getAverageFPS()));
        double r = System.nanoTime() / 10_000_000.0;
        for (Node child : root.getChildren()) if (!(child instanceof Text)) child.setRotate(r);
      }
    };
    timer.start();

    // add teapots
    Random random = new Random(0);
    for (int y = 0; y < 10; y++) {
      for (int x = 0; x < 10; x++) {
        MeshView meshView = getMeshView();
        meshView.setTranslateX((x - 4.5) * 6);
        meshView.setTranslateY((y - 4.5) * 5);
        meshView.setTranslateZ(100);
        meshView.setRotationAxis(new Point3D(random.nextDouble() - 0.5,
            random.nextDouble() - 0.5, random.nextDouble() - 0.5));
        root.getChildren().add(meshView);
      }
    }

    JavaFx.launch(scene);
  }

  public static void main(String[] args) {
    System.setProperty("prism.forceGPU", "true");
    new TeapotFxMark().run();
  }
}
