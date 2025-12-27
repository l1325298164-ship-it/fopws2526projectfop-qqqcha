package de.tum.cit.fop.maze.entities.EnemyBoba;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.entities.EnemyBullet;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

/**
 * 珍珠奶茶子弹 (Boba Pearl)
 * 特性：果冻弹性、撞墙反弹、带特效渲染
 */
public class BobaBullet extends EnemyBullet {

    public enum BobaState {
        FLYING,     // 正常飞行
        BOUNCING,   // 撞墙反弹中
        POPPING     // 破裂消失中
    }

    private BobaState state = BobaState.FLYING;

    // 视觉形变参数 (Squash & Stretch)
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float targetScaleX = 1.0f;
    private float targetScaleY = 1.0f;

    // 物理参数
    private int bounceCount = 0;
    private final int MAX_BOUNCES = 1; // 允许反弹1次
    private float rotation = 0f;
    private float rotationSpeed = 300f;

    // 特效参数
    private float wobbleTime = 0f;
    private boolean managedByEffectManager = false;

    // 破裂动画计时
    private float popTimer = 0f;
    private final float POP_DURATION = 0.15f; // 破裂动画持续时间

    public BobaBullet(float x, float y, float dx, float dy, int damage) {
        super(x, y, dx, dy, damage);
        this.speed = 7f; // 比普通子弹稍快

        // 重新计算速度向量以应用新的 speed
        float len = (float) Math.sqrt(dx*dx + dy*dy);
        if (len != 0) {
            this.vx = (dx / len) * speed;
            this.vy = (dy / len) * speed;
        }

        // 初始随机旋转
        this.rotation = MathUtils.random(0, 360);
    }

    @Override
    public void update(float delta, GameManager gm) {
        if (!active) return;

        // 0. 状态机逻辑
        if (state == BobaState.POPPING) {
            updatePoppingState(delta);
            return; // 破裂中不再移动
        }

        // 1. 移动逻辑 (自定义，不调用 super.update 以拦截销毁)
        float moveX = vx * delta;
        float moveY = vy * delta;

        // 预计算下一步位置
        float nextX = realX + moveX;
        float nextY = realY + moveY;
        int nextCellX = (int) nextX;
        int nextCellY = (int) nextY;

        // 2. 撞墙检测与反弹
        if (gm.getMazeCell(nextCellX, nextCellY) == 0) {
            handleWallCollision(gm, nextCellX, nextCellY);
        } else {
            // 未撞墙，应用移动
            realX = nextX;
            realY = nextY;
            traveled += Math.sqrt(moveX*moveX + moveY*moveY);
        }

        // 同步格子坐标
        this.x = (int) realX;
        this.y = (int) realY;

        // 3. 射程限制
        if (traveled >= maxRange && state != BobaState.POPPING) {
            triggerPop(); // 射程耗尽，破裂
        }

        // 4. 命中玩家
        Player player = gm.getPlayer();
        if (state != BobaState.POPPING && player.collidesWith(this)) {
            player.takeDamage(damage);
            // 这里可以添加击退效果
            triggerPop(); // 命中后破裂
        }

        // 5. 更新视觉特效
        updateVisuals(delta);
    }

    /**
     * 处理撞墙反弹
     */
    private void handleWallCollision(GameManager gm, int wallX, int wallY) {
        if (bounceCount >= MAX_BOUNCES) {
            triggerPop(); // 超过反弹次数，破裂
            return;
        }

        bounceCount++;
        state = BobaState.BOUNCING;

        // 简单的反弹逻辑：判断是横向还是纵向碰撞
        // 检查如果我们只在 X 轴移动是否会撞
        boolean hitX = gm.getMazeCell((int)(realX + vx * 0.05f), (int)realY) == 0;
        boolean hitY = gm.getMazeCell((int)realX, (int)(realY + vy * 0.05f)) == 0;

        if (hitX) {
            vx = -vx * 0.8f; // 反弹并损失动能
        }
        if (hitY) {
            vy = -vy * 0.8f;
        }
        // 如果刚好是角，两个都会反弹

        // 视觉反馈：撞击压扁 (Squash)
        scaleX = 1.5f;
        scaleY = 0.6f;

        // 调整方向朝向反弹方向（为了拉伸效果）
        rotation += 180f;
    }

    private void updateVisuals(float delta) {
        wobbleTime += delta;
        rotation += rotationSpeed * delta;

        // 动态形变逻辑 (Spring/Elasticity)
        // 目标是恢复到 1.0
        targetScaleX = 1.0f;
        targetScaleY = 1.0f;

        // 飞行时根据速度略微拉长 (Stretch)
        float currentSpeed = (float)Math.sqrt(vx*vx + vy*vy);
        if (state == BobaState.FLYING && currentSpeed > 1f) {
            // 简单的拉伸：速度越快越长
            targetScaleX = 1.0f - (currentSpeed * 0.02f); // 变窄
            targetScaleY = 1.0f + (currentSpeed * 0.02f); // 变长
        }

        // 线性插值恢复形状 (Lerp)
        scaleX = scaleX + (targetScaleX - scaleX) * 10f * delta;
        scaleY = scaleY + (targetScaleY - scaleY) * 10f * delta;
    }

    private void updatePoppingState(float delta) {
        popTimer += delta;

        // 破裂动画：迅速变大然后消失
        float progress = popTimer / POP_DURATION;
        scaleX = 1.0f + progress * 0.5f;
        scaleY = 1.0f + progress * 0.5f;

        if (popTimer >= POP_DURATION) {
            active = false; // 彻底销毁
        }
    }

    public void triggerPop() {
        if (state == BobaState.POPPING) return;
        state = BobaState.POPPING;
        popTimer = 0f;
        // 在此处，如果有特效管理器，它会检测到 active=false 并播放粒子
        // 但为了更精准的控制，我们让 active 在动画结束后才设为 false
    }

    // ==========================================
    // Getter / Setter 供特效管理器和渲染器使用
    // ==========================================

    @Override
    public void drawSprite(SpriteBatch batch) {
        // BobaBullet 通常由 BobaBulletManager 渲染
        // 如果没有管理器，这里可以放一个备用的渲染逻辑
    }

    public float getScaleX() { return scaleX; }
    public float getScaleY() { return scaleY; }
    public float getRotation() { return rotation; }

    // 供 BobaBulletRenderer 用的特效偏移
    public float getWobbleOffsetX() {
        return (float)Math.sin(wobbleTime * 15f) * 0.05f * GameConstants.CELL_SIZE * scaleX;
    }
    public float getWobbleOffsetY() {
        return (float)Math.cos(wobbleTime * 12f) * 0.05f * GameConstants.CELL_SIZE * scaleY;
    }

    // 状态查询
    public boolean isPopping() { return state == BobaState.POPPING; }

    public boolean isManagedByEffectManager() { return managedByEffectManager; }
    public void setManagedByEffectManager(boolean managed) { this.managedByEffectManager = managed; }
}