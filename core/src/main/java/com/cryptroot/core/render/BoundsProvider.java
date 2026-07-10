package com.cryptroot.core.render;

import com.badlogic.gdx.math.Rectangle;

/**
 * Supplies an axis-aligned bounding box in world space.
 *
 * <p>This is the minimal geometry contract shared by anything that needs to be positionally tracked
 * without the caller knowing its concrete type — for example a floating label or speech bubble that
 * anchors to the top edge of a drawable, or the normal-mapped render pass that Y-sorts by
 * bounding-box bottom edge ({@link NormalMappedDrawable} extends this interface).
 */
public interface BoundsProvider {

  /** Writes the current bounding box into {@code out} and returns {@code out} for chaining. */
  Rectangle bounds(Rectangle out);
}
