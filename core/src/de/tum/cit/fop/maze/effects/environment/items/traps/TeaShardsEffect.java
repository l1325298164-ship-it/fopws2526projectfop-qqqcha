package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class TeaShardsEffect extends EnvironmentEffect {
    // 瓷白色
    private static final Color SHARD_COLOR = new Color(0.95f, 0.95f, 1f, 1f);

    public TeaShardsEffect(float x, float y) {
        // 极快，一瞬间
        super(x, y, 0.3f);
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 第一帧爆发
        if (timer < delta * 2) {
            for (int i = 0; i < 5; i++) {
                ps.spawn(
                        x, y,
                        SHARD_COLOR,
                        MathUtils.random(-80, 80),   // 向四周快速崩开
                        MathUtils.random(-50, 100),  // 主要是向上跳起
                        MathUtils.random(2, 4),      // 碎片很小锐利
                        0.25f,                       // 闪现即逝
                        true,
                        true
                );
            }
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        // 画一个瞬时的放射状刺，强化“扎脚”的感觉
        if (timer < 0.1f) {
            sr.setColor(1f, 1f, 1f, 1f - timer * 10f);
            float s = 8f;
            // 简单的十字星
            sr.line(x - s, y, x + s, y);
            sr.line(x, y - s, x, y + s);
        }
    }

    @Override
    public void renderSprite(SpriteBatch batch) {}
}