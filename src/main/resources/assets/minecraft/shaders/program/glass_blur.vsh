#version 150

in vec3 Position;

uniform mat4 ProjMat;
uniform vec2 OutSize;

out vec2 texCoord;

void main() {
    float x = Position.x;
    float y = Position.y;
    gl_Position = ProjMat * vec4(x, y, 0.0, 1.0);
    texCoord = vec2(x / OutSize.x, y / OutSize.y);
}
