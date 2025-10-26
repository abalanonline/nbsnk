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

import java.util.Arrays;

public class FpsMeter {
  private final int fpsSize;
  private final long[] fpsTime;
  private final long[] medianTime;
  private final long[] results;
  private long smoothing;
  private int fpsI;
  private long nanoTime;

  public FpsMeter(int fpsSize) {
    this.fpsSize = fpsSize;
    this.fpsTime = new long[fpsSize];
    this.medianTime = new long[fpsSize];
    this.results = new long[fpsSize];
    nanoTime = System.nanoTime();
  }

  public FpsMeter() {
    this(399);
  }

  /**
   * String.format("fps: %.1f", fpsMeter.getFps())
   */
  public double getFps() {
    long nanoTime = System.nanoTime();
    fpsTime[fpsI] = nanoTime - this.nanoTime;
    this.nanoTime = nanoTime;
    fpsI = (fpsI + 1) % fpsSize;

    long[] medianTime;
    if (fpsTime[fpsI] == 0) {
      medianTime = Arrays.copyOf(fpsTime, fpsI);
    } else {
      medianTime = this.medianTime; // reuse heap
      System.arraycopy(fpsTime, 0, medianTime, 0, fpsSize);
    }
    Arrays.sort(medianTime);
    long result = medianTime[medianTime.length / 2];
    long result0 = results[fpsI];
    results[fpsI] = result;
    smoothing += result - result0;
    long x1000 = 1_000_000_000_000L * medianTime.length / smoothing;
    return x1000 / 1000.0;
  }

}
