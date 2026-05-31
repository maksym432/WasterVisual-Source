#version 150

uniform sampler2D Sampler0; // Copied background scene texture
uniform vec4 Color;          // Color containing alpha from Java
uniform float Time;
uniform float DistortionStrength;
uniform vec2 ScreenSize;
uniform vec2 Size;          // Dynamic panel size
uniform float Radius;       // Corner radius
uniform vec4 TexBounds;     // Screen-space texture coordinates boundaries (u1, v1, u2, v2)

in vec2 texCoord;           // Local quad coordinate, strictly [0, 1]
out vec4 fragColor;

// SDF function for a rounded rectangle
float sdRoundedRect(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

// Hash function to create glassy noise
float hash(vec2 p) {
    p = 50.0 * fract(p * 0.3183099 + vec2(0.71, 0.113));
    return -1.0 + 2.0 * fract(p.x * p.y * (p.x + p.y));
}

void main() {
    // 1. Calculate SDF rounded corner mask in local space
    vec2 localP = (texCoord - 0.5) * Size;
    float d = sdRoundedRect(localP, Size * 0.5, Radius);
    
    // Smooth anti-aliased edge clip
    float cornerAlpha = 1.0 - smoothstep(-1.0, 0.0, d);
    if (cornerAlpha <= 0.0) discard;

    // 2. Map local texture coordinate [0, 1] to screen-space [u1, v1] -> [u2, v2]
    vec2 uv = mix(TexBounds.xy, TexBounds.zw, texCoord);

    // 3. Calculate glass distortion
    vec2 noiseUV = uv * 55.0; 
    float noise = hash(noiseUV + sin(Time * 0.3));
    
    vec2 distortion = vec2(
        sin(uv.y * 25.0 + Time) * 0.0015 + noise * 0.001,
        cos(uv.x * 25.0 + Time) * 0.0015 + noise * 0.001
    );
    distortion *= DistortionStrength;

    vec2 targetUV = uv + distortion;

    // 4. 9-tap Gaussian blur
    vec2 blurStep = vec2(2.5) / ScreenSize; 
    vec4 blurredColor = vec4(0.0);
    
    float weights[9] = float[](
        1.0/16.0, 2.0/16.0, 1.0/16.0,
        2.0/16.0, 4.0/16.0, 2.0/16.0,
        1.0/16.0, 2.0/16.0, 1.0/16.0
    );
    
    int idx = 0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 offset = vec2(float(x), float(y)) * blurStep;
            // Clamp coordinates to stay within TexBounds limits
            vec2 sampledUV = clamp(targetUV + offset, min(TexBounds.xy, TexBounds.zw), max(TexBounds.xy, TexBounds.zw));
            blurredColor += texture(Sampler0, sampledUV) * weights[idx];
            idx++;
        }
    }

    // 5. Frosted glass overlay
    float distFromCenter = length(texCoord - vec2(0.5));
    float vignette = 1.0 - smoothstep(0.4, 0.8, distFromCenter);
    
    // Mix blurred scene with a light white frosted tint (15% opacity)
    vec3 frostedGlass = mix(blurredColor.rgb, vec3(1.0), 0.15);
    
    // Slightly dim screen borders
    frostedGlass *= mix(0.88, 1.0, vignette);

    fragColor = vec4(frostedGlass, Color.a * cornerAlpha);
}
