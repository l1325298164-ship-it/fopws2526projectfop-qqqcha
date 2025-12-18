// Key.java
package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public class Key extends GameObject {

    public Key(int x, int y) {
        super(x, y);
        Logger.gameEvent("Key spawned at " + getPositionString());
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {

    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;

        TextureManager textureManager = TextureManager.getInstance();
        batch.draw(textureManager.getKeyTexture(),
            x * GameConstants.CELL_SIZE + 15,
            y * GameConstants.CELL_SIZE + 15,
            GameConstants.CELL_SIZE - 30,
            GameConstants.CELL_SIZE - 30
        );
    }




    @Override
    public RenderType getRenderType() {
        return null;
    }

    public void collect() {
        active = false;
        Logger.gameEvent("Key collected from " + getPositionString());
    }
}
