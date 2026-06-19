#version 150

uniform sampler2D Sampler0;
uniform float Saturation;
uniform float Contrast;
uniform vec3 ColorTint;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord);
    
    // 1. Contrast
    color.rgb = (color.rgb - 0.5) * Contrast + 0.5;
    
    // 2. Saturation
    float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    color.rgb = mix(vec3(luma), color.rgb, Saturation);
    
    // 3. Color Tint
    color.rgb *= ColorTint;
    
    // Clamp to avoid artifacts
    color.rgb = clamp(color.rgb, 0.0, 1.0);
    
    fragColor = vec4(color.rgb, 1.0); // full alpha
}
