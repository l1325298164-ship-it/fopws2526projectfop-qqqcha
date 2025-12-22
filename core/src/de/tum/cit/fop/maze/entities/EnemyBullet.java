package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.TextureManager;

public class EnemyBullet extends GameObject {

    private float realX;
    private float realY;

    private float vx, vy;
    private float speed = 6f;
    private float traveled = 0f;
    private float maxRange = 8f;

    private int damage;

    public EnemyBullet(float x, float y, float dx, float dy, int damage) {
        super((int) x, (int) y); // ✅ 只用于初始化格子坐标
        this.realX = x;
        this.realY = y;
        float len = (float) Math.sqrt(dx*dx + dy*dy);
        vx = dx / len;
        vy = dy / len;
        this.damage = damage;
    }

    public void update(float delta, GameManager gm) {
        float move = speed * delta;

        realX += vx * move;
        realY += vy * move;
        traveled += move;

        // 同步到 GameObject（用于碰撞 / 渲染）
        this.x = (int) realX;
        this.y = (int) realY;
        // 撞墙
        if (gm.getMazeCell(x, y) == 0) {
            active = false;
            return;
        }

        // 射程限制
        if (traveled >= maxRange) {
            active = false;
            return;
        }

        // 命中玩家
        Player player = gm.getPlayer();
        if (player.collidesWith(this)) {
            player.takeDamage(damage);
            active = false;
        }
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {

    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;

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



}
