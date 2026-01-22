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
 * 宝箱实体 (纯净版)
 * 逻辑已移交 GameManager，此处仅处理渲染状态。
 */
public class Treasure extends GameObject {

    private boolean isOpened = false;
    private Texture closedTexture;
    private Texture openTexture;
    private TextureManager textureManager;
    private boolean needsTextureUpdate = true;

    public Treasure(int x, int y) {
        super(x, y);
        this.textureManager = TextureManager.getInstance();
        updateTexture();
    }

    @Override
    public void onInteract(Player player) {
        // 只改变视觉状态，不做任何游戏逻辑
        if (!isOpened) {
            isOpened = true;
            Logger.debug("Treasure visual state set to OPENED");
        }
    }

    @Override
    public boolean isInteractable() {
        return !isOpened;
    }

    @Override
    public boolean isPassable() {
        return true;
    }

    private void updateTexture() {
        if (closedTexture == null || openTexture == null) {
            try {
                closedTexture = new Texture(Gdx.files.internal("imgs/Items/chest_closed.png"));
                openTexture = new Texture(Gdx.files.internal("imgs/Items/chest_open.png"));
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
        Texture currentTexture = isOpened ? openTexture : closedTexture;
        if (currentTexture != null) {
            batch.draw(currentTexture, x * GameConstants.CELL_SIZE, y * GameConstants.CELL_SIZE, GameConstants.CELL_SIZE, GameConstants.CELL_SIZE);
        }
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        if (closedTexture != null) return;
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(isOpened ? Color.GRAY : Color.GOLD);
        shapeRenderer.rect(x * GameConstants.CELL_SIZE + 4, y * GameConstants.CELL_SIZE + 4, GameConstants.CELL_SIZE - 8, GameConstants.CELL_SIZE - 8);
        shapeRenderer.end();
    }

    @Override
    public RenderType getRenderType() {
        if (textureManager.getCurrentMode() == TextureManager.TextureMode.COLOR ||
                textureManager.getCurrentMode() == TextureManager.TextureMode.MINIMAL ||
                closedTexture == null) {
            return RenderType.SHAPE;
        }
        return RenderType.SPRITE;
    }

    public void dispose() {
        if (closedTexture != null) closedTexture.dispose();
        if (openTexture != null) openTexture.dispose();
    }
}