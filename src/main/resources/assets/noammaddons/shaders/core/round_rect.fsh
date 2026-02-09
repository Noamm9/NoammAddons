#version 150

#moj_import <minecraft:dynamictransforms.glsl>

layout(std140) uniform u {
    vec4 u_Rect;          // xy = center, zw = size
    vec4 u_Radii;         // per-corner radii (all same for simple usage)
    vec4 u_Color;         // primary fill color (RGBA)
    vec4 u_Color2;        // secondary color (for gradients)
    vec4 u_ColorShadow;   // shadow color
    vec2 u_GradientDir;   // gradient direction
    float u_EdgeSoftness;  // anti-aliasing softness (1.0 = smooth)
    float u_ShadowSoftness;
    float u_BorderWidth;   // 0 = filled, >0 = outline only
};

in vec2 f_Position;
out vec4 fragColor;

float roundedBoxSDF(vec2 CenterPosition, vec2 Size, vec4 Radius) {
    Radius.xy = (CenterPosition.x > 0.) ? Radius.xy : Radius.zw;
    Radius.x  = (CenterPosition.y > 0.) ? Radius.x  : Radius.y;
    vec2 q = abs(CenterPosition) - Size + Radius.x;
    return min(max(q.x, q.y), 0.) + length(max(q, 0.)) - Radius.x;
}

void main() {
    vec2 halfSize = u_Rect.zw / 2.0;
    float dist = roundedBoxSDF(f_Position - u_Rect.xy, halfSize, u_Radii);
    float outerAlpha = 1.0 - smoothstep(0.0, u_EdgeSoftness, dist);

    if (u_BorderWidth > 0.0) {
        // Outline mode: subtract inner rect
        float innerDist = roundedBoxSDF(f_Position - u_Rect.xy, halfSize - u_BorderWidth,
        max(u_Radii - u_BorderWidth, vec4(0.0)));
        float innerAlpha = 1.0 - smoothstep(0.0, u_EdgeSoftness, innerDist);
        fragColor = vec4(u_Color.rgb, u_Color.a * (outerAlpha - innerAlpha));
    }
    else {
        // Filled mode with optional gradient
        vec2 uv = (f_Position - u_Rect.xy) / u_Rect.zw;
        float gradientStrength = clamp(dot(uv, u_GradientDir) + 0.5, 0.0, 1.0);
        vec4 gradientColor = mix(u_Color, u_Color2, gradientStrength);
        fragColor = vec4(gradientColor.rgb, gradientColor.a * outerAlpha);
    }
}