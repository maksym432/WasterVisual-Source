#version 150

in vec4 vertexColor;
in vec2 localPos; // localPos.x = surface distance, localPos.y = angle

out vec4 fragColor;

uniform float Time;

void main() {
    float dist = localPos.x;
    float angle = localPos.y;
    
    // Holographic energy wave pattern pulsing outwards based on true surface distance!
    float wave = sin(dist * 15.0 - Time * 8.0) * 0.5 + 0.5;
    
    // Dynamic noise using both distance and angle
    float noise = fract(sin(dot(vec2(dist, angle), vec2(12.9898, 78.233))) * 43758.5453);
    
    // Atmospheric glow strength
    float glow = 0.6 + 0.4 * wave + 0.1 * noise;
    
    // Color tinting: make the inner core slightly brighter / white, and outer edge more saturated
    vec3 finalColor = mix(vertexColor.rgb, vec3(1.0), 0.3 * (1.0 - smoothstep(0.0, 1.5, dist)));
    
    // Boost RGB for intense additive neon glow
    fragColor = vec4(finalColor * glow * 1.6, vertexColor.a * glow);
}
