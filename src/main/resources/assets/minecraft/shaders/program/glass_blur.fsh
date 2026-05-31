#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;

in vec2 texCoord;
out vec4 fragColor;

float rand(vec2 co) {
    return fract(sin(dot(co, vec2(12.9898, 78.233))) * 43758.5453);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(rand(i), rand(i + vec2(1.0, 0.0)), u.x),
               mix(rand(i + vec2(0.0, 1.0)), rand(i + vec2(1.0, 1.0)), u.x), u.y);
}

void main() {
    // 1. Calculate premium frosted glass refraction (distortion)
    // Medium-frequency waves for organic glass surface refraction
    float nX = valueNoise(texCoord * 35.0) * 2.0 - 1.0;
    float nY = valueNoise(texCoord * 35.0 + vec2(15.2, 7.8)) * 2.0 - 1.0;
    vec2 waveDistort = vec2(nX, nY) * 0.012;
    
    // High-frequency per-pixel grain/noise for frosted texture scattering
    float gX = rand(texCoord * 1200.0) * 2.0 - 1.0;
    float gY = rand(texCoord * 1200.0 + vec2(3.1, 7.4)) * 2.0 - 1.0;
    vec2 grainDistort = vec2(gX, gY) * 0.004;
    
    vec2 distortedCoord = texCoord + waveDistort + grainDistort;
    
    // 2. Perform a 5x5 Gaussian blur on the distorted coordinates to diffuse the light
    vec4 color = vec4(0.0);
    vec2 step = 2.5 / InSize; // Step size controls the blur radius
    
    color += texture(DiffuseSampler, distortedCoord + vec2(-2.0, -2.0) * step) * 0.003;
    color += texture(DiffuseSampler, distortedCoord + vec2(-1.0, -2.0) * step) * 0.013;
    color += texture(DiffuseSampler, distortedCoord + vec2( 0.0, -2.0) * step) * 0.022;
    color += texture(DiffuseSampler, distortedCoord + vec2( 1.0, -2.0) * step) * 0.013;
    color += texture(DiffuseSampler, distortedCoord + vec2( 2.0, -2.0) * step) * 0.003;

    color += texture(DiffuseSampler, distortedCoord + vec2(-2.0, -1.0) * step) * 0.013;
    color += texture(DiffuseSampler, distortedCoord + vec2(-1.0, -1.0) * step) * 0.059;
    color += texture(DiffuseSampler, distortedCoord + vec2( 0.0, -1.0) * step) * 0.097;
    color += texture(DiffuseSampler, distortedCoord + vec2( 1.0, -1.0) * step) * 0.059;
    color += texture(DiffuseSampler, distortedCoord + vec2( 2.0, -1.0) * step) * 0.013;

    color += texture(DiffuseSampler, distortedCoord + vec2(-2.0,  0.0) * step) * 0.022;
    color += texture(DiffuseSampler, distortedCoord + vec2(-1.0,  0.0) * step) * 0.097;
    color += texture(DiffuseSampler, distortedCoord + vec2( 0.0,  0.0) * step) * 0.159;
    color += texture(DiffuseSampler, distortedCoord + vec2( 1.0,  0.0) * step) * 0.097;
    color += texture(DiffuseSampler, distortedCoord + vec2( 2.0,  0.0) * step) * 0.022;

    color += texture(DiffuseSampler, distortedCoord + vec2(-2.0,  1.0) * step) * 0.013;
    color += texture(DiffuseSampler, distortedCoord + vec2(-1.0,  1.0) * step) * 0.059;
    color += texture(DiffuseSampler, distortedCoord + vec2( 0.0,  1.0) * step) * 0.097;
    color += texture(DiffuseSampler, distortedCoord + vec2( 1.0,  1.0) * step) * 0.059;
    color += texture(DiffuseSampler, distortedCoord + vec2( 2.0,  1.0) * step) * 0.013;

    color += texture(DiffuseSampler, distortedCoord + vec2(-2.0,  2.0) * step) * 0.003;
    color += texture(DiffuseSampler, distortedCoord + vec2(-1.0,  2.0) * step) * 0.013;
    color += texture(DiffuseSampler, distortedCoord + vec2( 0.0,  2.0) * step) * 0.022;
    color += texture(DiffuseSampler, distortedCoord + vec2( 1.0,  2.0) * step) * 0.013;
    color += texture(DiffuseSampler, distortedCoord + vec2( 2.0,  2.0) * step) * 0.003;

    fragColor = vec4(color.rgb, 1.0);
}
