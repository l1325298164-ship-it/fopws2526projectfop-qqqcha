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
        this.radius = 20f;       // 初始稍大一点
        this.maxRadius = 350f;   // 扩散范围
        this.currentTime = 0f;
        this.maxTime = 0.8f;     // 加快节奏，0.8秒散开
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

        radius = 20f + (maxRadius - 20f) * t;

        // 透明度：快速淡出
        // 前 20% 时间渐显，后 80% 时间渐隐
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