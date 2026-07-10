#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform vec2  u_texelSize;      // vec2(1/fboWidth, 1/fboHeight)
uniform float u_outlineRadius;  // hard-edge ring width in FBO pixels (style 0)
uniform float u_glowRadius;     // glow outer radius in FBO pixels   (style 1)
uniform vec4  u_outlineColor;   // straight RGBA; premultiplied before output
// 0.0 = hard edge (binary ring), 1.0 = glow/highlight (smooth distance falloff)
uniform float u_outlineStyle;

// All three varyings are declared to match the shared normal-mapped.vert interface;
// only v_texCoords is read in this shader.
varying vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_worldPos;

void main() {
    vec4 center = texture2D(u_texture, v_texCoords);

    // Sprite body pixels — output the captured colour unchanged.
    if (center.a > 0.5) {
        gl_FragColor = center;
        return;
    }

    // MAX_R is a compile-time constant that bounds the loop for driver compatibility.
    // It must be >= max(u_outlineRadius, u_glowRadius).  6.0 covers both defaults.
    const float MAX_R = 6.0;

    if (u_outlineStyle < 0.5) {
        // --- Style 0: hard edge ---
        // Emit a solid ring wherever a transparent pixel borders an opaque one.
        float maxAlpha = 0.0;
        for (float dx = -MAX_R; dx <= MAX_R; dx += 1.0) {
            for (float dy = -MAX_R; dy <= MAX_R; dy += 1.0) {
                if (length(vec2(dx, dy)) > u_outlineRadius) continue;
                maxAlpha = max(maxAlpha,
                    texture2D(u_texture, v_texCoords + vec2(dx, dy) * u_texelSize).a);
            }
        }
        if (maxAlpha > 0.5) {
            // Premultiply for GL_ONE / GL_ONE_MINUS_SRC_ALPHA (PMA blend mode).
            gl_FragColor = vec4(u_outlineColor.rgb * u_outlineColor.a, u_outlineColor.a);
        } else {
            discard;
        }

    } else {
        // --- Style 1: glow / highlight ---
        // Find the distance to the nearest opaque pixel and map it to a smooth
        // alpha falloff: bright white right at the sprite edge, fading to zero at
        // u_glowRadius.  pow(t, 0.6) gives a broader, softer bloom shape.
        float minDist = MAX_R + 1.0;
        for (float dx = -MAX_R; dx <= MAX_R; dx += 1.0) {
            for (float dy = -MAX_R; dy <= MAX_R; dy += 1.0) {
                float d = length(vec2(dx, dy));
                if (d > u_glowRadius) continue;
                if (texture2D(u_texture, v_texCoords + vec2(dx, dy) * u_texelSize).a > 0.5) {
                    minDist = min(minDist, d);
                }
            }
        }
        if (minDist <= u_glowRadius) {
            // t = 1 at the sprite edge, 0 at u_glowRadius.
            float t = 1.0 - (minDist / u_glowRadius);
            // u_outlineColor.a carries the hover-fade alpha from Java, so the glow
            // fades in/out smoothly on hover enter/exit in addition to the distance falloff.
            float glowAlpha = pow(t, 0.6) * u_outlineColor.a;
            gl_FragColor = vec4(u_outlineColor.rgb * glowAlpha, glowAlpha);
        } else {
            discard;
        }
    }
}
