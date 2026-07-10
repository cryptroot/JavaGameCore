package com.cryptroot.core.render.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.cryptroot.core.event.Signal0;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.world.AlwaysOutlinedComponent;
import com.cryptroot.core.world.BoundsComponent;
import com.cryptroot.core.world.ClickableComponent;
import com.cryptroot.core.world.RenderComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * GL-free tests for {@link OutlineRenderSystem#collectOutlineTargets}: always-on markers plus the
 * hovered entity are selected, plain entities are not.
 */
class OutlineRenderSystemTest {

  /** Minimal no-op render component (draw is never invoked in these tests). */
  private static RenderComponent render() {
    return new RenderComponent() {
      @Override
      public void draw(PolygonSpriteBatch batch) {
        /* not called */
      }

      @Override
      public RenderPass renderPass() {
        return RenderPass.WORLD;
      }

      @Override
      public float sortKey() {
        return 0f;
      }
    };
  }

  /** A clickable + bounded hotspot covering the unit square at (x, y). */
  private static WorldEntity hotspot(float x, float y) {
    return new WorldEntity()
        .with(RenderComponent.class, render())
        .with(
            BoundsComponent.class,
            new BoundsComponent() {
              @Override
              public boolean containsPoint(float wx, float wy) {
                return wx >= x && wx <= x + 1 && wy >= y && wy <= y + 1;
              }

              @Override
              public Rectangle bounds(Rectangle out) {
                return out.set(x, y, 1, 1);
              }
            })
        .with(
            ClickableComponent.class,
            new ClickableComponent() {
              private final Signal0 c = new Signal0();
              private final Signal0 en = new Signal0();
              private final Signal0 ex = new Signal0();

              @Override
              public Signal0 onClicked() {
                return c;
              }

              @Override
              public Signal0 onHoverEnter() {
                return en;
              }

              @Override
              public Signal0 onHoverExit() {
                return ex;
              }
            });
  }

  @Test
  void collectsAlwaysOutlinedAndHoveredOnly() {
    World world = new World();

    WorldEntity always =
        new WorldEntity()
            .with(RenderComponent.class, render())
            .with(AlwaysOutlinedComponent.class, AlwaysOutlinedComponent.INSTANCE);
    WorldEntity plain = new WorldEntity().with(RenderComponent.class, render());
    WorldEntity icon = hotspot(10f, 10f);

    world.add(always);
    world.add(plain);
    world.add(icon);

    HoverSystem hover = new HoverSystem();
    hover.process(world, 10.5f, 10.5f, 0.1f); // cursor over the icon

    OutlineRenderSystem system = new OutlineRenderSystem();
    List<WorldEntity> targets = system.collectOutlineTargets(world, hover);

    assertEquals(2, targets.size());
    assertTrue(targets.contains(always), "always-outlined entity should be selected");
    assertTrue(targets.contains(icon), "hovered entity should be selected");
    assertFalse(targets.contains(plain), "plain entity should not be selected");
  }

  @Test
  void selectsOnlyAlwaysOutlinedWhenNothingHovered() {
    World world = new World();

    WorldEntity always =
        new WorldEntity()
            .with(RenderComponent.class, render())
            .with(AlwaysOutlinedComponent.class, AlwaysOutlinedComponent.INSTANCE);
    WorldEntity icon = hotspot(10f, 10f);

    world.add(always);
    world.add(icon);

    HoverSystem hover = new HoverSystem();
    hover.process(world, 200f, 200f, 0.1f); // cursor far away — nothing hovered

    List<WorldEntity> targets = new OutlineRenderSystem().collectOutlineTargets(world, hover);

    assertEquals(1, targets.size());
    assertTrue(targets.contains(always));
  }
}
