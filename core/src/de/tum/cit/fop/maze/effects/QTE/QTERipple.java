package de.tum.cit.fop.maze.effects.QTE;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Pool;

/**
 * 单个 QTE 波纹粒子（圆形扩散）
 */
public class QTERipple implements Pool.Poolable {
    public float x, y;          // 圆心坐标
    public float radius;        // 当前半径
    public float maxRadius;     // 最大半径（扩散多远）
    public float maxTime;       // 总存活时间
    public float currentTime;   // 当前存活时间
    public Color color;         // 颜色
    public boolean active;      // 是否存活

    public QTERipple() {
        this.active = false;
        this.color = new Color();
    }

    /**
     * 初始化波纹
     * @param centerX 圆心 X
     * @param centerY 圆心 Y
     */
    public void init(float centerX, float centerY) {
        this.x = centerX;
        this.y = centerY;
        this.radius = 5f;       // 初始半径
        this.maxRadius = 300f;  // 最大扩散半径（可以根据需求改大改小）
        this.currentTime = 0f;
        this.maxTime = 0.6f;    // 持续 0.6 秒
        this.active = true;

        randomizeColor();
    }

    private void randomizeColor() {
        float r = MathUtils.random();
        if (r < 0.33f) {
            // 🌸 亮粉色 (Hot Pink)
            color.set(1f, 0.2f, 0.6f, 1f);
        } else if (r < 0.66f) {
            // ⚡ 明黄色 (Bright Yellow)
            color.set(1f, 0.9f, 0.1f, 1f);
        } else {
            // 💎 青蓝色 (Cyan)
            color.set(0f, 1f, 1f, 1f);
        }
        // 初始 alpha 设为 1
        color.a = 1f;
    }

    public void update(float delta) {
        if (!active) return;

        currentTime += delta;
        float progress = currentTime / maxTime;

        if (progress >= 1f) {
            active = false;
            return;
        }

        // 🟢 动画逻辑
        // 1. 半径变大 (使用 easeOut 效果，先快后慢)
        float t = 1f - (float) Math.pow(1f - progress, 2);
        radius = 5f + (maxRadius - 5f) * t;

        // 2. 透明度变低 (最后阶段消失快一点)
        color.a = 1f - progress;
    }

    @Override
    public void reset() {
        x = 0; y = 0;
        radius = 0;
        currentTime = 0;
        active = false;
        color.set(1, 1, 1, 1);
    }
}