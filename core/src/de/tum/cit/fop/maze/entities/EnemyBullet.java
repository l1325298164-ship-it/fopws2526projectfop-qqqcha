package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.TextureManager;

public class EnemyBullet extends GameObject {

    // ⭐ 修改为 protected，供 BobaBullet 使用
    protected float realX;
    protected float realY;
    protected float vx, vy;
    protected float speed = 6f;
    protected float traveled = 0f;
    protected float maxRange = 8f;
    protected int damage;

    public EnemyBullet(float x, float y, float dx, float dy, int damage) {
        super((int) x, (int) y);
        this.realX = x;
        this.realY = y;
        this.damage = damage;

        // 归一化方向向量
        float len = (float) Math.sqrt(dx*dx + dy*dy);
        if (len != 0) {
            vx = (dx / len) * speed;
            vy = (dy / len) * speed;
        } else {
            vx = speed;
            vy = 0;
        }
    }

    public void update(float delta, GameManager gm) {
        float moveX = vx * delta;
        float moveY = vy * delta;

        realX += moveX;
        realY += moveY;
        traveled += Math.sqrt(moveX*moveX + moveY*moveY);

        // 同步格子坐标
        this.x = (int) realX;
        this.y = (int) realY;

        // 1. 撞墙检测
        if (gm.getMazeCell(x, y) == 0) {
            active = false; // 普通子弹直接销毁
            return;
        }

        // 2. 射程限制
        if (traveled >= maxRange) {
            active = false;
            return;
        }

        // 3. 命中玩家
        Player player = gm.getPlayer();
        if (player.collidesWith(this)) {
            player.takeDamage(damage);
            active = false;
        }
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        // 默认不绘制形状
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;

        // 简单的红色方形绘制，子类会覆盖此方法
        batch.setColor(1f, 0.3f, 0.3f, 1f);
        batch.draw(
                TextureManager.getInstance().getColorTexture(Color.RED),
                realX * GameConstants.CELL_SIZE,
                realY * GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE * 0.3f,
                GameConstants.CELL_SIZE * 0.3f
        );
        batch.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    // Getter 用于子类或外部访问
    public float getRealX() { return realX; }
    public float getRealY() { return realY; }
}