#version 150

uniform sampler2D Sampler0;
uniform vec2 ScreenSize;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = vec4(0.0);
    vec2 step = 1.5 / ScreenSize;
    
    // Gaussian Blur Kernel 5x5
    color += texture(Sampler0, texCoord + vec2(-2.0, -2.0) * step) * 0.003;
    color += texture(Sampler0, texCoord + vec2(-1.0, -2.0) * step) * 0.013;
    color += texture(Sampler0, texCoord + vec2( 0.0, -2.0) * step) * 0.022;
    color += texture(Sampler0, texCoord + vec2( 1.0, -2.0) * step) * 0.013;
    color += texture(Sampler0, texCoord + vec2( 2.0, -2.0) * step) * 0.003;

    color += texture(Sampler0, texCoord + vec2(-2.0, -1.0) * step) * 0.013;
    color += texture(Sampler0, texCoord + vec2(-1.0, -1.0) * step) * 0.059;
    color += texture(Sampler0, texCoord + vec2( 0.0, -1.0) * step) * 0.097;
    color += texture(Sampler0, texCoord + vec2( 1.0, -1.0) * step) * 0.059;
    color += texture(Sampler0, texCoord + vec2( 2.0, -1.0) * step) * 0.013;

    color += texture(Sampler0, texCoord + vec2(-2.0,  0.0) * step) * 0.022;
    color += texture(Sampler0, texCoord + vec2(-1.0,  0.0) * step) * 0.097;
    color += texture(Sampler0, texCoord + vec2( 0.0,  0.0) * step) * 0.159;
    color += texture(Sampler0, texCoord + vec2( 1.0,  0.0) * step) * 0.097;
    color += texture(Sampler0, texCoord + vec2( 2.0,  0.0) * step) * 0.022;

    color += texture(Sampler0, texCoord + vec2(-2.0,  1.0) * step) * 0.013;
    color += texture(Sampler0, texCoord + vec2(-1.0,  1.0) * step) * 0.059;
    color += texture(Sampler0, texCoord + vec2( 0.0,  1.0) * step) * 0.097;
    color += texture(Sampler0, texCoord + vec2( 1.0,  1.0) * step) * 0.059;
    color += texture(Sampler0, texCoord + vec2( 2.0,  1.0) * step) * 0.013;

    color += texture(Sampler0, texCoord + vec2(-2.0,  2.0) * step) * 0.003;
    color += texture(Sampler0, texCoord + vec2(-1.0,  2.0) * step) * 0.013;
    color += texture(Sampler0, texCoord + vec2( 0.0,  2.0) * step) * 0.022;
    color += texture(Sampler0, texCoord + vec2( 1.0,  2.0) * step) * 0.013;
    color += texture(Sampler0, texCoord + vec2( 2.0,  2.0) * step) * 0.003;

    // Apply dark frosted tint
    vec4 tint = vec4(0.05, 0.05, 0.07, 0.7);
    fragColor = vec4(mix(color.rgb, tint.rgb, tint.a), 1.0);
}
