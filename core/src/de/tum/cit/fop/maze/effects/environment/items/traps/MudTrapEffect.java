package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class MudTrapEffect extends EnvironmentEffect {
    // 深褐/紫黑泥浆色
    private final Color mudColor = new Color(0.25f, 0.15f, 0.1f, 0.9f);
    private final Color bubbleColor = new Color(0.35f, 0.25f, 0.2f, 0.7f);

    public MudTrapEffect(float x, float y) {
        super(x, y, 1.2f);
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 咕嘟冒泡
        if (MathUtils.randomBoolean(0.2f)) {
            ps.spawn(x + MathUtils.random(-15, 15), y + MathUtils.random(-15, 15),
                    bubbleColor, 0, 15, MathUtils.random(3, 8), 1.0f, false, false);
        }
        // 泥点飞溅
        if (MathUtils.randomBoolean(0.05f)) {
            ps.spawn(x, y, mudColor,
                    MathUtils.random(-40, 40), MathUtils.random(40, 80),
                    4, 0.6f, true, false);
        }
    }

    @Override
    public void render(ShapeRenderer sr) {
        float p = timer / maxDuration;
        sr.setColor(mudColor.r, mudColor.g, mudColor.b, 1f - p * 0.3f);

        // 泥潭
        sr.circle(x, y, 25);
        sr.circle(x, y, 22 + MathUtils.sin(timer * 15) * 1.5f);

        // 吸入波纹 (Inward Ripple)
        float rippleCycle = (timer * 2) % 1.0f;
        float rippleRadius = 30 * (1 - rippleCycle);

        sr.setColor(0.15f, 0.05f, 0f, 0.6f * (1-rippleCycle));
        sr.circle(x, y, rippleRadius);
        // 挖空中间
        sr.setColor(mudColor);
        sr.circle(x, y, rippleRadius - 2);
    }
}