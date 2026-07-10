package com.cryptroot.core.world.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.cryptroot.core.render.RenderPass;
import org.junit.jupiter.api.Test;

/**
 * GL-free tests for {@link HoverableSpriteComponent}: hit bounds, the configured render pass, and
 * the hover flag driven by the {@link com.cryptroot.core.world.ClickableComponent} signals.
 */
class HoverableSpriteComponentTest {

  /** A 16×16 region without a backing texture; only its reported size is used here. */
  private static HoverableSpriteComponent spriteAt(float x, float y, RenderPass pass) {
    TextureRegion region =
        new TextureRegion() {
          @Override
          public int getRegionWidth() {
            return 16;
          }

          @Override
          public int getRegionHeight() {
            return 16;
          }
        };
    return new HoverableSpriteComponent(region, x, y, pass);
  }

  @Test
  void rendersInConfiguredPass() {
    assertEquals(RenderPass.UI, spriteAt(8f, 10f, RenderPass.UI).renderPass());
    assertEquals(RenderPass.WORLD, spriteAt(8f, 10f, RenderPass.WORLD).renderPass());
  }

  @Test
  void sortKeyIsBottomLeftY() {
    assertEquals(10f, spriteAt(8f, 10f, RenderPass.WORLD).sortKey());
  }

  @Test
  void containsPointMatchesLogicalBounds() {
    HoverableSpriteComponent sprite = spriteAt(8f, 10f, RenderPass.UI);

    assertTrue(sprite.containsPoint(8f, 10f)); // bottom-left corner
    assertTrue(sprite.containsPoint(16f, 18f)); // inside
    assertTrue(sprite.containsPoint(24f, 26f)); // top-right corner
    assertFalse(sprite.containsPoint(7.9f, 10f)); // just left
    assertFalse(sprite.containsPoint(8f, 26.1f)); // just above
  }

  @Test
  void boundsReportPositionAndSize() {
    Rectangle out = new Rectangle();
    spriteAt(8f, 10f, RenderPass.UI).bounds(out);
    assertEquals(8f, out.x);
    assertEquals(10f, out.y);
    assertEquals(16f, out.width);
    assertEquals(16f, out.height);
  }

  @Test
  void hoverSignalsToggleHoveredFlag() {
    HoverableSpriteComponent sprite = spriteAt(8f, 10f, RenderPass.UI);
    assertFalse(sprite.isHovered());

    sprite.onHoverEnter().emit();
    assertTrue(sprite.isHovered());

    sprite.onHoverExit().emit();
    assertFalse(sprite.isHovered());
  }
}
