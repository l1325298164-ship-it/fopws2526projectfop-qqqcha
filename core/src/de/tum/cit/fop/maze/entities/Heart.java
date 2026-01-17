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
 * 回血道具 (柠檬脆波波)
 * 对应策划案：拾取后回复 1 点生命值
 */
public class Heart extends GameObject {
    // 设置默认颜色为红色 (在 SHAPE 模式或无图片时显示)
    private Color color = Color.RED;
    private Texture heartTexture;
    private boolean collected = false;

    // 纹理管理
    private TextureManager textureManager;
    private boolean needsTextureUpdate = true;

    // 回复量
    private static final int HEAL_AMOUNT = 10;

    public Heart(int x, int y) {
        super(x, y);
        this.textureManager = TextureManager.getInstance();
        updateTexture();
        Logger.debug("Heart created at " + getPositionString());
    }

    @Override
    public boolean isInteractable() {
        return active; // 只有激活状态（未被收集）才可交互
    }

    @Override
    public void onInteract(Player player) {
        if (active) {
            collect(); // 标记为已收集

            // 🔥 核心逻辑：调用 Player 的回血方法
            player.heal(HEAL_AMOUNT);

            Logger.gameEvent("玩家拾取了爱心，恢复生命值");
        }
    }

    @Override
    public boolean isPassable() {
        return true; // 道具可以通过，不会挡路
    }

    /**
     * 更新纹理
     */
    private void updateTexture() {
        // 如果纹理未加载，尝试加载
        if (heartTexture == null) {
            try {
                // 🔥 修改点：路径指向 Assets/Items/heart.png
                heartTexture = new Texture(Gdx.files.internal("imgs/Items/heart.png"));
            } catch (Exception e) {
                Logger.error("Could not load heart texture: " + e.getMessage());
            }
        }
        needsTextureUpdate = false;
    }

    /**
     * 响应纹理模式切换 (例如从 DEBUG 模式切换到正常模式)
     */
    @Override
    public void onTextureModeChanged() {
        needsTextureUpdate = true;
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        // 如果已收集、不活动或有图片，则不画形状
        if (!active || collected || heartTexture != null) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(color);
        // 画一个红色的圆形代表血包
        shapeRenderer.circle(
                x * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f,
                y * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f,
                GameConstants.CELL_SIZE / 3f // 大小设为格子的 1/3
        );
        shapeRenderer.end();
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        // 如果已收集、不活动或没图片，则不画贴图
        if (!active || collected || heartTexture == null) return;

        // 检查是否需要重新加载纹理
        if (needsTextureUpdate) {
            updateTexture();
        }

        // 绘制图片，稍微留一点内边距 (+4)，避免贴着格子边
        batch.draw(heartTexture,
                x * GameConstants.CELL_SIZE + 4,
                y * GameConstants.CELL_SIZE + 4,
                GameConstants.CELL_SIZE - 8,
                GameConstants.CELL_SIZE - 8);
    }

    @Override
    public RenderType getRenderType() {
        // 如果当前是极简模式/颜色模式，或者图片加载失败，就用形状渲染
        if (textureManager.getCurrentMode() == TextureManager.TextureMode.COLOR ||
                textureManager.getCurrentMode() == TextureManager.TextureMode.MINIMAL ||
                heartTexture == null) {
            return RenderType.SHAPE;
        }
        return RenderType.SPRITE;
    }

    /**
     * 收集道具动作
     */
    public void collect() {
        this.collected = true;
        this.active = false;
        Logger.gameEvent("Heart collected at " + getPositionString());
    }

    // 销毁资源，防止内存泄漏
    public void dispose() {
        if (heartTexture != null) {
            heartTexture.dispose();
        }
    }
}