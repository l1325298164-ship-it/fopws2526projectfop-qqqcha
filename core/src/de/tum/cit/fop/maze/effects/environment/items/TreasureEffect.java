package de.tum.cit.fop.maze.effects.environment.items;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class TreasureEffect extends EnvironmentEffect {
    private final Color coreColor = new Color(1.0f, 0.85f, 0.2f, 0.8f);
    private final Color auraColor = new Color(1.0f, 0.6f, 0.0f, 0.3f);

    public TreasureEffect(float x, float y) {
        super(x, y, 1.2f);
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 粒子逻辑保持不变
        if (timer < maxDuration * 0.8f) {
            if (MathUtils.randomBoolean(0.6f)) {
                float offsetX = MathUtils.random(-15, 15);
                float offsetY = MathUtils.random(-10, 10);
                ps.spawn(x + offsetX, y + offsetY, coreColor,
                        MathUtils.random(-10, 10), MathUtils.random(30, 80),
                        MathUtils.random(2, 4), 0.8f, false, false);
            }
        }
        if (timer == 0) {
            for (int i = 0; i < 12; i++) {
                float angle = i * 30f * MathUtils.degRad;
                float speed = MathUtils.random(80, 120);
                ps.spawn(x, y, coreColor, MathUtils.cos(angle)*speed, MathUtils.sin(angle)*speed, 5, 0.6f, false, false);
            }
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) { // 🔴 改名
        float p = timer / maxDuration;
        float fade = 1f - p;

        sr.setColor(coreColor.r, coreColor.g, coreColor.b, 0.6f * fade);
        float baseRadius = 20 + MathUtils.sin(timer * 5) * 5;
        sr.circle(x, y, baseRadius);

        sr.setColor(auraColor.r, auraColor.g, auraColor.b, 0.3f * fade);
        sr.circle(x, y, baseRadius + 10);

        sr.setColor(auraColor.r, auraColor.g, auraColor.b, 0.15f * fade);
        sr.circle(x, y, baseRadius + 20 + p * 20);

        sr.setColor(1f, 0.9f, 0.5f, 0.2f * fade);
        float beamWidth = (1-p) * 25;
        sr.rect(x - beamWidth/2, y, beamWidth, 40 + p * 60);
    }

    @Override
    public void renderSprite(SpriteBatch batch) {
        // 空实现
    }
}