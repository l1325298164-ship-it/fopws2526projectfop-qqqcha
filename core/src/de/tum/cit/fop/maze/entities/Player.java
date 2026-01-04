package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.abilities.AbilityManager;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.effects.Player.PlayerTrailManager;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
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

    // ===== Buffs =====
    private boolean buffAttack = false;
    private boolean buffRegen = false;
    private boolean buffManaEfficiency = false;
    private float regenTimer = 0f;
    private String notificationMessage = "";
    private float notificationTimer = 0f;

    // ===== Dash =====
    private boolean dashInvincible = false;
    private float dashInvincibleTimer = 0f;
    private boolean dashSpeedBoost = false;
    private float dashSpeedTimer = 0f;
    public static final float DASH_DURATION = 0.8f;
    public static final float DASH_SPEED_MULTIPLIER = 0.4f;
    private boolean dashJustEnded = false;

    // ===== 特效 =====
    private PlayerTrailManager trailManager;

    // ===== 动画 =====
    public enum Direction { UP, DOWN, LEFT, RIGHT }
    private Direction direction = Direction.DOWN;
    private TextureAtlas frontAtlas, backAtlas, leftAtlas, rightAtlas;
    private Animation<TextureRegion> frontAnim, backAnim, leftAnim, rightAnim;
    private float stateTime = 0f;
    private boolean isMovingAnim = false;

    // ===== 状态效果 =====
    private boolean slowed = false;
    private float slowTimer = 0f;

    // 🔥🔥 [关键] 分数系统
    private int score = 0;

    public Player(int x, int y, GameManager gameManager) {
        super(x, y);
        this.lives = 100000;
        this.maxLives = 100000;

        frontAtlas = new TextureAtlas("player/front.atlas");
        backAtlas  = new TextureAtlas("player/back.atlas");
        leftAtlas  = new TextureAtlas("player/left.atlas");
        rightAtlas = new TextureAtlas("player/right.atlas");

        frontAnim = new Animation<>(0.4f, frontAtlas.getRegions(), Animation.PlayMode.LOOP);
        backAnim  = new Animation<>(0.4f, backAtlas.getRegions(), Animation.PlayMode.LOOP);
        leftAnim  = new Animation<>(0.4f, leftAtlas.getRegions(), Animation.PlayMode.LOOP);
        rightAnim = new Animation<>(0.4f, rightAtlas.getRegions(), Animation.PlayMode.LOOP);

        abilityManager = new AbilityManager(this, gameManager);
        trailManager = new PlayerTrailManager();

        Logger.gameEvent("Player spawned at " + getPositionString());
    }

    public void update(float delta) {
        float animationSpeed = 1f / getMoveDelayMultiplier();
        stateTime += delta * animationSpeed;

        if (!isMovingAnim) stateTime = 0f;
        isMovingAnim = false;

        Animation<TextureRegion> currentAnim = switch (direction) {
            case UP -> backAnim;
            case LEFT -> leftAnim;
            case RIGHT -> rightAnim;
            default -> frontAnim;
        };
        TextureRegion currentFrame = currentAnim.getKeyFrame(stateTime, true);

        if (trailManager != null) {
            trailManager.update(delta, this.x, this.y, dashSpeedBoost, currentFrame);
        }

        if (isInvincible) {
            invincibleTimer += delta;
            if (invincibleTimer >= GameConstants.INVINCIBLE_TIME) {
                isInvincible = false;
                invincibleTimer = 0f;
            }
        }

        if (dashInvincible) {
            dashInvincibleTimer += delta;
            if (dashInvincibleTimer >= DASH_DURATION) {
                dashInvincible = false;
                dashInvincibleTimer = 0f;
                dashJustEnded = true;
            }
        }

        if (dashSpeedBoost) {
            dashSpeedTimer += delta;
            if (dashSpeedTimer >= DASH_DURATION) {
                dashSpeedBoost = false;
                dashSpeedTimer = 0f;
            }
        }

        if (slowed) {
            slowTimer -= delta;
            if (slowTimer <= 0f) {
                slowed = false;
                slowTimer = 0f;
            }
        }

        if (moving) {
            moveTimer += delta;
            if (moveTimer >= MOVE_COOLDOWN) {
                moving = false;
            }
        }

        if (mana < maxMana) {
            mana += manaRegenRate * delta;
            if (mana > maxMana) mana = maxMana;
        }

        abilityManager.update(delta);

        if (buffRegen) {
            regenTimer += delta;
            if (regenTimer >= 5.0f) {
                heal(5);
                regenTimer = 0f;
            }
        }

        if (notificationTimer > 0) {
            notificationTimer -= delta;
            if (notificationTimer <= 0) {
                notificationMessage = "";
            }
        }
        dashJustEnded = false;
    }

    // ===== 分数管理 =====
    public void addScore(int amount) {
        this.score += amount;
        Logger.debug("Score added: " + amount + ", Total: " + score);
    }

    public int getScore() { return score; }

    // 用于读档恢复分数
    public void setScore(int score) { this.score = score; }

    // ===== 移动与战斗 =====
    public boolean useMana(int manaCost) {
        if (buffManaEfficiency) {
            manaCost = manaCost / 2;
            if (manaCost < 1) manaCost = 1;
        }
        if (mana < manaCost) return false;
        mana -= manaCost;
        return true;
    }

    public void useAbility(int slot) {
        if (isDead() || abilityManager == null) return;
        abilityManager.activateSlot(slot);
    }

    public void startDash() {
        dashInvincible = true;
        dashSpeedBoost = true;
        dashInvincibleTimer = 0f;
        dashSpeedTimer = 0f;
    }

    public boolean isDashInvincible() { return dashInvincible; }
    public boolean isDashing() { return dashInvincible; }
    public boolean didDashJustEnd() { return dashJustEnded; }

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
    }

    public void applySlow(float duration) {
        slowed = true;
        slowTimer = Math.max(slowTimer, duration);
    }

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

    public void heal(int amount) {
        if (isDead) return;
        this.lives += amount;
        if (this.lives > this.maxLives) this.lives = this.maxLives;
    }

    public void increaseMaxLives(int amount) {
        this.maxLives += amount;
        this.lives += amount;
    }

    // ===== 渲染与 Getter =====
    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active || isDead) return;
        if (trailManager != null) trailManager.render(batch);

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
        float drawX = x * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f - drawW / 2f;
        float drawY = y * GameConstants.CELL_SIZE;

        if ((isInvincible || dashInvincible) && invincibleTimer % 0.2f > 0.1f) {
            batch.setColor(1, 1, 1, 0.6f);
        }
        batch.draw(frame, drawX, drawY, drawW, drawH);
        batch.setColor(1, 1, 1, 1f);
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {}

    @Override
    public RenderType getRenderType() { return RenderType.SPRITE; }

    // Getters & Setters
    public AbilityManager getAbilityManager() { return abilityManager; }
    public int getLives() { return lives; }
    public int getMaxLives() { return maxLives; }
    public int getMana() { return mana; }
    public boolean hasKey() { return hasKey; }
    public void setHasKey(boolean hasKey) { this.hasKey = hasKey; }
    public boolean isDead() { return isDead; }
    public boolean isMoving() { return moving; }
    public String getPositionString() { return "(" + x + ", " + y + ")"; }
    public Direction getDirection() { return direction; }

    // 重置
    public void reset() {
        this.lives = 100000;
        this.maxLives = 100000;
        this.isDead = false;
        this.hasKey = false;
        this.isInvincible = true;
        this.invincibleTimer = 0f;
        this.dashInvincible = false;
        this.dashInvincibleTimer = 0f;
        this.dashSpeedBoost = false;
        this.dashSpeedTimer = 0f;
        this.moving = false;
        this.moveTimer = 0f;
        this.slowed = false;
        this.slowTimer = 0f;
        this.mana = maxMana;
        this.score = 0; // 重置分数
        this.buffAttack = false;
        this.buffRegen = false;
        this.buffManaEfficiency = false;
        this.regenTimer = 0f;
        this.notificationMessage = "";
        if (abilityManager != null) abilityManager.reset();
        if (trailManager != null) trailManager.dispose();
    }

    // Buff API
    public void activateAttackBuff() {
        if (!buffAttack) {
            buffAttack = true;
            showNotification("Buff: ATK +50%!");
        }
    }
    public void activateRegenBuff() {
        if (!buffRegen) {
            buffRegen = true;
            showNotification("Buff: Auto-Regen!");
        }
    }
    public void activateManaBuff() {
        if (!buffManaEfficiency) {
            buffManaEfficiency = true;
            showNotification("Buff: Mana Saver!");
        }
    }
    public void showNotification(String msg) {
        this.notificationMessage = msg;
        this.notificationTimer = 3.0f;
    }
    public boolean hasBuffAttack() { return buffAttack; }
    public boolean hasBuffRegen() { return buffRegen; }
    public boolean hasBuffManaEfficiency() { return buffManaEfficiency; }
    public String getNotificationMessage() { return notificationMessage; }
    public float getDamageMultiplier() { return buffAttack ? 1.5f : 1.0f; }

    // 读档 Setter
    public void setHealthStatus(int currentLives, int maxLives) {
        this.maxLives = maxLives;
        this.lives = currentLives;
    }
    public void setMana(int mana) { this.mana = mana; }
    public void setBuffs(boolean attack, boolean regen, boolean manaEfficiency) {
        this.buffAttack = attack;
        this.buffRegen = regen;
        this.buffManaEfficiency = manaEfficiency;
    }
}