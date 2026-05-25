#version 150

uniform vec4 Color;
out vec4 fragColor;

void main() {
    // Explicitly ignore ANY texture sampling
    fragColor = Color;
}
