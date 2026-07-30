package com.cryptroot.core.debug;

import com.cryptroot.core.screen.BaseScreen;
import java.util.Objects;

/**
 * A parsed {@code --capture} command line request, letting any game's launcher opt into a
 * frame-accurate screenshot without writing its own argument parsing.
 *
 * <p>Recognised arguments (see {@link #parse(String[])}):
 *
 * <ul>
 *   <li>{@code --capture <path>} — destination PNG; presence of this flag is what enables capture
 *   <li>{@code --frames <n>} — draw {@code n} frames first (default {@link #DEFAULT_FRAMES})
 *   <li>{@code --exit} — quit once the PNG is written
 * </ul>
 *
 * <p>Typical launcher use:
 *
 * <pre>{@code
 * public static void main(String[] args) {
 *     CaptureRequest capture = CaptureRequest.parse(args);   // null when not requested
 *     new Lwjgl3Application(new MyGame(capture), config);
 * }
 * // ...then, once the screen exists:
 * if (capture != null) capture.applyTo(screen);
 * }</pre>
 *
 * @param path destination path for the PNG, resolved as a libGDX local file
 * @param frames number of frames to draw before capturing; always at least 1
 * @param exitAfter whether to exit the application once the file is written
 */
public record CaptureRequest(String path, int frames, boolean exitAfter) {

  /**
   * Frames drawn before capturing when {@code --frames} is absent. Three is enough to get past the
   * initial {@code resize()} and any first-frame texture upload.
   */
  public static final int DEFAULT_FRAMES = 3;

  public CaptureRequest {
    Objects.requireNonNull(path, "path must not be null");
    if (path.isBlank()) {
      throw new IllegalArgumentException("path must not be blank");
    }
    if (frames < 1) {
      throw new IllegalArgumentException("frames must be at least 1, got " + frames);
    }
  }

  /**
   * Parses launcher arguments, returning {@code null} when {@code --capture} is absent — the normal
   * interactive case. Unrecognised arguments are ignored so this composes with a game's own flags.
   *
   * @param args the raw {@code main} arguments; {@code null} is treated as empty
   * @return the request, or {@code null} if no capture was asked for
   * @throws IllegalArgumentException if {@code --capture} has no path, or {@code --frames} is
   *     missing, non-numeric, or less than 1
   */
  public static CaptureRequest parse(String[] args) {
    if (args == null) return null;
    String path = null;
    int frames = DEFAULT_FRAMES;
    boolean exitAfter = false;

    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--capture" -> {
          if (i + 1 >= args.length) {
            throw new IllegalArgumentException("--capture requires a file path");
          }
          path = args[++i];
        }
        case "--frames" -> {
          if (i + 1 >= args.length) {
            throw new IllegalArgumentException("--frames requires a count");
          }
          String raw = args[++i];
          try {
            frames = Integer.parseInt(raw);
          } catch (NumberFormatException e) {
            throw new IllegalArgumentException("--frames must be an integer, got '" + raw + "'", e);
          }
        }
        case "--exit" -> exitAfter = true;
        default -> {
          // Ignored: lets a game add its own flags without this parser rejecting them.
        }
      }
    }
    return path == null ? null : new CaptureRequest(path, frames, exitAfter);
  }

  /** Schedules this request on {@code screen}. */
  public void applyTo(BaseScreen<?> screen) {
    Objects.requireNonNull(screen, "screen must not be null");
    screen.requestCapture(path, frames, exitAfter);
  }
}
