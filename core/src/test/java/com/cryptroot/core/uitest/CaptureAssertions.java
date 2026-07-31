package com.cryptroot.core.uitest;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import java.io.File;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Machine checks on the PNGs a scenario captured.
 *
 * <h3>Deliberately weak</h3>
 *
 * These assert that <em>something was drawn</em> — nothing about which colours, where, or in what
 * font. Pixel-exact comparison across a Windows GPU driver and Mesa/llvmpipe under {@code xvfb}
 * does not hold: text rasterises differently, blending rounds differently, and the resulting
 * failures say nothing about the code under test. Correctness assertions belong on widget geometry,
 * widget text and game state; these images are the artefact a human reviews, and the only thing
 * worth automating about them is "the frame is not empty".
 *
 * <p>Call these from the test thread <em>after</em> {@link UiTestApp#run} returns: reading a PNG
 * needs the gdx natives (loaded by the run) but no GL context.
 */
public final class CaptureAssertions {

  /** Distinct colours a real UI frame must beat. A cleared-but-undrawn frame has one. */
  private static final int MIN_DISTINCT_COLOURS = 4;

  private CaptureAssertions() {}

  /**
   * Fails unless {@code capturePath} exists, is non-empty, and contains at least {@value
   * #MIN_DISTINCT_COLOURS} distinct pixel colours.
   *
   * @param capturePath path as passed to {@link UiScenario.Builder#capture(String)}, resolved
   *     against the run's capture directory
   */
  public static void assertNotBlank(String capturePath) {
    Objects.requireNonNull(capturePath, "capturePath must not be null");
    File file = new File(UiTestApp.capturePath(capturePath));
    if (!file.isFile()) {
      throw new AssertionError("no capture was written to " + file.getAbsolutePath());
    }
    if (file.length() == 0L) {
      throw new AssertionError("capture is empty: " + file.getAbsolutePath());
    }
    Pixmap image = new Pixmap(new FileHandle(file));
    try {
      int distinct = countDistinctColours(image);
      if (distinct < MIN_DISTINCT_COLOURS) {
        throw new AssertionError(
            "capture "
                + file.getAbsolutePath()
                + " looks blank: only "
                + distinct
                + " distinct colour(s) in a "
                + image.getWidth()
                + "x"
                + image.getHeight()
                + " image");
      }
    } finally {
      image.dispose();
    }
  }

  /** Distinct colours, sampled on a coarse grid — enough to tell "drawn" from "cleared". */
  private static int countDistinctColours(Pixmap image) {
    int stepX = Math.max(1, image.getWidth() / 64);
    int stepY = Math.max(1, image.getHeight() / 64);
    Set<Integer> colours = new HashSet<>();
    for (int y = 0; y < image.getHeight(); y += stepY) {
      for (int x = 0; x < image.getWidth(); x += stepX) {
        colours.add(image.getPixel(x, y));
      }
    }
    return colours.size();
  }
}
