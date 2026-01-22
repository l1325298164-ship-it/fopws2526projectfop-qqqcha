// GameObject.java - 添加纹理渲染支持
package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public abstract class GameObject {
    protected int x, y;
    protected boolean active = true;

    // 渲染类型枚举
    public enum RenderType {
        SHAPE,  // 使用ShapeRenderer绘制
        SPRITE  // 使用SpriteBatch绘制
    }

    public GameObject(int x, int y) {
        this.x = x;
        this.y = y;
    }

    // 抽象方法
    public abstract void drawShape(ShapeRenderer shapeRenderer);
    public abstract void drawSprite(SpriteBatch batch);
    public abstract RenderType getRenderType();

    // 响应纹理模式变化
    public void onTextureModeChanged() {
        // 子类可以重写此方法
    }
    // 是否为可交互对象（默认false）
    public boolean isInteractable() {
        return false;
    }

    // 交互方法（默认空实现）
    public void onInteract(Player player) {
        // 子类可以重写此方法来实现交互逻辑
    }

    // 是否可通过（默认true）
    public boolean isPassable() {
        return true;
    }
    // 碰撞检测
    public boolean collidesWith(GameObject other) {
        return this.x == other.x && this.y == other.y;
    }

    // Getter和Setter
    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    // 调试信息
    public String getPositionString() {
        return "(" + x + ", " + y + ")";
    }
}
