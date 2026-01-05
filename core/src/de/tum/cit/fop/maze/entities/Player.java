package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.abilities.AbilityManager;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.utils.Logger;

public class Player extends GameObject {


    private boolean hasKey = false;
    private int lives;
    private int maxLives;
    private float invincibleTimer = 0;
    private boolean isInvincible = false;

    private boolean isDead = false;

    // ===== 移动 =====
    private boolean moving = false;
    private float moveTimer = 0f;
    private static final float MOVE_COOLDOWN = 0.15f;

    // ===== Ability System =====
    private AbilityManager abilityManager;

    // ===== Mana =====
    private int mana = 100000;
    private int maxMana = 100000;
    private float manaRegenRate = 5.0f;

    // ==========================================
    // 🔥 [Treasure] 新增：三种唯一 Buff 状态
    // ==========================================
    private boolean buffAttack = false;         // 1. 攻击力 +50%
    private boolean buffRegen = false;          // 2. 每5秒回5血
    private boolean buffManaEfficiency = false; // 3. 耗蓝减半

    // 🔥 [Treasure] 辅助变量
    private float regenTimer = 0f;           // 回血计时器
    private String notificationMessage = ""; // 屏幕飘字内容
    private float notificationTimer = 0f;    // 飘字持续时间

    /* =======================================================
       ====================== DASH ===========================
       ======================================================= */

    private boolean dashInvincible = false;
    private float dashInvincibleTimer = 0f;

    private boolean dashSpeedBoost = false;
    private float dashSpeedTimer = 0f;

    public static final float DASH_DURATION = 0.8f;
    public static final float DASH_SPEED_MULTIPLIER = 0.4f; // delay * 0.4 = 更快

    public boolean useMana(int manaCost) {
        if (buffManaEfficiency) {
            manaCost = manaCost / 2;
            if (manaCost < 1) manaCost = 1;
        }

        if (mana < manaCost) {
            return false;
        }
        mana -= manaCost;
        return true;
    }

    public void useAbility(int slot) {
        if (isDead() || abilityManager == null) return;

        Logger.debug("Player.useAbility(" + slot + ") called");

        // 🔥 直接调用 AbilityManager.activateSlot
        boolean success = abilityManager.activateSlot(slot);

        if (success) {
            Logger.debug("Ability activation successful");
        } else {
            Logger.debug("Ability activation failed");
        }
    }
    private boolean dashJustEnded = false;

    public boolean didDashJustEnd() {
        return dashJustEnded;
    }

    public void addScore(int i) {
        score+=i;

    }

    public int getScore() {
        return this.score;
    }




    /* ======================================================= */

    // ===== 朝向 =====
    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private Direction direction = Direction.DOWN;

    // ===== 动画 =====
    private TextureAtlas frontAtlas, backAtlas, leftAtlas, rightAtlas;
    private Animation<TextureRegion> frontAnim, backAnim, leftAnim, rightAnim;
    private float stateTime = 0f;
    private boolean isMovingAnim = false;

    // ===== 状态效果 =====
    private boolean slowed = false;
    private float slowTimer = 0f;

    // ===== 分数 =====
    private int score = 0;

    public Player(int x, int y, GameManager gameManager) {
        super(x, y);
//        this.lives = GameConstants.MAX_LIVES;
//        this.maxLives = GameConstants.MAX_LIVES;
        this.lives = 200;
        this.maxLives = 200;

        frontAtlas = new TextureAtlas("player/front.atlas");
        backAtlas  = new TextureAtlas("player/back.atlas");
        leftAtlas  = new TextureAtlas("player/left.atlas");
        rightAtlas = new TextureAtlas("player/right.atlas");

        frontAnim = new Animation<>(0.4f, frontAtlas.getRegions(), Animation.PlayMode.LOOP);
        backAnim  = new Animation<>(0.4f, backAtlas.getRegions(), Animation.PlayMode.LOOP);
        leftAnim  = new Animation<>(0.4f, leftAtlas.getRegions(), Animation.PlayMode.LOOP);
        rightAnim = new Animation<>(0.4f, rightAtlas.getRegions(), Animation.PlayMode.LOOP);

        abilityManager = new AbilityManager(this, gameManager);

        Logger.gameEvent("Player spawned at " + getPositionString());
    }

    /* ====================== UPDATE ====================== */


    public void update(float delta) {

        // ===== 动画 =====
        float animationSpeed = 1f / getMoveDelayMultiplier();
        stateTime += delta * animationSpeed;

        if (!isMovingAnim) stateTime = 0f;
        isMovingAnim = false;

        // ===== 普通无敌 =====
        if (isInvincible) {
            invincibleTimer += delta;
            if (invincibleTimer >= GameConstants.INVINCIBLE_TIME) {
                isInvincible = false;
                invincibleTimer = 0f;
            }
        }

        // ===== Dash 无敌 =====
        if (dashInvincible) {
            dashInvincibleTimer += delta;
            if (dashInvincibleTimer >= DASH_DURATION) {
                dashInvincible = false;
                dashInvincibleTimer = 0f;
                dashJustEnded = true;
            }
        }

        // ===== Dash 加速 =====
        if (dashSpeedBoost) {
            dashSpeedTimer += delta;
            if (dashSpeedTimer >= DASH_DURATION) {
                dashSpeedBoost = false;
                dashSpeedTimer = 0f;
            }
        }

        // ===== 减速 =====
        if (slowed) {
            slowTimer -= delta;
            if (slowTimer <= 0f) {
                slowed = false;
                slowTimer = 0f;
            }
        }

        // ===== 移动冷却 =====
        if (moving) {
            moveTimer += delta;
            if (moveTimer >= MOVE_COOLDOWN) {
                moving = false;
            }
        }

        // ===== Mana 恢复 =====
        if (mana < maxMana) {
            mana += manaRegenRate * delta;
            if (mana > maxMana) mana = maxMana;
        }

        // ===== Ability =====
        abilityManager.update(delta);

        // ===== [Treasure] 自动回血逻辑 =====
        if (buffRegen) {
            regenTimer += delta;
            if (regenTimer >= 5.0f) { // 每 5 秒
                heal(5); // 回 5 点血
                regenTimer = 0f;
            }
        }

        // ===== [Treasure] UI通知倒计时 =====
        if (notificationTimer > 0) {
            notificationTimer -= delta;
            if (notificationTimer <= 0) {
                notificationMessage = ""; // 时间到，清空消息
            }
        }

        dashJustEnded = false;
    }

    /* ====================== DASH API（给 Ability 调）====================== */

    public void startDash() {
        dashInvincible = true;
        dashSpeedBoost = true;
        dashInvincibleTimer = 0f;
        dashSpeedTimer = 0f;

        Logger.debug("Dash started");
    }

    public boolean isDashInvincible() {
        return dashInvincible;
    }

    /* ====================== 移动相关 ====================== */

    public float getMoveDelayMultiplier() {
        float multiplier = 1f;

        if (slowed) multiplier *= 2.0f;
        if (dashSpeedBoost) multiplier *= DASH_SPEED_MULTIPLIER;

        return multiplier;
    }

    public void move(int dx, int dy) {
        if (isDead) return;

        if (dx > 0) direction = Direction.RIGHT;
        else if (dx < 0) direction = Direction.LEFT;
        else if (dy > 0) direction = Direction.UP;
        else if (dy < 0) direction = Direction.DOWN;

        isMovingAnim = true;
        moving = true;
        moveTimer = 0f;

        x += dx;
        y += dy;

        Logger.debug("Player moved to " + getPositionString());
    }


    /* ====================== 状态效果 ====================== */

    /**
     * 对玩家施加减速效果
     * 不叠加倍率，但会刷新持续时间
     */
    public void applySlow(float duration) {
        slowed = true;
        slowTimer = Math.max(slowTimer, duration);
    }
    /* ====================== 受伤 ====================== */

    public void takeDamage(int damage) {
        if (isDead || isInvincible || dashInvincible) return;

        lives -= damage;

        AudioManager.getInstance().play(AudioType.PLAYER_ATTACKED);
        isInvincible = true;
        invincibleTimer = 0f;

        if (lives <= 0) {
            isDead = true;
            Logger.gameEvent("Player died");
        }
    }
    // 🔥 新增：回复生命值 (对应 Heart / 柠檬脆波波)
    public void heal(int amount) {
        if (isDead) return;

        this.lives += amount;
        // 限制回血不能超过当前的上限
        if (this.lives > this.maxLives) {
            this.lives = this.maxLives;
        }
        Logger.gameEvent("Player healed by " + amount + ". Current HP: " + lives + "/" + maxLives);
    }

    // 🔥 新增：增加生命上限 (对应 HeartContainer / 焦糖核心)
    public void increaseMaxLives(int amount) {
        this.maxLives += amount;
        // 增加上限的同时，顺便把增加的那部分血补上
        this.lives += amount;

        Logger.gameEvent("Max HP increased by " + amount + ". New Max: " + maxLives);
    }

    // 🔥 新增：获取最大生命值 (UI可能需要用到)
    public int getMaxLives() {
        return maxLives;
    }

    /* ====================== 渲染 ====================== */

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active || isDead) return;

        Animation<TextureRegion> anim = switch (direction) {
            case UP -> backAnim;
            case LEFT -> leftAnim;
            case RIGHT -> rightAnim;
            default -> frontAnim;
        };

        TextureRegion frame = anim.getKeyFrame(stateTime, true);

        float scale = (float) GameConstants.CELL_SIZE / frame.getRegionHeight();
        float drawW = frame.getRegionWidth() * scale + 10;
        float drawH = GameConstants.CELL_SIZE + 10;

        float drawX = x * GameConstants.CELL_SIZE
                + GameConstants.CELL_SIZE / 2f - drawW / 2f;
        float drawY = y * GameConstants.CELL_SIZE;
//TODO 需要把受伤和无敌分开，现在受伤会更新无敌，防止帧数太高被杀掉了
        if ((isInvincible || dashInvincible) && invincibleTimer % 0.2f > 0.1f) {
            batch.setColor(1, 1, 1, 0.6f);
        }

        batch.draw(frame, drawX, drawY, drawW, drawH);
        batch.setColor(1, 1, 1, 1f);
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {}

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    /* ====================== Getter ====================== */

    public AbilityManager getAbilityManager() { return abilityManager; }
    public int getLives() { return lives; }
    public boolean hasKey() { return hasKey; }
    public void setHasKey(boolean hasKey) { this.hasKey = hasKey; }
    public boolean isDead() { return isDead; }
    public int getMana() {
        return mana;
    }
    public boolean isMoving() {
        return moving;
    }

    /**
     * 重置玩家状态
     */
    /**
     * 重置玩家状态（重开关卡 / 重新开始游戏）
     */
    public void reset() {

        // ===== 基础生命 =====
//        this.lives = GameConstants.MAX_LIVES;
//        this.maxLives = GameConstants.MAX_LIVES;
        this.lives = 100000;
        this.maxLives = 100000;

        this.isDead = false;

        // ===== 钥匙 =====
        this.hasKey = false;

        // ===== 无敌状态 =====
        this.isInvincible = true;
        this.invincibleTimer = 0f;

        // ===== Dash 状态 =====
        this.dashInvincible = false;
        this.dashInvincibleTimer = 0f;

        this.dashSpeedBoost = false;
        this.dashSpeedTimer = 0f;

        this.dashJustEnded = false;

        // ===== 移动状态 =====
        this.moving = false;
        this.moveTimer = 0f;

        // ===== 状态效果 =====
        this.slowed = false;
        this.slowTimer = 0f;

        // ===== 资源 =====
        this.mana = maxMana;
        this.score = 0;

        // 🔥 [Treasure] 重置 Buff
        this.buffAttack = false;
        this.buffRegen = false;
        this.buffManaEfficiency = false;
        this.regenTimer = 0f;
        this.notificationMessage = "";

        // ===== 能力系统 =====
        if (abilityManager != null) {
            abilityManager.reset();
        }

        Logger.debug(
                "Player reset complete | HP=" + lives + "/" + maxLives +
                        ", Mana=" + mana +
                        ", Key=" + hasKey
        );
    }

    public String getPositionString() {
        return "(" + x + ", " + y + ")";
    }
    public Direction getDirection() {
        return direction;
    }


    public boolean isDashing(){
        return dashInvincible;
    }// 现在 Dash 的唯一真状态

    /* ================= [Treasure] Buff API ================= */

    // 1. 激活攻击 Buff (Treasure调用)
    public void activateAttackBuff() {
        if (!buffAttack) {
            buffAttack = true;
            showNotification("Buff Acquired: ATK +50%!");
            Logger.gameEvent("acquire ATK Buff");
        }
    }

    // 2. 激活回血 Buff (Treasure调用)
    public void activateRegenBuff() {
        if (!buffRegen) {
            buffRegen = true;
            showNotification("Buff Acquired: Auto-Regen!");
            Logger.gameEvent("acquire HP Buff");
        }
    }

    // 3. 激活耗蓝 Buff (Treasure调用)
    public void activateManaBuff() {
        if (!buffManaEfficiency) {
            buffManaEfficiency = true;
            showNotification("Buff Acquired: Mana Saver (-50% Cost)!");
            Logger.gameEvent("acquire Mana Buff");
        }
    }

    // 显示屏幕通知
    public void showNotification(String msg) {
        this.notificationMessage = msg;
        this.notificationTimer = 3.0f; // 显示3秒
    }

    // Getters (HUD调用)
    public boolean hasBuffAttack() { return buffAttack; }
    public boolean hasBuffRegen() { return buffRegen; }
    public boolean hasBuffManaEfficiency() { return buffManaEfficiency; }
    public String getNotificationMessage() { return notificationMessage; }

    // 🔥 供 AbilityManager 计算伤害时调用
    public float getDamageMultiplier() {
        return buffAttack ? 1.5f : 1.0f;
    }

}