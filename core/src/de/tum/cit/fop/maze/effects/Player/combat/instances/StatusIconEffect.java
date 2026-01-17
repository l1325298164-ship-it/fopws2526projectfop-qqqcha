package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

/**
 * 状态图标特效：用于替代飘字
 * Type 0: 十字架 (+) - 回血/治疗
 * Type 1: 剑 (†) - 攻击力提升
 * Type 2: 星星/菱形 (◆) - 回蓝/法力
 */
public class StatusIconEffect extends CombatEffect {
    private final int type;
    private final float startY;

    public StatusIconEffect(float x, float y, int type) {
        super(x, y, 1.5f); // 持续 1.5秒
        this.type = type;
        this.startY = y;
        this.y = y;
    }

    @Override
    public void update(float delta, CombatParticleSystem particleSystem) {
        super.update(delta, particleSystem);
        // 缓慢向上飘
        this.y += 30f * delta;
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        float progress = timer / maxDuration; // 0 -> 1
        float alpha = 1.0f;

        // 最后 0.5秒 淡出
        if (progress > 0.7f) {
            alpha = 1f - (progress - 0.7f) / 0.3f;
        }

        float size = 12f; // 图标基础大小

        switch (type) {
            case 0: // 十字架 (Green)
                sr.setColor(0, 1, 0, alpha);
                // 竖线
                sr.rect(x - 2, y - size, 4, size * 2);
                // 横线
                sr.rect(x - size, y - 2, size * 2, 4);
                break;

            case 1: // 剑 (Red)
                sr.setColor(1, 0.2f, 0.2f, alpha);
                // 剑刃 (下长上短的视觉效果，其实画个十字类似剑就行)
                sr.rect(x - 2, y - size, 4, size * 2.5f); // 刃
                sr.rect(x - 8, y - 4, 16, 4);             // 护手
                sr.rect(x - 2, y - size - 6, 4, 6);       // 剑柄
                break;

            case 2: // 能量/菱形 (Cyan)
                sr.setColor(0, 1, 1, alpha);
                // 画一个菱形 (旋转的矩形需要多点逻辑，这里简单画个十字星或者矩形)
                sr.rect(x - 6, y - 6, 12, 12);
                // 简单的光晕装饰
                if (alpha > 0.5f) {
                    sr.setColor(1, 1, 1, alpha * 0.5f);
                    sr.rect(x - 3, y - 3, 6, 6);
                }
                break;
        }
    }

    @Override
    public void renderSprite(SpriteBatch batch) {
        // 纯形状渲染，不需要贴图
    }
}