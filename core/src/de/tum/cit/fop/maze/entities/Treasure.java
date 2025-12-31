package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

/**
 * 宝箱实体
 * 对应策划案：宝箱里有道具
 */
public class Treasure extends GameObject {

    private boolean isOpened = false; // 记录是否已经打开

    // 纹理
    private Texture closedTexture;
    private Texture openTexture;
    private TextureManager textureManager;
    private boolean needsTextureUpdate = true;

    // 默认颜色 (备用，无图片时显示金色)
    private Color color = Color.GOLD;

    public Treasure(int x, int y) {
        super(x, y);
        this.textureManager = TextureManager.getInstance();
        updateTexture();
        Logger.debug("Treasure chest created at " + getPositionString());
    }

    /**
     * 玩家与宝箱交互逻辑
     * 当玩家坐标与宝箱重合时被调用
     */
    @Override
    public void onInteract(Player player) {
        // 只有关着的时候才能交互
        if (!isOpened) {
            open(player);
        }
    }

    /**
     * 执行开箱动作
     */
    private void open(Player player) {
        isOpened = true; // 标记为已打开

        // 🔥 TODO: 在这里实现具体的奖励逻辑
        // 根据你的策划案，这里可以决定给什么道具
        // 例如：
        // 1. 随机给一个道具
        // 2. 加分
        // 3. 获得钥匙 (如果这是钥匙宝箱)

        // 示例：简单加分
        player.addScore(100);

        Logger.gameEvent("宝箱打开了！获得了奖励！");
    }

    @Override
    public boolean isInteractable() {
        return !isOpened; // 只有没打开时才算“可交互”
    }

    @Override
    public boolean isPassable() {
        return true; // 允许玩家踩上去 (踩上去触发开箱)
    }

    // ================= 纹理与渲染 =================

    private void updateTexture() {
        // 加载两张图片
        if (closedTexture == null || openTexture == null) {
            try {
                // 确保 assets/Items/ 下有这两张图
                closedTexture = new Texture(Gdx.files.internal("Items/chest_closed.png"));
                openTexture = new Texture(Gdx.files.internal("Items/chest_open.png"));
            } catch (Exception e) {
                Logger.error("Failed to load treasure textures: " + e.getMessage());
            }
        }
        needsTextureUpdate = false;
    }

    @Override
    public void onTextureModeChanged() {
        needsTextureUpdate = true;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (needsTextureUpdate) updateTexture();

        // 🔥 关键逻辑：根据状态选择画哪张图
        Texture currentTexture = isOpened ? openTexture : closedTexture;

        if (currentTexture != null) {
            // 绘制
            batch.draw(currentTexture,
                    x * GameConstants.CELL_SIZE,
                    y * GameConstants.CELL_SIZE,
                    GameConstants.CELL_SIZE,
                    GameConstants.CELL_SIZE
            );
        }
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        // 如果有图片就不画形状
        if (closedTexture != null) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        // 如果开了是灰色(空了)，没开是金色
        shapeRenderer.setColor(isOpened ? Color.GRAY : Color.GOLD);
        shapeRenderer.rect(
                x * GameConstants.CELL_SIZE + 4,
                y * GameConstants.CELL_SIZE + 4,
                GameConstants.CELL_SIZE - 8,
                GameConstants.CELL_SIZE - 8
        );
        shapeRenderer.end();
    }

    @Override
    public RenderType getRenderType() {

        return RenderType.SPRITE;
    }

    // 释放资源，防止内存泄漏
    public void dispose() {
        if (closedTexture != null) closedTexture.dispose();
        if (openTexture != null) openTexture.dispose();
    }
}