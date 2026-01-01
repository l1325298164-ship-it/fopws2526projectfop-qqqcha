package de.tum.cit.fop.maze.effects.Player;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.game.GameConstants;

/**
 * 冲刺残影特效
 * 原理：记录历史位置队列，渲染半透明的玩家纹理
 */
public class PlayerTrailManager {

    private static class Ghost {
        float x, y;
        float alpha;
        float rotation; // 如果之后玩家有旋转，这里也要存
        // 甚至可以存 TextureRegion 如果有动画帧

        public Ghost(float x, float y) {
            this.x = x;
            this.y = y;
            this.alpha = 1.0f;
        }
    }

    private Array<Ghost> ghosts = new Array<>();
    private float spawnTimer = 0;
    private final float SPAWN_INTERVAL = 0.05f; // 每0.05秒生成一个残影

    // 配置参数
    private boolean isEnabled = false;
    private Color trailColor = new Color(0.3f, 0.8f, 1.0f, 1f); // 青蓝色残影

    public void update(float delta, float playerX, float playerY, boolean isDashing) {
        // 1. 生成逻辑
        if (isDashing) {
            spawnTimer += delta;
            if (spawnTimer >= SPAWN_INTERVAL) {
                spawnTimer = 0;
                ghosts.add(new Ghost(playerX, playerY));
            }
        } else {
            // 如果不在冲刺，立即重置计时器，保证下次冲刺立刻出残影
            spawnTimer = SPAWN_INTERVAL;
        }

        // 2. 更新残影（淡出）
        for (int i = ghosts.size - 1; i >= 0; i--) {
            Ghost g = ghosts.get(i);
            g.alpha -= delta * 2.0f; // 0.5秒内消失 (1.0 / 0.5 = 2.0)
            if (g.alpha <= 0) {
                ghosts.removeIndex(i);
            }
        }
    }

    public void render(SpriteBatch batch, Texture playerTexture) {
        if (ghosts.size == 0 || playerTexture == null) return;

        // 保存旧的混合模式和颜色
        int srcFunc = batch.getBlendSrcFunc();
        int dstFunc = batch.getBlendDstFunc();
        Color oldColor = batch.getColor();

        // 使用加法混合 (Additive)，让残影看起来像光影
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        for (Ghost g : ghosts) {
            // 颜色插值：随着 alpha 降低，颜色也可以稍微变化（可选）
            batch.setColor(trailColor.r, trailColor.g, trailColor.b, g.alpha * 0.6f);

            // 注意坐标转换：Manager里通常存的是格子坐标，需要转像素
            float drawX = g.x * GameConstants.CELL_SIZE;
            float drawY = g.y * GameConstants.CELL_SIZE;

            // 假设 GameConstants.CELL_SIZE 就是纹理大小，如果不是需要调整
            batch.draw(playerTexture, drawX, drawY, GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);
        }

        // 恢复状态
        batch.setColor(oldColor);
        batch.setBlendFunction(srcFunc, dstFunc);
    }

    public void setTrailColor(float r, float g, float b) {
        trailColor.set(r, g, b, 1f);
    }

    public void dispose() {
        ghosts.clear();
    }
}