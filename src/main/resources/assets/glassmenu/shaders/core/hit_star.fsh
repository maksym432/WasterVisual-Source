#version 150

in vec4 vertexColor;
in vec2 localPos;

out vec4 fragColor;

uniform float Time;
uniform float Mode;
uniform float Opacity;

// ── Smooth-minimum: blends two signed-distance fields without hard seams ─────
float smin(float a, float b, float k) {
    float h = clamp(0.5 + 0.5 * (b - a) / k, 0.0, 1.0);
    return mix(b, a, h) - k * h * (1.0 - h);
}

// ── Puff-star SDF = smooth union of 5 circles arranged in a star pattern ────
// Result < 0  → inside the star body
// Result = 0  → on the surface
// Result > 0  → outside (used for glow decay)
float puffStarSDF(vec2 p) {
    float result  = 1e6;
    float orbitR  = 0.22;   // how far out the 5 circle-centres sit
    float circleR = 0.50;   // each circle's radius — large = fat/puffy body
    float blend   = 0.22;   // smin blend width — larger = smoother joins

    // First tip points straight up (offset –π/2)
    for (int i = 0; i < 5; i++) {
        float angle  = float(i) * 6.28318530 / 5.0 - 1.57079632;
        vec2  centre = vec2(cos(angle), sin(angle)) * orbitR;
        float d      = length(p - centre) - circleR;
        result = smin(result, d, blend);
    }
    return result;
}

void main() {
    // Remap UV [0,1]² → centred [-1,1]²
    vec2 p   = localPos * 2.0 - 1.0;
    float sdf = puffStarSDF(p);

    // ── Base colour ──────────────────────────────────────────────────────────
    vec3 baseColor;
    if (Mode < 0.5) {
        baseColor = vertexColor.rgb;
    } else {
        float phi = atan(p.y, p.x);
        float hue = fract(phi / 6.28318530 - Time * 0.35);
        vec3 c = abs(fract(hue + vec3(0.0, 0.333, 0.667)) * 6.0 - 3.0) - 1.0;
        baseColor = clamp(c, 0.0, 1.0);
    }

    // ── Star body mask — ultra-wide smoothstep for maximum edge softness ─────
    float body = 1.0 - smoothstep(-0.10, 0.18, sdf);

    // ── Inner volume / self-illumination ─────────────────────────────────────
    // The star glows brighter at its centre, darker towards the surface edge —
    // giving the "dense warm light from inside" look.
    float distCenter = length(p);
    float innerGlow  = 1.0 - smoothstep(0.0, 0.72, distCenter);

    // Surface depth gradient using the SDF:
    //   deep inside (sdf ≈ -0.5) → very bright
    //   near edge   (sdf ≈  0.0) → base colour
    float depth = clamp(-sdf / 0.55, 0.0, 1.0);   // 0 at edge, 1 deep inside

    // Combine into a rich, saturated surface colour
    vec3 centerColor  = min(baseColor * 2.0, vec3(1.0));  // over-bright for the core
    vec3 surfaceColor = mix(baseColor * 0.75, centerColor,
                            depth * 0.7 + innerGlow * 0.3);

    // ── Specular highlight ────────────────────────────────────────────────────
    // Small, soft, slightly off-centre bright spot — fakes a light source above.
    vec2  specCentre = vec2(-0.12, 0.18);
    float specD      = length(p - specCentre);
    float spec       = exp(-specD * specD * 9.0) * 0.55 * body;
    // Warm white specular (tinted slightly yellow so it doesn't look cold)
    surfaceColor += vec3(spec * 0.95, spec * 0.90, spec * 0.45);

    // ── Outer glow (aura) ────────────────────────────────────────────────────
    // Decays exponentially outside the star body.
    float outerSDF = max(sdf, 0.0);
    float glow     = exp(-3.2 * outerSDF) * 0.72;
    // Glow colour is slightly brighter / warmer than the star surface
    vec3 glowColor  = min(baseColor * 1.3, vec3(1.0));

    // ── Composite ────────────────────────────────────────────────────────────
    // Layer: glow underneath → body on top (alpha-blended)
    vec3  finalRGB   = mix(glowColor * glow, surfaceColor, body);
    float finalAlpha = max(body, glow * 0.60) * Opacity;

    if (finalAlpha < 0.002) discard;
    fragColor = vec4(finalRGB, finalAlpha);
}
