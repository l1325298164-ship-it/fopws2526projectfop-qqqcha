package de.tum.cit.fop.maze.effects.environment.portal;

import com.badlogic.gdx.math.Vector2;

/**
 * 传送门粒子数据单元
 */
public class PortalParticle {
    public Vector2 position = new Vector2();
    public Vector2 velocity = new Vector2(); // 用于计算拖尾朝向
    public float angle;      // 螺旋运动的当前角度
    public float height;     // 当前上升的高度
    public float radius;     // 螺旋半径
    public float speed;      // 上升速度
    public float lifeTimer;  // 生命周期计时
    public float maxLife;    // 最大生命周期
    public float scale;      // 粒子大小缩放
    public boolean active;   // 是否存活
}