#version 330

in vec4 v_color;
in vec2 v_texCoords;

uniform sampler2D u_texture;
uniform vec2 u_maskCenter;
uniform float u_maskRadius;
uniform float u_maxAlpha;
uniform float u_time; // 接收时间，用于边缘蠕动

out vec4 fragColor;

void main() {
    // 1. 计算当前像素到中心的距离
    float dx = gl_FragCoord.x - u_maskCenter.x;
    float dy = gl_FragCoord.y - u_maskCenter.y;
    float dist = sqrt(dx*dx + dy*dy);

    // 2. 边缘扭曲逻辑 (让圆圈边缘像云雾一样波动)
    // atan2 计算角度，sin 根据角度产生波浪，u_time 让波浪动起来
    float angle = atan(dy, dx);
    float distortion = sin(angle * 8.0 + u_time * 2.5) * (u_maskRadius * 0.05);

    // 3. 应用扭曲后的距离
    float finalDist = dist + distortion;

    // 4. 计算遮罩 (0.7 这里的比例可以微调亮部大小)
    float alphaMask = smoothstep(u_maskRadius * 0.7, u_maskRadius, finalDist);

    // 5. 输出：纯黑背景 + 动态透明度
    fragColor = vec4(0.0, 0.0, 0.0, u_maxAlpha * alphaMask);
}