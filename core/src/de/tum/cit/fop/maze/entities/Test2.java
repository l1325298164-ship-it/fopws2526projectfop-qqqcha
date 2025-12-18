package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;

public class Test2 extends GameObject {
    private Color color = GameConstants.TEST2_COLOR;
    private int lives;
    private float invincibleTimer = 0;
    private boolean isInvincible = false;
    private boolean isDead = false;

    public Test2(int x, int y) {
        super(x,y);
        this.lives = GameConstants.INITIAL_PLAYER_LIVES;
        Logger.gameEvent("Tester2 spawned at " + getPositionString() + " with " + lives + " lives");
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        if (!active ) return;

        shapeRenderer.setColor(color);
        shapeRenderer.rect(
            x * GameConstants.CELL_SIZE + 2,
            y * GameConstants.CELL_SIZE + 2,
            GameConstants.CELL_SIZE - 10,
            GameConstants.CELL_SIZE - 10
        );
    }

    @Override
    public void drawSprite(SpriteBatch batch) {

    }

    @Override
    public RenderType getRenderType() {
        return null;
    }

    public void move(int dx, int dy) {
        if (isDead) return;
        this.x += dx;
        this.y += dy;
        Logger.debug("Player moved to " + getPositionString());
    }


    @Override
    public int getX() {
        return super.getX();
    }

    @Override
    public int getY() {
        return super.getY();
    }

    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
    }

    @Override
    public boolean isActive() {
        return super.isActive();
    }

    @Override
    public void setActive(boolean active) {
        super.setActive(active);
    }

    @Override
    public boolean collidesWith(GameObject other) {
        return super.collidesWith(other);
    }

    @Override
    public String getPositionString() {
        return super.getPositionString();
    }


}
