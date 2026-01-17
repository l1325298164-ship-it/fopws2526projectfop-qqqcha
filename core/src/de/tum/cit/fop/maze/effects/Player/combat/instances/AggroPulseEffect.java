package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

public class AggroPulseEffect extends CombatEffect {
    private float maxRadius = 60f;

    public AggroPulseEffect(float x, float y) {
        super(x, y, 0.5f); // 0.5秒扩散
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        float progress = timer / maxDuration; // 0 -> 1
        float radius = maxRadius * progress;
        float alpha = 1f - progress;

        // 暗紫色
        sr.setColor(0.4f, 0f, 0.6f, alpha);
        sr.circle(x, y, radius);

        // 内圈空心感（通过画一个略小的圆遮盖？不，ShapeRenderer支持 circle 只有填充或线框）
        // 我们画线框圆环即可
        sr.setColor(0.3f, 0f, 0.5f, alpha);
        sr.circle(x, y, radius * 0.8f);
    }

    @Override
    public void renderSprite(SpriteBatch batch) {}
}