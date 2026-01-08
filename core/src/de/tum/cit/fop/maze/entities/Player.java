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
    protected boolean isTutorial = false;

    // 残影管理器
    private PlayerTrailManager trailManager;

    public Player(int x, int y) {
        super(x, y);
        initDefaults(x, y);
        loadPlayer1Animations();
        this.playerIndex = PlayerIndex.P1;
        this.isTutorial = true;
        this.abilityManager = null;
    }

    public Player(int x, int y, GameManager gameManager, PlayerIndex index) {
        super(x, y);
        initDefaults(x, y);
        this.playerIndex = index;

        if (playerIndex == PlayerIndex.P2) {
            loadPlayer2Animations();
        } else {
            loadPlayer1Animations();
        }

        abilityManager = new AbilityManager(this, gameManager);
        Logger.gameEvent("Player spawned at " + getPositionString());

        loadAttackAnimations(index);
    }

    private void initDefaults(int x, int y) {
        this.worldX = x;
        this.worldY = y;
        this.targetX = x;
        this.targetY = y;
        this.lives = 200;
        this.maxLives = 200;
        this.trailManager = new PlayerTrailManager();
    }

    public enum PlayerIndex {
        P1, P2
    }
    private PlayerIndex playerIndex;
    public PlayerIndex getPlayerIndex() { return playerIndex; }

    private static final float VISUAL_SCALE = 2.9f;
    private static final float ANIM_SPEED_MULTIPLIER = 0.15f;

    // ===== 坐标 =====
    private float worldX;
    private float worldY;
    private float targetX;
    private float targetY;
    private boolean isMovingContinuous = false;

    private boolean hasKey = false;
    private int lives;
    private int maxLives;

    private boolean isDead = false;

    // ===== 受伤无敌 =====
    private boolean damageInvincible = false;
    private float damageInvincibleTimer = 0f;
    private static final float DAMAGE_INVINCIBLE_TIME = 0.6f;

    // ===== 受击闪烁 =====
    private boolean hitFlash = false;
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_TIME = 0.25f;

    // ===== 移动 =====
    private boolean moving = false;
    private float moveTimer = 0f;
    private static final float MOVE_COOLDOWN = 0.15f;

    // 动画资源
    private TextureAtlas frontAtkAtlas, backAtkAtlas, leftAtkAtlas, rightAtkAtlas;
    private Animation<TextureRegion> frontAtkAnim, backAtkAnim, leftAtkAnim, rightAtkAnim;

    // 攻击状态控制
    private boolean isAttacking = false;
    private float attackAnimTimer = 0f;
    private static final float ATTACK_DURATION = 0.4f;

    // ===== Ability System =====
    private AbilityManager abilityManager;

    // ===== Mana =====
    private float mana = 100;
    private float maxMana = 100;
    private float manaRegenRate = 30.0f;

    // ==========================================
    // 🔥 [Treasure] Buffs
    // ==========================================
    private boolean buffAttack = false;
    private boolean buffRegen = false;
    private boolean buffManaEfficiency = false;
    private float regenTimer = 0f;
    private String notificationMessage = "";
    private float notificationTimer = 0f;

    /* ================= DASH ================= */
    private boolean dashInvincible = false;
    private float dashInvincibleTimer = 0f;
    private boolean dashSpeedBoost = false;
    private float dashSpeedTimer = 0f;
    public static final float DASH_DURATION = 0.8f;
    public static final float DASH_SPEED_MULTIPLIER = 0.4f;
    private boolean dashJustEnded = false;

    /* ================= 朝向 & 动画 ================= */
    public enum Direction { UP, DOWN, LEFT, RIGHT }
    private Direction direction = Direction.DOWN;
    private TextureAtlas frontAtlas, backAtlas, leftAtlas, rightAtlas;
    private Animation<TextureRegion> frontAnim, backAnim, leftAnim, rightAnim;
    private float stateTime = 0f;
    private boolean isMovingAnim = false;

    // ===== 状态效果 =====
    private boolean slowed = false;
    private float slowTimer = 0f;

    // ===== 分数 =====
    private int score = 0;

    private void loadPlayer1Animations() {
        frontAtlas = new TextureAtlas("Character/player1/front.atlas");
        backAtlas  = new TextureAtlas("Character/player1/back.atlas");
        leftAtlas  = new TextureAtlas("Character/player1/left.atlas");
        rightAtlas = new TextureAtlas("Character/player1/right.atlas");
        createWalkAnims(frontAtlas, backAtlas, leftAtlas, rightAtlas);
    }

    private void loadPlayer2Animations() {
        TextureAtlas atlas = new TextureAtlas("Character/player2/player2.atlas");
        frontAnim = new Animation<>(0.1f, atlas.findRegions("player2_front"), Animation.PlayMode.LOOP);
        backAnim  = new Animation<>(0.1f, atlas.findRegions("player2_back"), Animation.PlayMode.LOOP);
        leftAnim  = new Animation<>(0.1f, atlas.findRegions("player2_left"), Animation.PlayMode.LOOP);
        rightAnim = new Animation<>(0.1f, atlas.findRegions("player2_right"), Animation.PlayMode.LOOP);
    }

    private void createWalkAnims(TextureAtlas f, TextureAtlas b, TextureAtlas l, TextureAtlas r) {
        frontAnim = new Animation<>(0.1f, f.getRegions(), Animation.PlayMode.LOOP);
        backAnim  = new Animation<>(0.1f, b.getRegions(), Animation.PlayMode.LOOP);
        leftAnim  = new Animation<>(0.1f, l.getRegions(), Animation.PlayMode.LOOP);
        rightAnim = new Animation<>(0.1f, r.getRegions(), Animation.PlayMode.LOOP);
    }

    private void loadAttackAnimations(PlayerIndex index) {
        String path = (index == PlayerIndex.P1) ? "Character/melee/player1.atlas" : "Character/magic/player2.atlas";
        String prefix = (index == PlayerIndex.P1) ? "player1" : "player2";
        try {
            TextureAtlas attackAtlas = new TextureAtlas(path);
            backAtkAnim = new Animation<>(0.08f, attackAtlas.findRegions(prefix + "_back"), Animation.PlayMode.NORMAL);
            frontAtkAnim = new Animation<>(0.08f, attackAtlas.findRegions(prefix + "_front"), Animation.PlayMode.NORMAL);
            leftAtkAnim = new Animation<>(0.08f, attackAtlas.findRegions(prefix + "_left"), Animation.PlayMode.NORMAL);
            rightAtkAnim = new Animation<>(0.08f, attackAtlas.findRegions(prefix + "_right"), Animation.PlayMode.NORMAL);
        } catch (Exception e) {
            Logger.error("Failed to load attack animations for " + prefix);
        }
    }

    /* ====================== UPDATE ====================== */

    public void update(float delta) {
        if (isTutorial) {
            stateTime += delta * ANIM_SPEED_MULTIPLIER;
            if (!isMovingAnim) stateTime = 0f;
            isMovingAnim = false;
            return;
        }

        if (inHitStun) {
            hitStunTimer -= delta;
            if (hitStunTimer <= 0f) inHitStun = false;
            return;
        }

        float animationSpeed = 1f / getMoveDelayMultiplier();
        stateTime += delta * animationSpeed * ANIM_SPEED_MULTIPLIER;

        if (!isMovingAnim) stateTime = 0f;
        isMovingAnim = false;

        Animation<TextureRegion> currentAnim = getCurrentAnimation();
        TextureRegion currentFrame = currentAnim.getKeyFrame(isAttacking ? attackAnimTimer : stateTime, !isAttacking);

        if (trailManager != null) {
            trailManager.update(delta, worldX, worldY, isDashInvincible(), currentFrame);
        }

        if (isAttacking) {
            attackAnimTimer += delta;
            if (attackAnimTimer >= ATTACK_DURATION) {
                isAttacking = false;
                attackAnimTimer = 0f;
            }
        }

        updateStatusEffects(delta);
        updateMana(delta);

        if (abilityManager != null) {
            abilityManager.update(delta);
        }

        updateBuffs(delta);
        dashJustEnded = false;
        updateContinuousMovement(delta);
    }

    private void updateStatusEffects(float delta) {
        if (damageInvincible) {
            damageInvincibleTimer += delta;
            if (damageInvincibleTimer >= DAMAGE_INVINCIBLE_TIME) {
                damageInvincible = false;
                damageInvincibleTimer = 0f;
            }
        }
        if (hitFlash) {
            hitFlashTimer += delta;
            if (hitFlashTimer >= HIT_FLASH_TIME) {
                hitFlash = false;
                hitFlashTimer = 0f;
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
            if (slowTimer <= 0f) slowed = false;
        }
        if (moving) {
            moveTimer += delta;
            if (moveTimer >= MOVE_COOLDOWN) moving = false;
        }
    }

    private void updateMana(float delta) {
        if (mana < maxMana) {
            mana += manaRegenRate * delta;
            if (mana > maxMana) mana = maxMana;
        }
    }

    private void updateBuffs(float delta) {
        if (buffRegen) {
            regenTimer += delta;
            if (regenTimer >= 5.0f) {
                heal(5);
                regenTimer = 0f;
            }
        }
        if (notificationTimer > 0) {
            notificationTimer -= delta;
            if (notificationTimer <= 0) notificationMessage = "";
        }
    }

    private void updateContinuousMovement(float delta) {
        if (isMovingContinuous) {
            float dx = targetX - worldX;
            float dy = targetY - worldY;
            float distSq = dx * dx + dy * dy;

            if (distSq < 0.0001f) {
                worldX = targetX;
                worldY = targetY;
                x = (int) targetX;
                y = (int) targetY;
                isMovingContinuous = false;
            } else {
                float dist = (float) Math.sqrt(distSq);
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

    public void startDash() {
        dashInvincible = true;
        dashSpeedBoost = true;
        dashInvincibleTimer = 0f;
        dashSpeedTimer = 0f;
        Logger.debug("Dash started");
    }

    public boolean isDashInvincible() { return dashInvincible; }

    private Animation<TextureRegion> getCurrentAnimation() {
        if (isAttacking) {
            return switch (direction) {
                case UP -> backAtkAnim;
                case LEFT -> leftAtkAnim;
                case RIGHT -> rightAtkAnim;
                default -> frontAtkAnim;
            };
        } else {
            return switch (direction) {
                case UP -> backAnim;
                case LEFT -> leftAnim;
                case RIGHT -> rightAnim;
                default -> frontAnim;
            };
        }
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active || isDead) return;

        if (trailManager != null) {
            trailManager.render(batch);
        }

        Animation<TextureRegion> anim = getCurrentAnimation();
        TextureRegion frame = anim.getKeyFrame(isAttacking ? attackAnimTimer : stateTime, !isAttacking);

        float baseScale = (float) GameConstants.CELL_SIZE / frame.getRegionHeight();
        float scale = baseScale * VISUAL_SCALE;
        float drawW = frame.getRegionWidth() * scale;
        float drawH = frame.getRegionHeight() * scale;
        float drawX = worldX * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f - drawW / 2f;
        float drawY = worldY * GameConstants.CELL_SIZE;

        if (hitFlash && hitFlashTimer % 0.1f > 0.05f) batch.setColor(1, 1, 1, 0.6f);
        else if (dashInvincible && dashInvincibleTimer % 0.1f > 0.05f) batch.setColor(0.8f, 0.9f, 1f, 0.7f);
        else batch.setColor(1, 1, 1, 1);

        batch.draw(frame, drawX, drawY, drawW, drawH);
        batch.setColor(1, 1, 1, 1);
    }

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

    public boolean onPushedBy(PushSource source, int dx, int dy, GameManager gm) {
        int strength = source.getPushStrength();
        int targetX = x + dx * strength;
        int targetY = y + dy * strength;
        if (!gm.canPlayerMoveTo(targetX, targetY)) {
            takeDamage(1);
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

    public boolean didDashJustEnd() { return dashJustEnded; }
    public void addScore(int i) { score += i; }
    public int getScore() { return this.score; }
    public float getWorldX() { return worldX; }
    public float getWorldY() { return worldY; }
    public float getMaxMana() { return maxMana; }

    public float getMoveDelayMultiplier() {
        float multiplier = 1f;
        if (slowed) multiplier *= 2.0f;
        if (dashSpeedBoost) multiplier *= DASH_SPEED_MULTIPLIER;
        return multiplier;
    }

    public void move(int dx, int dy) {
        if (isDead || inHitStun) return;
        updateDirection(dx, dy);
        if (isMovingContinuous || isAttacking) return;
        isMovingAnim = true;
        moving = true;
        moveTimer = 0f;
        targetX = x + dx;
        targetY = y + dy;
        isMovingContinuous = true;
    }

    public void updateDirection(int dx, int dy) {
        if (dx != 0 || dy != 0) {
            if (dx != 0) {
                direction = (dx > 0) ? Direction.RIGHT : Direction.LEFT;
            } else {
                direction = (dy > 0) ? Direction.UP : Direction.DOWN;
            }
            stateTime = 0f;
        }
    }

    public void startAttack() {
        if (isAttacking || isDead) return;
        isAttacking = true;
        attackAnimTimer = 0f;
    }

    public void applySlow(float duration) {
        slowed = true;
        slowTimer = Math.max(slowTimer, duration);
    }

    public void takeDamage(int damage) {
        if (isDead || damageInvincible || dashInvincible) return;
        if (damage <= 0) return;
        lives -= damage;
        AudioManager.getInstance().play(AudioType.PLAYER_ATTACKED);
        damageInvincible = true;
        damageInvincibleTimer = 0f;
        hitFlash = true;
        hitFlashTimer = 0f;
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

    public int getMaxLives() { return maxLives; }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {}

    @Override
    public RenderType getRenderType() { return RenderType.SPRITE; }

    public AbilityManager getAbilityManager() { return abilityManager; }
    public int getLives() { return lives; }
    public boolean hasKey() { return hasKey; }
    public void setHasKey(boolean hasKey) { this.hasKey = hasKey; }
    public boolean isDead() { return isDead; }
    public float getMana() { return mana; }
    public boolean isMoving() { return moving; }

    public void reset() {
        this.lives = 100000;
        this.maxLives = 100000;
        this.isDead = false;
        this.hasKey = false;
        this.dashInvincible = false;
        this.dashInvincibleTimer = 0f;
        this.dashSpeedBoost = false;
        this.dashSpeedTimer = 0f;
        this.dashJustEnded = false;
        this.moving = false;
        this.moveTimer = 0f;
        this.slowed = false;
        this.slowTimer = 0f;
        this.mana = maxMana;
        this.score = 0;
        this.buffAttack = false;
        this.buffRegen = false;
        this.buffManaEfficiency = false;
        this.regenTimer = 0f;
        this.notificationMessage = "";
        if (abilityManager != null) abilityManager.reset();
        if (trailManager != null) trailManager.dispose();
        trailManager = new PlayerTrailManager();
    }

    public String getPositionString() { return "(" + x + ", " + y + ")"; }
    public Direction getDirection() { return direction; }
    public boolean isDashing(){ return dashInvincible; }

    // ==========================================
    // 🔥 [Fix] 新增 Setter 和 Buff 接口
    // ==========================================

    public void setLives(int lives) {
        this.lives = lives;
        if (this.lives > this.maxLives) this.lives = this.maxLives;
    }

    public void setMaxLives(int maxLives) {
        this.maxLives = maxLives;
    }

    public void setMana(float mana) {
        this.mana = mana;
        if (this.mana > this.maxMana) this.mana = this.maxMana;
    }

    public void applyAttackBuff(float duration) {
        activateAttackBuff();
    }

    public void applyRegenBuff(float duration) {
        activateRegenBuff();
    }

    public void applyManaEfficiencyBuff(float duration) {
        activateManaBuff();
    }

    public void activateAttackBuff() {
        if (!buffAttack) {
            buffAttack = true;
            showNotification("Buff Acquired: ATK +50%!");
        }
    }
    public void activateRegenBuff() {
        if (!buffRegen) {
            buffRegen = true;
            showNotification("Buff Acquired: Auto-Regen!");
        }
    }
    public void activateManaBuff() {
        if (!buffManaEfficiency) {
            buffManaEfficiency = true;
            showNotification("Buff Acquired: Mana Saver (-50% Cost)!");
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
    public float getMoveSpeed() { return Math.max(0.01f, 1f / MOVE_COOLDOWN); }
    public void setWorldPosition(float worldX, float worldY) {
        this.worldX = worldX;
        this.worldY = worldY;
    }
    public void setMovingAnim(boolean moving) { this.isMovingAnim = moving; }

    public void dispose() {
        if (trailManager != null) trailManager.dispose();
    }
}