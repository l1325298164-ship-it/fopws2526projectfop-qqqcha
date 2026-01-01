package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class PearlMineEffect extends EnvironmentEffect {
    private final Color pearlColor = new Color(0.1f, 0.05f, 0.1f, 1f); // 黑珍珠色

    public PearlMineEffect(float x, float y) {
        super(x, y, 0.5f); // 爆炸瞬间很快
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 只有第一帧触发爆炸 (生成一堆粒子)
        if (timer < delta * 2) {
            for (int i = 0; i < 15; i++) {
                // 向四周炸开
                float angle = MathUtils.random(0, 360);
                float speed = MathUtils.random(50, 150);

                ps.spawn(x, y + 10, pearlColor,
                        MathUtils.cosDeg(angle) * speed, MathUtils.sinDeg(angle) * speed,
                        MathUtils.random(6, 12), // 大块粘液
                        0.8f,
                        true, true); // gravity=true(掉落), friction=true(粘稠减速)
            }
        }
    }

    @Override
    public void render(ShapeRenderer sr) {
        // 爆炸前的一瞬间画个圆球，爆炸后消失
        if (timer < 0.05f) {
            sr.setColor(pearlColor);
            sr.circle(x, y + 10, 15);
            // 高光
            sr.setColor(Color.WHITE);
            sr.circle(x + 5, y + 15, 4);
        } else {
            // 爆炸后的地面残留印记
            float p = timer / maxDuration;
            sr.setColor(0f, 0f, 0f, 0.5f * (1-p));
            sr.ellipse(x - 20, y - 5, 40, 10);
        }
    }
}