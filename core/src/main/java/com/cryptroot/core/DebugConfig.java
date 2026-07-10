package com.cryptroot.core;

/**
 * Global debug-mode flag.
 *
 * <p>Enable by calling {@link #enable()} before constructing the game (e.g. from the desktop
 * launcher when {@code --debug} is passed on the command line). Query via {@link #isEnabled()} from
 * anywhere.
 */
public final class DebugConfig {

  private static boolean enabled = false;

  private DebugConfig() {}

  /** Enables debug mode. Must be called before {@link com.badlogic.gdx.Game#create()}. */
  public static void enable() {
    enabled = true;
  }

  /** Returns {@code true} if debug mode is active. */
  public static boolean isEnabled() {
    return enabled;
  }
}
