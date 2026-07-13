package com.cryptroot.core.world.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.cryptroot.core.render.RenderPass;
import org.junit.jupiter.api.Test;

/**
 * GL-free tests for {@link TextureRenderComponent}: position/move, render pass, and the tint /
 * visibility knobs used by placement-preview and range-indicator overlays. {@link
 * TextureRenderComponent#draw} itself is not exercised here (it requires a live GL batch — see
 * {@code core/CLAUDE.md}).
 */
class TextureRenderComponentTest {

  private static TextureRenderComponent at(float x, float y, RenderPass pass) {
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
    return new TextureRenderComponent(region, x, y, 12f, 20f, pass);
  }

  @Test
  void rejectsNonPositiveSize() {
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
    assertThrows(
        IllegalArgumentException.class,
        () -> new TextureRenderComponent(region, 0f, 0f, 0f, 10f, RenderPass.WORLD));
    assertThrows(
        IllegalArgumentException.class,
        () -> new TextureRenderComponent(region, 0f, 0f, 10f, -1f, RenderPass.WORLD));
  }

  @Test
  void positionAndMoveTo() {
    TextureRenderComponent render = at(8f, 10f, RenderPass.WORLD);
    assertEquals(8f, render.x());
    assertEquals(10f, render.y());

    render.moveTo(1f, 2f);
    assertEquals(1f, render.x());
    assertEquals(2f, render.y());
  }

  @Test
  void sortKeyIsWorldY() {
    assertEquals(10f, at(8f, 10f, RenderPass.WORLD).sortKey());
  }

  @Test
  void rendersInConfiguredPass() {
    assertEquals(RenderPass.FOREGROUND_WORLD, at(0f, 0f, RenderPass.FOREGROUND_WORLD).renderPass());
  }

  @Test
  void defaultsToVisibleAndWhiteTint() {
    TextureRenderComponent render = at(0f, 0f, RenderPass.WORLD);
    assertTrue(render.isVisible());
    assertEquals(Color.WHITE, render.tint());
  }

  @Test
  void setVisibleToggles() {
    TextureRenderComponent render = at(0f, 0f, RenderPass.WORLD);
    render.setVisible(false);
    assertFalse(render.isVisible());
    render.setVisible(true);
    assertTrue(render.isVisible());
  }

  @Test
  void setTintIsCopiedAndReadBack() {
    TextureRenderComponent render = at(0f, 0f, RenderPass.WORLD);
    Color mutableInput = new Color(1f, 0f, 0f, 0.5f);

    render.setTint(mutableInput);
    assertEquals(new Color(1f, 0f, 0f, 0.5f), render.tint());

    mutableInput.set(0f, 1f, 0f, 1f);
    assertEquals(
        new Color(1f, 0f, 0f, 0.5f), render.tint(), "later mutation of the input must not leak in");
  }
}
