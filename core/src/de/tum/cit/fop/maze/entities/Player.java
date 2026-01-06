package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import de.tum.cit.fop.maze.abilities.AbilityManager;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.LinkedList;
import java.util.Queue;

public class Player extends GameObject {
    private float speed = 200f;
    private int score = 0;
    private int lives = 3;
    private int maxLives = 3;
    private int mana = 100;
    private boolean hasKey = false;

    // 状态机变量
    private boolean isMoving = false;
    private float stateTime = 0f;
    private Direction direction = Direction.DOWN;
    private GameManager gameManager;
    private AbilityManager abilityManager;

    // Buffs
    private boolean buffAttack = false;
    private boolean buffRegen = false;
    private boolean buffManaEfficiency = false;

    // Dash / Invincible state
    private boolean isDashInvincible = false;
    private boolean isInvincible = false;
    private float invincibleTimer = 0f;
    private static final float INVINCIBLE_TIME_ON_HIT = 1.0f;

    // Dash Logic
    private boolean isDashing = false;
    private float dashTimer = 0f;
    private final float DASH_DURATION = 0.2f;
    private final float DASH_SPEED_MULTIPLIER = 2.5f;

    // 🔥 [Phase 4 New] 通知队列 (用于 HUD 显示成就)
    private Queue<String> notificationQueue = new LinkedList<>();

    public enum Direction { UP, DOWN, LEFT, RIGHT }

    public Player(float x, float y, GameManager gameManager) {
        super(x, y, GameObject.RenderType.PLAYER);
        this.gameManager = gameManager;
        this.abilityManager = new AbilityManager(this, gameManager);
        TextureManager.loadPlayerAnimations();
    }

    public void update(float delta) {
        stateTime += delta;
        abilityManager.update(delta);

        // 无敌闪烁更新
        if (isInvincible) {
            invincibleTimer -= delta;
            if (invincibleTimer <= 0) {
                isInvincible = false;
            }
        }

        // 冲刺状态更新
        if (isDashing) {
            dashTimer -= delta;
            if (dashTimer <= 0) {
                isDashing = false;
                isDashInvincible = false;
            }
        }
    }

    public void draw(SpriteBatch batch) {
        drawSprite(batch, this.x, this.y);
    }

    private void drawSprite(SpriteBatch batch, float x, float y) {
        TextureRegion currentFrame = TextureManager.getPlayerFrame(direction, isMoving, stateTime);

        // 受伤闪烁效果
        if (isInvincible && !isDashing) {
            if (((int)(invincibleTimer * 20)) % 2 == 0) {
                batch.setColor(1, 1, 1, 0.5f);
            }
        }
        // 冲刺半透明
        if (isDashing) {
            batch.setColor(1, 1, 1, 0.6f);
        }

        float drawScale = 1.0f;
        float width = GameConstants.CELL_SIZE * drawScale;
        float height = GameConstants.CELL_SIZE * drawScale;

        if (currentFrame != null) {
            batch.draw(currentFrame,
                    x * GameConstants.CELL_SIZE,
                    y * GameConstants.CELL_SIZE,
                    width, height);
        }

        // 重置颜色
        batch.setColor(1, 1, 1, 1);
    }

    public void move(int dx, int dy) {
        this.x += dx;
        this.y += dy;
        isMoving = true;

        if (dx > 0) direction = Direction.RIGHT;
        if (dx < 0) direction = Direction.LEFT;
        if (dy > 0) direction = Direction.UP;
        if (dy < 0) direction = Direction.DOWN;
    }

    public void useAbility(int slot) {
        abilityManager.useAbility(slot);
    }

    public void takeDamage(int damage) {
        if (isInvincible || isDashInvincible) return;

        this.lives -= damage;
        if (this.lives < 0) this.lives = 0;

        // 受伤无敌时间
        this.isInvincible = true;
        this.invincibleTimer = INVINCIBLE_TIME_ON_HIT;
    }

    public void startDash() {
        if (!isDashing) {
            isDashing = true;
            isDashInvincible = true;
            dashTimer = DASH_DURATION;
        }
    }

    // ==========================================
    // 🔥 [Phase 4 New] 通知系统方法
    // ==========================================

    public void showNotification(String message) {
        notificationQueue.add(message);
    }

    public String pollNotification() {
        return notificationQueue.poll();
    }

    public boolean hasNotifications() {
        return !notificationQueue.isEmpty();
    }

    // Getters & Setters
    public int getScore() { return score; }
    public void addScore(int points) { this.score += points; }
    public void setScore(int score) { this.score = score; }

    public int getLives() { return lives; }
    public int getMaxLives() { return maxLives; }
    public void setHealthStatus(int lives, int maxLives) { this.lives = lives; this.maxLives = maxLives; }

    public int getMana() { return mana; }
    public void setMana(int mana) { this.mana = mana; }

    public boolean hasKey() { return hasKey; }
    public void setHasKey(boolean hasKey) { this.hasKey = hasKey; }

    public void setBuffs(boolean atk, boolean reg, boolean mana) {
        this.buffAttack = atk;
        this.buffRegen = reg;
        this.buffManaEfficiency = mana;
    }

    public boolean isDead() { return lives <= 0; }
    public Direction getDirection() { return direction; }
    public boolean isDashing() { return isDashing; }
    public boolean isDashInvincible() { return isDashInvincible; }
    public boolean isInvincible() { return isInvincible; }

    public void reset() {
        lives = maxLives;
        mana = 100;
        score = 0;
        isInvincible = false;
        isDashing = false;
        notificationQueue.clear();
    }
}