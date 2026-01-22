package de.tum.cit.fop.maze.effects.Player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.game.GameConstants;

/**
 * 冲刺残影特效管理器
 * <p>
 * 修改记录：
 * - 增加支持动态颜色 (Tint)，用于区分不同等级的冲刺 (白/青/金)
 * - 调整生成逻辑，支持不同密度的残影 (冲刺密集，跑步稀疏)
 * - 修正渲染尺寸计算，使其与 Player.java 的 drawSprite 逻辑保持一致
 */
public class PlayerTrailManager {

    private static class Ghost {
        float x, y; // 玩家的格子坐标
        float alpha;
        TextureRegion region; // 记录生成时的那个瞬间的动画帧
        Color tintColor;      // 残影颜色

        public Ghost(float x, float y, TextureRegion region, Color color) {
            this.x = x;
            this.y = y;
            this.region = region;
            this.alpha = 1.0f;
            this.tintColor = color;
        }
    }

    private Array<Ghost> ghosts = new Array<>();
    private float spawnTimer = 0;

    // 不同状态下的生成间隔
    private final float DASH_SPAWN_INTERVAL = 0.03f; // 冲刺时残影非常密集
    private final float RUN_SPAWN_INTERVAL = 0.1f;   // (预留) 普通跑步时残影较稀疏

    /**
     * 更新残影逻辑
     * @param shouldCreateTrail 是否应该产生残影
     * @param currentFrame 玩家当前的动画帧 (通过 player.getCurrentFrame() 获取)
     * @param trailColor 残影的颜色 (建议传入 new Color(r,g,b,1))，不要复用同一个对象，或者确保在 render 中不修改它
     */
    public void update(float delta, float playerX, float playerY, boolean shouldCreateTrail, TextureRegion currentFrame, Color trailColor) {
        // 1. 生成逻辑
        if (shouldCreateTrail) {
            spawnTimer += delta;

            // 简单逻辑：如果颜色非常亮(接近白色或青色)，认为是高能冲刺，使用高频生成
            // 否则认为是普通移动，使用低频生成
            float interval = (trailColor.r + trailColor.g + trailColor.b > 2.5f) ? DASH_SPAWN_INTERVAL : RUN_SPAWN_INTERVAL;

            if (spawnTimer >= interval) {
                spawnTimer = 0;
                // 只有当有有效帧时才生成
                if (currentFrame != null) {
                    ghosts.add(new Ghost(playerX, playerY, currentFrame, trailColor));
                }
            }
        } else {
            spawnTimer = 0.5f; // 重置计时器，确保下一次动作立即生成第一个残影
        }

        // 2. 更新残影（淡出）
        for (int i = ghosts.size - 1; i >= 0; i--) {
            Ghost g = ghosts.get(i);
            g.alpha -= delta * 4.0f; // 消失速度加快 (原为3.0f)，让视觉更清爽
            if (g.alpha <= 0) {
                ghosts.removeIndex(i);
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (ghosts.size == 0) return;

        // 保存旧状态
        int srcFunc = batch.getBlendSrcFunc();
        int dstFunc = batch.getBlendDstFunc();
        Color oldColor = batch.getColor();

        // 🔥 使用加法混合 (Additive Blending)
        // 让残影有发光感
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        for (Ghost g : ghosts) {
            if (g.region == null) continue;

            // 应用颜色和透明度
            // alpha * 0.5f 降低整体亮度，防止过曝
            batch.setColor(g.tintColor.r, g.tintColor.g, g.tintColor.b, g.alpha * 0.5f);

            // 尺寸计算逻辑需与 Player.java 保持一致
            // Player.java 使用 VISUAL_SCALE = 2.9f (根据之前的讨论)
            // 这里我们动态计算以匹配
            float scale = (float) GameConstants.CELL_SIZE / g.region.getRegionHeight();
            float visualScale = 2.9f;
            float finalScale = scale * visualScale;

            float drawW = g.region.getRegionWidth() * finalScale;
            float drawH = g.region.getRegionHeight() * finalScale;

            // 居中绘制
            float drawX = g.x * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f - drawW / 2f;
            float drawY = g.y * GameConstants.CELL_SIZE;

            batch.draw(g.region, drawX, drawY, drawW, drawH);
        }

        // 恢复状态
        batch.setColor(oldColor);
        batch.setBlendFunction(srcFunc, dstFunc);
    }

    public void dispose() {
        ghosts.clear();
    }
}