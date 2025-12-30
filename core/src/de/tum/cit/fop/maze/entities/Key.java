// Key.java - 更新版本
package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public class Key extends GameObject {
    private Color color = GameConstants.KEY_COLOR;
    private Texture keyTexture;
    private boolean collected = false;

    // 纹理管理
    private TextureManager textureManager;
    private boolean needsTextureUpdate = true;

    public Key(int x, int y) {
        super(x, y);
        this.textureManager = TextureManager.getInstance();
        updateTexture();
        Logger.debug("Key created at " + getPositionString());
    }
    @Override
    public boolean isInteractable() {
        return active; // 只有激活状态才可交互
    }

    @Override
    public void onInteract(Player player) {
        if (active) {
            collect(); // 收集钥匙
            player.setHasKey(true);
            Logger.gameEvent("钥匙被拾取");
        }
    }

    @Override
    public boolean isPassable() {
        return true; // 钥匙可以通过
    }
    /**
     * 更新纹理
     */
    private void updateTexture() {
        keyTexture = textureManager.getKeyTexture();
        needsTextureUpdate = false;
    }

    /**
     * 响应纹理模式切换
     */
    @Override
    public void onTextureModeChanged() {
        needsTextureUpdate = true;
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        if (!active || collected || keyTexture != null) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(color);
        shapeRenderer.circle(
            x * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2,
            y * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2,
            GameConstants.CELL_SIZE / 2 - 4
        );
        shapeRenderer.end();
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active || collected || keyTexture == null) return;

        // 如果需要更新纹理
        if (needsTextureUpdate) {
            updateTexture();
        }

        batch.draw(keyTexture,
            x * GameConstants.CELL_SIZE + 4,
            y * GameConstants.CELL_SIZE + 4,
            GameConstants.CELL_SIZE +10,
            GameConstants.CELL_SIZE +10);
    }

    @Override
    public RenderType getRenderType() {
        // 如果当前模式是COLOR或没有纹理，使用SHAPE
        if (textureManager.getCurrentMode() == TextureManager.TextureMode.COLOR ||
            textureManager.getCurrentMode() == TextureManager.TextureMode.MINIMAL ||
            keyTexture == null) {
            return RenderType.SHAPE;
        }
        return RenderType.SPRITE;
    }

    /**
     * 收集钥匙
     */
    public void collect() {
        this.collected = true;
        this.active = false;
        Logger.gameEvent("Key collected at " + getPositionString());
    }
}
