package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class TeaShardsEffect extends EnvironmentEffect {
    private final Color shardColor = new Color(0.9f, 0.95f, 1f, 1f); // 瓷白色

    public TeaShardsEffect(float x, float y) {
        super(x, y, 0.8f);
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 持续生成少量尖刺碎片飞溅
        if (MathUtils.randomBoolean(0.1f)) {
            ps.spawn(x, y, shardColor,
                    MathUtils.random(-40, 40), MathUtils.random(50, 100),
                    5, 0.5f,
                    true, false); // 碎片受重力掉落
        }
    }

    @Override
    public void render(ShapeRenderer sr) {
        float p = timer / maxDuration;
        sr.setColor(shardColor.r, shardColor.g, shardColor.b, 1f - p);

        // 画地上的尖刺 (3个三角形组成)
        // 简单的抖动动画
        float shake = MathUtils.sin(timer * 50) * 2;

        drawTriangle(sr, x - 10 + shake, y, 10, 20);
        drawTriangle(sr, x + shake, y, 12, 25); // 中间的大刺
        drawTriangle(sr, x + 10 + shake, y, 8, 15);
    }

    private void drawTriangle(ShapeRenderer sr, float tx, float ty, float w, float h) {
        sr.triangle(tx - w/2, ty, tx + w/2, ty, tx, ty + h);
    }
}