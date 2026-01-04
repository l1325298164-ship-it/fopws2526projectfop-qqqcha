#version 330

in vec4 v_color;
in vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec2 u_maskCenter;
uniform float u_maskRadius;
uniform float u_maxAlpha;

// 定义输出颜色变量
out vec4 fragColor;

void main() {
    // 计算当前像素到猫的距离
    float dist = distance(gl_FragCoord.xy, u_maskCenter);

    // 计算圆洞的遮罩
    // 0.7 意味着 70% 范围内是完全透明的，之后平滑变黑
    float alphaMask = smoothstep(u_maskRadius * 0.7, u_maskRadius, dist);

    // 最终输出：纯黑色的 RGB (0,0,0) 配合动态透明度
    // u_maxAlpha 是从 Java 传过来的淡入值 (0.0 -> 0.85)
    fragColor = vec4(0.0, 0.0, 0.0, u_maxAlpha * alphaMask);
}