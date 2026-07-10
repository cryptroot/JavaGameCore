package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;

/**
 * Contract for an interactive UI widget managed by a {@link UiLayer}.
 *
 * <p>Implementations are responsible for their own visual state (hover, pressed, disabled) and for
 * emitting signal events when interactions occur.
 *
 * <h3>Lifecycle</h3>
 *
 * <ol>
 *   <li>{@link UiLayer#add(UiWidget, int)} calls {@link #layout()} immediately after the widget is
 *       added. Call {@link UiLayer#layout()} again after every viewport resize to propagate the
 *       updated bounds.
 *   <li>{@link UiLayer#update(float)} polls the cursor position, calls {@link #updateHover(float,
 *       float)}, then {@link #update(float)} on every widget each frame.
 *   <li>The {@link com.badlogic.gdx.InputProcessor InputProcessor} returned by {@link
 *       UiLayer#inputProcessor()} routes {@link #hit(float, float)} and {@link #scrolled(float,
 *       float, float, float)} calls in descending z-order, stopping at the first consumer.
 *   <li>Call {@link UiLayer#reset()} from the screen's {@code hide()} to clear all transient widget
 *       state before the screen may be re-entered.
 * </ol>
 *
 * <h3>Frame-consumption contract</h3>
 *
 * When {@link #update(float)} returns {@code true}, the owning screen <em>must</em> return from
 * {@code render()} immediately without issuing any further draw calls. A screen transition may
 * already be in progress.
 */
public interface UiWidget {

  /**
   * Repositions the widget to the given world-space origin and marks layout dirty. {@link
   * #layout()} must be called afterwards to apply the position (a {@link CompositeWidget} does this
   * automatically for all its children after {@link CompositeWidget#doLayout()} returns).
   *
   * <p>The default implementation is a no-op — leaf widgets whose position is fixed at construction
   * time do not need to implement this.
   */
  default void setPosition(float x, float y) {}

  /**
   * Returns whether this widget should participate in drawing and hit-testing. {@link
   * CompositeWidget#draw(PolygonSpriteBatch)} skips rendering entirely when this returns {@code
   * false}.
   *
   * <p>The default implementation returns {@code true}; override to add show/hide support (e.g.,
   * {@link Panel#setVisible(boolean)}).
   */
  default boolean isVisible() {
    return true;
  }

  /**
   * Returns whether this widget occludes the given world point — i.e. it is an opaque, visible
   * surface that should prevent lower-z widgets from receiving hover (and outline) treatment at
   * that point.
   *
   * <p>This is a side-effect-free query used by {@link UiLayer} to suppress hover and outline
   * capture on widgets sitting beneath an opaque panel or dialog. It must <em>not</em> arm any
   * interaction state (unlike {@link #hit(float, float)}).
   *
   * <p>The default implementation returns {@code false} — leaf widgets and pass-through containers
   * never occlude anything. Override in opaque surfaces (e.g., {@link Panel#setOpaque(boolean)
   * opaque panels}).
   */
  default boolean blocksPointer(float worldX, float worldY) {
    return false;
  }

  /**
   * Recomputes text positions, hit bounds, and any layout-dependent state from the current viewport
   * dimensions. Called by {@link UiLayer} after add and after every viewport resize.
   */
  void layout();

  /**
   * Updates hover state from already-unprojected world coordinates. Called every frame by {@link
   * UiLayer#update(float)} via cursor polling.
   */
  void updateHover(float worldX, float worldY);

  /**
   * Tests whether the given world-coordinate point hits this widget and arms any interaction (e.g.,
   * click-feedback timer, pressed state).
   *
   * @return {@code true} if the event was consumed; {@link UiLayer} will not offer the event to
   *     widgets with lower z-order.
   */
  boolean hit(float worldX, float worldY);

  /**
   * Ticks per-frame state (animations, timers, deferred actions).
   *
   * <p><b>Frame-consumption contract:</b> when this method returns {@code true}, {@link
   * UiLayer#update(float)} returns {@code true} immediately, and the screen <em>must</em> return
   * from {@code render()} without issuing any draw calls. A screen transition may already be in
   * progress.
   *
   * @return {@code true} if this widget consumed the frame.
   */
  boolean update(float delta);

  /**
   * Draws this widget. Must be called inside a {@code batch.begin()} / {@code batch.end()} block.
   */
  void draw(PolygonSpriteBatch batch);

  /**
   * Resets all transient interaction state (hover, pressed, timers). Called by {@link
   * UiLayer#reset()} from the screen's {@code hide()} to prevent stale state on screen re-entry.
   */
  void reset();

  /**
   * Handles a scroll event whose screen-space origin unprojected to the given world coordinates.
   * {@code amountX} and {@code amountY} follow the libGDX scroll convention: positive Y = scroll
   * down, negative Y = scroll up.
   *
   * <p>The default implementation returns {@code false} (not consumed). Override in widgets that
   * support scrolling (e.g., a scroll window).
   *
   * @param worldX unprojected world X of the cursor at scroll time
   * @param worldY unprojected world Y of the cursor at scroll time
   * @return {@code true} if the event was consumed; {@link UiLayer} will not offer the event to
   *     widgets with lower z-order.
   */
  default boolean scrolled(float worldX, float worldY, float amountX, float amountY) {
    return false;
  }

  /**
   * Returns the {@link Focusable} widget that should receive keyboard focus when this widget is
   * hit.
   *
   * <p>The default implementation returns {@code this} if this widget itself implements {@link
   * Focusable}, or {@code null} otherwise. Container widgets that host {@link Focusable} children
   * (e.g., {@link TabbedPanel}) should override this to return the focused child found during the
   * most recent {@link #hit(float, float)} call.
   *
   * <p>Called by {@link UiLayer} immediately after a {@link #hit} call returns {@code true}.
   */
  default Focusable hitFocusable() {
    return this instanceof Focusable f ? f : null;
  }

  /**
   * Called by {@link UiLayer} each frame while the pointer is dragged after this widget consumed a
   * {@link #hit(float, float)} call.
   *
   * <p>The drag is "captured" — the widget continues to receive {@code dragged()} calls even when
   * the pointer strays outside its bounds, until the pointer is released.
   *
   * <p>The default implementation is a no-op. Override in drag-driven widgets (e.g., {@link
   * Slider}).
   *
   * @param worldX unprojected world X of the current pointer position
   * @param worldY unprojected world Y of the current pointer position
   */
  default void dragged(float worldX, float worldY) {}
}
