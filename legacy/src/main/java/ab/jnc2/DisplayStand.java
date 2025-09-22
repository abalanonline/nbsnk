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
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.image.Image;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.function.Consumer;

/**
 * A virtual rotating display stand for showcasing 3D objects.
 * Provides continuous automatic rotation for clear viewing from all angles.
 */
public class DisplayStand implements AutoCloseable {

  public static class Obj {
    public int[] face; // vertex, normal, texture
    public double[] vertex;
    public double[] normal;
    public double[] texture; // 0 <= (x, y) <= 1, Y-up, as in .obj
    public BufferedImage image;
    public String id;

    public static final Obj TETRAHEDRON = createTetrahedron();
    private static Obj createTetrahedron() {
      Obj obj = new Obj();
      obj.id = "tetrahedron";
      obj.vertex = new double[]{1, 1, 1, -1, -1, 1, -1, 1, -1, 1, -1, -1};
      int[] face = new int[]{0, 2, 1, 0, 1, 3, 0, 3, 2, 1, 2, 3};
      obj.face = new int[face.length * 3];
      for (int i = 0; i < face.length; i++) obj.face[i * 3] = face[i];
      return obj;
    }
  }

  private final Rotate rd;
  private final Rotate ry;
  private final Rotate rx;
  private final Rotate rz;
  private final MeshView meshView;
  private final Text nameText;
  private Consumer<String> keyListener;
  private long mouseTime;

  public Scene getScene() {
    // TODO: improve this method when necessary
    PhongMaterial material = new PhongMaterial();
    material.setDiffuseColor(Color.DARKGRAY);
    material.setSpecularColor(Color.WHITE);
    meshView.setMaterial(material);
    meshView.getTransforms().addAll(rx, ry, rz, rd);

    Group root = new Group(meshView);

    PerspectiveCamera camera = new PerspectiveCamera(true);
    camera.getTransforms().addAll(
        new Rotate(180, Rotate.X_AXIS), // flip Y
        new Translate(0, 0, -4));

    PointLight light = new PointLight(Color.WHITE);
    light.setTranslateX(-5);
    light.setTranslateY(3);
    light.setTranslateZ(5);
    root.getChildren().add(light);

    AnimationTimer timer = new AnimationTimer() {
      @Override
      public void handle(long now) {
        if (System.nanoTime() < mouseTime + 2_000_000_000) return;
        long epochMilli = Instant.now().toEpochMilli();
        rx.setAngle(0);
        rz.setAngle(23.44);
        rd.setAngle(epochMilli / 6_000.0 * 360);
        ry.setAngle(epochMilli / 60_000.0 * 360);
      }
    };
    timer.start();

    SubScene subScene = new SubScene(root, 512, 512, true, SceneAntialiasing.DISABLED);
    subScene.setFill(Color.GRAY);
    subScene.setCamera(camera);

    Text timeText = new Text();
    //timeText.setText("Time: " + Instant.now());
    timeText.setFont(Font.font(20));
    timeText.setFill(Color.WHITE);

    nameText.setFont(Font.font("monospace", 20));
    nameText.setFill(Color.WHITE);

    StackPane overlay = new StackPane();
    overlay.setPadding(new Insets(10));
    overlay.getChildren().addAll(timeText, nameText);
    StackPane.setAlignment(timeText, Pos.BOTTOM_LEFT);
    StackPane.setAlignment(nameText, Pos.BOTTOM_LEFT);
    timeText.setTranslateY(-25);
    Scene scene = new Scene(new StackPane(subScene, overlay));
    scene.setOnKeyPressed(this::onKeyEvent);
    scene.setOnMousePressed(this::onMouseEvent);
    scene.setOnMouseDragged(this::onMouseEvent);
    return scene;
  }

  private double onMouseEventX;
  private double onMouseEventY;
  public void onMouseEvent(MouseEvent mouseEvent) {
    this.mouseTime = System.nanoTime();
    double x = mouseEvent.getSceneX();
    double y = mouseEvent.getSceneY();
    if (mouseEvent.getEventType().equals(MouseEvent.MOUSE_DRAGGED)) {
      rd.setAngle(0);
      rz.setAngle(0);
      ry.setAngle(ry.getAngle() + (x - onMouseEventX) / 2);
      rx.setAngle(rx.getAngle() + (y - onMouseEventY) / 2);
    }
    onMouseEventX = x;
    onMouseEventY = y;
  }

  public void onKeyEvent(KeyEvent keyEvent) {
    // TODO: improve this method when necessary
    String s = keyEvent.getCode().getName();
    switch (keyEvent.getCode()) {
      case ESCAPE: s = "Esc"; break;
      case LEFT: s = "Left"; break;
      case DOWN: s = "Down"; break;
      case UP: s = "Up"; break;
      case RIGHT: s = "Right"; break;
    }
    if (keyEvent.isShiftDown()) s = "Shift+" + s;
    if (keyEvent.isAltDown()) s = "Alt+" + s;
    if (keyEvent.isControlDown()) s = "Ctrl+" + s;
    if (s.equals("Esc")) close();
    Consumer<String> keyListener = this.keyListener;
    if (keyListener != null) keyListener.accept(s);
  }

  public void setKeyListener(Consumer<String> keyListener) {
    this.keyListener = keyListener;
  }

  public void setObj(Obj obj) {
    // normal
    boolean useNormal = obj.normal != null;
    float[] normals = null;
    if (useNormal) {
      normals = new float[obj.normal.length];
      for (int i = 0; i < normals.length; i++) normals[i] = (float) obj.normal[i];
    }

    // face
    int[] faces = new int[obj.face.length / 3 * (useNormal ? 3 : 2)];
    for (int i = 0, j = 0; i < faces.length;) {
      faces[i++] = obj.face[j++]; // v
      faces[i] = obj.face[j++]; // n
      if (useNormal) i++;
      faces[i++] = Math.max(0, obj.face[j++]); // t
    }

    // vertex
    double maxPoint = 0;
    for (int i = 0; i < obj.vertex.length;) {
      double x = obj.vertex[i++];
      double y = obj.vertex[i++];
      double z = obj.vertex[i++];
      maxPoint = Math.max(maxPoint, x * x + y * y + z * z);
    }
    if (maxPoint == 0) maxPoint = 1;
    maxPoint = Math.sqrt(maxPoint);
    float[] points = new float[obj.vertex.length];
    // manually scaling points because meshView.setScaleX(), .setScaleY() don't work as expected
    for (int i = 0; i < points.length; i++) points[i] = (float) (obj.vertex[i] / maxPoint);

    // texture
    boolean useTexture = obj.texture != null;
    float[] texCoords = new float[2];
    if (useTexture) {
      texCoords = new float[obj.texture.length];
      for (int i = 0; i < texCoords.length; i += 2) {
        texCoords[i] = (float) obj.texture[i];
        texCoords[i + 1] = (float) (1 - obj.texture[i + 1]); // flip Y
      }
    }
    PhongMaterial material = new PhongMaterial();
    if (useTexture && obj.image != null) {
      ByteArrayOutputStream stream = new ByteArrayOutputStream();
      try {
        ImageIO.write(obj.image, "png", stream);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      ByteArrayInputStream inputStream = new ByteArrayInputStream(stream.toByteArray());
      material.setDiffuseMap(new Image(inputStream));
    } else {
      material.setDiffuseColor(Color.DARKGRAY);
    }
    material.setSpecularColor(Color.WHITE);

    // mesh
    TriangleMesh mesh = new TriangleMesh(useNormal ? VertexFormat.POINT_NORMAL_TEXCOORD : VertexFormat.POINT_TEXCOORD);
    mesh.getPoints().addAll(points);
    mesh.getTexCoords().addAll(texCoords);
    mesh.getFaces().addAll(faces);
    if (useNormal) mesh.getNormals().addAll(normals);
    meshView.setMesh(mesh);
    meshView.setMaterial(material);
    nameText.setText(obj.id);
  }

  public DisplayStand() {
    System.setProperty("prism.forceGPU", "true"); // can fail if the PrismSettings static initializer has been invoked
    meshView = new MeshView(); // MeshView constructor triggers static initializers, so don't create it in field level
    nameText = new Text(); // and this too
    rd = new Rotate(0, Rotate.Y_AXIS);
    ry = new Rotate(0, Rotate.Y_AXIS);
    rx = new Rotate(0, Rotate.X_AXIS);
    rz = new Rotate(0, Rotate.Z_AXIS);
    setObj(Obj.TETRAHEDRON);
  }

  public void open() {
    if (!com.sun.prism.impl.PrismSettings.forceGPU) throw new IllegalStateException("failed to set property");
    JavaFx.launch(getScene());
  }

  @Override
  public void close() {
    Platform.exit();
  }

  public static class JavaFx extends Application {
    private static Scene scene;
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
    new DisplayStand().open();
  }

}
