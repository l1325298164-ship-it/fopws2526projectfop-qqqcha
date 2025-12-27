package de.tum.cit.fop.maze.entities.EnemyBoba;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.entities.Enemy;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

/**
 * 堕落珍珠敌人 (Corrupted Boba Enemy)
 * 行为：巡逻 -> 蓄力(吸取珍珠) -> 喷射
 * 特效：程序化动画 (蓄力拉伸、发射压扁)
 */
public class EnemyCorruptedBoba extends Enemy {

    private enum State {
        PATROL,
        PREPARE, // 蓄力阶段 (Sucking up)
        ATTACK,  // 攻击瞬间 (Spitting)
        COOLDOWN // 攻击后摇
    }

    private State state = State.PATROL;
    private float stateTimer = 0f;

    // 战斗参数
    private final float PREPARE_TIME = 0.6f; // 蓄力时间
    private final float COOLDOWN_TIME = 1.0f;
    private final float DETECT_RANGE = 7f;

    // 视觉参数 (程序化动画)
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float shakeOffsetX = 0f;
    private float shakeOffsetY = 0f;

    // 颜色脉冲
    private float colorPulseTimer = 0f;

    public EnemyCorruptedBoba(int x, int y) {
        super(x, y);
        this.hp = 8;        // 比普通敌人肉一点
        this.attack = 15;   // 珍珠伤害较高
        this.moveSpeed = 4.0f;

        // 初始化贴图
        updateTexture();
    }

    @Override
    public void update(float delta, GameManager gm) {
        if (!active) return;

        Player player = gm.getPlayer();
        float dist = distanceTo(player);
        stateTimer += delta;
        colorPulseTimer += delta;

        switch (state) {
            case PATROL:
                handlePatrol(delta, gm, dist);
                break;
            case PREPARE:
                handlePrepare(delta, gm);
                break;
            case ATTACK:
                handleAttack(gm, player); // 只执行一帧
                break;
            case COOLDOWN:
                handleCooldown(delta);
                break;
        }

        // 更新视觉动画 (果冻效果)
        updateProceduralAnimation(delta);
    }

    // ================= 行为逻辑 =================

    private void handlePatrol(float delta, GameManager gm, float dist) {
        // 1. 简单的随机巡逻
        tryMoveRandom(delta, gm);

        // 2. 索敌
        if (dist <= DETECT_RANGE) {
            // 发现玩家，进入蓄力状态
            transitionTo(State.PREPARE);
        }
    }

    private void handlePrepare(float delta, GameManager gm) {
        // 蓄力期间不移动，转向玩家
        // 可以在这里添加根据玩家位置调整朝向的逻辑

        if (stateTimer >= PREPARE_TIME) {
            transitionTo(State.ATTACK);
        }
    }

    private void handleAttack(GameManager gm, Player player) {
        // 计算射击方向
        float dx = player.getX() - x;
        float dy = player.getY() - y;

        // 发射 BobaBullet
        // 注意：我们发射的位置稍微偏移一点，模拟从嘴里吐出来
        BobaBullet bullet = new BobaBullet(
                x + 0.5f, // 中心发射
                y + 0.5f,
                dx, dy,
                attack
        );

        gm.spawnProjectile(bullet);

        // 发射完直接进入冷却
        transitionTo(State.COOLDOWN);
    }

    private void handleCooldown(float delta) {
        if (stateTimer >= COOLDOWN_TIME) {
            transitionTo(State.PATROL);
        }
    }

    private void transitionTo(State newState) {
        this.state = newState;
        this.stateTimer = 0f;

        // 状态切换时的瞬间视觉反馈
        if (newState == State.ATTACK) {
            // 发射瞬间：压扁 (Squash)
            scaleX = 1.4f;
            scaleY = 0.6f;
        }
    }

    // ================= 视觉逻辑 =================

    private void updateProceduralAnimation(float delta) {
        float targetScaleX = 1.0f;
        float targetScaleY = 1.0f;
        shakeOffsetX = 0f;
        shakeOffsetY = 0f;

        if (state == State.PREPARE) {
            // 蓄力动画：拉长变细 (模拟从下往上吸气)
            // 随着时间推移，拉伸越明显
            float progress = stateTimer / PREPARE_TIME;
            targetScaleX = 1.0f - (0.3f * progress); // 变细
            targetScaleY = 1.0f + (0.3f * progress); // 变高

            // 蓄力震动 (Shaking)
            float shakePower = 0.05f * progress * GameConstants.CELL_SIZE;
            shakeOffsetX = MathUtils.random(-shakePower, shakePower);
            shakeOffsetY = MathUtils.random(-shakePower, shakePower);

        } else if (state == State.COOLDOWN) {
            // 冷却期间慢慢恢复正常
            // 已经在 transitionTo 里设置了初始压扁值，这里利用插值让它弹回来
        }

        // 弹性插值 (Lerp) - 让形变平滑恢复
        float speed = 10f;
        scaleX += (targetScaleX - scaleX) * speed * delta;
        scaleY += (targetScaleY - scaleY) * speed * delta;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;
        if (needsTextureUpdate) updateTexture();

        // 计算绘制参数
        float drawX = x * GameConstants.CELL_SIZE;
        float drawY = y * GameConstants.CELL_SIZE;
        float centerOffset = GameConstants.CELL_SIZE / 2f;

        // 保存 Batch 原始颜色
        Color originalColor = batch.getColor();

        // 1. 颜色变化
        if (state == State.PREPARE) {
            // 蓄力时变红/变亮
            float pulse = 0.8f + MathUtils.sin(colorPulseTimer * 20f) * 0.2f;
            batch.setColor(1f, pulse, pulse, 1f);
        } else {
            // 普通状态：稍微带点奶茶色
            batch.setColor(0.9f, 0.8f, 0.7f, 1f);
        }

        // 2. 绘制带形变和震动的贴图
        // 这里的关键是 originX/Y 设为中心，这样缩放才是向中心缩放
        if (texture != null) {
            batch.draw(
                    texture, // 使用 Texture
                    drawX + shakeOffsetX,
                    drawY + shakeOffsetY,
                    centerOffset, centerOffset, // 旋转/缩放中心
                    GameConstants.CELL_SIZE, GameConstants.CELL_SIZE,
                    scaleX, scaleY, // 应用我们的动态缩放
                    0, // 旋转 (如果需要也可以加)
                    0, 0,
                    texture.getWidth(), texture.getHeight(),
                    false, false
            );
        }

        // 恢复颜色
        batch.setColor(originalColor);
    }

    @Override
    protected void updateTexture() {
        // 复用 Enemy1 的贴图，或者你可以加载专门的贴图
        texture = textureManager.getEnemy1Texture();
        needsTextureUpdate = false;
    }

    private float distanceTo(Player p) {
        float dx = p.getX() - x;
        float dy = p.getY() - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}