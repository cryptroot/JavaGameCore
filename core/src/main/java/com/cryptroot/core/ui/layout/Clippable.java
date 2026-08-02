package com.cryptroot.core.ui.layout;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Implemented by widgets that clip their drawing to a rectangle and therefore need the scene's
 * {@link Viewport} and {@link Camera} to map world coordinates to GL scissor pixels.
 *
 * <p>Exists so that clipping widgets do not have to take a viewport and camera through every
 * constructor and have game code thread them down the tree. Instead {@link
 * com.cryptroot.core.ui.UiLayer UiLayer} — which already owns both — walks the widget tree when a
 * widget is added and on every layout pass, and pushes them into any {@code Clippable} it finds.
 * Containers forward the call to their children.
 *
 * <p>A {@code Clippable} that has not yet received a clip context must draw unclipped rather than
 * fail, so that a widget used outside a {@code UiLayer} still renders.
 */
public interface Clippable {

  /**
   * Supplies the viewport and camera to clip against. May be called more than once (e.g. after a
   * window resize); the latest values win.
   *
   * @param viewport the scene viewport, used for the screen rect
   * @param camera the camera the widget's world coordinates are expressed in
   */
  void setClipContext(Viewport viewport, Camera camera);
}
