#version 150

/*
 * inv_glass.vsh — Inventory HUD Glass Panel Vertex Shader
 * Primary Responsibility:
 *   Passes screen-space UV coordinates to the fragment shader for full-screen
 *   framebuffer sampling in the frosted-glass effect.
 */

in vec3 Position;
in vec2 TexCoord0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec2 texCoord;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    texCoord = TexCoord0;
}
