package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class PearlMineEffect extends EnvironmentEffect {
    // 芋圆三色：芋头紫、地瓜橙、糯米白
    private static final Color TARO_PURPLE = new Color(0.65f, 0.4f, 0.9f, 0.8f);
    private static final Color POTATO_ORANGE = new Color(1.0f, 0.6f, 0.2f, 0.8f);
    private static final Color RICE_WHITE = new Color(0.95f, 0.9f, 0.85f, 0.8f);

    private final Color[] juiceColors = {TARO_PURPLE, POTATO_ORANGE, RICE_WHITE};

    // 冲击波参数
    private float shockwaveRadius = 0f;
    private final float SHOCKWAVE_MAX_RADIUS = 60f;
    private final float SHOCKWAVE_DURATION = 0.25f;

    public PearlMineEffect(float x, float y) {
        super(x, y, 3.0f); // 持续较长以显示残留印记
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 只有第一帧触发爆炸 (生成果汁飞溅)
        if (timer < delta * 2) {
            for (int i = 0; i < 20; i++) {
                // 向四周炸开
                float angle = MathUtils.random(0, 360);
                float speed = MathUtils.random(80, 200);

                // 随机选一个颜色
                Color randomColor = juiceColors[MathUtils.random(0, juiceColors.length - 1)];

                ps.spawn(x, y + 10, randomColor,
                        MathUtils.cosDeg(angle) * speed, MathUtils.sinDeg(angle) * speed,
                        MathUtils.random(4, 8), // 颗粒较小，像汁水
                        0.6f,
                        true, true); // gravity=true(掉落), friction=true(减速)
            }
        }
    }

    // 🔴 修正点 1: 改名
    @Override
    public void renderShape(ShapeRenderer sr) {
        // 1. 绘制冲击波 (仅在爆炸初期)
        if (timer < SHOCKWAVE_DURATION) {
            float p = timer / SHOCKWAVE_DURATION;
            // 缓动：快出慢停
            float radius = SHOCKWAVE_MAX_RADIUS * (float)Math.pow(p, 0.5);
            float alpha = 1.0f - p;

            sr.setColor(1f, 1f, 1f, alpha * 0.5f); // 半透明白环
            // 绘制圆环
            sr.circle(x, y, radius);
        }

        // 2. 爆炸后的地面残留印记 (糖水渍)
        float stainAlpha = 0f;
        if (timer > 0.1f) { // 爆炸后才显示
            float fadeP = (timer - 0.1f) / (maxDuration - 0.1f);
            stainAlpha = 0.5f * (1f - fadeP);
        }

        if (stainAlpha > 0) {
            sr.setColor(0.4f, 0.3f, 0.2f, stainAlpha);
            sr.ellipse(x - 20, y - 6, 40, 12);
        }
    }

    // 🔴 修正点 2: 新增空实现
    @Override
    public void renderSprite(SpriteBatch batch) {
        // 不需要贴图
    }
}