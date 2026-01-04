package de.tum.cit.fop.maze.effects.Player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.game.GameConstants;

/**
 * 冲刺残影特效管理器
 */
public class PlayerTrailManager {

    private static class Ghost {
        float x, y; // 玩家的格子坐标
        float alpha;
        TextureRegion region; // 记录生成时的那个瞬间的动画帧

        public Ghost(float x, float y, TextureRegion region) {
            this.x = x;
            this.y = y;
            this.region = region;
            this.alpha = 1.0f;
        }
    }

    private Array<Ghost> ghosts = new Array<>();
    private float spawnTimer = 0;
    private final float SPAWN_INTERVAL = 0.05f; // 残影生成间隔

    // 配置参数
    private Color trailColor = new Color(0.3f, 0.8f, 1.0f, 1f); // 青蓝色残影

    /**
     * 更新残影逻辑
     * @param delta 时间增量
     * @param playerX 玩家格子X
     * @param playerY 玩家格子Y
     * @param isDashing 是否正在冲刺
     * @param currentFrame 当前玩家显示的动画帧（关键！）
     */
    public void update(float delta, float playerX, float playerY, boolean isDashing, TextureRegion currentFrame) {
        // 1. 生成逻辑
        if (isDashing) {
            spawnTimer += delta;
            if (spawnTimer >= SPAWN_INTERVAL) {
                spawnTimer = 0;
                // 只有当有有效帧时才生成
                if (currentFrame != null) {
                    ghosts.add(new Ghost(playerX, playerY, currentFrame));
                }
            }
        } else {
            spawnTimer = SPAWN_INTERVAL; // 重置，保证下次冲刺立刻出残影
        }

        // 2. 更新残影（淡出）
        for (int i = ghosts.size - 1; i >= 0; i--) {
            Ghost g = ghosts.get(i);
            g.alpha -= delta * 3.0f; // 消失速度 (数值越大消失越快)
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

        // 使用加法混合 (Additive Blending) 让残影发光
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        for (Ghost g : ghosts) {
            if (g.region == null) continue;

            batch.setColor(trailColor.r, trailColor.g, trailColor.b, g.alpha * 0.5f);

            // 🔥 核心：复刻 Player.drawSprite 中的位置和缩放算法
            // 确保残影和玩家本体大小、位置完全一致
            float scale = (float) GameConstants.CELL_SIZE / g.region.getRegionHeight();
            float drawW = g.region.getRegionWidth() * scale + 10;
            float drawH = GameConstants.CELL_SIZE + 10;

            float drawX = g.x * GameConstants.CELL_SIZE
                    + GameConstants.CELL_SIZE / 2f - drawW / 2f;
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