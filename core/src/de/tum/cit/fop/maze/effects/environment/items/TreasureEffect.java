package de.tum.cit.fop.maze.effects.environment.items;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class TreasureEffect extends EnvironmentEffect {
    private final Color goldColor = new Color(1f, 0.9f, 0.2f, 1f);

    public TreasureEffect(float x, float y) {
        super(x, y, 1.5f);
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 金币喷泉：每次产生2个，有重力
        for(int i=0; i<2; i++) {
            ps.spawn(x, y, goldColor,
                    MathUtils.random(-30, 30), MathUtils.random(150, 250),
                    MathUtils.random(5, 8), 0.8f,
                    true, false); // gravity=true
        }
    }

    @Override
    public void render(ShapeRenderer sr) {
        float p = timer / maxDuration;
        sr.setColor(goldColor.r, goldColor.g, goldColor.b, 1f - p);

        // 光圈
        sr.circle(x, y, 20 + MathUtils.sin(timer * 5) * 5);
        // 上升光柱
        float width = (1-p) * 40;
        sr.rect(x - width/2, y, width, p * 150);
        // 宝箱图标
        float size = 10; float rise = p * 40;
        sr.rect(x - size, y + rise - size, size*2, size*2);
    }
}