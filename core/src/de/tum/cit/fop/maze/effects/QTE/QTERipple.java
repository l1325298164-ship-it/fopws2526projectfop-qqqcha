package de.tum.cit.fop.maze.effects.QTE;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Pool;

/**
 * 单个 QTE 波纹粒子
 */
public class QTERipple implements Pool.Poolable {
    public float x, y;          // 圆心
    public float radius;        // 当前半径
    public float maxRadius;     // 最大扩散半径
    public float maxTime;       // 总存活时间
    public float currentTime;   // 当前时间
    public Color color;         // 颜色
    public boolean active;      // 是否存活

    public QTERipple() {
        this.active = false;
        this.color = new Color();
    }

    public void init(float centerX, float centerY) {
        this.x = centerX;
        this.y = centerY;
        this.radius = 10f;       // 初始半径
        this.maxRadius = 400f;   // 扩散半径
        this.currentTime = 0f;
        this.maxTime = 1.2f;     // 持续 1.2 秒
        this.active = true;

        randomizeColor();
    }

    private void randomizeColor() {
        float r = MathUtils.random();
        if (r < 0.33f) {
            color.set(1f, 0.2f, 0.7f, 1f); // 粉
        } else if (r < 0.66f) {
            color.set(1f, 0.95f, 0.2f, 1f); // 黄
        } else {
            color.set(0.1f, 1f, 1f, 1f); // 青
        }
    }

    public void update(float delta) {
        if (!active) return;

        currentTime += delta;
        float progress = currentTime / maxTime;

        if (progress >= 1f) {
            active = false;
            return;
        }

        // 动画：三次缓动 (Out Cubic)
        float t = 1f - (float) Math.pow(1f - progress, 3);
        radius = 10f + (maxRadius - 10f) * t;

        // 透明度：淡入淡出
        if (progress < 0.2f) {
            color.a = progress / 0.2f;
        } else {
            color.a = 1f - (progress - 0.2f) / 0.8f;
        }
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