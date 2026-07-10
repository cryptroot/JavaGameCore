package com.cryptroot.demo;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.cryptroot.tiled.io.TmxParser;
import com.cryptroot.tiled.model.TmxMap;
import java.io.IOException;

/**
 * libGDX {@link Game} entry point for the Cave TMX demo.
 *
 * <p>On {@link #create()} it reads the demo map's dimensions (a cheap, GL-free parse) to size the
 * {@link DemoGameContext} to the map, then shows the {@link CaveDemoScreen}. The screen re-loads
 * the map to build its textures.
 */
public final class CaveDemoGame extends Game {

  private DemoGameContext context;

  @Override
  public void create() {
    TmxMap map;
    try {
      map = new TmxParser().parse(CaveDemoScreen.MAP);
    } catch (IOException e) {
      throw new GdxRuntimeException("Failed to read demo map: " + CaveDemoScreen.MAP, e);
    }
    float worldWidth = (float) map.width() * map.tileWidth();
    float worldHeight = (float) map.height() * map.tileHeight();

    context = new DemoGameContext(worldWidth, worldHeight);
    setScreen(new CaveDemoScreen(context));
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
