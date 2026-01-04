package de.tum.cit.fop.maze.entities.Obstacle;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.entities.GameObject;
import de.tum.cit.fop.maze.game.GameManager;

public abstract class DynamicObstacle extends GameObject {

    public DynamicObstacle(int x, int y) {
        super(x, y);  // 使用 GameObject 的坐标系统
    }

    protected float worldX, worldY;
    protected boolean isMoving;
    protected float targetX, targetY;

    protected float moveInterval;
    protected float moveCooldown;

    public abstract void update(float delta, GameManager gm);
    public abstract void draw(SpriteBatch batch);

    public boolean isPassable() {
        return false;
    }
    public float getWorldX() {
        return worldX;
    }

    public float getWorldY() {
        return worldY;
    }

}
