package com.cryptroot.core.ui.layout;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * Gives every child the container's entire content rectangle, so children stack front-to-back
 * rather than side by side. Later-added children draw on top.
 *
 * <p>Use for mutually exclusive views that share one region — a tab body, a modal over a page — or
 * for layering a badge over content. Combine with {@link
 * com.cryptroot.core.ui.Panel#setVisible(boolean)} or {@link
 * com.cryptroot.core.ui.TextLabel#setVisible(boolean)} to choose which child shows: every child is
 * still measured and laid out, so switching between them cannot shift the layout.
 */
public final class Overlay extends LayoutContainer<Overlay> {

  /** Natural size: the largest child in each dimension, plus padding. */
  @Override
  public Vector2 preferredSize(Vector2 out) {
    float w = 0f;
    float h = 0f;
    Vector2 tmp = measureScratch();
    for (LayoutElement child : managed()) {
      child.preferredSize(tmp);
      w = Math.max(w, tmp.x);
      h = Math.max(h, tmp.y);
    }
    Insets pad = padding();
    return out.set(w + pad.horizontal(), h + pad.vertical());
  }

  @Override
  protected void arrange(Rectangle content) {
    for (LayoutElement child : managed()) {
      child.setBounds(content.x, content.y, content.width, content.height);
    }
  }
}
