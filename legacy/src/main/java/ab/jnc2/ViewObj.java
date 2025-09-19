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

package ab.jnc2;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ViewObj {
  float[] points = {1, 1, 1, -1, -1, 1, -1, 1, -1, 1, -1, -1}; // tetrahedron
  int[] faces = {0, 0, 2, 0, 1, 0, 0, 0, 1, 0, 3, 0, 0, 0, 3, 0, 2, 0, 1, 0, 2, 0, 3, 0};
  Rotate rd = new Rotate(0, Rotate.Y_AXIS);
  Rotate ry = new Rotate(0, Rotate.Y_AXIS);

  public void load(String objFileName) {
    String string;
    try {
      string = Files.readString(Paths.get(objFileName));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    List<Double> points = new ArrayList<>();
    List<Integer> faces = new ArrayList<>();
    for (String line : string.split("\r?\n")) {
      String[] s = line.split("\\s+");
      switch (s[0]) {
        case "v":
          for (int i = 1; i < 4; i++) points.add(Double.parseDouble(s[i]));
          break;
        case "f":
          for (int t = 3; t < s.length; t++)
            for (int i : new int[]{1, t - 1, t})
              faces.add(Integer.parseInt(s[i].split("/")[0]));
          break;
        //default:
        //  System.out.println(s);
      }
    }
    int size = points.size();
    this.points = new float[size];
    for (int i = 0; i < size; i++) this.points[i] = points.get(i).floatValue();
    size = faces.size();
    this.faces = new int[size * 2];
    for (int i = 0; i < size; i++) this.faces[i * 2] = faces.get(i) - 1;
  }

  public Scene getScene() {
    float pointMax = 0;
    for (int i = 0; i < points.length;) {
      float x = points[i++];
      float y = points[i++];
      float z = points[i++];
      pointMax = Math.max(pointMax, x * x + y * y + z * z);
    }
    pointMax = (float) Math.sqrt(pointMax);

    TriangleMesh mesh = new TriangleMesh();
    mesh.getPoints().addAll(points);
    mesh.getTexCoords().addAll(0, 0);
    mesh.getFaces().addAll(faces);
    MeshView meshView = new MeshView(mesh);
    PhongMaterial material = new PhongMaterial();
    material.setDiffuseColor(Color.DARKGRAY);
    material.setSpecularColor(Color.WHITE);
    meshView.setMaterial(material);
    meshView.getTransforms().addAll(ry, new Rotate(23.44, Rotate.Z_AXIS), rd);

    Group root = new Group(meshView);

    PerspectiveCamera camera = new PerspectiveCamera(true);
    camera.setFarClip(10 * pointMax);
    camera.getTransforms().addAll(
        new Rotate(180, Rotate.X_AXIS), // one line that fixes everything
        new Translate(0, 0, -5 * pointMax));

    PointLight light = new PointLight(Color.WHITE);
    light.setTranslateX(-5 * pointMax);
    light.setTranslateY(3 * pointMax);
    light.setTranslateZ(5 * pointMax);
    root.getChildren().add(light);

    AnimationTimer timer = new AnimationTimer() {
      @Override
      public void handle(long now) {
        long epochMilli = Instant.now().toEpochMilli();
        rd.setAngle(epochMilli / 6_000.0 * 360);
        ry.setAngle(epochMilli / 60_000.0 * 360);
      }
    };
    timer.start();

    Scene scene = new Scene(root, 512, 512, true);
    scene.setFill(Color.GRAY);
    scene.setCamera(camera);
    return scene;
  }

  public ViewObj(String objFileName) {
    System.setProperty("prism.forceGPU", "true");
    if (objFileName != null) load(objFileName);
    JavaFx.launch(getScene());
  }

  public static class JavaFx extends Application {
    static Scene scene;
    @Override
    public void start(Stage stage) {
      stage.setScene(scene);
      stage.show();
    }
    public static void launch(Scene scene) {
      JavaFx.scene = scene;
      launch();
    }
  }

  public static void main(String[] args) {
    //args = new String[]{"legacy/src/main/resources/jnc2/teapot.obj"};
    new ViewObj(args.length > 0 ? args[0] : null);
  }

}
