// ExitDoor.java
package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public class ExitDoor extends GameObject {
    private boolean locked = true;

    public ExitDoor(int x, int y) {
        super(x, y);
        Logger.gameEvent("Exit door spawned at " + getPositionString());
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {

    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;

        TextureManager textureManager = TextureManager.getInstance();
        Texture texture = locked ?
            textureManager.getLockedDoorTexture() :
            textureManager.getDoorTexture();

        batch.draw(texture,
            x * GameConstants.CELL_SIZE,
            y * GameConstants.CELL_SIZE,
            GameConstants.CELL_SIZE,
            GameConstants.CELL_SIZE
        );
    }

    @Override
    public RenderType getRenderType() {
        return null;
    }

    public boolean isLocked() { return locked; }
    public void unlock() {
        locked = false;
        Logger.gameEvent("Exit door unlocked at " + getPositionString());
    }



    public boolean canExit(boolean playerHasKey) {
        return !locked || playerHasKey;
    }
}
