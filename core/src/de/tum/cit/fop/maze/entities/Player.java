// Player.java - 更新版本
package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;

public class Player extends GameObject {
    private Color color = GameConstants.PLAYER_COLOR;
    private boolean hasKey = false;
    private int lives;
    private float invincibleTimer = 0;
    private boolean isInvincible = false;
    private boolean isDead = false;

    public Player(int x, int y) {
        super(x, y);
        this.lives = GameConstants.INITIAL_PLAYER_LIVES;
        Logger.gameEvent("Player spawned at " + getPositionString() + " with " + lives + " lives");
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        if (!active) return;

        shapeRenderer.setColor(color);
        shapeRenderer.rect(
            x * GameConstants.CELL_SIZE + 2,
            y * GameConstants.CELL_SIZE + 2,
            GameConstants.CELL_SIZE - 4,
            GameConstants.CELL_SIZE - 4
        );

    }

    @Override
    public void drawSprite(SpriteBatch batch) {

    }

    @Override
    public RenderType getRenderType() {
        return null;
    }



    public void update(float deltaTime) {
        if (isInvincible) {
            invincibleTimer += deltaTime;
            if (invincibleTimer >= GameConstants.INVINCIBLE_TIME) {
                isInvincible = false;
                invincibleTimer = 0;
                Logger.debug("Player invincibility ended");
            }
        }
    }

    public boolean hasKey() { return hasKey; }
    public void setHasKey(boolean hasKey) {
        this.hasKey = hasKey;
        if (hasKey) {
            Logger.gameEvent("Player obtained the key!");
        }
    }

    public void move(int dx, int dy) {
        if (isDead) return;
        this.x += dx;
        this.y += dy;
        Logger.debug("Player moved to " + getPositionString());
    }

}
