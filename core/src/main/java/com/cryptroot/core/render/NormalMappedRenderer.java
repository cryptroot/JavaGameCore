package com.cryptroot.core.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.PolygonSpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import java.util.Collection;
import java.util.Collections;

public final class NormalMappedRenderer implements Disposable {

  /** Must match {@code #define MAX_LIGHTS} in {@code normal-mapped.frag}. */
  public static final int MAX_LIGHTS = 8;

  private static final String VERTEX_SHADER_PATH = "shaders/normal-mapped.vert";
  private static final String FRAGMENT_SHADER_PATH = "shaders/normal-mapped.frag";

  private final ShaderProgram shader;
  private final Texture fallbackNormalMap;
  private final Vector3 ambientColor = new Vector3(0.44f, 0.46f, 0.54f);
  private final Vector3 lightColor = new Vector3(1.18f, 1.12f, 1.06f);
  private float lightRadius = 640f;
  private float normalIntensity = 1f;
  private int activeLightCount = 0;

  public NormalMappedRenderer() {
    String vertexShader = Gdx.files.classpath(VERTEX_SHADER_PATH).readString();
    String fragmentShader = Gdx.files.classpath(FRAGMENT_SHADER_PATH).readString();
    shader = new ShaderProgram(vertexShader, fragmentShader);
    if (!shader.isCompiled()) {
      throw new IllegalStateException(
          "Failed to compile normal-mapped Spine shader: " + shader.getLog());
    }

    fallbackNormalMap = createFallbackNormalMap();
  }

  public void setLightRadius(float lightRadius) {
    this.lightRadius = lightRadius;
  }

  /**
   * Opens a normal-mapped draw session.
   *
   * <p>Each light in {@code lightPositions} contributes diffuse illumination. Light positions use
   * world-space XY with the Z component as height above the ground plane (e.g. 260f for a typical
   * standing-height point light). Extra lights beyond {@link #MAX_LIGHTS} are ignored; missing
   * lights are treated as inactive (zero contribution).
   *
   * @param batch the batch to draw into (must not be begun)
   * @param projectionMatrix world-camera combined matrix
   * @param lightPositions active scene lights; pass an empty collection for ambient-only
   */
  public void begin(
      PolygonSpriteBatch batch, Matrix4 projectionMatrix, Collection<Vector3> lightPositions) {
    batch.setProjectionMatrix(projectionMatrix);
    batch.setShader(shader);
    batch.begin();

    shader.setUniformi("u_texture", 0);
    shader.setUniformi("u_normalTexture", 1);
    shader.setUniformf("u_ambientColor", ambientColor);
    shader.setUniformf("u_lightColor", lightColor);
    shader.setUniformf("u_lightRadius", lightRadius);
    shader.setUniformf("u_normalIntensity", normalIntensity);

    // Upload per-light data — one setUniformf call per slot for broad driver compatibility.
    int i = 0;
    for (Vector3 light : lightPositions) {
      if (i >= MAX_LIGHTS) break;
      shader.setUniformf("u_lightPositions[" + i + "]", light.x, light.y, light.z);
      shader.setUniformf("u_lightEnabled[" + i + "]", 1f);
      i++;
    }
    activeLightCount = i;
    // Zero out any remaining slots.
    for (; i < MAX_LIGHTS; i++) {
      shader.setUniformf("u_lightPositions[" + i + "]", 0f, 0f, 0f);
      shader.setUniformf("u_lightEnabled[" + i + "]", 0f);
    }
  }

  /**
   * Convenience overload for a single light source (the common interactive case).
   *
   * @param lightPosition world-space XY position with Z = height above ground
   */
  public void begin(PolygonSpriteBatch batch, Matrix4 projectionMatrix, Vector3 lightPosition) {
    begin(batch, projectionMatrix, Collections.singletonList(lightPosition));
  }

  /**
   * Draws {@code instance} using the normal-mapped shader. Must be called between {@link #begin}
   * and {@link #end}.
   *
   * <p>Light positions were supplied to {@link #begin} — they are not repeated here, so all units
   * in one session share the same scene lights.
   */
  public void draw(PolygonSpriteBatch batch, NormalMappedDrawable instance) {
    Texture normalMap = instance.normalMap();
    boolean unlit = (normalMap == null);
    if (unlit) {
      normalMap = fallbackNormalMap;
      for (int i = 0; i < activeLightCount; i++) {
        shader.setUniformf("u_lightEnabled[" + i + "]", 0f);
      }
      shader.setUniformf("u_ambientColor", 1f, 1f, 1f);
    }

    normalMap.bind(1);
    Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

    instance.draw(batch);

    if (unlit) {
      for (int i = 0; i < activeLightCount; i++) {
        shader.setUniformf("u_lightEnabled[" + i + "]", 1f);
      }
      shader.setUniformf("u_ambientColor", ambientColor);
    }
  }

  public void end(PolygonSpriteBatch batch) {
    batch.end();
    batch.setShader(null);
    Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
  }

  @Override
  public void dispose() {
    fallbackNormalMap.dispose();
    shader.dispose();
  }

  private static Texture createFallbackNormalMap() {
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(0.5f, 0.5f, 1f, 1f);
    pixmap.drawPixel(0, 0);

    Texture texture = new Texture(pixmap);
    texture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
    pixmap.dispose();
    return texture;
  }
}
