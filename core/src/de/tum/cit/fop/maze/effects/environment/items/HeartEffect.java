package de.tum.cit.fop.maze.effects.environment.items;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class HeartEffect extends EnvironmentEffect {
    private final Color pinkColor = new Color(1f, 0.4f, 0.7f, 1f);

    public HeartEffect(float x, float y) {
        super(x, y, 1.5f);
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 治愈气泡：无重力，飘起
        ps.spawn(x, y, pinkColor,
                MathUtils.random(-20, 20), MathUtils.random(80, 150),
                MathUtils.random(4, 7), 1.0f,
                false, false); // gravity=false
    }

    @Override
    public void render(ShapeRenderer sr) {
        float p = timer / maxDuration;
        sr.setColor(pinkColor.r, pinkColor.g, pinkColor.b, 1f - p);

        sr.circle(x, y, 15 + MathUtils.sin(timer * 4) * 5);
        float width = (1-p) * 30;
        sr.rect(x - width/2, y, width, p * 120);

        // 十字爱心图标
        float size = 8; float rise = p * 80;
        sr.rect(x - 2, y + rise - size, 4, size*2);
        sr.rect(x - size, y + rise - 2, size*2, 4);
    }
}