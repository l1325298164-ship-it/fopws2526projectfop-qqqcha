package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class GeyserTrapEffect extends EnvironmentEffect {
    // 蒸汽白
    private static final Color STEAM_COLOR = new Color(1f, 1f, 1f, 0.5f);

    public GeyserTrapEffect(float x, float y) {
        // 持续冒烟 1 秒
        super(x, y, 1.0f);
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 持续生成蒸汽 (Continuous)
        if (MathUtils.random() < 0.2f) { // 稍微控制一下频率，不用每帧都生
            ps.spawn(
                    x + MathUtils.random(-8, 8),
                    y + MathUtils.random(0, 10),
                    STEAM_COLOR,
                    MathUtils.random(-10, 10),   // 横向飘动很小
                    MathUtils.random(60, 100),   // 稳定向上
                    MathUtils.random(6, 12),     // 蒸汽团较大
                    0.8f,                        // 慢慢消散
                    false,                       // 无阻力
                    false                        // 无重力 (可以给一点负重力模拟热气上升，但在粒子系统里通常 gravity=false 就够了，或者靠 vy 实现)
            );
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        // 可以在这里画一个瞬间的淡红光晕表示“烫”，一闪而过
        if (timer < 0.2f) {
            sr.setColor(1f, 0.2f, 0.2f, (0.2f - timer) * 2f); // 淡红 -> 透明
            sr.circle(x, y, 20);
        }
    }

    @Override
    public void renderSprite(SpriteBatch batch) {}
}