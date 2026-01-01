package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

public class DebuffEffect extends CombatEffect {
    public DebuffEffect(float x, float y) { super(x, y, 2.0f); }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {
        // 持续冒出紫色气泡
        if (MathUtils.randomBoolean(0.1f)) {
            float offsetX = MathUtils.random(-15, 15);
            ps.spawn(x + offsetX, y, Color.PURPLE,
                    0, 20,
                    6, 1.5f, false, true); // 慢慢飘
        }
    }

    @Override
    public void render(ShapeRenderer sr) {
        sr.setColor(0.4f, 0f, 0.6f, 0.4f);
        sr.circle(x, y + 40, 5 + MathUtils.sin(timer*5)*2); // 头顶小标记
    }
}