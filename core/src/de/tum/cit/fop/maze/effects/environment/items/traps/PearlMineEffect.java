package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class PearlMineEffect extends EnvironmentEffect {
    // 糖浆色：紫/橙
    private static final Color SYRUP_PURPLE = new Color(0.6f, 0.4f, 0.8f, 0.9f);
    private static final Color SYRUP_ORANGE = new Color(1.0f, 0.6f, 0.2f, 0.9f);

    public PearlMineEffect(float x, float y) {
        // 持续 2 秒 (模拟粘在身上的状态)
        super(x, y, 2.0f);
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 每隔一会儿滴落一滴
        if (MathUtils.random() < 0.15f) {
            Color c = MathUtils.randomBoolean() ? SYRUP_PURPLE : SYRUP_ORANGE;

            ps.spawn(
                    x + MathUtils.random(-6, 6), // 附着在身上
                    y + MathUtils.random(5, 15), // 从身上较高处产生
                    c,
                    0,                          // X轴基本不动
                    MathUtils.random(-30, -60), // 缓慢下坠 (粘稠感)
                    MathUtils.random(3, 6),     // 液滴大小
                    0.6f,                       // 存活时间
                    false,                      // 粘稠液体受阻力影响小，主要受重力，这里简单模拟匀速下落
                    false
            );
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        // 不需要额外几何图形
    }

    @Override
    public void renderSprite(SpriteBatch batch) {}
}