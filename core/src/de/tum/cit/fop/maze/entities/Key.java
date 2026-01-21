// Key.java
package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public class Key extends GameObject {

    private Color color = GameConstants.KEY_COLOR;
    private Texture keyTexture;
    private boolean collected = false;

    private final TextureManager textureManager;
    private final GameManager gm;   // ✅ 新增

    private boolean needsTextureUpdate = true;
    public boolean playerCollectedKey;

    // ✅ 构造器必须传 GameManager
    public Key(int x, int y, GameManager gm) {
        super(x, y);
        this.gm = gm;
        this.textureManager = TextureManager.getInstance();

        this.active = true;      // 🔥 必须
        this.collected = false;  // 🔥 明确

        updateTexture();
        Logger.debug("Key created at " + getPositionString());
    }


    @Override
    public boolean isInteractable() {
        return active;
    }

    @Override
    public void onInteract(Player player) {
        if (!active) return;

        collect();
        AudioManager.getInstance().play(AudioType.PLAYER_GET_KEY);
        // 🔥 唯一正确的钥匙逻辑入口
        gm.onKeyCollected();
        playerCollectedKey = true;

        Logger.gameEvent("Key picked up");
    }

    @Override
    public boolean isPassable() {
        return true;
    }

    private void updateTexture() {
        keyTexture = textureManager.getKeyTexture();
        needsTextureUpdate = false;
    }

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
                x * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f,
                y * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f,
                GameConstants.CELL_SIZE / 2f - 4
        );
        shapeRenderer.end();
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active || collected || keyTexture == null) return;

        if (needsTextureUpdate) updateTexture();

        batch.draw(
                keyTexture,
                x * GameConstants.CELL_SIZE + 4,
                y * GameConstants.CELL_SIZE + 4,
                GameConstants.CELL_SIZE + 10,
                GameConstants.CELL_SIZE + 10
        );
    }

    @Override
    public RenderType getRenderType() {
        if (textureManager.getCurrentMode() == TextureManager.TextureMode.COLOR ||
                textureManager.getCurrentMode() == TextureManager.TextureMode.MINIMAL ||
                keyTexture == null) {
            return RenderType.SHAPE;
        }
        return RenderType.SPRITE;
    }

    public void collect() {
        collected = true;
        active = false;
        Logger.gameEvent("Key collected at " + getPositionString());
    }

    public Texture getTexture() {
        return  keyTexture;
    }

    public boolean isCollected() {
        return playerCollectedKey;
    }
}
