package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.entities.chapter.Chapter1Relic;
import de.tum.cit.fop.maze.game.ChapterContext;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.ArrayList;
import java.util.List;

/**
 * 宝箱实体
 *
 * - Chapter 1：100% 掉章节遗物
 * - 其他情况：原 Buff 掉落逻辑
 */
public class Treasure extends GameObject {

    private boolean isOpened = false;

    private Texture closedTexture;
    private Texture openTexture;
    private final TextureManager textureManager;
    private boolean needsTextureUpdate = true;


    public Treasure(int x, int y) {
        super(x, y);
        this.textureManager = TextureManager.getInstance();
        updateTexture();

        Logger.debug("Treasure chest created at " + getPositionString());
    }

    @Override
    public void onInteract(Player player) {
        if (isOpened) return;
        isOpened = true;

        player.requestChapter1RelicFromTreasure(this);
    }


    @Override
    public boolean isInteractable() {
        return !isOpened;
    }

    @Override
    public boolean isPassable() {
        return true;
    }

    /* ================= 渲染 ================= */

    private void updateTexture() {
        if (closedTexture == null || openTexture == null) {
            try {
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

        Texture current = isOpened ? openTexture : closedTexture;
        if (current != null) {
            batch.draw(
                    current,
                    x * GameConstants.CELL_SIZE,
                    y * GameConstants.CELL_SIZE,
                    GameConstants.CELL_SIZE,
                    GameConstants.CELL_SIZE
            );
        }
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        if (closedTexture != null) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
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
        if (textureManager.getCurrentMode() == TextureManager.TextureMode.COLOR
                || textureManager.getCurrentMode() == TextureManager.TextureMode.MINIMAL
                || closedTexture == null) {
            return RenderType.SHAPE;
        }
        return RenderType.SPRITE;
    }

    public void dispose() {
        if (closedTexture != null) closedTexture.dispose();
        if (openTexture != null) openTexture.dispose();
    }
}
