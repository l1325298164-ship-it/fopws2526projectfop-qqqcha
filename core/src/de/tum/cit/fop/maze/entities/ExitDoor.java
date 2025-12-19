// ExitDoor.java - 更新版本（支持多个出口）
package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public class ExitDoor extends GameObject {
    private Color lockedColor = GameConstants.LOCKED_DOOR_COLOR;
    private Color unlockedColor = GameConstants.DOOR_COLOR;
    private boolean locked = true;
    private boolean isNearest = false; // 标记是否为最近的出口
    private int doorId; // 出口ID

    // 纹理管理
    private TextureManager textureManager;
    private Texture doorTexture;
    private boolean needsTextureUpdate = true;

    public ExitDoor(int x, int y, int doorId) {
        super(x, y);
        this.doorId = doorId;
        this.textureManager = TextureManager.getInstance();
        updateTexture();
        Logger.debug("ExitDoor " + doorId + " created at " + getPositionString());
    }

    /**
     * 更新纹理
     */
    private void updateTexture() {
        doorTexture = locked ?
            textureManager.getLockedDoorTexture() :
            textureManager.getDoorTexture();
        needsTextureUpdate = false;
    }

    /**
     * 响应纹理模式切换
     */
    public void onTextureModeChanged() {
        needsTextureUpdate = true;
    }

    /**
     * 解锁门
     */
    public void unlock() {
        this.locked = false;
        updateTexture();
        Logger.gameEvent("ExitDoor " + doorId + " unlocked at " + getPositionString());
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        if (!active) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // 如果是最近的出口，用不同颜色高亮
        if (isNearest && !locked) {
            shapeRenderer.setColor(Color.GOLD);
        } else {
            shapeRenderer.setColor(locked ? lockedColor : unlockedColor);
        }

        // 绘制门主体
        shapeRenderer.rect(
            x * GameConstants.CELL_SIZE + 2,
            y * GameConstants.CELL_SIZE + 2,
            GameConstants.CELL_SIZE - 4,
            GameConstants.CELL_SIZE - 4
        );

        // 如果是最近的出口，绘制边框
        if (isNearest && !locked) {
            shapeRenderer.set(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.YELLOW);
            shapeRenderer.rect(
                x * GameConstants.CELL_SIZE,
                y * GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE
            );
        }

        shapeRenderer.end();
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active || doorTexture == null) return;

        // 如果需要更新纹理
        if (needsTextureUpdate) {
            updateTexture();
        }

        batch.draw(doorTexture,
            x * GameConstants.CELL_SIZE,
            y * GameConstants.CELL_SIZE,
            GameConstants.CELL_SIZE,
            GameConstants.CELL_SIZE);

        // 如果是最近的出口，绘制高亮效果
        if (isNearest && !locked) {
            // 可以使用叠加效果或特殊纹理
            // 这里简单地在门上画一个金色边框
            ShapeRenderer shapeRenderer = new ShapeRenderer();
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.YELLOW);
            shapeRenderer.rect(
                x * GameConstants.CELL_SIZE,
                y * GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE
            );
            shapeRenderer.end();
        }
    }

    @Override
    public RenderType getRenderType() {
        if (textureManager.getCurrentMode() == TextureManager.TextureMode.COLOR ||
            textureManager.getCurrentMode() == TextureManager.TextureMode.MINIMAL ||
            doorTexture == null) {
            return RenderType.SHAPE;
        }
        return RenderType.SPRITE;
    }

    public boolean isLocked() {
        return locked;
    }

    public int getDoorId() {
        return doorId;
    }

    public void setNearest(boolean nearest) {
        this.isNearest = nearest;
    }

    public boolean isNearest() {
        return isNearest;
    }
}
