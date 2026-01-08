package de.tum.cit.fop.maze.effects.QTE;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Pool;

/**
 * 单个 QTE 波纹粒子
 */
public class QTERipple implements Pool.Poolable {
    public float x, y;
    public float radius;        // 当前渲染半径
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
        this.radius = 50f;       // 增大初始半径，确保一开始就清晰可见
        this.maxRadius = 400f;   // 增大扩散范围，让效果更明显
        this.currentTime = 0f;
        this.maxTime = 1.2f;     // 延长持续时间，让玩家能看清楚
        this.active = true;

        randomizeColor();
    }

    private void randomizeColor() {
        float r = MathUtils.random();
        // 调整颜色为高饱和度、高亮度，适合加法混合
        if (r < 0.33f) {
            color.set(1f, 0.2f, 0.8f, 1f); // 霓虹粉
        } else if (r < 0.66f) {
            color.set(1f, 0.9f, 0.1f, 1f); // 电光黄
        } else {
            color.set(0.1f, 1f, 1f, 1f);   // 赛博青
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

        // 动画：指数衰减 (Out Expo) - 爆发感更强
        // 这是一个快速冲出去然后变慢的曲线
        float t = 1f - (float) Math.pow(2, -10 * progress);

        radius = 50f + (maxRadius - 50f) * t;

        // 透明度：优化可见性
        // 前 10% 时间快速渐显到全亮，中间 60% 保持全亮，后 30% 渐隐
        if (progress < 0.1f) {
            color.a = progress / 0.1f;  // 快速渐显
        } else if (progress < 0.7f) {
            color.a = 1f;  // 保持全亮，确保可见
        } else {
            color.a = 1f - (progress - 0.7f) / 0.3f;  // 最后30%渐隐
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