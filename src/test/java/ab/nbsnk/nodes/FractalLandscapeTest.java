package ab.nbsnk.nodes;

import ab.jnc3.Screen;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class FractalLandscapeTest {

  boolean systemExit;
  private double[][] cloud;

  @Disabled
  @Test
  void diamondSquare() {
    int size = 240;
    cloud = FractalLandscape.diamondSquare(size, 3);
    Screen screen = new Screen();
    int width = screen.image.getWidth();
    int height = screen.image.getHeight();
    int brightness = 0xFF;
    screen.keyListener = key -> {
      if ("Esc".equals(key)) systemExit = true;
      if ("Tab".equals(key)) cloud = FractalLandscape.diamondSquare(size, System.nanoTime());
    };
    while (!systemExit) {
      for (int y = 0; y < Math.min(size, height); y++) for (int x = 0; x < Math.min(size, width); x++) {
        screen.image.setRGB(x, y, Math.min(Math.max(0, (int) ((cloud[y][x] + 0.5) * brightness)), 0xFF) * 0x010101);
      }
      screen.update();
      try { Thread.sleep(100); } catch (InterruptedException ignore) {}
    }
    screen.close();
  }
}
