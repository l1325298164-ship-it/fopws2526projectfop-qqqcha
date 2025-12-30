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
    @Override
    public boolean isInteractable() {
        return true; // 门总是可交互的
    }

    @Override
    public void onInteract(Player player) {
        if (locked) {
            if (player.hasKey()) {
                unlock();
                Logger.gameEvent("门已解锁");
            } else {
                Logger.gameEvent("门被锁住了，需要钥匙");
            }
        } else {
            Logger.gameEvent("门已解锁，可以直接通过");
        }
    }

    @Override
    public boolean isPassable() {
        return !locked; // 只有解锁后才能通过
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
    public void drawShape(ShapeRenderer sr) {
        if (!active) return;

        // 最近出口高亮
        if (isNearest && !locked) {
            sr.setColor(Color.GOLD);
        } else {
            sr.setColor(locked ? lockedColor : unlockedColor);
        }

        sr.rect(
                x * GameConstants.CELL_SIZE + 2,
                y * GameConstants.CELL_SIZE + 2,
                GameConstants.CELL_SIZE - 4,
                GameConstants.CELL_SIZE - 4
        );
    }


    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active || doorTexture == null) return;

        if (needsTextureUpdate) {
            updateTexture();
        }

        batch.draw(
                doorTexture,
                x * GameConstants.CELL_SIZE,
                y * GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE+5
        );
    }


    @Override
    public RenderType getRenderType() {
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
