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

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;

public class EngineFx implements Engine3d {

  private int imageWidth;
  private int imageHeight;
  private BufferedImage image;
  private NodeFx camera;

  private static TriangleMesh loadObj(Obj obj) {
    int[] faces = Arrays.copyOf(obj.face, obj.face.length);
    float[] points = new float[obj.vertex.length];
    for (int i = 0; i < points.length; i += 3) {
      points[i] = (float) obj.vertex[i];
      points[i + 1] = (float) -obj.vertex[i + 1];
      points[i + 2] = (float) -obj.vertex[i + 2];
    }
    float[] normals = new float[obj.normal.length];
    for (int i = 0; i < normals.length; i += 3) {
      normals[i] = (float) obj.normal[i];
      normals[i + 1] = (float) -obj.normal[i + 1];
      normals[i + 2] = (float) -obj.normal[i + 2];
    }
    float[] texCoords = new float[2];
    if (obj.texture != null) {
      texCoords = new float[obj.texture.length];
      for (int i = 0; i < texCoords.length; i += 2) {
        texCoords[i] = (float) obj.texture[i];
        texCoords[i + 1] = (float) (1 - obj.texture[i + 1]); // flip Y
      }
    }

    TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
    mesh.getPoints().addAll(points);
    mesh.getTexCoords().addAll(texCoords);
    mesh.getFaces().addAll(faces);
    mesh.getNormals().addAll(normals);
    return mesh;
  }

  private static Image loadImg(BufferedImage image) {
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    try {
      ImageIO.write(image, "png", stream);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    ByteArrayInputStream inputStream = new ByteArrayInputStream(stream.toByteArray());
    return new Image(inputStream);
  }

  @Override
  public EngineFx open(BufferedImage image) {
    System.setProperty("prism.forceGPU", "true");
    this.image = image;
    this.imageWidth = image.getWidth();
    this.imageHeight = image.getHeight();

    JavaFx.App.imageWidth = this.imageWidth;
    JavaFx.App.imageHeight = this.imageHeight;
    CompletableFuture.runAsync(() -> JavaFx.App.main(new String[0]));
    try {
      JavaFx.App.io.take();
    } catch (InterruptedException ignore) {}
    this.camera = new NodeFx(JavaFx.App.camera);
    return this;
  }

  @Override
  public void background(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    WritableImage writableImage = new WritableImage(width, height);
    int[] data = new int[width * height];
    image.getRaster().getDataElements(0, 0, width, height, data);
    writableImage.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbPreInstance(), data, 0, width);
    JavaFx.App.scene.setFill(new ImagePattern(writableImage));
  }

  @Override
  public ShapeFx shape(Obj obj) {
    MeshView meshView = new MeshView(loadObj(obj));
    if (obj.image != null) {
      PhongMaterial material = new PhongMaterial();
      material.setDiffuseMap(loadImg(obj.image));
      //double cl = 0.5;
      //double tr = 0.5;
      //material.setDiffuseColor(Color.color(cl, cl, cl, tr));
      meshView.setMaterial(material);
    }
    return new ShapeFx(meshView);
  }

  @Override
  public GroupFx group() {
    return new GroupFx();
  }

  @Override
  public LightFx light() {
    return new LightFx();
  }

  @Override
  public NodeFx camera() {
    return this.camera;
  }

  private void snapshot() { // instrumentation ready
    try {
      JavaFx.App.io.put(this);
      JavaFx.App.io.take();
    } catch (InterruptedException ignore) {}
  }

  @Override
  public void update() {
    snapshot();
    int[] data = new int[imageWidth * imageHeight];
    JavaFx.App.writableImage.getPixelReader()
        .getPixels(0, 0, imageWidth, imageHeight, WritablePixelFormat.getIntArgbPreInstance(), data, 0, imageWidth);
    image.getRaster().setDataElements(0, 0, imageWidth, imageHeight, data);
  }

  @Override
  public void close() {
    Platform.exit();
  }

  private static class JavaFx { // private modifier for Application which is required to be public
    public static class App extends Application {
      public static int imageWidth;
      public static int imageHeight;
      public static Scene scene;
      public static javafx.scene.Group root;
      public static PerspectiveCamera camera;
      public static WritableImage writableImage;
      public static BlockingQueue<Object> io = new SynchronousQueue<>();

      @Override
      public void start(Stage primaryStage) {
        root = new javafx.scene.Group();
//        PointLight light = new PointLight(Color.WHITE);
//        light.setTranslateX(-10000); // FIXME: 2025-09-30 remove this test light
//        light.setTranslateY(0);
//        light.setTranslateZ(0);
//        root.getChildren().add(light);

//        double ambientBrightness = 0.31;
//        AmbientLight ambientLight = new AmbientLight(Color.color(ambientBrightness, ambientBrightness, ambientBrightness));
//        ambientLight.setTranslateX(10000);
//        ambientLight.setTranslateY(0);
//        ambientLight.setTranslateZ(0);
//        root.getChildren().add(ambientLight);

        scene = new Scene(root, imageWidth, imageHeight, true, SceneAntialiasing.DISABLED);
        camera = new PerspectiveCamera(true);
        camera.setFieldOfView(Math.atan2(24.0 / 2, 50.0) * 2 / (Math.PI * 2) * 360); // 50mm full frame
        scene.setCamera(camera);
        //stage.setScene(scene);
        //stage.show(); // do not open a window
        while (true) {
          try {
            io.put(this); // ready
            io.take(); // wait for request
            snapshot();
          } catch (InterruptedException ignore) {
            break;
          }
        }
      }

      private static void snapshot() { // instrumentation ready
        writableImage = scene.snapshot(writableImage);
      }

      public static void main(String[] args) {
        launch();
      }
    }
  }

  private static class NodeFx implements Node {
    protected final javafx.scene.Node node;
    private javafx.scene.Group group;
    private Translate t  = new Translate();
    private Rotate rx = new Rotate(0.0, Rotate.X_AXIS);
    private Rotate ry = new Rotate(0.0, Rotate.Y_AXIS);
    private Rotate rz = new Rotate(0.0, Rotate.Z_AXIS);

    private NodeFx(javafx.scene.Node node) {
      node.getTransforms().addAll(t, ry, rx, rz);
      this.node = node;
      this.group = JavaFx.App.root;
      this.group.getChildren().add(this.node);
    }

    @Override
    public NodeFx translation(double x, double y, double z) {
      t.setX(x);
      t.setY(-y);
      t.setZ(-z);
      return this;
    }

    @Override
    public NodeFx rotation(double y, double p, double r) {
      ry.setAngle(y * 360); // negative, the yaw axis directed towards the bottom, multiply by negative, y axis flipped
      rx.setAngle(p * 360);
      rz.setAngle(r * 360); // negative, the longitudinal axis directed forward, multiply by negative, z axis flipped
      return this;
    }

    @Override
    public NodeFx setPivot() {
      t = new Translate();
      rx = new Rotate(0.0, Rotate.X_AXIS);
      ry = new Rotate(0.0, Rotate.Y_AXIS);
      rz = new Rotate(0.0, Rotate.Z_AXIS);
      node.getTransforms().addAll(0, List.of(t, ry, rx, rz)); // add a new empty transformation
      return this;
    }

    private void connect(javafx.scene.Group group) {
      this.group.getChildren().remove(this.node);
      this.group = group;
      this.group.getChildren().add(this.node);
    }

    @Override
    public NodeFx connect(Node node) {
      connect((javafx.scene.Group) ((NodeFx) node).node);
      return this;
    }
  }

  private static class ShapeFx extends NodeFx implements Shape {

    public ShapeFx(MeshView meshView) {
      super(meshView);
    }

  }

  private static class GroupFx extends NodeFx implements Group {

    public GroupFx() {
      super(new javafx.scene.Group());
    }

  }

  private static class LightFx extends NodeFx implements Light {

    public LightFx() {
      super(new PointLight(Color.WHITE));
    }

    @Override
    public LightFx setColor(int color) {
      ((PointLight) this.node).setColor(Color.rgb(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF));
      return null;
    }

  }

}
