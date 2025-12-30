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
 * 对应策划案：拾取后增加生命上限 + 回复生命值
 */
public class HeartContainer extends GameObject {
    // 设置默认颜色为橙色 (区分于回血道具的红色)
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

    @Override
    public boolean isInteractable() {
        return active; // 只有激活状态才可交互
    }

    @Override
    public void onInteract(Player player) {
        if (active) {
            collect();

            // 🔥 核心逻辑：调用 Player 的增加上限方法
            player.increaseMaxLives(INCREASE_AMOUNT);

            Logger.gameEvent("玩家拾取了焦糖核心，生命上限增加！");
        }
    }

    @Override
    public boolean isPassable() {
        return true; // 道具可以通过
    }

    /**
     * 更新纹理
     */
    private void updateTexture() {
        if (containerTexture == null) {
            try {
                // 🔥 修改点：路径指向 Assets/Items/heart_container.png
                containerTexture = new Texture(Gdx.files.internal("Items/heart_container.png"));
            } catch (Exception e) {
                Logger.error("Could not load container texture: " + e.getMessage());
            }
        }
        needsTextureUpdate = false;
    }

    @Override
    public void onTextureModeChanged() {
        needsTextureUpdate = true;
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        if (!active || collected || containerTexture != null) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(color);
        // 画一个方形代表核心，区分于圆形的血包
        shapeRenderer.rect(
                x * GameConstants.CELL_SIZE + 8,
                y * GameConstants.CELL_SIZE + 8,
                GameConstants.CELL_SIZE - 16,
                GameConstants.CELL_SIZE - 16
        );
        shapeRenderer.end();
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active || collected || containerTexture == null) return;

        if (needsTextureUpdate) {
            updateTexture();
        }

        // 绘制图片，稍微留一点边距
        batch.draw(containerTexture,
                x * GameConstants.CELL_SIZE + 4,
                y * GameConstants.CELL_SIZE + 4,
                GameConstants.CELL_SIZE - 8,
                GameConstants.CELL_SIZE - 8);
    }

    @Override
    public RenderType getRenderType() {
        if (textureManager.getCurrentMode() == TextureManager.TextureMode.COLOR ||
                textureManager.getCurrentMode() == TextureManager.TextureMode.MINIMAL ||
                containerTexture == null) {
            return RenderType.SHAPE;
        }
        return RenderType.SPRITE;
    }

    public void collect() {
        this.collected = true;
        this.active = false;
        Logger.gameEvent("HeartContainer collected at " + getPositionString());
    }

    public void dispose() {
        if (containerTexture != null) {
            containerTexture.dispose();
        }
    }
}