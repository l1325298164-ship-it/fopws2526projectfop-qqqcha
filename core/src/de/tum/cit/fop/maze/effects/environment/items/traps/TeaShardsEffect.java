package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class TeaShardsEffect extends EnvironmentEffect {
    private final Color shardColor = new Color(0.9f, 0.95f, 1f, 1f); // 瓷白色
    private final Color dustColor = new Color(0.8f, 0.8f, 0.75f, 0.4f); // 灰白尘土色

    public TeaShardsEffect(float x, float y) {
        super(x, y, 1.5f); // 足够尘土飘散
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 一次性爆发
        if (timer < delta * 2) {
            // 1. 锐利的碎瓷片 (快速飞溅)
            for (int i = 0; i < 8; i++) {
                ps.spawn(x, y, shardColor,
                        MathUtils.random(-50, 50), MathUtils.random(80, 150),
                        MathUtils.random(3, 6), // 小碎片
                        0.5f,
                        true, false); // 受重力，掉得快
            }

            // 2. 升腾的尘土云 (Dust Cloud)
            for (int i = 0; i < 6; i++) {
                float angle = MathUtils.random(60, 120); // 向上
                float speed = MathUtils.random(30, 80);  // 较慢

                ps.spawn(x + MathUtils.random(-10, 10), y,
                        dustColor,
                        MathUtils.cosDeg(angle) * speed,
                        MathUtils.sinDeg(angle) * speed,
                        MathUtils.random(8, 16), // 大颗粒云雾
                        1.2f,                    // 存活久，飘散
                        false, true);            // 无重力，有阻力(漂浮感)
            }
        }
    }

    // 🔴 修正点 1: 改名
    @Override
    public void renderShape(ShapeRenderer sr) {
        // 移除所有几何绘制，保持为空
    }

    // 🔴 修正点 2: 新增空实现
    @Override
    public void renderSprite(SpriteBatch batch) {
        // 不需要贴图
    }
}