package com.cryptroot.core.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Saves the current back buffer to a PNG on disk.
 *
 * <p>Exists so that a rendering change can be verified from the outside — by a script, a CI job, or
 * a developer comparing against a previously captured reference image — rather than only by a human
 * watching the window. See {@link com.cryptroot.core.screen.BaseScreen#requestCapture(String, int,
 * boolean)} for the frame-accurate entry point.
 *
 * <p>Stateless utility: every method is {@code static} and holds no capture state between calls, so
 * this class is not a service and does not belong on {@link com.cryptroot.core.GameContext}.
 *
 * <p><b>Must be called with GL current and no batch drawing</b> — i.e. at the very end of a frame,
 * after the last {@code batch.end()}. {@code glReadPixels} returns whatever is currently in the
 * back buffer, so calling it mid-frame captures a partially drawn image.
 */
public final class ScreenCapture {

  private static final int BYTES_PER_PIXEL = 4;
  private static final byte OPAQUE = (byte) 0xFF;

  private ScreenCapture() {}

  /**
   * Captures the whole back buffer to {@code path}, resolved via {@link
   * com.badlogic.gdx.Files#local(String)}. Parent directories are created if missing.
   *
   * <p>The captured image is flipped to top-down row order and forced fully opaque, so the PNG
   * matches what is on screen: {@code glReadPixels} returns rows bottom-up, and blended UI can
   * leave sub-255 alpha that would otherwise render as transparency in an image viewer.
   *
   * @param path destination path, e.g. {@code "target/debug_shot.png"}
   * @return the file that was written
   * @throws NullPointerException if {@code path} is null
   */
  public static FileHandle capture(String path) {
    Objects.requireNonNull(path, "path must not be null");
    return capture(path, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
  }

  /**
   * Captures a {@code width} × {@code height} region anchored at the back buffer's bottom-left
   * corner.
   *
   * @throws IllegalArgumentException if {@code width} or {@code height} is not positive
   */
  public static FileHandle capture(String path, int width, int height) {
    Objects.requireNonNull(path, "path must not be null");
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException(
          "capture dimensions must be positive, got " + width + "x" + height);
    }
    Pixmap raw = Pixmap.createFromFrameBuffer(0, 0, width, height);
    try {
      Pixmap image = flipVerticallyOpaque(raw);
      try {
        FileHandle file = Gdx.files.local(path);
        file.parent().mkdirs();
        PixmapIO.writePNG(file, image);
        return file;
      } finally {
        image.dispose();
      }
    } finally {
      raw.dispose();
    }
  }

  /**
   * Returns a new {@link Pixmap} with {@code src}'s rows in reverse order and every alpha byte set
   * to 255. Package-private rather than private so the row arithmetic stays reachable from a test.
   *
   * <p>{@code src} is not modified and remains owned by the caller.
   */
  static Pixmap flipVerticallyOpaque(Pixmap src) {
    int width = src.getWidth();
    int height = src.getHeight();
    Pixmap out = new Pixmap(width, height, Pixmap.Format.RGBA8888);

    ByteBuffer in = src.getPixels();
    ByteBuffer dst = out.getPixels();
    int stride = width * BYTES_PER_PIXEL;
    byte[] row = new byte[stride];

    for (int y = 0; y < height; y++) {
      in.position((height - 1 - y) * stride);
      in.get(row, 0, stride);
      for (int a = BYTES_PER_PIXEL - 1; a < stride; a += BYTES_PER_PIXEL) {
        row[a] = OPAQUE;
      }
      dst.position(y * stride);
      dst.put(row, 0, stride);
    }

    // Leave both buffers rewound — PixmapIO and any later reader expect position 0.
    in.position(0);
    dst.position(0);
    return out;
  }
}
