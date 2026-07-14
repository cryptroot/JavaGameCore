package com.cryptroot.performance;

import com.badlogic.gdx.Game;

/** libGDX {@link Game} entry point for the box-field performance showcase. */
public final class BoxFieldGame extends Game {

  static final float ARENA_WIDTH = 1600f;
  static final float ARENA_HEIGHT = 900f;

  private PerfDemoContext context;

  @Override
  public void create() {
    context = new PerfDemoContext(ARENA_WIDTH, ARENA_HEIGHT);
    setScreen(new BoxFieldScreen(context));
  }

  @Override
  public void dispose() {
    if (screen != null) {
      screen.dispose();
    }
    if (context != null) {
      context.dispose();
    }
  }
}
