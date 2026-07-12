package com.cryptroot.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.Objects;

/**
 * Renders a per-pixel white selection outline around any {@link OutlineSource} — Spine units,
 * texture world entities, UI hotspots, and low-resolution pixel sprites alike.
 *
 * <p><b>This is the single, shared outline mechanism for the whole engine.</b> Every outline-able
 * thing contributes through the same {@link OutlineSource} contract; do <em>not</em> add a parallel
 * "texture outline" component or a game-specific outline renderer. World entities opt in with
 * {@link com.cryptroot.core.world.AlwaysOutlinedComponent} (always-on) or by being the hovered
 * entity; UI widgets implement {@link com.cryptroot.core.ui.OutlineCaptureSource}.
 *
 * <p><b>Technique:</b> the source is captured into an RGBA {@link FrameBuffer}, then blitted back
 * using {@code outline.frag} — a neighbour-sampling shader that emits the outline colour wherever a
 * transparent pixel borders an opaque one. The {@code alpha} parameter passed to {@link
 * #drawOutline} drives a smooth fade-in/out effect; the body always renders at full opacity so only
 * the white ring fades.
 *
 * <p><b>Capture entry points:</b> {@link #captureSources} (the unified multi-source path used by
 * the UI layer and the world outline system), {@link #captureWith} (arbitrary caller-supplied
 * drawer), and {@link #capture} (a single {@link NormalMappedDrawable}). All clear the FBO once and
 * restore the screen viewport.
 *
 * <p><b>FBO resolution:</b> the FBO tracks the screen viewport's physical pixel dimensions and is
 * recreated whenever those change. Call {@link #resize} from each screen's {@code resize()} method
 * after {@code viewport.update()} to keep it in sync.
 *
 * <p><b>Normal-mapped path note:</b> the outline is intentionally scoped to the plain
 * (lighting-disabled) render path. Extending it to {@link NormalMappedSpineRenderer} would require
 * the FBO capture to bind both diffuse and normal textures simultaneously, which significantly
 * complicates the two-texture capture step.
 *
 * <p><b>Stencil buffer note:</b> this implementation does not use a stencil buffer. If a
 * stencil-based multi-pass masking approach is ever needed, request a stencil attachment in {@code
 * DesktopLauncher} via {@code Lwjgl3ApplicationConfiguration.setStencilBits(8)}.
 */
public final class SelectionOutlineRenderer implements Disposable {

  /** The two available outline rendering styles. */
  public enum OutlineStyle {
    /** Solid uniform ring — crisp, strong selection indicator. */
    HARD_EDGE,
    /** Bright-white edge that fades outward — soft glow / highlight look. */
    GLOW
  }

  private static final String VERTEX_SHADER_PATH = "shaders/normal-mapped.vert";
  private static final String FRAGMENT_SHADER_PATH = "shaders/outline.frag";

  /**
   * Default outline ring width in FBO pixels. 2 px at 1600×900 gives a crisp 1-2 screen-pixel ring.
   */
  private static final float DEFAULT_OUTLINE_RADIUS = 2f;

  /** Glow outer radius in FBO pixels. Larger than the outline radius so the fade is visible. */
  private static final float GLOW_RADIUS = 6f;

  /**
   * Alpha change per second used by callers to drive hover fade-in and fade-out. At 6f the outline
   * reaches full opacity in ~167 ms and fades out in the same time.
   */
  public static final float FADE_SPEED = 6f;

  private final ShaderProgram outlineShader;
  private final float worldWidth;
  private final float worldHeight;

  private FrameBuffer fbo;
  private TextureRegion fboRegion;
  private int fboWidth;
  private int fboHeight;
  private OutlineStyle currentStyle = OutlineStyle.HARD_EDGE;
  private float outlineRadius = DEFAULT_OUTLINE_RADIUS;
  private boolean resolutionLocked = false;

  /**
   * @param worldWidth logical world width (e.g. {@code MyJourneyGame.WORLD_WIDTH})
   * @param worldHeight logical world height (e.g. {@code MyJourneyGame.WORLD_HEIGHT})
   */
  public SelectionOutlineRenderer(float worldWidth, float worldHeight) {
    this.worldWidth = worldWidth;
    this.worldHeight = worldHeight;

    String vert = Gdx.files.classpath(VERTEX_SHADER_PATH).readString();
    String frag = Gdx.files.classpath(FRAGMENT_SHADER_PATH).readString();
    outlineShader = new ShaderProgram(vert, frag);
    if (!outlineShader.isCompiled()) {
      throw new IllegalStateException(
          "Outline shader failed to compile: " + outlineShader.getLog());
    }

    // Initialise FBO at world resolution; resized on first resize() call.
    createFbo((int) worldWidth, (int) worldHeight);
  }

  /**
   * Recreates the FBO when the render viewport's physical pixel dimensions change. Must be called
   * from each screen's {@code resize()} <em>after</em> {@code viewport.update()}.
   *
   * <p>No-op when the resolution is {@linkplain #lockResolution() locked} — used by fixed
   * low-resolution render targets that must keep the FBO at the logical world size rather than
   * tracking the physical window.
   */
  public void resize(Viewport viewport) {
    Objects.requireNonNull(viewport, "viewport must not be null");
    if (resolutionLocked) {
      return;
    }
    int w = viewport.getScreenWidth();
    int h = viewport.getScreenHeight();
    if (w < 1 || h < 1 || (w == fboWidth && h == fboHeight)) {
      return;
    }
    if (fbo != null) {
      fbo.dispose();
    }
    createFbo(w, h);
  }

  /**
   * Pins the FBO to its current (constructor) resolution and makes {@link #resize} a no-op. Use for
   * fixed low-resolution render targets (e.g. a 160×120 pixel-art framebuffer) where the outline
   * must be captured and blitted at the logical world resolution, not the physical window
   * resolution.
   */
  public void lockResolution() {
    this.resolutionLocked = true;
  }

  /**
   * Sets the outline ring width in FBO pixels (default {@value #DEFAULT_OUTLINE_RADIUS}). At a 1:1
   * logical FBO, a radius of {@code 1f} yields a crisp 1-pixel border. Must not exceed the {@code
   * MAX_R} constant compiled into {@code outline.frag}.
   */
  public void setOutlineRadius(float radius) {
    this.outlineRadius = radius;
  }

  /** Sets the outline style used by subsequent {@link #drawOutline} calls. */
  public void setStyle(OutlineStyle style) {
    this.currentStyle = Objects.requireNonNull(style, "style must not be null");
  }

  /** Returns the currently active outline style. */
  public OutlineStyle getStyle() {
    return currentStyle;
  }

  /**
   * Captures {@code instance} into the internal FBO. <em>Must be called before any {@code
   * batch.begin()} for the current frame.</em>
   *
   * @param projectionMatrix the same {@code camera.combined} used for the main scene
   * @param viewport used to restore the screen GL viewport after capture
   */
  public void capture(
      PolygonSpriteBatch batch,
      Matrix4 projectionMatrix,
      Viewport viewport,
      NormalMappedDrawable instance) {
    fbo.begin();
    Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

    batch.setProjectionMatrix(projectionMatrix);
    batch.begin();
    instance.draw(batch);
    batch.end();

    // Restore default blend mode: SkeletonRenderer sets PMA blend (GL_ONE /
    // GL_ONE_MINUS_SRC_ALPHA) internally, and batch does not reset it on end().
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

    fbo.end();
    viewport.apply(); // fbo.end() does not restore the GL viewport; do it explicitly
  }

  /**
   * Captures every {@linkplain OutlineSource#outlineActive() active} {@link OutlineSource} into the
   * FBO in a single pass and reports the ring opacity to blit at.
   *
   * <p>This is the unified multi-source capture entry point shared by the UI layer (hover hotspots)
   * and the world outline system (always-on / hovered entities). Each active source draws its own
   * pixels via {@link OutlineSource#drawForCapture}; the returned alpha is the maximum {@link
   * OutlineSource#outlineAlpha()} across the active set, so the subsequent {@link #drawOutline}
   * blit rings every captured source at the brightest alpha.
   *
   * <p>When no source is active the FBO is left untouched and {@code 0f} is returned, signalling
   * the caller to skip the blit.
   *
   * <p><em>Must be called before any {@code batch.begin()} for the current frame.</em>
   *
   * @param projectionMatrix the projection the sources are drawn with
   * @param viewport used to restore the screen GL viewport after capture
   * @param sources the candidate sources (inactive ones are skipped)
   * @return the ring opacity to blit at, or {@code 0f} if nothing was captured
   */
  public float captureSources(
      PolygonSpriteBatch batch,
      Matrix4 projectionMatrix,
      Viewport viewport,
      Iterable<? extends OutlineSource> sources) {
    float maxAlpha = 0f;
    for (OutlineSource s : sources) {
      if (s.outlineActive()) maxAlpha = Math.max(maxAlpha, s.outlineAlpha());
    }
    if (maxAlpha <= 0f) return 0f;

    captureWith(
        batch,
        projectionMatrix,
        viewport,
        b -> {
          for (OutlineSource s : sources) {
            if (s.outlineActive()) s.drawForCapture(b);
          }
        });
    return maxAlpha;
  }

  /**
   * Captures arbitrary drawable content into the FBO via a caller-supplied drawer.
   *
   * <p>Clears the FBO once, opens the batch with {@code projectionMatrix}, invokes {@code drawer}
   * to render the content (e.g. each active entity's {@link
   * com.cryptroot.core.world.RenderComponent#draw(PolygonSpriteBatch)}), then closes the FBO and
   * restores the screen viewport. The subsequent {@link #drawOutline} blit then rings every opaque
   * pixel that was drawn.
   *
   * <p><em>Must be called before any {@code batch.begin()} for the current frame.</em>
   *
   * @param projectionMatrix the projection to draw the captured content with
   * @param viewport used to restore the screen GL viewport after capture
   * @param drawer renders the content into the open batch
   */
  public void captureWith(
      PolygonSpriteBatch batch,
      Matrix4 projectionMatrix,
      Viewport viewport,
      java.util.function.Consumer<PolygonSpriteBatch> drawer) {
    fbo.begin();
    Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

    batch.setProjectionMatrix(projectionMatrix);
    batch.begin();
    drawer.accept(batch);
    batch.end();

    // Restore the default blend mode in case the drawer changed it.
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

    fbo.end();
    viewport.apply();
  }

  /**
   * Blits the last captured FBO with a per-pixel outline. <em>Must be called inside an active
   * {@code batch.begin()/end()} block.</em>
   *
   * @param alpha 0–1 outline ring opacity; the sprite body always renders at full opacity — only
   *     the white edge fades during hover enter/exit
   */
  public void drawOutline(PolygonSpriteBatch batch, float alpha) {
    // setShader() on an active batch flushes buffered geometry before switching.
    batch.setShader(outlineShader);
    batch.setBlendFunctionSeparate(
        GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA,
        GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

    outlineShader.setUniformf("u_texelSize", 1f / fboWidth, 1f / fboHeight);
    outlineShader.setUniformf("u_outlineRadius", outlineRadius);
    outlineShader.setUniformf("u_glowRadius", GLOW_RADIUS);
    outlineShader.setUniformf("u_outlineStyle", currentStyle == OutlineStyle.GLOW ? 1f : 0f);
    outlineShader.setUniformf("u_outlineColor", 1f, 1f, 1f, alpha);

    batch.draw(fboRegion, 0f, 0f, worldWidth, worldHeight);

    batch.setShader(null);
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
  }

  /**
   * Camera-relative variant of {@link #drawOutline(PolygonSpriteBatch, float)}.
   *
   * <p>Use this overload when the FBO was captured with a <em>different</em> (e.g. panned) camera
   * than the one active for the current batch pass. Pass the rectangle that corresponds to the
   * capturing camera's visible area in the <em>current batch's</em> coordinate space so that each
   * FBO pixel lands on the correct screen position.
   *
   * <p>Example — isometric world camera captured into FBO, then blitted in a fixed UI camera pass:
   *
   * <pre>{@code
   * float left   = worldCam.position.x - worldCam.viewportWidth  / 2f;
   * float bottom = worldCam.position.y - worldCam.viewportHeight / 2f;
   * renderer.drawOutline(batch, alpha, left, bottom,
   *                      worldCam.viewportWidth, worldCam.viewportHeight);
   * }</pre>
   *
   * @param x left edge of the capture rectangle in the current batch's space
   * @param y bottom edge
   * @param w width
   * @param h height
   */
  public void drawOutline(
      PolygonSpriteBatch batch, float alpha, float x, float y, float w, float h) {
    batch.setShader(outlineShader);
    batch.setBlendFunctionSeparate(
        GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA,
        GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

    outlineShader.setUniformf("u_texelSize", 1f / fboWidth, 1f / fboHeight);
    outlineShader.setUniformf("u_outlineRadius", outlineRadius);
    outlineShader.setUniformf("u_glowRadius", GLOW_RADIUS);
    outlineShader.setUniformf("u_outlineStyle", currentStyle == OutlineStyle.GLOW ? 1f : 0f);
    outlineShader.setUniformf("u_outlineColor", 1f, 1f, 1f, alpha);

    batch.draw(fboRegion, x, y, w, h);

    batch.setShader(null);
    batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
  }

  @Override
  public void dispose() {
    outlineShader.dispose();
    if (fbo != null) {
      fbo.dispose();
    }
  }

  private void createFbo(int width, int height) {
    fboWidth = width;
    fboHeight = height;
    fbo = new FrameBuffer(Pixmap.Format.RGBA8888, fboWidth, fboHeight, false);
    fboRegion = new TextureRegion(fbo.getColorBufferTexture());
    fboRegion.flip(false, true); // FBO is Y-up; flip V for SpriteBatch's Y-down convention
  }
}
