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
 * 生命上限道具 (焦糖核心)
 * 逻辑：
 * 1. 被 GameManager 创建 (E04 死亡掉落)
 * 2. 被 GameManager 渲染
 * 3. 被 GameManager 检测到碰撞后调用 onInteract
 * 4. 执行效果：调用 Player 的 increaseMaxLives 方法
 */
public class HeartContainer extends GameObject {

    // 设置默认颜色为橙色 (当没有图片或在调试模式时显示)
    private Color color = Color.ORANGE;
    private Texture containerTexture;
    private boolean collected = false;

    // 纹理管理
    private TextureManager textureManager;
    private boolean needsTextureUpdate = true;

    // 增加上限的数量
    private static final int INCREASE_AMOUNT = 10;

    public HeartContainer(int x, int y) {
        super(x, y);
        this.textureManager = TextureManager.getInstance();
        updateTexture();
        Logger.debug("HeartContainer created at " + getPositionString());
    }

    /**
     * 这里控制是否允许交互。
     * GameManager 在 checkAutoPickup 中会检查这个状态。
     */
    @Override
    public boolean isInteractable() {
        return active && !collected;
    }

    /**
     * 核心交互逻辑
     * 由 GameManager 在检测到玩家踩在上面时调用
     */
    @Override
    public void onInteract(Player player) {
        if (isInteractable()) {
            collect();

            // 1. 核心逻辑：调用 Player 的增加上限方法
            player.increaseMaxLives(INCREASE_AMOUNT);

            // 🔥 2. 新增：飘字效果
            if (player.getGameManager() != null && player.getGameManager().getCombatEffectManager() != null) {
                float tx = x * GameConstants.CELL_SIZE;
                float ty = y * GameConstants.CELL_SIZE + 50; // 稍微高一点，避免挡住玩家

                // 飘出橙色的提示字
                player.getGameManager().getCombatEffectManager().spawnStatusText(
                        tx, ty,
                        "MAX HP +" + INCREASE_AMOUNT,
                        Color.ORANGE
                );
            }

            Logger.gameEvent("玩家拾取了焦糖核心，生命上限 +10！");
        }
    }

    @Override
    public boolean isPassable() {
        return true; // 道具不应该阻挡玩家移动
    }

    /**
     * 加载/更新纹理
     * 遵循 "Asset 定死" 原则，这里虽然暂时直接加载，
     * 但理想情况下应该通过 TextureManager.get() 获取。
     */
    private void updateTexture() {
        if (containerTexture == null) {
            try {
                // ⚠️ 确保 assets/Items/heart_container.png 存在！
                // 如果没有图片，会捕获异常并显示为橙色方块
                containerTexture = new Texture(Gdx.files.internal("Items/heart_container.png"));
            } catch (Exception e) {
                Logger.error("HeartContainer texture missing, using fallback shape: " + e.getMessage());
            }
        }
        needsTextureUpdate = false;
    }

    @Override
    public void onTextureModeChanged() {
        needsTextureUpdate = true;
    }

    /**
     * 绘制形状 (备用/调试模式)
     */
    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        if (!active || collected || containerTexture != null) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(color);
        // 画一个稍微小一点的橙色方块
        shapeRenderer.rect(
                x * GameConstants.CELL_SIZE + 8,
                y * GameConstants.CELL_SIZE + 8,
                GameConstants.CELL_SIZE - 16,
                GameConstants.CELL_SIZE - 16
        );
        shapeRenderer.end();
    }

    /**
     * 绘制图片 (正常模式)
     */
    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active || collected) return;

        // 如果纹理还没加载成功，尝试加载
        if (containerTexture == null || needsTextureUpdate) {
            updateTexture();
        }

        // 如果还是没有纹理，就什么都不画（会 fallback 到 drawShape）
        if (containerTexture == null) return;

        // 绘制图片，稍微留一点内边距 (4像素) 显得精致
        batch.draw(containerTexture,
                x * GameConstants.CELL_SIZE + 4,
                y * GameConstants.CELL_SIZE + 4,
                GameConstants.CELL_SIZE - 8,
                GameConstants.CELL_SIZE - 8);
    }

    /**
     * 决定当前是画图还是画形状
     */
    @Override
    public RenderType getRenderType() {
        // 如果是“极简模式”或者图片加载失败，就画形状
        if (textureManager.getCurrentMode() == TextureManager.TextureMode.MINIMAL ||
                containerTexture == null) {
            return RenderType.SHAPE;
        }
        return RenderType.SPRITE;
    }

    public void collect() {
        this.collected = true;
        this.active = false;
    }

    /**
     * 资源清理
     * GameManager 在 dispose() 时应该调用这个
     */
    public void dispose() {
        if (containerTexture != null) {
            containerTexture.dispose();
            containerTexture = null;
        }
    }
}