package de.tum.cit.fop.maze.effects.environment.items;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class TreasureEffect extends EnvironmentEffect {
    // 调整为偏橙的金黄色，避免发白
    private final Color coreColor = new Color(1.0f, 0.85f, 0.2f, 0.8f); // 核心亮黄
    private final Color auraColor = new Color(1.0f, 0.6f, 0.0f, 0.3f);  // 外圈橙金

    public TreasureEffect(float x, float y) {
        super(x, y, 1.2f); // 持续 1.2 秒
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 1. 持续产生上升的星尘粒子
        if (timer < maxDuration * 0.8f) { // 也就是前80%的时间产生粒子
            if (MathUtils.randomBoolean(0.6f)) { // 没帧60%概率产生，避免太密
                float offsetX = MathUtils.random(-15, 15);
                float offsetY = MathUtils.random(-10, 10);

                ps.spawn(x + offsetX, y + offsetY,
                        coreColor,
                        MathUtils.random(-10, 10), MathUtils.random(30, 80), // 缓慢上升
                        MathUtils.random(2, 4), // 小粒子
                        0.8f, // 寿命
                        false, false);
            }
        }

        // 2. 爆发瞬间 (第一帧) 产生一圈扩散粒子
        if (timer == 0) {
            for (int i = 0; i < 12; i++) {
                float angle = i * 30f * MathUtils.degRad;
                float speed = MathUtils.random(80, 120);
                float vx = MathUtils.cos(angle) * speed;
                float vy = MathUtils.sin(angle) * speed;

                ps.spawn(x, y, coreColor, vx, vy, 5, 0.6f, false, false);
            }
        }
    }

    @Override
    public void render(ShapeRenderer sr) {
        float p = timer / maxDuration; // 进度 0.0 -> 1.0
        float fade = 1f - p; // 透明度 1.0 -> 0.0

        // === 绘制底部光环 (多层圆模拟光晕) ===
        // 核心圈
        sr.setColor(coreColor.r, coreColor.g, coreColor.b, 0.6f * fade);
        float baseRadius = 20 + MathUtils.sin(timer * 5) * 5; // 呼吸效果
        sr.circle(x, y, baseRadius);

        // 外围柔光圈 (画两层，模拟模糊边缘)
        sr.setColor(auraColor.r, auraColor.g, auraColor.b, 0.3f * fade);
        sr.circle(x, y, baseRadius + 10);

        sr.setColor(auraColor.r, auraColor.g, auraColor.b, 0.15f * fade);
        sr.circle(x, y, baseRadius + 20 + p * 20); // 随时间略微扩散

        // === 绘制上升的光柱感 (可选，淡淡的) ===
        // 用一个变细的椭圆或者矩形模拟光柱
        sr.setColor(1f, 0.9f, 0.5f, 0.2f * fade);
        float beamWidth = (1-p) * 25;
        sr.rect(x - beamWidth/2, y, beamWidth, 40 + p * 60);
    }
}