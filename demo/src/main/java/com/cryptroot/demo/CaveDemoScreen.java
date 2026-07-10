package com.cryptroot.demo;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.cryptroot.core.screen.BaseGameScreen;
import com.cryptroot.tiled.render.TiledMap;
import com.cryptroot.tiled.render.TiledMapLoader;
import java.io.IOException;

/**
 * Demo screen that loads {@code Cave.tmx} and renders it through the core world pipeline.
 *
 * <p>The map's tile layers are added to the world in the {@code BACKGROUND} pass, so they are drawn
 * in document order by the core render pipeline. The world camera is centred by {@link
 * BaseGameScreen} (the context is sized to the map), and this screen wires drag-to-pan and
 * scroll-to-zoom so the map can be explored.
 */
public final class CaveDemoScreen extends BaseGameScreen<DemoGameContext> {

  /** Classpath of the demo map (its TSX and PNG are resolved alongside it). */
  public static final String MAP = "assets/maps/Cave.tmx";

  public CaveDemoScreen(DemoGameContext context) {
    super(context);
  }

  @Override
  protected Color clearColor() {
    return Color.BLACK;
  }

  @Override
  public void show() {
    TiledMapLoader loader = new TiledMapLoader(context.assets().resources());
    TiledMap map;
    try {
      map = loader.load(MAP);
    } catch (IOException e) {
      throw new GdxRuntimeException("Failed to load demo map: " + MAP, e);
    }
    map.addTo(world);

    InputMultiplexer input =
        new InputMultiplexer(worldCamera.dragAdapter(), worldCamera.scrollAdapter());
    Gdx.input.setInputProcessor(input);
  }
}
