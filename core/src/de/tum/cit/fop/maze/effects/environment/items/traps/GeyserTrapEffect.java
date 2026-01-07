package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class GeyserTrapEffect extends EnvironmentEffect {
    // 蒸汽色：纯白带透
    private final Color steamColor = new Color(1f, 1f, 1f, 0.4f);
    // 水滴色：清澈蓝
    private final Color waterColor = new Color(0.6f, 0.8f, 1.0f, 0.7f);
    // 碎石色：深灰
    private final Color rubbleColor = new Color(0.4f, 0.35f, 0.3f, 1f);

    private static final float WARNING_TIME = 0.8f;
    private static final float ERUPT_TIME = 1.2f;

    public GeyserTrapEffect(float x, float y) {
        // 总时长 = 警告 + 喷发
        super(x, y, WARNING_TIME + ERUPT_TIME);
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        if (timer < WARNING_TIME) {
            // === 阶段1: 地表震颤 (Warning) ===
            float progress = timer / WARNING_TIME;
            if (MathUtils.random() < 0.05f + progress * 0.1f) {
                ps.spawn(
                        x + MathUtils.random(-15, 15),
                        y - 10 + MathUtils.random(-5, 5),
                        rubbleColor,
                        0, MathUtils.random(20, 50),
                        MathUtils.random(2, 4),
                        0.3f,
                        true, true
                );
            }
        } else {
            // === 阶段2: 喷发 (Eruption) ===
            // 蒸汽
            for (int i = 0; i < 2; i++) {
                float angle = MathUtils.random(85, 95);
                float speed = MathUtils.random(180, 350);

                ps.spawn(
                        x + MathUtils.random(-8, 8),
                        y + 5,
                        steamColor,
                        MathUtils.cosDeg(angle) * speed,
                        MathUtils.sinDeg(angle) * speed,
                        MathUtils.random(8, 15),
                        0.5f,
                        false, true
                );
            }

            // 水滴
            if (MathUtils.randomBoolean(0.3f)) {
                float angle = MathUtils.random(60, 120);
                float speed = MathUtils.random(100, 200);

                ps.spawn(
                        x, y + 15,
                        waterColor,
                        MathUtils.cosDeg(angle) * speed,
                        MathUtils.sinDeg(angle) * speed,
                        MathUtils.random(3, 5),
                        0.8f,
                        true, false
                );
            }
        }
    }

    // 🔴 修正点 1: 改名
    @Override
    public void renderShape(ShapeRenderer sr) {
        // 移除几何绘制，全靠粒子
    }

    // 🔴 修正点 2: 新增空实现
    @Override
    public void renderSprite(SpriteBatch batch) {
        // 不需要贴图
    }
}