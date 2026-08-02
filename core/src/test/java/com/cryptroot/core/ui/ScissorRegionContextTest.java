package com.cryptroot.core.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import org.junit.jupiter.api.Test;

/**
 * A clip context is all-or-nothing: {@code begin()} cannot clip with only one half of it, so a
 * half-set context would silently disable clipping for the rest of the widget's life — content
 * spilling out of its panel with nothing logged. Rejecting it at the setter turns that into an
 * exception at the call that got it wrong.
 *
 * <p>GL-free: constructing a {@link ScissorRegion}, a bare camera and a viewport touches no GL.
 */
class ScissorRegionContextTest {

  private static Viewport viewport() {
    return new StretchViewport(100f, 100f, new OrthographicCamera());
  }

  @Test
  void bothSuppliedIsAccepted() {
    Viewport viewport = viewport();
    assertDoesNotThrow(() -> new ScissorRegion().setClipContext(viewport, viewport.getCamera()));
  }

  @Test
  void neitherSuppliedClearsTheContext() {
    ScissorRegion region = new ScissorRegion();
    Viewport viewport = viewport();
    region.setClipContext(viewport, viewport.getCamera());

    assertDoesNotThrow(() -> region.setClipContext(null, null));
  }

  @Test
  void viewportWithoutCameraIsRejected() {
    Viewport viewport = viewport();
    assertThrows(
        IllegalArgumentException.class, () -> new ScissorRegion().setClipContext(viewport, null));
  }

  @Test
  void cameraWithoutViewportIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ScissorRegion().setClipContext(null, new OrthographicCamera()));
  }

  @Test
  void theConstructorRejectsAHalfSetContextToo() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new ScissorRegion(viewport(), null, 0f, 0f, 10f, 10f));
  }
}
