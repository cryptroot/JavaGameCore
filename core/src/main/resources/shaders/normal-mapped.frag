#ifdef GL_ES
precision mediump float;
#endif

// Maximum simultaneous point lights.  Must match NormalMappedRenderer.MAX_LIGHTS.
#define MAX_LIGHTS 8

uniform sampler2D u_texture;
uniform sampler2D u_normalTexture;

// Per-light data.  Inactive lights have u_lightEnabled[i] == 0.0 and contribute nothing.
// x, y = world position; z = height above ground plane.
uniform vec3  u_lightPositions[MAX_LIGHTS];
uniform float u_lightEnabled[MAX_LIGHTS];

uniform vec3  u_lightColor;
uniform vec3  u_ambientColor;
uniform float u_lightRadius;
uniform float u_normalIntensity;

varying vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_worldPos;

void main() {
    vec4 baseColor = texture2D(u_texture, v_texCoords) * v_color;
    vec3 normal = texture2D(u_normalTexture, v_texCoords).rgb * 2.0 - 1.0;
    normal.xy *= u_normalIntensity;
    normal = normalize(normal);

    vec3 totalLight = u_ambientColor;

    for (int i = 0; i < MAX_LIGHTS; i++) {
        vec3  lightVec  = vec3(u_lightPositions[i].xy - v_worldPos, u_lightPositions[i].z);
        // Safe normalise: avoid NaN when lightVec is the zero vector.
        float lightLen  = max(length(lightVec), 0.001);
        vec3  lightDir  = lightVec / lightLen;
        float dist      = length(lightVec.xy);

        // Quadratic attenuation, scaled to zero at u_lightRadius.
        // Multiply by u_lightEnabled[i] so inactive lights (enabled == 0) add nothing.
        float attenuation = max(1.0 - (dist / u_lightRadius), 0.0) * u_lightEnabled[i];
        attenuation *= attenuation;

        float diffuse = max(dot(normal, lightDir), 0.0) * attenuation;
        totalLight += u_lightColor * diffuse;
    }

    gl_FragColor = vec4(baseColor.rgb * totalLight, baseColor.a);
}