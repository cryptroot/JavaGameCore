package com.cryptroot.core.ui.layout;

import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/**
 * GL-free {@link LayoutElement} stub for container tests: reports a fixed natural size and records
 * the last rectangle it was assigned.
 *
 * <p>Containers only ever ask a child for {@link #preferredSize} and then call {@link #setBounds},
 * so this is enough to test every arrangement rule without a font, a texture, or a GL context.
 */
final class FakeElement implements LayoutElement {

  private final float naturalWidth;
  private final float naturalHeight;
  private final float weight;

  /** The rectangle most recently assigned by a container. */
  final Rectangle assigned = new Rectangle();

  /** Number of times {@link #setBounds} has been called. */
  int assignCount;

  FakeElement(float naturalWidth, float naturalHeight) {
    this(naturalWidth, naturalHeight, 0f);
  }

  FakeElement(float naturalWidth, float naturalHeight, float weight) {
    this.naturalWidth = naturalWidth;
    this.naturalHeight = naturalHeight;
    this.weight = weight;
  }

  @Override
  public Vector2 preferredSize(Vector2 out) {
    return out.set(naturalWidth, naturalHeight);
  }

  @Override
  public float growWeight() {
    return weight;
  }

  @Override
  public void setBounds(float x, float y, float width, float height) {
    assigned.set(x, y, width, height);
    assignCount++;
  }

  @Override
  public void layout() {}

  @Override
  public void updateHover(float worldX, float worldY) {}

  @Override
  public boolean hit(float worldX, float worldY) {
    return false;
  }

  @Override
  public boolean update(float delta) {
    return false;
  }

  @Override
  public void draw(PolygonSpriteBatch batch) {}

  @Override
  public void reset() {}
}
