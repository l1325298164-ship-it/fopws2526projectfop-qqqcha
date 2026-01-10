package de.tum.cit.fop.maze.effects.Player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.game.GameConstants;

/**
 * 冲刺残影特效管理器
 * 特性：高亮发光混合模式，显示为原色发光残影
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
            g.alpha -= delta * 3.0f; // 消失速度
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

        // 🔥 保留：使用加法混合 (Additive Blending)
        // 这会让残影看起来更亮、有“能量感”，且重叠部分会变亮
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        for (Ghost g : ghosts) {
            if (g.region == null) continue;

            // 使用纯白 (1f, 1f, 1f)
            // 这样残影会显示角色原本的颜色，但因为加法混合，看起来会比本体更亮/发光
            // 透明度系数设为 0.6f，避免在白色背景下过曝
            batch.setColor(1f, 1f, 1f, g.alpha * 0.6f);

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