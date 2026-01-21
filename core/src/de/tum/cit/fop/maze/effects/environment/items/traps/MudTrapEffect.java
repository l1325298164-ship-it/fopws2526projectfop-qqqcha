package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class MudTrapEffect extends EnvironmentEffect {
    // 泥浆色：深褐/巧克力色
    private static final Color MUD_COLOR = new Color(0.35f, 0.2f, 0.05f, 1f);

    public MudTrapEffect(float x, float y) {
        // 极短的生命周期，只是溅一下
        super(x, y, 0.4f);
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 只在第一帧生成粒子 (One-shot)
        if (timer < delta * 2) {
            for (int i = 0; i < 4; i++) {
                ps.spawn(
                        x + MathUtils.random(-5, 5), // 就在脚底
                        y,
                        MUD_COLOR,
                        MathUtils.random(-60, 60),  // 向两侧飞溅
                        MathUtils.random(20, 80),   // 稍微向上溅起
                        MathUtils.random(3, 5),     // 泥点很小
                        0.3f,                       // 消失得很快
                        true,                       // 有阻力
                        true                        // 受重力 (会掉下来)
                );
            }
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        // 移除几何绘制，完全交给粒子系统
    }

    @Override
    public void renderSprite(SpriteBatch batch) {
        // 无贴图
    }
}