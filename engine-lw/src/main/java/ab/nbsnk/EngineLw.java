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

import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryUtil;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.function.Supplier;

public class EngineLw implements Engine3d {

  private int screenWidth;
  private int screenHeight;
  private BufferedImage screenImage;
  private long windowHandle;
  private ByteBuffer pixelBuffer;
  private byte[] pixelBytes;
  private int[] pixelInts;

  @Override
  public EngineLw open(BufferedImage image) {
    if (image.getType() != BufferedImage.TYPE_INT_RGB && image.getType() != BufferedImage.TYPE_INT_ARGB) throw new IllegalArgumentException();
    screenWidth = image.getWidth();
    screenHeight = image.getHeight();
    screenImage = image;
    if (!GLFW.glfwInit()) throw new IllegalStateException();
    GLFW.glfwInit();
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
    windowHandle = GLFW.glfwCreateWindow(screenWidth, screenHeight, "", MemoryUtil.NULL, MemoryUtil.NULL);
    GLFW.glfwMakeContextCurrent(windowHandle);
    GL.createCapabilities();
    FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(3 * 2).put(0f).put(0f).put(1f).put(0f).put(0f).put(1f).flip();
    int vbo = GL15.glGenBuffers();
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertexBuffer, GL15.GL_STATIC_DRAW);
    MemoryUtil.memFree(vertexBuffer);
    GL15.glEnableClientState(GL15.GL_VERTEX_ARRAY);
    GL15.glVertexPointer(2, GL15.GL_FLOAT, 0, 0L);
    GLFW.glfwPollEvents();
    GL15.glDrawArrays(GL15.GL_TRIANGLES, 0, 3);

    pixelBuffer = MemoryUtil.memAlloc(4 * screenWidth * screenHeight);
    pixelBytes = new byte[4 * screenWidth * screenHeight];
    pixelInts = new int[screenWidth * screenHeight];
    return this;
  }

  @Override
  public EngineLw background(BufferedImage image) {
    return this;
  }

  @Override
  public ShapeLw shape(Obj obj) {
    return new ShapeLw();
  }

  @Override
  public GroupLw group() {
    return new GroupLw();
  }

  @Override
  public LightLw light() {
    return new LightLw();
  }

  @Override
  public EngineLw setAmbient(int color) {
    return this;
  }

  @Override
  public NodeLw camera() {
    return new NodeLw();
  }

  @Override
  public EngineLw setFarClip(double value) {
    return this;
  }

  @Override
  public EngineLw setFocalLength(double value) {
    return this;
  }

  @Override
  public void update() {
    GL15.glReadPixels(0, 0, screenWidth, screenHeight, GL15.GL_RGBA, GL15.GL_UNSIGNED_BYTE, pixelBuffer);
    pixelBuffer.get(pixelBytes);
    pixelBuffer.flip();
    for (int y = 0, i = 0, j = 4 * (screenHeight - 1) * screenWidth; y < screenHeight; y++) {
      for (int x = 0; x < screenWidth; x++) pixelInts[i++] = pixelBytes[j++] << 16 & 0xFF0000 |
          pixelBytes[j++] << 8 & 0xFF00 | pixelBytes[j++] & 0xFF | pixelBytes[j++] << 24 & 0xFF000000;
      j -= 8 * screenWidth;
    }
    screenImage.getRaster().setDataElements(0, 0, screenWidth, screenHeight, pixelInts);
  }

  @Override
  public void sysex(int i) {

  }

  @Override
  public EngineLw textSupplier(Supplier<String> supplier) {
    return this;
  }

  @Override
  public void close() {
    GLFW.glfwDestroyWindow(windowHandle);
    GLFW.glfwTerminate();
    MemoryUtil.memFree(pixelBuffer);
  }

  public static class NodeLw implements Node {
    @Override
    public NodeLw translation(double x, double y, double z) {
      return this;
    }

    @Override
    public NodeLw rotation(double yaw, double pitch, double roll) {
      return this;
    }

    @Override
    public NodeLw setPivot() {
      return this;
    }

    @Override
    public NodeLw connect(Group node) {
      return this;
    }

    @Override
    public NodeLw setVisible(boolean value) {
      return this;
    }
  }

  public static class ShapeLw extends NodeLw implements Shape {
    @Override
    public ShapeLw setColor(int color) {
      return this;
    }

    @Override
    public ShapeLw setSpecular(int color, double power) {
      return this;
    }

    @Override
    public ShapeLw selfIllumination(int color) {
      return this;
    }

    @Override
    public ShapeLw setBumpMap(BufferedImage image) {
      return this;
    }

    @Override
    public ShapeLw setReflectionMap(BufferedImage image, double alpha, Node skybox) {
      return this;
    }
  }

  public static class GroupLw extends NodeLw implements Group {

  }

  public static class LightLw extends NodeLw implements Light {
    @Override
    public LightLw setColor(int color) {
      return this;
    }
  }

}
