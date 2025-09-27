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

import ab.jnc3.Screen;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.stage.Stage;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.SynchronousQueue;

public class SceneThumbnailSketch {

  public static class JavaFx extends Application {
    public static final CountDownLatch latch = new CountDownLatch(1);
    public static int width;
    public static int height;
    public static Scene scene;
    public static BlockingQueue<Object> input = new SynchronousQueue<>();
    public static BlockingQueue<WritableImage> output = new SynchronousQueue<>();
    @Override
    public void start(Stage primaryStage) {
      TriangleMesh mesh = Scene3d.loadObj(Obj.load(("" +
          "# Blender 4.5.3 LTS\n" +
          "# www.blender.org\n" +
          "mtllib Untitled.mtl\n" +
          "o Cube\n" +
          "v 1.000000 1.000000 -1.000000\n" +
          "v 1.000000 -1.000000 -1.000000\n" +
          "v 1.000000 1.000000 1.000000\n" +
          "v 1.000000 -1.000000 1.000000\n" +
          "v -1.000000 1.000000 -1.000000\n" +
          "v -1.000000 -1.000000 -1.000000\n" +
          "v -1.000000 1.000000 1.000000\n" +
          "v -1.000000 -1.000000 1.000000\n" +
          "vn -0.0000 1.0000 -0.0000\n" +
          "vn -0.0000 -0.0000 1.0000\n" +
          "vn -1.0000 -0.0000 -0.0000\n" +
          "vn -0.0000 -1.0000 -0.0000\n" +
          "vn 1.0000 -0.0000 -0.0000\n" +
          "vn -0.0000 -0.0000 -1.0000\n" +
          "vt 0.625000 0.500000\n" +
          "vt 0.875000 0.500000\n" +
          "vt 0.875000 0.750000\n" +
          "vt 0.625000 0.750000\n" +
          "vt 0.375000 0.750000\n" +
          "vt 0.625000 1.000000\n" +
          "vt 0.375000 1.000000\n" +
          "vt 0.375000 0.000000\n" +
          "vt 0.625000 0.000000\n" +
          "vt 0.625000 0.250000\n" +
          "vt 0.375000 0.250000\n" +
          "vt 0.125000 0.500000\n" +
          "vt 0.375000 0.500000\n" +
          "vt 0.125000 0.750000\n" +
          "s 0\n" +
          "usemtl Material\n" +
          "f 1/1/1 5/2/1 7/3/1 3/4/1\n" +
          "f 4/5/2 3/4/2 7/6/2 8/7/2\n" +
          "f 8/8/3 7/9/3 5/10/3 6/11/3\n" +
          "f 6/12/4 2/13/4 4/5/4 8/14/4\n" +
          "f 2/13/5 1/1/5 3/4/5 4/5/5\n" +
          "f 6/11/6 5/10/6 1/1/6 2/13/6\n").getBytes()));
      PhongMaterial material = new PhongMaterial();
      material.setDiffuseColor(Color.DARKGRAY);
      material.setSpecularColor(Color.WHITE);

      MeshView meshView = new MeshView();
      meshView.setMesh(mesh);
      meshView.setMaterial(material);
      meshView.setTranslateZ(10);
      Group root = new Group(meshView);

      scene = new Scene(root, width, height, true, SceneAntialiasing.DISABLED);
      scene.setFill(Color.BLACK);

      PerspectiveCamera camera = new PerspectiveCamera(true);
      scene.setCamera(camera);
      //stage.setScene(scene);
      //stage.show(); // do not open a window
      latch.countDown();
      while (true) {
        try {
          input.take();
          output.put(scene.snapshot(null));
        } catch (InterruptedException e) {
          break;
        }
      }
    }
    public static void main(String[] args) {
      launch();
    }
  }

  public static class Scene3d implements AutoCloseable {
    final int width;
    final int height;

    public static TriangleMesh loadObj(Obj obj) {
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

    public Scene3d(int width, int height) {
      System.setProperty("prism.forceGPU", "true");
      this.width = width;
      this.height = height;
    }

    public Scene3d open() {
      if (JavaFx.latch.getCount() == 0) throw new IllegalStateException();
      JavaFx.width = this.width;
      JavaFx.height = this.height;
      CompletableFuture.runAsync(() -> JavaFx.main(new String[0]));
      try {
        JavaFx.latch.await();
      } catch (InterruptedException ignore) {}
      return this;
    }

    public void update(BufferedImage image) {
      WritableImage writableImage;
      try {
        JavaFx.input.put(this);
        writableImage = JavaFx.output.take();
      } catch (InterruptedException ignore) {
        return;
      }
      int[] data = new int[width * height];
      writableImage.getPixelReader().getPixels(0, 0, width, height, WritablePixelFormat.getIntArgbPreInstance(), data, 0, width);
      image.getRaster().setDataElements(0, 0, width, height, data);
    }

    @Override
    public void close() {
      Platform.exit();
    }
  }

  public static void main(String[] args) {
    Screen screen = new Screen();
//    screen.image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB);
//    screen.preferredSize = new Dimension(1920, 1080);
    boolean[] open = {true};
    try (Scene3d scene3d = new Scene3d(screen.image.getWidth(), screen.image.getHeight()).open()) {
      Graphics graphics = screen.image.createGraphics();
      graphics.setColor(java.awt.Color.DARK_GRAY);
      screen.keyListener = key -> {
        if (key.equals("Esc")) open[0] = false;
      };
      FpsMeter fpsMeter = new FpsMeter();
      while (open[0]) {
        scene3d.update(screen.image);
        graphics.drawString(String.format("fps: %.0f", fpsMeter.getFps()), 20, 20);
        screen.update();
      }
    }
  }

}
