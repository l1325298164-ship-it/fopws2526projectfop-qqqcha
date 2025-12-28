package de.tum.cit.fop.maze.entities.EnemyBoba;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.entities.Enemy;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

public class EnemyCorruptedBoba extends Enemy {

    private enum State {
        PATROL,
        CHASE,   // 追击状态
        PREPARE, // 蓄力 (停止移动)
        ATTACK,  // 发射
        COOLDOWN
    }

    private State state = State.PATROL;
    private float stateTimer = 0f;

    // 距离参数
    private final float DETECT_RANGE = 7f; // 发现玩家的距离
    private final float ATTACK_RANGE = 3.5f; // 开始蓄力攻击的距离 (这就意味着它会追到3.5格才停下)

    private final float PREPARE_TIME = 0.6f;
    private final float COOLDOWN_TIME = 1.0f;

    // 视觉参数
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;
    private float shakeOffsetX = 0f;
    private float shakeOffsetY = 0f;

    public EnemyCorruptedBoba(int x, int y) {
        super(x, y);
        this.hp = 8;
        this.attack = 15;
        this.moveSpeed = 3.5f; // 稍微调快一点，方便追击
        updateTexture();
    }

    @Override
    public void update(float delta, GameManager gm) {
        if (!active) return;
        // ⭐【修复 1】必须添加这行，否则敌人动一步就会永远卡住
        moveCooldown -= delta;

        Player player = gm.getPlayer();
        float dist = distanceTo(player);
        stateTimer += delta;

        // 状态机
        switch (state) {
            case PATROL:
                // 巡逻：随机走，看到玩家就追
                tryMoveRandom(delta, gm);
                if (dist <= DETECT_RANGE) {
                    transitionTo(State.CHASE);
                }
                break;

            case CHASE:
                // 追击：走向玩家
                moveToward(player.getX(), player.getY(), gm);

                // 距离够近 -> 蓄力准备攻击
                if (dist <= ATTACK_RANGE) {
                    transitionTo(State.PREPARE);
                }
                // 玩家跑太远 -> 放弃追击
                else if (dist > DETECT_RANGE * 1.5f) {
                    transitionTo(State.PATROL);
                }
                break;

            case PREPARE:
                // 蓄力：原地不动，调整朝向，等待动画
                if (stateTimer >= PREPARE_TIME) {
                    transitionTo(State.ATTACK);
                }
                // 如果蓄力时玩家跑出射程，是否取消？目前保持必发，增加难度
                break;

            case ATTACK:
                handleAttack(gm, player);
                break;

            case COOLDOWN:
                if (stateTimer >= COOLDOWN_TIME) {
                    // 冷却结束，如果玩家还在附近继续追，否则巡逻
                    if (dist <= DETECT_RANGE) {
                        transitionTo(State.CHASE);
                    } else {
                        transitionTo(State.PATROL);
                    }
                }
                break;
        }

        updateProceduralAnimation(delta);
    }

    private void handleAttack(GameManager gm, Player player) {
        float dx = player.getX() - x;
        float dy = player.getY() - y;

        BobaBullet bullet = new BobaBullet(
                x + 0.5f,
                y + 0.5f,
                dx, dy,
                attack
        );
        gm.spawnProjectile(bullet);
        transitionTo(State.COOLDOWN);
    }

    private void transitionTo(State newState) {
        this.state = newState;
        this.stateTimer = 0f;

        if (newState == State.ATTACK) {
            scaleX = 1.4f; // 发射瞬间变宽
            scaleY = 0.6f; // 发射瞬间变扁
        }
    }

    private void updateProceduralAnimation(float delta) {
        float targetScaleX = 1.0f;
        float targetScaleY = 1.0f;
        shakeOffsetX = 0f;
        shakeOffsetY = 0f;

        if (state == State.PREPARE) {
            // 蓄力：变细变长，震动
            float progress = stateTimer / PREPARE_TIME;
            targetScaleX = 1.0f - (0.25f * progress);
            targetScaleY = 1.0f + (0.25f * progress);

            float shakePower = 0.05f * progress * GameConstants.CELL_SIZE;
            shakeOffsetX = MathUtils.random(-shakePower, shakePower);
            shakeOffsetY = MathUtils.random(-shakePower, shakePower);
        }

        // 平滑插值恢复形状
        float speed = 10f;
        scaleX += (targetScaleX - scaleX) * speed * delta;
        scaleY += (targetScaleY - scaleY) * speed * delta;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;
        if (needsTextureUpdate) updateTexture();

        float drawX = x * GameConstants.CELL_SIZE;
        float drawY = y * GameConstants.CELL_SIZE;
        float centerOffset = GameConstants.CELL_SIZE / 2f;

        // ⭐ 删除了之前的 batch.setColor 变红逻辑，只保留默认颜色
        // 如果想要一点点受击反馈，可以在 takeDamage 里处理，这里保持纯净

        if (texture != null) {
            batch.draw(
                    texture,
                    drawX + shakeOffsetX,
                    drawY + shakeOffsetY,
                    centerOffset, centerOffset,
                    GameConstants.CELL_SIZE, GameConstants.CELL_SIZE,
                    scaleX, scaleY,
                    0,
                    0, 0,
                    texture.getWidth(), texture.getHeight(),
                    false, false
            );
        }
    }

    @Override
    protected void updateTexture() {
        texture = textureManager.getEnemy1Texture();
        needsTextureUpdate = false;
    }

    private float distanceTo(Player p) {
        float dx = p.getX() - x;
        float dy = p.getY() - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}