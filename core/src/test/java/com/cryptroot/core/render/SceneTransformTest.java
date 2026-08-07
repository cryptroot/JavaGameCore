package com.cryptroot.core.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.badlogic.gdx.math.Rectangle;
import org.junit.jupiter.api.Test;

/**
 * Covers {@link SceneTransform}'s pan/zoom/clamp maths without a GL context. Drawing, hit-testing
 * and drag capture are exercised through {@link com.cryptroot.core.ui.WorldViewport} in a UI test
 * instead (per the module convention: keep algorithmic code GL-free and unit-test it, leave {@code
 * draw()} bodies uncovered).
 */
class SceneTransformTest {

  private static final float EPSILON = 1e-4f;

  private static SceneTransform viewOf(float x, float y, float w, float h) {
    SceneTransform t = new SceneTransform();
    t.setView(x, y, w, h);
    return t;
  }

  // -------------------------------------------------------------------------
  // Static clamp maths
  // -------------------------------------------------------------------------

  @Test
  void clampCentreKeepsWindowInsideLargerScene() {
    // scene [0, 1000] seen through a 200-wide window: the centre must stay within [100, 900].
    assertEquals(100f, SceneTransform.clampCentre(-50f, 0f, 1000f, 200f), EPSILON);
    assertEquals(900f, SceneTransform.clampCentre(950f, 0f, 1000f, 200f), EPSILON);
    assertEquals(400f, SceneTransform.clampCentre(400f, 0f, 1000f, 200f), EPSILON);
  }

  @Test
  void clampCentreCentresWhenSceneSmallerThanWindow() {
    // scene [0, 100] inside a 300-wide window: nothing to scroll to, so it sits in the middle.
    assertEquals(50f, SceneTransform.clampCentre(0f, 0f, 100f, 300f), EPSILON);
    assertEquals(50f, SceneTransform.clampCentre(500f, 0f, 100f, 300f), EPSILON);
  }

  @Test
  void clampCentreCentresWhenSceneExactlyFillsWindow() {
    assertEquals(100f, SceneTransform.clampCentre(123f, 0f, 200f, 200f), EPSILON);
  }

  @Test
  void sceneToViewAndBackIsARoundTrip() {
    float view = SceneTransform.sceneToView(256f, 40f, 16f, 1.75f);
    assertEquals(256f, SceneTransform.viewToScene(view, 40f, 16f, 1.75f), EPSILON);
  }

  // -------------------------------------------------------------------------
  // Conversion
  // -------------------------------------------------------------------------

  @Test
  void centredScenePointLandsAtTheViewCentre() {
    SceneTransform t = viewOf(100f, 200f, 400f, 300f);
    t.centreOn(10f, 20f);

    assertEquals(300f, t.sceneToViewX(10f), EPSILON);
    assertEquals(350f, t.sceneToViewY(20f), EPSILON);
  }

  @Test
  void zoomScalesDistancesFromTheCentre() {
    SceneTransform t = viewOf(0f, 0f, 400f, 400f);
    t.centreOn(0f, 0f);
    t.setZoom(2f);

    // 10 scene units right of centre is 20 view units right of the view's middle.
    assertEquals(220f, t.sceneToViewX(10f), EPSILON);
    assertEquals(10f, t.viewToSceneX(220f), EPSILON);
  }

  @Test
  void visibleSceneRectShrinksAsZoomIncreases() {
    SceneTransform t = viewOf(0f, 0f, 400f, 200f);
    t.centreOn(0f, 0f);
    t.setZoom(2f);

    Rectangle visible = t.visibleSceneRect(new Rectangle());
    assertEquals(200f, visible.width, EPSILON);
    assertEquals(100f, visible.height, EPSILON);
    assertEquals(-100f, visible.x, EPSILON);
    assertEquals(-50f, visible.y, EPSILON);
  }

  // -------------------------------------------------------------------------
  // Pan
  // -------------------------------------------------------------------------

  @Test
  void panFollowsThePointerAndIsZoomAware() {
    SceneTransform t = viewOf(0f, 0f, 400f, 400f);
    t.centreOn(0f, 0f);

    t.panByView(50f, 0f);
    assertEquals(-50f, t.centreX(), EPSILON);

    t.setZoom(2f);
    t.panByView(50f, 0f);
    // At 2x, the same pointer travel covers half as much scene.
    assertEquals(-75f, t.centreX(), EPSILON);
  }

  // -------------------------------------------------------------------------
  // Zoom about an anchor
  // -------------------------------------------------------------------------

  @Test
  void zoomAtKeepsTheAnchoredScenePointUnderTheCursor() {
    SceneTransform t = viewOf(0f, 0f, 400f, 400f);
    t.centreOn(0f, 0f);

    float anchorX = 350f;
    float anchorY = 120f;
    float sceneUnderAnchorX = t.viewToSceneX(anchorX);
    float sceneUnderAnchorY = t.viewToSceneY(anchorY);

    t.zoomAt(1.6f, anchorX, anchorY);

    assertEquals(1.6f, t.zoom(), EPSILON);
    assertEquals(anchorX, t.sceneToViewX(sceneUnderAnchorX), EPSILON);
    assertEquals(anchorY, t.sceneToViewY(sceneUnderAnchorY), EPSILON);
  }

  @Test
  void zoomIsClampedToTheConfiguredRange() {
    SceneTransform t = viewOf(0f, 0f, 400f, 400f);
    t.setZoomRange(0.5f, 2f);

    t.setZoom(10f);
    assertEquals(2f, t.zoom(), EPSILON);

    t.setZoom(0.01f);
    assertEquals(0.5f, t.zoom(), EPSILON);
  }

  @Test
  void invalidZoomRangeIsRejected() {
    SceneTransform t = new SceneTransform();
    assertThrows(IllegalArgumentException.class, () -> t.setZoomRange(0f, 2f));
    assertThrows(IllegalArgumentException.class, () -> t.setZoomRange(3f, 2f));
  }

  // -------------------------------------------------------------------------
  // Bounds
  // -------------------------------------------------------------------------

  @Test
  void panningIsClampedToSceneBounds() {
    SceneTransform t = viewOf(0f, 0f, 200f, 200f);
    t.setSceneBounds(0f, 0f, 1000f, 1000f);

    t.centreOn(-500f, 5000f);
    assertEquals(100f, t.centreX(), EPSILON);
    assertEquals(900f, t.centreY(), EPSILON);
  }

  @Test
  void zoomingOutPastTheSceneRecentresIt() {
    SceneTransform t = viewOf(0f, 0f, 200f, 200f);
    t.setSceneBounds(0f, 0f, 400f, 400f);
    t.setZoomRange(0.1f, 4f);
    t.centreOn(400f, 400f);
    assertEquals(300f, t.centreX(), EPSILON);

    // At 0.25x the 200-wide view shows 800 scene units — more than the scene has, so it centres.
    t.setZoom(0.25f);
    assertEquals(200f, t.centreX(), EPSILON);
    assertEquals(200f, t.centreY(), EPSILON);
  }

  @Test
  void sceneBoundsMustNotBeInverted() {
    SceneTransform t = new SceneTransform();
    assertThrows(IllegalArgumentException.class, () -> t.setSceneBounds(10f, 0f, 0f, 10f));
    assertThrows(IllegalArgumentException.class, () -> t.setSceneBounds(0f, 10f, 10f, 0f));
  }

  @Test
  void clearingSceneBoundsAllowsUnlimitedPanning() {
    SceneTransform t = viewOf(0f, 0f, 200f, 200f);
    t.setSceneBounds(0f, 0f, 100f, 100f);
    t.clearSceneBounds();
    assertTrue(!t.isBounded());

    t.centreOn(9999f, -9999f);
    assertEquals(9999f, t.centreX(), EPSILON);
    assertEquals(-9999f, t.centreY(), EPSILON);
  }
}
