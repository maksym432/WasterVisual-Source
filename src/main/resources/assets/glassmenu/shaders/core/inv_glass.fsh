#version 150

/*
 * inv_glass.fsh — Inventory HUD Glass Panel Fragment Shader
 * Primary Responsibility:
 *   Renders a frosted-glass panel for the Inventory HUD transparent mode.
 *   Samples the captured game framebuffer (Sampler0) with a Gaussian blur to
 *   simulate backdrop-filter: blur(). An SDF rounded-rectangle mask clips the
 *   result so it stays inside the panel's rounded corners.
 *   A subtle light tint + specular highlight give the iOS liquid-glass look.
 */

uniform sampler2D Sampler0;   // Captured framebuffer (game scene)
uniform vec2 ScreenSize;      // Framebuffer resolution in pixels
uniform vec2 PanelPos;        // Top-left of panel in screen pixels
uniform vec2 PanelSize;       // Width / Height of panel in pixels
uniform float CornerRadius;   // Corner radius in pixels

in vec2 texCoord;             // UV over the full screen [0..1]
out vec4 fragColor;

// --- SDF rounded rectangle ---
float sdRoundedBox(vec2 p, vec2 halfSize, float radius) {
    vec2 q = abs(p) - halfSize + radius;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - radius;
}

// --- 5x5 Gaussian blur ---
vec4 gaussianBlur(vec2 uv) {
    vec4 color = vec4(0.0);
    vec2 step = 2.0 / ScreenSize;

    color += texture(Sampler0, uv + vec2(-2.0, -2.0) * step) * 0.003;
    color += texture(Sampler0, uv + vec2(-1.0, -2.0) * step) * 0.013;
    color += texture(Sampler0, uv + vec2( 0.0, -2.0) * step) * 0.022;
    color += texture(Sampler0, uv + vec2( 1.0, -2.0) * step) * 0.013;
    color += texture(Sampler0, uv + vec2( 2.0, -2.0) * step) * 0.003;

    color += texture(Sampler0, uv + vec2(-2.0, -1.0) * step) * 0.013;
    color += texture(Sampler0, uv + vec2(-1.0, -1.0) * step) * 0.059;
    color += texture(Sampler0, uv + vec2( 0.0, -1.0) * step) * 0.097;
    color += texture(Sampler0, uv + vec2( 1.0, -1.0) * step) * 0.059;
    color += texture(Sampler0, uv + vec2( 2.0, -1.0) * step) * 0.013;

    color += texture(Sampler0, uv + vec2(-2.0,  0.0) * step) * 0.022;
    color += texture(Sampler0, uv + vec2(-1.0,  0.0) * step) * 0.097;
    color += texture(Sampler0, uv + vec2( 0.0,  0.0) * step) * 0.159;
    color += texture(Sampler0, uv + vec2( 1.0,  0.0) * step) * 0.097;
    color += texture(Sampler0, uv + vec2( 2.0,  0.0) * step) * 0.022;

    color += texture(Sampler0, uv + vec2(-2.0,  1.0) * step) * 0.013;
    color += texture(Sampler0, uv + vec2(-1.0,  1.0) * step) * 0.059;
    color += texture(Sampler0, uv + vec2( 0.0,  1.0) * step) * 0.097;
    color += texture(Sampler0, uv + vec2( 1.0,  1.0) * step) * 0.059;
    color += texture(Sampler0, uv + vec2( 2.0,  1.0) * step) * 0.013;

    color += texture(Sampler0, uv + vec2(-2.0,  2.0) * step) * 0.003;
    color += texture(Sampler0, uv + vec2(-1.0,  2.0) * step) * 0.013;
    color += texture(Sampler0, uv + vec2( 0.0,  2.0) * step) * 0.022;
    color += texture(Sampler0, uv + vec2( 1.0,  2.0) * step) * 0.013;
    color += texture(Sampler0, uv + vec2( 2.0,  2.0) * step) * 0.003;

    return color;
}

void main() {
    // Pixel position on screen
    vec2 fragPx = texCoord * ScreenSize;

    // Local position relative to panel center
    vec2 panelCenter = PanelPos + PanelSize * 0.5;
    vec2 localPx = fragPx - panelCenter;

    // SDF mask: distance from inside the rounded rect (negative = inside)
    float dist = sdRoundedBox(localPx, PanelSize * 0.5, CornerRadius);

    // Discard pixels outside the panel shape
    if (dist > 0.0) discard;

    // Anti-aliased edge alpha
    float edgeAlpha = 1.0 - smoothstep(-1.0, 0.0, dist);

    // Sample blurred framebuffer
    vec4 blurred = gaussianBlur(texCoord);

    // iOS-style glass tint: very dark semi-transparent overlay
    vec3 tint = vec3(0.08, 0.08, 0.12);
    float tintStrength = 0.45;
    vec3 glassColor = mix(blurred.rgb, tint, tintStrength);

    // Subtle specular highlight on the top edge
    float normalizedY = (fragPx.y - PanelPos.y) / PanelSize.y;
    float highlight = smoothstep(0.15, 0.0, normalizedY) * 0.12;
    glassColor += vec3(highlight);

    fragColor = vec4(glassColor, edgeAlpha * 0.92);
}
