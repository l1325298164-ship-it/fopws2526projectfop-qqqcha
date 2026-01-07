package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class GeyserTrapEffect extends EnvironmentEffect {
    private final Color steamColor = new Color(0.9f, 0.9f, 0.95f, 0.6f);

    public GeyserTrapEffect(float x, float y) {
        super(x, y, 1.0f); // 喷发持续1秒
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 持续产生向上喷射的蒸汽粒子
        if (timer < 0.8f) { // 前0.8秒喷发
            for (int i = 0; i < 3; i++) {
                float angle = MathUtils.random(80, 100); // 主要是向上，稍微有点左右扩散
                float speed = MathUtils.random(150, 300); // 高速喷发

                ps.spawn(x + MathUtils.random(-10, 10), y, steamColor,
                        MathUtils.cosDeg(angle) * speed, MathUtils.sinDeg(angle) * speed,
                        MathUtils.random(5, 10), // 粒子大小
                        0.6f, // 存活时间短
                        false, true); // 无重力(反向重力靠代码模拟或忽略)，有阻力(friction)
            }
        }
    }

    @Override
    public void render(ShapeRenderer sr) {
        float p = timer / maxDuration;

        // 1. 地面喷口裂缝
        sr.setColor(0.3f, 0.3f, 0.3f, 1f - p);
        sr.ellipse(x - 15, y - 5, 30, 10);

        // 2. 冲击水柱 (快速冲出，然后变淡)
        if (p < 0.3f) {
            float rise = p / 0.3f; // 0 -> 1
            sr.setColor(1f, 1f, 1f, 0.8f * (1-rise));
            sr.rect(x - 10, y, 20, rise * 100); // 瞬间冲高
        }
    }
}