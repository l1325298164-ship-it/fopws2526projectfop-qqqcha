package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

public class HitSparkEffect extends CombatEffect {
    private final float size;

    public HitSparkEffect(float x, float y) {
        super(x, y, 0.15f); // 极短的生存时间
        this.size = 20f;
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        float alpha = 1.0f - (timer / maxDuration);
        sr.setColor(1f, 1f, 0.5f, alpha); // 淡黄色

        // 绘制一个“X”形或者爆炸刺
        float s = size * (0.5f + 0.5f * (timer / maxDuration)); // 稍微变大

        // 第一条线 \
        sr.line(x - s, y + s, x + s, y - s);
        // 第二条线 /
        sr.line(x - s, y - s, x + s, y + s);
        // 十字线 +
        sr.line(x - s*1.2f, y, x + s*1.2f, y);
        sr.line(x, y - s*1.2f, x, y + s*1.2f);
    }

    @Override
    public void renderSprite(SpriteBatch batch) {}
}