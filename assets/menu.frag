#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoord;
varying vec4 v_color;

uniform sampler2D u_candy;
uniform sampler2D u_hell;

uniform float u_time;
uniform float u_corruption;

// ===== 噪音 =====
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    vec2 u = f * f * (3.0 - 2.0 * f);

    return mix(a, b, u.x)
    + (c - a) * u.y * (1.0 - u.x)
    + (d - b) * u.x * u.y;
}

void main() {

    vec4 candy = texture2D(u_candy, v_texCoord);
    vec4 hell  = texture2D(u_hell,  v_texCoord);

    // ⭐ 把噪音尺度放大，确保可见
    float n = noise(v_texCoord * 8.0 + vec2(0.0, u_time * 0.2));

    // ⭐ 明确语义：corruption 越大，越偏向 hell
    float mask = smoothstep(
    n - 0.2,
    n + 0.2,
    u_corruption
    );

    gl_FragColor =  v_color * mix(candy, hell, mask);
}
