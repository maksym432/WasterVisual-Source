#version 150

uniform sampler2D Sampler0;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    // Perfectly clean glass (no distortion, no streaks)
    vec4 color = texture(Sampler0, texCoord);
    
    // Subtle glass tint and brightening
    color.rgb = color.rgb * 1.05 + 0.05;
    
    // Very soft vignette for depth
    vec2 centerOffset = texCoord - vec2(0.5);
    float vignette = 1.0 - length(centerOffset) * 0.2;
    color.rgb *= vignette;

    // Very subtle uniform gloss
    color.rgb += 0.02;

    fragColor = vec4(color.rgb, 1.0);
}
