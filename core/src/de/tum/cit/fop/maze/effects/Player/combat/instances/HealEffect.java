package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

public class HealEffect extends CombatEffect {
    public HealEffect(float x, float y) { super(x, y, 1.2f); }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {
        // 产生缓慢上升的气泡
        if (timer < 0.5f) { // 只在前0.5秒生成
            ps.spawn(x, y, Color.LIME,
                    0, 50, // 初始向上速度
                    5, 1.0f, false, true); // gravity=true 模拟上升浮力
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) { // 修正：重命名为 renderShape
        float p = timer / maxDuration;
        sr.setColor(0.2f, 1f, 0.2f, 1f - p);

        // 地面光圈
        sr.circle(x, y, 15 + p * 30);

        // 上升的十字架
        float rise = p * 60;
        float size = 12;
        sr.rect(x - 3, y + rise - size, 6, size*2);
        sr.rect(x - size, y + rise - 3, size*2, 6);
    }
}