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

import ab.nbsnk.nodes.Col;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.AmbientLight;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;
import javafx.stage.Stage;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.SynchronousQueue;

/**
 * JavaFx limitations:
 * Always Phong, no way to switch to flat shading
 * No light attenuation with distance
 * No shadows
 * Missing setSelfIlluminationColor method that can change the brightness or color
 * of setSelfIlluminationMap the similar way as setDiffuseColor can alter setDiffuseMap
 */
public class EngineFx implements Engine3d {

  private int imageWidth;
  private int imageHeight;
  private BufferedImage image;
  private NodeFx camera;
  private FpsMeter fpsMeter;
  private Map<BufferedImage, Image> imageCache = new HashMap<>();

  public static TriangleMesh loadObj(Obj obj) {
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
        texCoords[i + 1] = (float) (obj.texture[i + 1]); // do not flip Y, flip the image instead
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
    // If we have a texture image and an array of (x, y) coordinates of the image,
    // and we need to pass this image to JavaFX, knowing that it uses a y-down coordinate system.
    // We might think we can simply invert the y coordinate, right? Wrong.
    // Because the moment we switch from a texture to a normal map, its green color
    // corresponds to the increase of the y coordinate. And it will always point in the wrong direction
    // unless we mirror flip the image itself.
    int width = image.getWidth();
    int height = image.getHeight();
    WritableImage writableImage = new WritableImage(width, height);
    Object data = image.getRaster().getDataElements(0, 0, width, height, null);
    switch (image.getType()) {
      case BufferedImage.TYPE_INT_ARGB:
        writableImage.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(),
            (int[]) data, width * (height - 1), -width);
        break;
      case BufferedImage.TYPE_3BYTE_BGR:
        writableImage.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getByteRgbInstance(),
            (byte[]) data, 3 * width * (height - 1), -3 * width);
        break;
      case BufferedImage.TYPE_4BYTE_ABGR:
        byte[] dataBytes = (byte[]) data;
        for (int i = 0; i < dataBytes.length; i += 4) {
          byte red = dataBytes[i];
          dataBytes[i] = dataBytes[i + 2]; // blue
          dataBytes[i + 2] = red;
        }
        writableImage.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getByteBgraInstance(),
            dataBytes, 4 * width * (height - 1), -4 * width);
        break;
      default: throw new IllegalStateException(image.toString());
    }
    return writableImage;
  }

  private static Color color(int color) {
    return Color.rgb(color >> 16 & 0xFF, color >> 8 & 0xFF, color & 0xFF, (color >> 24 & 0xFF) / 255.0);
  }

  @Override
  public EngineFx open(BufferedImage image) {
    System.setProperty("prism.forceGPU", "true");
    //assert com.sun.prism.impl.PrismSettings.forceGPU : "prism.forceGPU=true";
    if (!com.sun.prism.impl.PrismSettings.forceGPU) throw new AssertionError("prism.forceGPU=true");
    if (image.getType() != BufferedImage.TYPE_INT_RGB && image.getType() != BufferedImage.TYPE_INT_ARGB) throw new IllegalArgumentException();
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
  public EngineFx background(BufferedImage image) {
    if (image.getType() != BufferedImage.TYPE_INT_ARGB) throw new IllegalArgumentException();
    int width = image.getWidth();
    int height = image.getHeight();
    WritableImage writableImage = new WritableImage(width, height);
    int[] data = new int[width * height];
    image.getRaster().getDataElements(0, 0, width, height, data);
    writableImage.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), data, 0, width);
    JavaFx.App.scene.setFill(new ImagePattern(writableImage));
    return this;
  }

  @Override
  public ShapeFx shape(Obj obj) {
    return new ShapeFx(obj);
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
  public EngineFx setAmbient(int color) {
    JavaFx.App.ambientLight.setColor(color(color));
    return this;
  }

  @Override
  public NodeFx camera() {
    return this.camera;
  }

  @Override
  public EngineFx setFarClip(double value) {
    JavaFx.App.camera.setFarClip(value);
    return this;
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
        .getPixels(0, 0, imageWidth, imageHeight, PixelFormat.getIntArgbInstance(), data, 0, imageWidth);
    image.getRaster().setDataElements(0, 0, imageWidth, imageHeight, data);
    if (fpsMeter != null) {
      String fps = String.format("fps: %.0f", fpsMeter.getFps());
      Graphics graphics = image.createGraphics();
      graphics.setColor(java.awt.Color.DARK_GRAY);
      graphics.drawString(fps, 2, imageHeight - 4);
    }
  }

  @Override
  public void sysex(int i) {
  }

  @Override
  public EngineFx showFps() {
    fpsMeter = new FpsMeter();
    return this;
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
      public static javafx.scene.AmbientLight ambientLight;
      public static PerspectiveCamera camera;
      public static WritableImage writableImage;
      public static BlockingQueue<Object> io = new SynchronousQueue<>();

      @Override
      public void start(Stage primaryStage) {
        ambientLight = new AmbientLight(Color.BLACK);
        root = new javafx.scene.Group(ambientLight);
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
    public NodeFx connect(Group node) {
      connect((javafx.scene.Group) ((GroupFx) node).node);
      return this;
    }

    @Override
    public NodeFx setVisible(boolean value) {
      node.setVisible(value);
      return this;
    }
  }

  private class ShapeFx extends NodeFx implements Shape {

    private PhongMaterial material;

    public ShapeFx(Obj obj) {
      super(new MeshView(loadObj(obj)));
      MeshView meshView = (MeshView) this.node;
      material = new PhongMaterial();
      if (obj.image != null) {
        material.setDiffuseMap(imageCache.computeIfAbsent(obj.image, EngineFx::loadImg));
        //double cl = 0.5;
        //double tr = 0.5;
        //material.setDiffuseColor(Color.color(cl, cl, cl, tr));
      }
      meshView.setMaterial(material);
    }

    @Override
    public ShapeFx setColor(int color) {
      material.setDiffuseColor(color(color));
      return this;
    }

    @Override
    public ShapeFx setSpecular(int color, double power) {
      material.setSpecularColor(color(color));
      material.setSpecularPower(power);
      return this;
    }

    @Override
    public ShapeFx selfIllumination(int color) {
      Image image = material.getDiffuseMap();
      material.setDiffuseMap(null);
      if ((color & 0xFFFFFF) != 0xFFFFFF) {
        Col mul = new Col(color);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        int[] data = new int[width * height];
        image.getPixelReader().getPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), data, 0, width);
        for (int i = 0; i < data.length; i++) {
          data[i] = new Col(data[i]).mul(mul).rgb();
        }
        WritableImage writableImage = new WritableImage(width, height);
        writableImage.getPixelWriter().setPixels(0, 0, width, height, PixelFormat.getIntArgbInstance(), data, 0, width);
        image = writableImage;
      }
      material.setSelfIlluminationMap(image);
      material.setDiffuseColor(Color.BLACK);
      return this;
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
      ((PointLight) this.node).setColor(color(color));
      return this;
    }

  }

}
