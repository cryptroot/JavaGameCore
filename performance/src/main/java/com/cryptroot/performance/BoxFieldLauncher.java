package com.cryptroot.performance;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/**
 * Desktop launcher for the box-field performance showcase.
 *
 * <p>Run from the IDE, or from the command line with:
 *
 * <pre>{@code mvn -pl performance -am exec:java}</pre>
 *
 * <p>The window is sized to match the arena 1:1 so world units map directly to screen pixels. An
 * 8-bit stencil buffer is requested for parity with the other launchers even though this showcase
 * does not use the selection-outline renderer.
 */
public final class BoxFieldLauncher {

  private BoxFieldLauncher() {}

  public static void main(String[] args) {
    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    config.setTitle("Performance Demo \u2014 Parallel Collision Broad-Phase");
    config.setWindowedMode((int) BoxFieldGame.ARENA_WIDTH, (int) BoxFieldGame.ARENA_HEIGHT);
    config.setBackBufferConfig(8, 8, 8, 8, 16, 8, 0);
    new Lwjgl3Application(new BoxFieldGame(), config);
  }
}
