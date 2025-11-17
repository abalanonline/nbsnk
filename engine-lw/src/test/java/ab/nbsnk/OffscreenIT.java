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

import org.junit.jupiter.api.Test;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

// mvn test -pl engine-lw -Dtest=OffscreenIT
class OffscreenIT {

  static void print(byte[] bytes, int width) {
    int p = bytes.length - 4 * width + 1;
    while (p > 0) {
      for (int x = 0; x < width; x++) {
        int b = bytes[p] & 0xFF;
        p += 4;
        System.out.print(b < 0x80 ? (b < 0x40 ? ".." : "++") : (b < 0xC0 ? "**" : "%%"));
      }
      p -= 2 * 4 * width;
      System.out.println();
    }
  }

  @Test
  void test() {
    GLFW.glfwInit();
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
    long window = GLFW.glfwCreateWindow(32, 18, "", MemoryUtil.NULL, MemoryUtil.NULL);
    GLFW.glfwMakeContextCurrent(window);
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
    //try { Thread.sleep(2000); } catch (InterruptedException ignore) {}
//        while (!GLFW.glfwWindowShouldClose(window)) {
//            GLFW.glfwPollEvents();
//            GL15.glDrawArrays(GL15.GL_TRIANGLES, 0, 3);
//            GLFW.glfwSwapBuffers(window);
//        }
    byte[] pixelBytes = null;
    ByteBuffer pixelMemory = MemoryUtil.memAlloc(4 * 32 * 18);
    for (int i = 0; i < 5; i++) {
      GL15.glReadPixels(0, 0, 32, 18, GL15.GL_RGBA, GL15.GL_UNSIGNED_BYTE, pixelMemory);
      pixelBytes = new byte[32 * 18 * 4];
      pixelMemory.get(pixelBytes);
      pixelMemory.flip();
    }
    MemoryUtil.memFree(pixelMemory);

    ByteBuffer pixelBuffer = BufferUtils.createByteBuffer(32 * 18 * 4);
    GL15.glReadPixels(0, 0, 32, 18, GL15.GL_RGBA, GL15.GL_UNSIGNED_BYTE, pixelBuffer);
    byte[] arr = new byte[32 * 18 * 4];
    pixelBuffer.get(arr);
    GLFW.glfwDestroyWindow(window);
    GLFW.glfwTerminate();
    print(pixelBytes, 32);
  }

}
