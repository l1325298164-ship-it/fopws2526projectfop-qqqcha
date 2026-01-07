package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.graphics.Color;
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
            // 随着时间推移，震动频率变高
            float progress = timer / WARNING_TIME;
            if (MathUtils.random() < 0.05f + progress * 0.1f) {
                // 生成细小的碎石，微微跳起
                ps.spawn(
                        x + MathUtils.random(-15, 15),
                        y - 10 + MathUtils.random(-5, 5),
                        rubbleColor,
                        0, MathUtils.random(20, 50), // 只有向上的微小初速度
                        MathUtils.random(2, 4),      // 很小
                        0.3f,                        // 存活极短
                        true, true                   // 受重力落下，有阻力
                );
            }
        } else {
            // === 阶段2: 喷发 (Eruption) ===
            // 持续生成大量蒸汽（向上冲）
            for (int i = 0; i < 2; i++) {
                float angle = MathUtils.random(85, 95); // 几乎垂直向上
                float speed = MathUtils.random(180, 350);

                ps.spawn(
                        x + MathUtils.random(-8, 8),
                        y + 5, // 从喷口上方生成
                        steamColor,
                        MathUtils.cosDeg(angle) * speed,
                        MathUtils.sinDeg(angle) * speed,
                        MathUtils.random(8, 15), // 蒸汽团较大
                        0.5f,
                        false, true // 无重力(持续上升)，有空气阻力
                );
            }

            // 伴随水滴飞溅（向四周抛洒）
            if (MathUtils.randomBoolean(0.3f)) {
                float angle = MathUtils.random(60, 120); // 扇形喷洒
                float speed = MathUtils.random(100, 200);

                ps.spawn(
                        x, y + 15,
                        waterColor,
                        MathUtils.cosDeg(angle) * speed,
                        MathUtils.sinDeg(angle) * speed,
                        MathUtils.random(3, 5), // 水滴较小
                        0.8f,
                        true, false // 受重力，无阻力(抛物线)
                );
            }
        }
    }

    @Override
    public void render(ShapeRenderer sr) {
        // 完全移除几何绘制，只靠粒子表现
    }
}