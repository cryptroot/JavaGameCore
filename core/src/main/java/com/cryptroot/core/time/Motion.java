package com.cryptroot.core.time;

import com.badlogic.gdx.math.Vector2;
import java.util.Objects;

/**
 * Movement helpers — the libGDX-flavoured equivalent of Unity's {@code Vector3.MoveTowards}, which
 * {@code core} does not otherwise provide.
 */
public final class Motion {

  private Motion() {}

  /**
   * Moves {@code pos} toward {@code (tx,ty)} by at most {@code maxDelta} world units, mutating
   * {@code pos} in place.
   *
   * @return {@code true} if the target was reached (or overshot) this step, in which case {@code
   *     pos} is snapped exactly onto the target
   */
  public static boolean moveTowards(Vector2 pos, float tx, float ty, float maxDelta) {
    Objects.requireNonNull(pos, "pos must not be null");
    float dx = tx - pos.x;
    float dy = ty - pos.y;
    float distSq = dx * dx + dy * dy;
    if (distSq == 0f || (maxDelta >= 0f && distSq <= maxDelta * maxDelta)) {
      pos.set(tx, ty);
      return true;
    }
    float dist = (float) Math.sqrt(distSq);
    pos.x += dx / dist * maxDelta;
    pos.y += dy / dist * maxDelta;
    return false;
  }

  /** {@link #moveTowards(Vector2, float, float, float)} toward {@code target}. */
  public static boolean moveTowards(Vector2 pos, Vector2 target, float maxDelta) {
    Objects.requireNonNull(target, "target must not be null");
    return moveTowards(pos, target.x, target.y, maxDelta);
  }
}
