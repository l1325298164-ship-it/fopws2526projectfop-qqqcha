package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

public class AggroPulseEffect extends CombatEffect {
    private float maxRadius = 60f;

    public AggroPulseEffect(float x, float y) {
        super(x, y, 0.5f);
    }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {
        // 无需更新逻辑，只通过 timer 控制渲染
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        float progress = timer / maxDuration;
        float radius = maxRadius * progress;
        float alpha = 1f - progress;
        sr.setColor(0.4f, 0f, 0.6f, alpha);
        sr.circle(x, y, radius);
        sr.setColor(0.3f, 0f, 0.5f, alpha);
        sr.circle(x, y, radius * 0.8f);
    }

    @Override
    public void renderSprite(SpriteBatch batch) {}
}