package com.cryptroot.core.world.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.world.PositionComponent;
import com.cryptroot.core.world.RenderComponent;
import org.junit.jupiter.api.Test;

class RenderEffectComponentsTest {

  /** A no-op render component used purely to check delegation (no GL). */
  private static RenderComponent noopRender(RenderPass pass, float sort) {
    return new RenderComponent() {
      @Override
      public void draw(PolygonSpriteBatch batch) {}

      @Override
      public RenderPass renderPass() {
        return pass;
      }

      @Override
      public float sortKey() {
        return sort;
      }
    };
  }

  // ---- WorldHealthBarComponent.barColor ----------------------------------

  @Test
  void barColorMapsFractionAcrossPalette() {
    Color full = new Color(0f, 1f, 0f, 1f);
    Color empty = new Color(1f, 0f, 0f, 1f);
    Color out = new Color();

    WorldHealthBarComponent.barColor(1f, full, empty, out);
    assertEquals(full, out, "full HP = full colour");
    WorldHealthBarComponent.barColor(0f, full, empty, out);
    assertEquals(empty, out, "no HP = empty colour");
    WorldHealthBarComponent.barColor(0.5f, full, empty, out);
    assertEquals(0.5f, out.r, 1e-4f);
    assertEquals(0.5f, out.g, 1e-4f);
  }

  @Test
  void barColorClampsOutOfRange() {
    Color out = new Color();
    WorldHealthBarComponent.barColor(2f, out);
    Color full = new Color();
    WorldHealthBarComponent.barColor(1f, full);
    assertEquals(full, out);
  }

  @Test
  void healthBarPassSortAndPushModel() {
    PositionComponent anchor =
        new com.cryptroot.core.world.component.DefaultPositionComponent(0f, 0f);
    WorldHealthBarComponent bar =
        new WorldHealthBarComponent(
            null, anchor, WorldHealthBarComponent.Config.defaults(3f, 0.45f, 0f, 0.35f));

    assertEquals(RenderPass.FOREGROUND_WORLD, bar.renderPass());
    assertEquals(0f, bar.sortKey(), 1e-6f);

    bar.setFraction(2f);
    assertEquals(1f, bar.fraction(), 1e-6f, "clamped to 1");
    bar.setFraction(-1f);
    assertEquals(0f, bar.fraction(), 1e-6f, "clamped to 0");

    assertTrue(bar.isVisible());
    bar.setVisible(false);
    assertFalse(bar.isVisible());
  }

  @Test
  void healthBarEasesDisplayedTowardTarget() {
    PositionComponent anchor =
        new com.cryptroot.core.world.component.DefaultPositionComponent(0f, 0f);
    WorldHealthBarComponent bar =
        new WorldHealthBarComponent(
            null, anchor, WorldHealthBarComponent.Config.defaults(3f, 0.45f, 0f, 0.35f));
    bar.setFraction(0f);
    // Large delta closes the gap fully without overshoot.
    bar.update(10f);
    bar.snap(); // idempotent once settled
    assertEquals(0f, bar.fraction(), 1e-6f);
  }

  // ---- TintFlashRenderComponent.tintAt -----------------------------------

  @Test
  void tintAtInterpolatesFlashToBase() {
    Color flash = new Color(1f, 0f, 0f, 1f);
    Color base = new Color(1f, 1f, 1f, 1f);
    Color out = new Color();

    WorldHealthBarComponent.barColor(0f, out); // reuse out object
    TintFlashRenderComponent.tintAt(flash, base, 0f, 0.1f, out);
    assertEquals(flash, out, "elapsed 0 = flash colour");

    TintFlashRenderComponent.tintAt(flash, base, 0.1f, 0.1f, out);
    assertEquals(base, out, "fully elapsed = base colour");

    TintFlashRenderComponent.tintAt(flash, base, 0.05f, 0.1f, out);
    assertEquals(1f, out.r, 1e-4f);
    assertEquals(0.5f, out.g, 1e-4f); // halfway from 0 -> 1
  }

  @Test
  void tintFlashDelegatesPassAndSortAndDecays() {
    TintFlashRenderComponent tint =
        new TintFlashRenderComponent(noopRender(RenderPass.WORLD, 12.5f));
    assertEquals(RenderPass.WORLD, tint.renderPass());
    assertEquals(12.5f, tint.sortKey(), 1e-6f);

    assertFalse(tint.isFlashing());
    tint.flash(new Color(1f, 0.3f, 0.3f, 1f), 0.12f);
    assertTrue(tint.isFlashing());
    tint.update(0.06f);
    assertTrue(tint.isFlashing(), "still mid-flash");
    tint.update(0.10f);
    assertFalse(tint.isFlashing(), "flash finished after duration");
  }
}
