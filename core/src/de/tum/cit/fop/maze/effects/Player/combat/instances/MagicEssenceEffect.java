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

    // 颜色：魔力是紫色的，接近玩家时可能会变亮
    private final Color color = new Color(0.7f, 0.3f, 1f, 1f);

    public MagicEssenceEffect(float startX, float startY, float targetX, float targetY) {
        super(startX, startY, 0.6f); // 飞行时间 0.6秒
        this.startX = startX;
        this.startY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
    }

    @Override
    public void update(float delta, CombatParticleSystem ps) {
        super.update(delta, ps);

        float progress = timer / maxDuration;

        // 使用 EaseOutQuad 插值，让它飞向玩家时有种“被吸过去”的加速感
        // 或者 EaseInQuad (先慢后快) 更像吸取？试一下 EaseIn
        float p = progress * progress;

        this.x = startX + (targetX - startX) * p;
        this.y = startY + (targetY - startY) * p;

        // 拖尾效果
        if (MathUtils.randomBoolean(0.6f)) {
            ps.spawn(
                    x + MathUtils.random(-3, 3),
                    y + MathUtils.random(-3, 3),
                    new Color(0.8f, 0.5f, 1f, 0.8f), // 稍浅一点的紫色拖尾
                    0, 0,
                    3, 0.3f, false, false
            );
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        sr.setColor(color);
        // 绘制一个菱形或小圆形代表精华
        sr.circle(x, y, 5);
        sr.setColor(1f, 1f, 1f, 0.8f);
        sr.circle(x, y, 2); // 亮白核心
    }

    @Override
    public void renderSprite(SpriteBatch batch) {}
}