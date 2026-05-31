#version 150

uniform vec4 Color;
uniform vec2 Size;
uniform float Radius;
uniform float EdgeSoftness;
uniform sampler2D Sampler0;
uniform vec4 TexBounds;
uniform float OutlineThickness;
uniform float UseTexture;

in vec2 texCoord;
out vec4 fragColor;

float sdRoundedRect(vec2 p, vec2 b, float r) {
    vec2 q = abs(p) - b + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

void main() {
    // texCoord is strictly [0, 1] representing the local geometry quad
    vec2 p = (texCoord - 0.5) * Size;
    
    float d;
    if (OutlineThickness > 0.0) {
        d = abs(sdRoundedRect(p, Size * 0.5 - OutlineThickness * 0.5, Radius - OutlineThickness * 0.5)) - OutlineThickness * 0.5;
    } else {
        d = sdRoundedRect(p, Size * 0.5, Radius);
    }
    
    float softness = max(fwidth(d), EdgeSoftness);
    float alpha = 1.0 - smoothstep(-softness, 0.0, d);
    
    if (alpha <= 0.0) discard;
    
    // Map geometry [0, 1] texCoord into actual texture bounds [u1, v1] to [u2, v2]
    vec2 mappedTexCoord = mix(TexBounds.xy, TexBounds.zw, texCoord);
    
    vec4 texColor = texture(Sampler0, mappedTexCoord);
    if (UseTexture > 0.5) {
        fragColor = texColor * vec4(Color.rgb, Color.a * alpha);
    } else {
        fragColor = vec4(Color.rgb, Color.a * alpha);
    }
}
