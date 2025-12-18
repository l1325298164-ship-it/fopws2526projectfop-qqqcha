// GameObject.java
package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.utils.Logger;

public abstract class GameObject {
    protected int x;
    protected int y;
    protected boolean active = true;

    public GameObject(int x, int y) {
        this.x = x;
        this.y = y;
        Logger.debug(getClass().getSimpleName() + " created at (" + x + ", " + y + ")");
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // 明确区分渲染方式
    public abstract void drawShape(ShapeRenderer shapeRenderer);

    // 可选：如果也用纹理
    public abstract void drawSprite(SpriteBatch batch);


    // 或者更灵活：标记渲染类型
    public enum RenderType { SHAPE, SPRITE, BOTH }
    public abstract RenderType getRenderType();

    public boolean collidesWith(GameObject other) {
        return this.x == other.x && this.y == other.y;
    }

    public String getPositionString() {
        return "(" + x + ", " + y + ")";
    }
}
