package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.entities.GameObject;
import de.tum.cit.fop.maze.game.GameConstants;

/**
 * 通用的头顶状态图标特效
 * 功能：跟随目标、倒计时销毁、结束前闪烁
 */
public class StatusIconEffect extends CombatEffect {

    private final GameObject target;
    private final TextureRegion icon;
    private float duration;
    private final float offsetY; // 头顶高度偏移
    private float blinkTimer = 0f;

    public StatusIconEffect(GameObject target, TextureRegion icon, float duration) {
        super(target.getWorldX(), target.getWorldY());
        this.target = target;
        this.icon = icon;
        this.duration = duration;

        // 默认悬浮在头顶上方 (约 0.8 格的位置)
        this.offsetY = GameConstants.CELL_SIZE * 0.8f;
    }

    @Override
    public void update(float delta) {
        // 1. 如果目标没了，图标也没了
        if (target == null || !target.isActive()) {
            isFinished = true;
            return;
        }

        // 2. 倒计时
        duration -= delta;
        if (duration <= 0) {
            isFinished = true;
            return;
        }

        // 3. 核心：实时更新坐标跟随目标
        this.x = target.getWorldX() + GameConstants.CELL_SIZE / 2f;
        this.y = target.getWorldY() + offsetY;

        // 4. 快结束时闪烁计时
        if (duration < 2.0f) {
            blinkTimer += delta;
        }
    }

    @Override
    public void render(SpriteBatch batch) {
        if (isFinished || icon == null) return;

        // 闪烁逻辑：最后2秒，每0.2秒隐藏一次
        if (duration < 2.0f && (blinkTimer % 0.2f) > 0.1f) {
            return;
        }

        float w = 24f; // 图标大小
        float h = 24f;

        batch.draw(icon, x - w / 2f, y, w, h);
    }
}