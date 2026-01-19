package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

public class MagicEssenceEffect extends CombatEffect {
    private final float targetX, targetY;
    private final float startX, startY;
    private final Color color = new Color(0.7f, 0.3f, 1f, 1f);

    public MagicEssenceEffect(float startX, float startY, float targetX, float targetY) {
        super(startX, startY, 0.6f);
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {
        float progress = timer / maxDuration;
        float p = progress * progress;
        this.x = startX + (targetX - startX) * p;
        this.y = startY + (targetY - startY) * p;

        if (MathUtils.randomBoolean(0.6f)) {
            ps.spawn(
                    x + MathUtils.random(-3, 3),
                    y + MathUtils.random(-3, 3),
                    new Color(0.8f, 0.5f, 1f, 0.8f),
                    0, 0,
                    3, 0.3f, false, false
            );
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        sr.setColor(color);
        sr.circle(x, y, 5);
        sr.setColor(1f, 1f, 1f, 0.8f);
        sr.circle(x, y, 2);
    }

    @Override
    public void renderSprite(SpriteBatch batch) {}
}