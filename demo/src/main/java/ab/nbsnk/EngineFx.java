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
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.stage.Stage;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;

public class EngineFx implements Engine3d {

  private int imageWidth;
  private int imageHeight;
  private BufferedImage image;

  private static class JavaFx { // private modifier for Application which is required to be public
    public static class App extends Application {
      public static int imageWidth;
      public static int imageHeight;
      public static Scene scene;
      public static Group root;
      public static WritableImage writableImage;
      public static BlockingQueue<Object> io = new SynchronousQueue<>();

      @Override
      public void start(Stage primaryStage) {
        root = new Group();
        scene = new Scene(root, imageWidth, imageHeight, true, SceneAntialiasing.DISABLED);
        scene.setCamera(new PerspectiveCamera(true));
        //stage.setScene(scene);
        //stage.show(); // do not open a window
        try {
          io.put(this);
          while (true) {
            io.take();
            writableImage = scene.snapshot(writableImage);
            io.put(this);
          }
        } catch (InterruptedException ignore) {
        }
      }

      public static void main(String[] args) {
        launch();
      }
    }
  }

  private static class ShapeFx implements Engine3d.Shape {
    private final Node node;
    private Group group;

    public ShapeFx(Node node) {
      this.node = node;
      this.group = JavaFx.App.root;
      this.group.getChildren().add(this.node);
    }

    @Override
    public void translation(double x, double y, double z) {
      node.setTranslateX(x);
      node.setTranslateY(-y);
      node.setTranslateZ(-z);
    }

    @Override
    public void rotation(double z) {
      node.setRotate(-z);
    }

    @Override
    public void connect(Shape shape) {
      Group group = (Group) ((ShapeFx) shape).node;
      this.group.getChildren().remove(this.node);
      this.group = group;
      this.group.getChildren().add(this.node);
    }
  }

  private static TriangleMesh loadObj(Obj obj) {
    int[] faces = Arrays.copyOf(obj.face, obj.face.length);
    float[] points = new float[obj.vertex.length];
    for (int i = 0; i < points.length; i++) points[i] = (float) obj.vertex[i];
    float[] normals = new float[obj.normal.length];
    for (int i = 0; i < normals.length; i++) normals[i] = (float) obj.normal[i];
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

  @Override
  public Engine3d open(BufferedImage image) {
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
    return this;
  }

  @Override
  public void background(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();
    WritableImage writableImage = new WritableImage(width, height);
    int[] data = new int[width * height];
    image.getRaster().getDataElements(0, 0, width, height, data);
    for (int i = 0; i < data.length; i++) data[i] |= 0xFF000000; // opacity
    writableImage.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbPreInstance(), data, 0, width);
    JavaFx.App.scene.setFill(new ImagePattern(writableImage));
  }

  @Override
  public Shape shape(Obj obj) {
    if (obj == null) return new ShapeFx(new Group());
    return new ShapeFx(new MeshView(loadObj(obj)));
  }

  @Override
  public void update() {
    try {
      JavaFx.App.io.put(this);
      JavaFx.App.io.take();
    } catch (InterruptedException ignore) {
      return;
    }
    int[] data = new int[imageWidth * imageHeight];
    JavaFx.App.writableImage.getPixelReader()
        .getPixels(0, 0, imageWidth, imageHeight, WritablePixelFormat.getIntArgbPreInstance(), data, 0, imageWidth);
    image.getRaster().setDataElements(0, 0, imageWidth, imageHeight, data);
  }

  @Override
  public void close() {
    Platform.exit();
  }
}
