package de.tum.cit.fop.maze.entities.EnemyBoba;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.entities.EnemyBullet;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

/**
 * 珍珠奶茶子弹 (Boba Pearl)
 */
public class BobaBullet extends EnemyBullet {

    public enum BobaState {
        FLYING, BOUNCING, POPPING
    }

    private BobaState state = BobaState.FLYING;

    // 视觉形变参数
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float targetScaleX = 1.0f;
    private float targetScaleY = 1.0f;

    // 物理参数
    private int bounceCount = 0;
    private final int MAX_BOUNCES = 1;
    private float rotation = 0f;
    private float rotationSpeed = 300f;

    // 特效参数
    private float wobbleTime = 0f;
    private boolean managedByEffectManager = false;
    private float popTimer = 0f;
    private final float POP_DURATION = 0.15f;

    public BobaBullet(float x, float y, float dx, float dy, int damage) {
        super(x, y, dx, dy, damage);
        this.speed = 7f;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len != 0) {
            this.vx = (dx / len) * speed;
            this.vy = (dy / len) * speed;
        }
        this.rotation = MathUtils.random(0, 360);
    }

    @Override
    public void update(float delta, GameManager gm) {
        if (!active) return;

        // 状态机处理：如果正在破裂，只播放动画
        if (state == BobaState.POPPING) {
            updatePoppingState(delta);
            return;
        }

        float moveX = vx * delta;
        float moveY = vy * delta;
        float nextX = realX + moveX;
        float nextY = realY + moveY;
        int nextCellX = (int) nextX;
        int nextCellY = (int) nextY;

        // 碰撞检测：撞墙反弹
        if (gm.getMazeCell(nextCellX, nextCellY) == 0) {
            handleWallCollision(gm, nextCellX, nextCellY);
        } else {
            realX = nextX;
            realY = nextY;
            traveled += Math.sqrt(moveX * moveX + moveY * moveY);
        }

        // 同步格子坐标
        this.x = (int) realX;
        this.y = (int) realY;

        // 射程检测
        if (traveled >= maxRange && state != BobaState.POPPING) {
            triggerPop();
        }

        // 玩家碰撞检测
        Player player = gm.getPlayer();
        if (state != BobaState.POPPING && player.collidesWith(this)) {
            player.takeDamage(damage);
            triggerPop();
        }

        updateVisuals(delta);
    }

    // 1. 撞墙逻辑更新：减小撞击时的压扁程度
    private void handleWallCollision(GameManager gm, int wallX, int wallY) {
        if (bounceCount >= MAX_BOUNCES) {
            triggerPop();
            return;
        }
        bounceCount++;
        state = BobaState.BOUNCING;

        boolean hitX = gm.getMazeCell((int) (realX + vx * 0.05f), (int) realY) == 0;
        boolean hitY = gm.getMazeCell((int) realX, (int) (realY + vy * 0.05f)) == 0;

        // 反弹速度保留更多 (0.8f -> 0.9f)，看起来更弹
        if (hitX) vx = -vx * 0.9f;
        if (hitY) vy = -vy * 0.9f;

        // ⭐ 修改：形变幅度减小
        // 原来是 1.5f / 0.6f (太夸张)
        // 现在是 1.25f / 0.8f (轻微挤压)
        scaleX = 1.25f;
        scaleY = 0.8f;

        rotation += 180f;
    }

    // 2. 视觉逻辑更新：减小飞行时的拉伸程度
    private void updateVisuals(float delta) {
        wobbleTime += delta;
        rotation += rotationSpeed * delta;
        targetScaleX = 1.0f;
        targetScaleY = 1.0f;

        float currentSpeed = (float) Math.sqrt(vx * vx + vy * vy);
        if (state == BobaState.FLYING && currentSpeed > 1f) {
            // ⭐ 修改：拉伸系数减半 (0.02f -> 0.01f)
            targetScaleX = 1.0f - (currentSpeed * 0.01f); // 微微变细
            targetScaleY = 1.0f + (currentSpeed * 0.01f); // 微微变长
        }

        // 加快恢复速度 (10f -> 15f)，让它回弹更干脆
        scaleX = scaleX + (targetScaleX - scaleX) * 15f * delta;
        scaleY = scaleY + (targetScaleY - scaleY) * 15f * delta;
    }

    private void updatePoppingState(float delta) {
        popTimer += delta;
        float progress = popTimer / POP_DURATION;
        scaleX = 1.0f + progress * 0.5f;
        scaleY = 1.0f + progress * 0.5f;
        if (popTimer >= POP_DURATION) {
            active = false;
        }
    }

    public void triggerPop() {
        if (state == BobaState.POPPING) return;
        state = BobaState.POPPING;
        popTimer = 0f;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        // ✅ 留空：既然特效系统已经能画出贴图了，这里就不需要画红块了
        // 这样可以避免红块遮挡或重叠
    }

    // Getter 方法
    public float getScaleX() { return scaleX; }
    public float getScaleY() { return scaleY; }
    public float getRotation() { return rotation; }

    public float getWobbleOffsetX() {
        return (float) Math.sin(wobbleTime * 15f) * 0.05f * GameConstants.CELL_SIZE * scaleX;
    }

    public float getWobbleOffsetY() {
        return (float) Math.cos(wobbleTime * 12f) * 0.05f * GameConstants.CELL_SIZE * scaleY;
    }

    public boolean isPopping() { return state == BobaState.POPPING; }
    public boolean isManagedByEffectManager() { return managedByEffectManager; }
    public void setManagedByEffectManager(boolean managed) { this.managedByEffectManager = managed; }
}