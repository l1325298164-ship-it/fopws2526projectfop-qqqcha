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
    private static final float VISUAL_SCALE = 2.9f; // ⭐ 1.2 ~ 1.6 都很舒服
    private static final float ANIM_SPEED_MULTIPLIER = 0.55f; // ⭐ 0.45 ~ 0.65 最舒服
//move
// ===== 连续移动坐标 =====
private float worldX;
    private float worldY;

    private float targetX;
    private float targetY;

    private boolean isMovingContinuous = false;


    private boolean hasKey = false;
    private int lives;
    private int maxLives;

    private boolean isDead = false;
//判定效果重新设计
// ===== 受伤无敌（i-frame）=====
private boolean damageInvincible = false;
    private float damageInvincibleTimer = 0f;
    private static final float DAMAGE_INVINCIBLE_TIME = 0.6f;

    // ===== 受击闪烁（仅视觉）=====
    private boolean hitFlash = false;
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_TIME = 0.25f;

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
    public boolean onPushedBy(PushSource source, int dx, int dy, GameManager gm) {

        int strength = source.getPushStrength();

        int targetX = x + dx * strength;
        int targetY = y + dy * strength;

        if (!gm.canPlayerMoveTo(targetX, targetY)) {
            // 推不动：可以选择受伤 / 硬直 / 死亡
            takeDamage(1);//推不动扣血 移动墙扣血
            return false;
        }

        setPosition(targetX, targetY);
        enterHitStun(0.1f);

        return true;
    }
    @Override
    public void setPosition(int x, int y) {
        super.setPosition(x, y);
        this.worldX = x;
        this.worldY = y;
        this.targetX = x;
        this.targetY = y;
        this.isMovingContinuous = false;
    }


    private float hitStunTimer = 0f;
    private boolean inHitStun = false;

    private void enterHitStun(float duration) {
        inHitStun = true;
        hitStunTimer = duration;
    }

    public boolean didDashJustEnd() {
        return dashJustEnded;
    }

    public void addScore(int i) {
        score+=i;

    }

    public int getScore() {
        return this.score;
    }

    public float getWorldX() {
        return worldX;
    }

    public float getWorldY() {
        return worldY;
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
          this.lives = 100000;
          this.maxLives = 100000;
        this.worldX = x;
        this.worldY = y;
        this.targetX = x;
        this.targetY = y;


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
        if (inHitStun) {
            hitStunTimer -= delta;
            if (hitStunTimer <= 0f) {
                inHitStun = false;
            }
            return; // ⛔ 本帧不处理移动 / 能力
        }
        // ===== 动画 =====
        float animationSpeed = 1f / getMoveDelayMultiplier();
        stateTime += delta * animationSpeed * ANIM_SPEED_MULTIPLIER;

        if (!isMovingAnim) stateTime = 0f;
        isMovingAnim = false;

        // ===== 无敌 =====
        // 1️⃣ 受伤无敌（i-frame）
        if (damageInvincible) {
            damageInvincibleTimer += delta;
            if (damageInvincibleTimer >= DAMAGE_INVINCIBLE_TIME) {
                damageInvincible = false;
                damageInvincibleTimer = 0f;
            }
        }

// 2️⃣ 受击闪烁（纯视觉）
        if (hitFlash) {
            hitFlashTimer += delta;
            if (hitFlashTimer >= HIT_FLASH_TIME) {
                hitFlash = false;
                hitFlashTimer = 0f;
            }
        }

// 3️⃣ Dash 无敌（技能）
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
//连续移动
        if (isMovingContinuous) {
            float dx = targetX - worldX;
            float dy = targetY - worldY;
            float distSq = dx * dx + dy * dy;

            if (distSq < 0.0001f) {
                // 到达目标，强制对齐
                worldX = targetX;
                worldY = targetY;
                x = (int) targetX;
                y = (int) targetY;
                isMovingContinuous = false;
            } else {
                float dist = (float) Math.sqrt(distSq);
                // 根据当前的移动延迟倍率计算速度（加速/减速会影响滑动感）
                float currentMoveDelay = MOVE_COOLDOWN * getMoveDelayMultiplier();
                float speed = 1f / currentMoveDelay;
                float step = speed * delta;

                if (step >= dist) {
                    worldX = targetX;
                    worldY = targetY;
                    x = (int) targetX;
                    y = (int) targetY;
                    isMovingContinuous = false;
                } else {
                    worldX += (dx / dist) * step;
                    worldY += (dy / dist) * step;
                }
            }
        }

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
        if (isDead || isMovingContinuous) return;

        int nx = x + dx;
        int ny = y + dy;
        if (dx > 0) direction = Direction.RIGHT;
        else if (dx < 0) direction = Direction.LEFT;
        else if (dy > 0) direction = Direction.UP;
        else if (dy < 0) direction = Direction.DOWN;

        isMovingAnim = true;
        moving = true;
        moveTimer = 0f;

        targetX = nx;
        targetY = ny;
        isMovingContinuous = true;

        Logger.debug("Player start move to (" + targetX + "," + targetY + ")");

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
        if (isDead || damageInvincible || dashInvincible) return;
        if (damage <= 0) return;
        lives -= damage;
        AudioManager.getInstance().play(AudioType.PLAYER_ATTACKED);

        // ⭐ 受伤无敌（防秒杀）
        damageInvincible = true;
        damageInvincibleTimer = 0f;

        // ⭐ 受击闪烁（视觉）
        hitFlash = true;
        hitFlashTimer = 0f;

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

        float baseScale = (float) GameConstants.CELL_SIZE / frame.getRegionHeight();
        float scale = baseScale * VISUAL_SCALE;

        float drawW = frame.getRegionWidth() * scale;
        float drawH = frame.getRegionHeight() * scale;

        float drawX = worldX * GameConstants.CELL_SIZE
                + GameConstants.CELL_SIZE / 2f - drawW / 2f;
        float drawY = worldY * GameConstants.CELL_SIZE;

        if (hitFlash && hitFlashTimer % 0.1f > 0.05f) {
            batch.setColor(1f, 1f, 1f, 0.6f);
        } else if (dashInvincible && dashInvincibleTimer % 0.1f > 0.05f) {
            // Dash 无敌闪烁（可选不同风格）
            batch.setColor(0.8f, 0.9f, 1f, 0.7f);
        } else {
            batch.setColor(1f, 1f, 1f, 1f);
        }

        // ⭐⭐⭐ 真正画出来的关键一行 ⭐⭐⭐
        batch.draw(frame, drawX, drawY, drawW, drawH);

        // 重置颜色（防止污染后续渲染）
        batch.setColor(1f, 1f, 1f, 1f);

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




    public float getMoveSpeed() {
        // MOVE_COOLDOWN 表示「走一格需要多少秒」
        // 所以速度 = 1 / cooldown 防止除数为0
        return Math.max(0.01f, 1f / MOVE_COOLDOWN);
    }


}
