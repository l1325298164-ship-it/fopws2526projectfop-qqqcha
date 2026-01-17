package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

public class StatusIconEffect extends CombatEffect {
    private final int type;

    // Type 0: 十字架 (+) - 回血/治疗
    // Type 1: 剑 (†) - 攻击力提升
    // Type 2: 星星/菱形 (◆) - 回蓝/法力

    public StatusIconEffect(float x, float y, int type) {
        super(x, y, 1.5f); // 持续 1.5秒
        this.type = type;
        this.y = y; // 确保初始Y正确
    }

    @Override
    public void update(float delta, CombatParticleSystem particleSystem) {
        super.update(delta, particleSystem);
        // 缓慢向上飘
        this.y += 25f * delta;
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        float progress = timer / maxDuration;
        float alpha = 1.0f;

        // 淡出
        if (progress > 0.7f) {
            alpha = 1f - (progress - 0.7f) / 0.3f;
        }

        float size = 10f; // 图标大小

        // 简单的绘制逻辑
        switch (type) {
            case 0: // Green Cross
                sr.setColor(0, 1, 0, alpha);
                sr.rect(x - 2, y - size, 4, size * 2);
                sr.rect(x - size, y - 2, size * 2, 4);
                break;
            case 1: // Red Sword-like shape
                sr.setColor(1, 0.3f, 0.3f, alpha);
                sr.rect(x - 2, y - size, 4, size * 2.5f);
                sr.rect(x - 8, y - 4, 16, 4);
                break;
            case 2: // Cyan Diamond
                sr.setColor(0, 1, 1, alpha);
                sr.rect(x - 5, y - 5, 10, 10); // 简化为方块，旋转比较麻烦先不搞
                break;
        }
    }

    @Override
    public void renderSprite(SpriteBatch batch) {}
}