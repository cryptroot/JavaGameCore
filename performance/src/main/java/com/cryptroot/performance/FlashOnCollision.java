package com.cryptroot.performance;

import com.cryptroot.core.event.Signal;
import com.cryptroot.core.physics.CollisionListener;
import com.cryptroot.core.world.WorldEntity;
import java.util.Objects;

/**
 * Flashes a box red on every collision enter (see {@link MovingBoxComponent#flash()}); collision
 * exit is intentionally a no-op — this showcase only needs a visible "something touched me" pulse.
 */
public final class FlashOnCollision implements CollisionListener {

  private final Signal<WorldEntity> onCollisionEnter = new Signal<>();
  private final Signal<WorldEntity> onCollisionExit = new Signal<>();

  public FlashOnCollision(MovingBoxComponent box) {
    Objects.requireNonNull(box, "box must not be null");
    onCollisionEnter.connect(other -> box.flash());
  }

  @Override
  public Signal<WorldEntity> onCollisionEnter() {
    return onCollisionEnter;
  }

  @Override
  public Signal<WorldEntity> onCollisionExit() {
    return onCollisionExit;
  }
}
