#version 150

in vec4 vertexColor;
in vec2 localPos;

out vec4 fragColor;

uniform float Time;

void main() {
    float dist = length(localPos);
    
    // Holographic energy wave pattern pulsing outwards
    float wave = sin(dist * 15.0 - Time * 8.0) * 0.5 + 0.5;
    
    // Dynamic noise
    float noise = fract(sin(dot(localPos, vec2(12.9898, 78.233))) * 43758.5453);
    
    // Atmospheric glow strength
    float glow = 0.6 + 0.4 * wave + 0.1 * noise;
    
    // Color tinting: make the inner core slightly brighter / white, and outer edge more saturated
    vec3 finalColor = mix(vertexColor.rgb, vec3(1.0), 0.3 * (1.0 - smoothstep(0.0, 1.5, dist)));
    
    // Boost RGB for intense additive neon glow
    fragColor = vec4(finalColor * glow * 1.6, vertexColor.a * glow);
}
