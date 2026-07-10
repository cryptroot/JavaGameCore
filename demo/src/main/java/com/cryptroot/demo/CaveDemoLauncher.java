package com.cryptroot.demo;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/**
 * Desktop launcher for the Cave TMX demo.
 *
 * <p>Run from the IDE, or from the command line with:
 *
 * <pre>{@code mvn -pl demo -am exec:java}</pre>
 *
 * <p>The window is sized 5:6 to match the 20×24 tile ({@code 1280×1536} px) demo map so the whole
 * cave is framed. An 8-bit stencil buffer is requested because the core {@code
 * SelectionOutlineRenderer} relies on it.
 */
public final class CaveDemoLauncher {

  private CaveDemoLauncher() {}

  public static void main(String[] args) {
    Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
    config.setTitle("Cave TMX Demo");
    config.setWindowedMode(800, 960);
    // r, g, b, a, depth, stencil, samples — stencil 8 for the outline renderer.
    config.setBackBufferConfig(8, 8, 8, 8, 16, 8, 0);
    new Lwjgl3Application(new CaveDemoGame(), config);
  }
}
