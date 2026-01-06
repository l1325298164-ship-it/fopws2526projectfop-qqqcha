package de.tum.cit.fop.maze.entities.trap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

public class TrapT04_Mud extends Trap {

    /* ===== 参数 ===== */
    private static final float SLOW_DURATION = 1.5f;

    // 用于动画的时间计数器
    private float stateTime = 0f;

    public TrapT04_Mud(int x, int y) {
        super(x, y);
    }

    @Override
    public void update(float delta) {
        // ❌ 之前的逻辑写在这里，但 GameManager 不会调用它，导致动画静止
    }

    @Override
    public void update(float delta, GameManager gameManager) {
        // ✅ 移到这里：只有被激活(未被移除)时才更新动画
        if (active) {
            stateTime += delta;
        }
    }

    @Override
    public boolean isPassable() {
        return true;
    }

    @Override
    public void onPlayerStep(Player player) {
        player.applySlow(SLOW_DURATION);
    }

    /* ================= 渲染 ================= */

    @Override
    public void drawShape(ShapeRenderer sr) {
        if (!active) return;

        float size = GameConstants.CELL_SIZE;
        float px = x * size;
        float py = y * size;

        // 基础布局计算
        float marginX = size * 0.1f;
        float baseWidth = size - 2 * marginX;
        float baseHeight = size * 0.7f;
        float centerX = px + size / 2f;
        float centerY = py + size / 2f;

        // 1. 【溢出感】边缘呼吸动画
        float overflow = MathUtils.sin(stateTime * 5f) * 3f; // 速度5，幅度3像素

        // 绘制底层
        sr.setColor(0.4f, 0.3f, 0.2f, 0.8f);
        sr.ellipse(centerX - (baseWidth + overflow)/2,
                centerY - (baseHeight + overflow * 0.7f)/2,
                baseWidth + overflow,
                baseHeight + overflow * 0.7f);

        // 绘制核心层
        sr.setColor(0.3f, 0.2f, 0.1f, 1f);
        sr.ellipse(centerX - baseWidth/2, centerY - baseHeight/2, baseWidth, baseHeight);

        // 2. 【沸腾感】绘制冒泡泡
        sr.setColor(0.45f, 0.35f, 0.25f, 1f);

        for (int i = 0; i < 3; i++) {
            float offset = i * 2.5f;
            float bubbleTime = (stateTime + offset) % 2.0f;

            if (bubbleTime < 1.0f) {
                float bx = centerX + MathUtils.sin(i * 132.1f) * (baseWidth * 0.3f);
                float by = centerY + MathUtils.cos(i * 57.3f) * (baseHeight * 0.3f);
                float radius = 2f + bubbleTime * 4f;
                sr.circle(bx, by, radius);
            }
        }
    }

    @Override
    public void drawSprite(SpriteBatch batch) {}

    @Override
    public RenderType getRenderType() {
        return RenderType.SHAPE;
    }
}