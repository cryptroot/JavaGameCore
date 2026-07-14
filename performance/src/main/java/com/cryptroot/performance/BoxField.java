package com.cryptroot.performance;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.cryptroot.core.physics.BoxCollider;
import com.cryptroot.core.physics.Collider;
import com.cryptroot.core.physics.CollisionListener;
import com.cryptroot.core.render.RenderPass;
import com.cryptroot.core.world.RenderComponent;
import com.cryptroot.core.world.UpdateComponent;
import com.cryptroot.core.world.World;
import com.cryptroot.core.world.WorldEntity;
import com.cryptroot.core.world.component.TextureRenderComponent;
import java.util.Objects;
import java.util.Random;

/**
 * Populates a {@link World} with {@code count} colourful, moving, colliding boxes for the {@code
 * performance} module's visual showcase — see {@code BoxFieldScreen}.
 *
 * <p>Each box shares the same 1x1 white pixel {@link TextureRegion} (tinted per-box), so the whole
 * field draws as one batched run of quads with no texture swaps — the batch-rendering angle this
 * module exists to showcase.
 */
public final class BoxField {

  private static final float BOX_SIZE = 10f;
  private static final float MIN_SPEED = 30f;
  private static final float MAX_SPEED = 90f;
  private static final long SEED = 1234L;

  private BoxField() {}

  /** Adds {@code count} boxes to {@code world}, uniformly scattered across the arena. */
  public static void populate(
      World world, int count, float arenaWidth, float arenaHeight, TextureRegion pixel) {
    Objects.requireNonNull(world, "world must not be null");
    Objects.requireNonNull(pixel, "pixel must not be null");
    if (count < 0) {
      throw new IllegalArgumentException("count must be >= 0, got " + count);
    }
    if (arenaWidth <= 0f || arenaHeight <= 0f) {
      throw new IllegalArgumentException("arena size must be positive");
    }

    Random rng = new Random(SEED);
    for (int i = 0; i < count; i++) {
      spawnBox(world, pixel, arenaWidth, arenaHeight, rng);
    }
  }

  private static void spawnBox(
      World world, TextureRegion pixel, float arenaWidth, float arenaHeight, Random rng) {
    float x = rng.nextFloat() * (arenaWidth - BOX_SIZE);
    float y = rng.nextFloat() * (arenaHeight - BOX_SIZE);

    TextureRenderComponent render =
        new TextureRenderComponent(pixel, x, y, BOX_SIZE, BOX_SIZE, RenderPass.WORLD);
    render.setTint(randomColor(rng));

    float speed = MIN_SPEED + rng.nextFloat() * (MAX_SPEED - MIN_SPEED);
    float angle = rng.nextFloat() * MathUtils.PI2;
    Vector2 velocity = new Vector2(MathUtils.cos(angle) * speed, MathUtils.sin(angle) * speed);
    MovingBoxComponent moving =
        new MovingBoxComponent(render, velocity, BOX_SIZE, arenaWidth, arenaHeight);

    BoxCollider collider = new BoxCollider(render, 0f, 0f, BOX_SIZE, BOX_SIZE);
    FlashOnCollision listener = new FlashOnCollision(moving);

    world.add(
        new WorldEntity()
            .with(RenderComponent.class, render)
            .with(UpdateComponent.class, moving)
            .with(Collider.class, collider)
            .with(CollisionListener.class, listener));
  }

  private static Color randomColor(Random rng) {
    return new Color(rng.nextFloat(), rng.nextFloat(), rng.nextFloat(), 1f);
  }
}
