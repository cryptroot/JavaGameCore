package com.cryptroot.core.ui;

import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract base class for widgets that own and delegate to child {@link UiWidget}s.
 *
 * <p>Children are drawn, laid out, and reset as a group. They do not need to be registered with the
 * owning {@link UiLayer} separately — only the {@code CompositeWidget} itself is added to the
 * layer.
 *
 * <h3>Lifecycle delegation</h3>
 *
 * <ul>
 *   <li>{@link #layout()} — calls {@link #doLayout()} first (so the composite can position children
 *       via {@link UiWidget#setPosition(float, float)}), then propagates to every child.
 *   <li>{@link #draw(PolygonSpriteBatch)} — calls {@link #doDraw(PolygonSpriteBatch)} first
 *       (composite chrome / background), then every child in insertion order (last added = on top).
 *   <li>{@link #reset()} — calls {@link #doReset()} first, then every child.
 * </ul>
 *
 * <h3>Template methods</h3>
 *
 * Subclasses implement {@link #doLayout()} and optionally override {@link #doDraw}/{@link
 * #doReset}. The three public lifecycle methods ({@code layout}, {@code draw}, {@code reset}) are
 * {@code final} to guarantee the delegation invariant; subclasses may not skip child forwarding.
 *
 * <h3>Hit-testing</h3>
 *
 * The default {@link #hit} implementation tests children in reverse-insertion order (last added is
 * topmost). Subclasses that want a composite-level bounds check (e.g., {@link Button}) should
 * {@code @Override} {@code hit} entirely rather than calling {@code super.hit}.
 *
 * <h3>Example</h3>
 *
 * <pre>{@code
 * public final class MyPanel extends CompositeWidget {
 *
 *     private final TextLabel heading;
 *     private final Rectangle panelBounds = new Rectangle(80f, 600f, 400f, 200f);
 *
 *     public MyPanel(BitmapFont font) {
 *         heading = new TextLabel(font, "My Panel", 0f, 0f);
 *         addChild(heading);
 *     }
 *
 *     \@Override
 *     protected void doLayout() {
 *         heading.setPosition(panelBounds.x + 12f, panelBounds.y + panelBounds.height - 16f);
 *     }
 *
 *     \@Override
 *     protected void doDraw(PolygonSpriteBatch batch) {
 *         // draw panel background before children
 *     }
 * }
 * }</pre>
 */
public abstract class CompositeWidget implements UiWidget {

  private final List<UiWidget> children = new ArrayList<>();

  // -------------------------------------------------------------------------
  // Child management
  // -------------------------------------------------------------------------

  protected final void addChild(UiWidget child) {
    children.add(child);
  }

  protected final void removeChild(UiWidget child) {
    children.remove(child);
  }

  /** Returns an unmodifiable view of the child list in insertion order. */
  public final List<UiWidget> children() {
    return Collections.unmodifiableList(children);
  }

  // -------------------------------------------------------------------------
  // UiWidget delegation (final — delegates cannot be bypassed)
  // -------------------------------------------------------------------------

  /**
   * Calls {@link #doLayout()} (subclass positions children), then calls {@link UiWidget#layout()}
   * on every child so they remeasure themselves at their new positions.
   */
  @Override
  public final void layout() {
    doLayout();
    for (UiWidget c : children) c.layout();
  }

  /**
   * Calls {@link #doDraw(PolygonSpriteBatch)} (subclass chrome), then {@link
   * UiWidget#draw(PolygonSpriteBatch)} on every child in insertion order (first = behind, last = on
   * top), then {@link #doAfterDraw(PolygonSpriteBatch)} for any post-child cleanup (e.g. disabling
   * GL scissor).
   */
  @Override
  public final void draw(PolygonSpriteBatch batch) {
    if (!isVisible()) return;
    doDraw(batch);
    for (UiWidget c : children) c.draw(batch);
    doAfterDraw(batch);
  }

  /**
   * Calls {@link #doReset()} (subclass transient state), then {@link UiWidget#reset()} on every
   * child.
   */
  @Override
  public final void reset() {
    doReset();
    for (UiWidget c : children) c.reset();
  }

  // -------------------------------------------------------------------------
  // UiWidget non-final — subclass may override as needed
  // -------------------------------------------------------------------------

  /** Forwards to all children. Override to intercept hover at the composite level. */
  @Override
  public void updateHover(float worldX, float worldY) {
    for (UiWidget c : children) c.updateHover(worldX, worldY);
  }

  /**
   * Tests children in reverse-insertion order (last added = topmost). Subclasses may override
   * entirely to do a composite-level bounds check.
   */
  @Override
  public boolean hit(float worldX, float worldY) {
    for (int i = children.size() - 1; i >= 0; i--) {
      if (children.get(i).hit(worldX, worldY)) return true;
    }
    return false;
  }

  /** Forwards to all children; returns {@code true} on the first frame-consuming child. */
  @Override
  public boolean update(float delta) {
    for (UiWidget c : children) {
      if (c.update(delta)) return true;
    }
    return false;
  }

  /** Forwards to all children. Override to add composite-level drag behaviour. */
  @Override
  public void dragged(float worldX, float worldY) {
    for (UiWidget c : children) c.dragged(worldX, worldY);
  }

  /** Forwards to children in reverse-insertion order; returns {@code true} on first consumer. */
  @Override
  public boolean scrolled(float worldX, float worldY, float amountX, float amountY) {
    for (int i = children.size() - 1; i >= 0; i--) {
      if (children.get(i).scrolled(worldX, worldY, amountX, amountY)) return true;
    }
    return false;
  }

  // -------------------------------------------------------------------------
  // Template methods
  // -------------------------------------------------------------------------

  /**
   * Subclass computes its own bounds and positions children by calling {@link
   * UiWidget#setPosition(float, float)} on each. Called before children's {@link UiWidget#layout()}
   * methods — children will remeasure themselves at whatever position is set here.
   */
  protected abstract void doLayout();

  /**
   * Subclass draws its own background or chrome. Called before children are drawn so chrome appears
   * underneath. Default: no-op.
   */
  protected void doDraw(PolygonSpriteBatch batch) {}

  /**
   * Subclass resets its own transient state (hover, timers, etc.). Children are reset separately by
   * {@link #reset()}. Default: no-op.
   */
  protected void doReset() {}

  /**
   * Called after all children have been drawn. Use for post-draw cleanup, for example disabling a
   * GL scissor test that was opened in {@link #doDraw(PolygonSpriteBatch)}. Default: no-op.
   */
  protected void doAfterDraw(PolygonSpriteBatch batch) {}
}
