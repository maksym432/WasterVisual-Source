#version 150

uniform vec4 Color;
uniform vec2 Size;
uniform float Thickness;
uniform float Gap;
uniform float Mode;
uniform float Rainbow;
uniform float Time;

in vec2 texCoord;
out vec4 fragColor;

float sdSegment(vec2 p, vec2 a, vec2 b) {
    vec2 pa = p - a, ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h);
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

void main() {
    vec2 p = (texCoord - 0.5) * Size;
    float d = 1000.0;
    
    int modeInt = int(Mode + 0.5);
    float r = Thickness * 0.5;
    float c = Gap;
    float l = Size.x * 0.4;
    
    if (modeInt == 0) { // Cross
        float dL = sdSegment(p, vec2(-c-l, 0.0), vec2(-c, 0.0)) - r;
        float dR = sdSegment(p, vec2(c, 0.0), vec2(c+l, 0.0)) - r;
        float dT = sdSegment(p, vec2(0.0, -c-l), vec2(0.0, -c)) - r;
        float dB = sdSegment(p, vec2(0.0, c), vec2(0.0, c+l)) - r;
        d = min(min(dL, dR), min(dT, dB));
    } else if (modeInt == 1) { // Dot
        d = length(p) - Thickness;
    } else if (modeInt == 2) { // Circle
        d = abs(length(p) - Gap) - r;
    } else if (modeInt == 3) { // 4-Corners
        vec2 q = abs(p) - vec2(c);
        if (q.x > q.y) q.xy = q.yx;
        vec2 cl = vec2(0.0, clamp(q.y, 0.0, l));
        d = length(q - cl) - r;
    }
    
    vec4 finalColor = Color;
    if (Rainbow > 0.5) {
        vec2 centerDir = texCoord - vec2(0.5);
        float angle = atan(centerDir.y, centerDir.x); // -PI to PI
        float hue = angle / 6.2831853 + Time;
        finalColor = vec4(hsv2rgb(vec3(hue, 1.0, 1.0)), Color.a);
    }
    
    float alpha = 1.0 - smoothstep(-1.0, 0.0, d);
    if (alpha <= 0.0) discard;
    
    fragColor = vec4(finalColor.rgb, finalColor.a * alpha);
}
