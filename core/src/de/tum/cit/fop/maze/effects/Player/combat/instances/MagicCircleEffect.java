package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

public class MagicCircleEffect extends CombatEffect {
    private final float radius;
    private float rotation = 0f;

    public MagicCircleEffect(float x, float y, float radius, float duration) {
        super(x, y, duration);
        this.radius = radius;
    }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem particleSystem) {
        rotation += 90f * delta;
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        float alpha = 1.0f;
        if (timer < 0.2f) alpha = timer / 0.2f;
        if (timer > maxDuration - 0.2f) alpha = (maxDuration - timer) / 0.2f;

        sr.setColor(0.6f, 0f, 0.8f, alpha);
        sr.circle(x, y, radius);
        sr.circle(x, y, radius * 0.95f);

        float innerR = radius * 0.7f;
        drawRotatingTriangle(sr, x, y, innerR, rotation, alpha);
        drawRotatingTriangle(sr, x, y, innerR, rotation + 180f, alpha);

        float pulse = 0.5f + 0.5f * MathUtils.sin(timer * 10f);
        sr.setColor(0.8f, 0.4f, 1f, alpha * pulse);
        sr.circle(x, y, radius * 0.1f);
    }

    private void drawRotatingTriangle(ShapeRenderer sr, float cx, float cy, float r, float angleOffset, float alpha) {
        float x1 = cx + r * MathUtils.cosDeg(angleOffset);
        float y1 = cy + r * MathUtils.sinDeg(angleOffset);
        float x2 = cx + r * MathUtils.cosDeg(angleOffset + 120);
        float y2 = cy + r * MathUtils.sinDeg(angleOffset + 120);
        float x3 = cx + r * MathUtils.cosDeg(angleOffset + 240);
        float y3 = cy + r * MathUtils.sinDeg(angleOffset + 240);

        sr.setColor(0.5f, 0f, 0.7f, alpha);
        sr.triangle(x1, y1, x2, y2, x3, y3);
    }

    @Override
    public void renderSprite(SpriteBatch batch) {}
}