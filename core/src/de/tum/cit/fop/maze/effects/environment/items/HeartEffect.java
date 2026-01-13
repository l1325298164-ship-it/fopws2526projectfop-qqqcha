package de.tum.cit.fop.maze.effects.environment.items;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class HeartEffect extends EnvironmentEffect {
    private final Color coreColor = new Color(1.0f, 0.4f, 0.7f, 0.9f);
    private final Color glowColor = new Color(1.0f, 0.7f, 0.85f, 0.4f);

    public HeartEffect(float x, float y) {
        super(x, y, 1.5f);
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 粒子逻辑保持不变...
        if (timer < maxDuration * 0.9f) {
            if (MathUtils.randomBoolean(0.4f)) {
                float offsetX = MathUtils.random(-15, 15);
                ps.spawn(x + offsetX, y, glowColor,
                        MathUtils.random(-5, 5), MathUtils.random(40, 100),
                        MathUtils.random(3, 8), 1.2f, false, false);
            }
        }
        if (timer == 0) {
            for(int i=0; i<6; i++) {
                float angle = MathUtils.random(0, 360) * MathUtils.degRad;
                float speed = MathUtils.random(50, 80);
                ps.spawn(x, y, coreColor, MathUtils.cos(angle)*speed, MathUtils.sin(angle)*speed, 8, 0.5f, false, false);
            }
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) { // 🔴 改名
        float p = timer / maxDuration;
        float fade = 1f - p;

        sr.setColor(coreColor.r, coreColor.g, coreColor.b, 0.5f * fade);
        float r1 = 15 + MathUtils.sin(timer * 3) * 3;
        sr.circle(x, y, r1);

        sr.setColor(glowColor.r, glowColor.g, glowColor.b, 0.3f * fade);
        float r2 = r1 + 15;
        sr.circle(x, y, r2);

        sr.setColor(glowColor.r, glowColor.g, glowColor.b, 0.1f * fade);
        sr.circle(x, y, r2 + 10 + MathUtils.sin(timer * 8) * 5);
    }

    @Override
    public void renderSprite(SpriteBatch batch) {
        // 空实现
    }
}