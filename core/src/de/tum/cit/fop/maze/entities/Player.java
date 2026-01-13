package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.Color;
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

    private GameManager gameManager;

    protected boolean isTutorial = false;

    public void setLives(int lives) {
        this.lives=lives;
    }

    public void setMana(float mana) {
        this.mana=mana;
    }

    public void enableTutorialMode() {

    }

    public enum PlayerIndex {
        P1, P2
    }

    private PlayerIndex playerIndex;

    public PlayerIndex getPlayerIndex() {
        return playerIndex;
    }

    private static final float VISUAL_SCALE = 2.9f;
    private static final float ANIM_SPEED_MULTIPLIER = 0.15f;

    private float worldX;
    private float worldY;

    private float targetX;
    private float targetY;

    private boolean isMovingContinuous = false;

    private boolean hasKey = false;
    private int lives;
    private int maxLives;

    private boolean isDead = false;

    private boolean damageInvincible = false;
    private float damageInvincibleTimer = 0f;
    private static final float DAMAGE_INVINCIBLE_TIME = 0.6f;

    private boolean hitFlash = false;
    private float hitFlashTimer = 0f;
    private static final float HIT_FLASH_TIME = 0.25f;

    private boolean moving = false;
    private float moveTimer = 0f;
    private static final float MOVE_COOLDOWN = 0.15f;

    private TextureAtlas frontAtkAtlas, backAtkAtlas, leftAtkAtlas, rightAtkAtlas;
    private Animation<TextureRegion> frontAtkAnim, backAtkAnim, leftAtkAnim, rightAtkAnim;

    private TextureAtlas castAtlas;
    private Animation<TextureRegion> frontCastAnim;
    private Animation<TextureRegion> backCastAnim;
    private Animation<TextureRegion> leftCastAnim;
    private Animation<TextureRegion> rightCastAnim;

    private boolean isCasting = false;
    private float castAnimTimer = 0f;
    private static final float CAST_DURATION = 0.8f;

    private boolean isAttacking = false;
    private float attackAnimTimer = 0f;
    private static final float ATTACK_DURATION = 0.4f;

    private AbilityManager abilityManager;

    private float mana = 100;
    private float maxMana = 100;
    private float manaRegenRate = 5.0f;

    private boolean buffAttack = false;
    private boolean buffRegen = false;
    private boolean buffManaEfficiency = false;

    private float regenTimer = 0f;
    private String notificationMessage = "";
    private float notificationTimer = 0f;

    private boolean dashInvincible = false;
    private float dashInvincibleTimer = 0f;

    private boolean dashSpeedBoost = false;
    private float dashSpeedTimer = 0f;

    public static final float DASH_DURATION = 1f;
    public static final float DASH_SPEED_MULTIPLIER = 0.35f;

    private boolean dashJustEnded = false;

    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    private Direction direction = Direction.DOWN;

    private TextureAtlas frontAtlas, backAtlas, leftAtlas, rightAtlas;
    private Animation<TextureRegion> frontAnim, backAnim, leftAnim, rightAnim;
    private float stateTime = 0f;
    private boolean isMovingAnim = false;

    private boolean slowed = false;
    private float slowTimer = 0f;

    private int score = 0;

    private float hitStunTimer = 0f;
    private boolean inHitStun = false;

    public Player(int x, int y, GameManager gameManager, PlayerIndex index) {
        super(x, y);

        this.gameManager = gameManager;

        this.lives = 200;
        this.maxLives = 200;
        this.worldX = x;
        this.worldY = y;
        this.targetX = x;
        this.targetY = y;
        this.playerIndex = index;

        if (playerIndex == PlayerIndex.P2) {
            loadPlayer2Animations();
            castAtlas = new TextureAtlas("Character/magic/player2.atlas");

            frontCastAnim = new Animation<>(0.08f, castAtlas.findRegions("player2_front"), Animation.PlayMode.NORMAL);
            backCastAnim = new Animation<>(0.08f, castAtlas.findRegions("player2_back"), Animation.PlayMode.NORMAL);
            leftCastAnim = new Animation<>(0.08f, castAtlas.findRegions("player2_left"), Animation.PlayMode.NORMAL);
            rightCastAnim = new Animation<>(0.08f, castAtlas.findRegions("player2_right"), Animation.PlayMode.NORMAL);
        } else {
            loadPlayer1Animations();
        }

        abilityManager = new AbilityManager(this, gameManager);

        Logger.gameEvent("Player spawned at " + getPositionString());

        if (playerIndex == PlayerIndex.P1) {
            TextureAtlas attackAtlas = new TextureAtlas("Character/melee/player1.atlas");
            backAtkAnim = new Animation<>(0.08f, attackAtlas.findRegions("player1_back"), Animation.PlayMode.NORMAL);
            frontAtkAnim = new Animation<>(0.08f, attackAtlas.findRegions("player1_front"), Animation.PlayMode.NORMAL);
            leftAtkAnim = new Animation<>(0.08f, attackAtlas.findRegions("player1_left"), Animation.PlayMode.NORMAL);
            rightAtkAnim = new Animation<>(0.08f, attackAtlas.findRegions("player1_right"), Animation.PlayMode.NORMAL);
        } else {
            TextureAtlas attackAtlas = new TextureAtlas("Character/magic/player2.atlas");
            backAtkAnim = new Animation<>(0.08f, attackAtlas.findRegions("player2_back"), Animation.PlayMode.NORMAL);
            frontAtkAnim = new Animation<>(0.08f, attackAtlas.findRegions("player2_front"), Animation.PlayMode.NORMAL);
            leftAtkAnim = new Animation<>(0.08f, attackAtlas.findRegions("player2_left"), Animation.PlayMode.NORMAL);
            rightAtkAnim = new Animation<>(0.08f, attackAtlas.findRegions("player2_right"), Animation.PlayMode.NORMAL);
        }
    }

    public Player(int x, int y) {
        super(x, y);
        this.worldX = x;
        this.worldY = y;
        this.targetX = x;
        this.targetY = y;
        this.playerIndex = PlayerIndex.P1;
        this.isTutorial = true;
        this.lives = 1;
        this.maxLives = 1;

        loadPlayer1Animations();
        this.abilityManager = null;
    }

    private void loadPlayer1Animations() {
        frontAtlas = new TextureAtlas("Character/player1/front.atlas");
        backAtlas  = new TextureAtlas("Character/player1/back.atlas");
        leftAtlas  = new TextureAtlas("Character/player1/left.atlas");
        rightAtlas = new TextureAtlas("Character/player1/right.atlas");

        frontAnim = new Animation<>(0.1f, frontAtlas.getRegions(), Animation.PlayMode.LOOP);
        backAnim  = new Animation<>(0.1f, backAtlas.getRegions(), Animation.PlayMode.LOOP);
        leftAnim  = new Animation<>(0.1f, leftAtlas.getRegions(), Animation.PlayMode.LOOP);
        rightAnim = new Animation<>(0.1f, rightAtlas.getRegions(), Animation.PlayMode.LOOP);
    }

    private void loadPlayer2Animations() {
        TextureAtlas atlas = new TextureAtlas("Character/player2/player2.atlas");
        frontAnim = new Animation<>(0.1f, atlas.findRegions("player2_front"));
        backAnim  = new Animation<>(0.1f, atlas.findRegions("player2_back"));
        leftAnim  = new Animation<>(0.1f, atlas.findRegions("player2_left"));
        rightAnim = new Animation<>(0.1f, atlas.findRegions("player2_right"));
    }

    public void reviveAt(int x, int y, int hp) {
        this.isDead = false;
        this.lives = Math.min(hp, this.maxLives);
        if (this.lives <= 0) {
            this.lives = 1;
        }
        setPosition(x, y);
        this.damageInvincible = true;
        this.damageInvincibleTimer = 0f;
        this.hitFlash = false;
        this.hitFlashTimer = 0f;
        this.inHitStun = false;
        this.hitStunTimer = 0f;
        this.isAttacking = false;
        this.attackAnimTimer = 0f;
        this.isCasting = false;
        this.castAnimTimer = 0f;
        this.moving = false;
        this.isMovingContinuous = false;

        Logger.gameEvent("Player " + playerIndex + " revived at (" + x + "," + y + ") with HP=" + lives);
    }

    public void update(float delta) {
        if (isTutorial) {
            float animationSpeed = 1f;
            stateTime += delta * animationSpeed * ANIM_SPEED_MULTIPLIER;
            if (!isMovingAnim) stateTime = 0f;
            isMovingAnim = false;
            return;
        }

        if (isCasting) {
            castAnimTimer += delta;
            if (castAnimTimer >= CAST_DURATION) {
                isCasting = false;
                castAnimTimer = 0f;
            }
        }

        if (inHitStun) {
            hitStunTimer -= delta;
            if (hitStunTimer <= 0f) {
                inHitStun = false;
            }
        }

        float animationSpeed = 1f / getMoveDelayMultiplier();
        stateTime += delta * animationSpeed * ANIM_SPEED_MULTIPLIER;

        if (!isMovingAnim) stateTime = 0f;
        isMovingAnim = false;

        if (isAttacking) {
            attackAnimTimer += delta;
            if (attackAnimTimer >= ATTACK_DURATION) {
                isAttacking = false;
                attackAnimTimer = 0f;
            }
        }

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

        if (abilityManager != null) {
            abilityManager.update(delta);
        }

        // 自动回血逻辑
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
        boolean success = abilityManager.activateSlot(slot);
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

    public float getWorldX() { return worldX; }
    public float getWorldY() { return worldY; }
    public float getMaxMana() { return maxMana; }

    public void startDash() {
        dashInvincible = true;
        dashSpeedBoost = true;
        dashInvincibleTimer = 0f;
        dashSpeedTimer = 0f;
    }

    public boolean isDashInvincible() {
        return dashInvincible;
    }

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
        if (isDead) return;
        isAttacking = true;
        attackAnimTimer = 0f;
    }

    public void applySlow(float duration) {
        slowed = true;
        slowTimer = Math.max(slowTimer, duration);
    }

    // ==========================================
    // 受伤 -> 小字 RED "HP -x"
    // ==========================================
    public void takeDamage(int damage) {
        if (isDead || damageInvincible || dashInvincible) return;
        if (damage <= 0) return;

        lives -= damage;
        AudioManager.getInstance().play(AudioType.PLAYER_ATTACKED);

        if (gameManager != null && gameManager.getCombatEffectManager() != null) {
            gameManager.getCombatEffectManager().spawnStatusText(
                    this.worldX * GameConstants.CELL_SIZE,
                    this.worldY * GameConstants.CELL_SIZE + 40,
                    "HP -" + damage,
                    Color.RED
            );
        }

        damageInvincible = true;
        damageInvincibleTimer = 0f;

        hitFlash = true;
        hitFlashTimer = 0f;

        if (lives <= 0) {
            isDead = true;
            Logger.gameEvent("Player died");
        }
    }

    // ==========================================
    // 回血 -> 小字 GREEN "HP +x"
    // ==========================================
    public void heal(int amount) {
        if (isDead) return;

        int oldLives = this.lives;
        this.lives += amount;
        if (this.lives > this.maxLives) {
            this.lives = this.maxLives;
        }

        int actualHeal = this.lives - oldLives;

        if (actualHeal > 0 && gameManager != null && gameManager.getCombatEffectManager() != null) {
            gameManager.getCombatEffectManager().spawnStatusText(
                    this.worldX * GameConstants.CELL_SIZE,
                    this.worldY * GameConstants.CELL_SIZE + 40,
                    "HP +" + actualHeal,
                    Color.GREEN
            );
        }

        Logger.gameEvent("Player healed by " + amount + ". Current HP: " + lives + "/" + maxLives);
    }

    public void increaseMaxLives(int amount) {
        this.maxLives += amount;
        this.lives += amount;
        Logger.gameEvent("Max HP increased by " + amount + ". New Max: " + maxLives);
    }

    public int getMaxLives() {
        return maxLives;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active || isDead) return;

        Animation<TextureRegion> anim;

        if (isCasting && playerIndex == PlayerIndex.P2) {
            anim = switch (direction) {
                case UP -> backCastAnim;
                case LEFT -> leftCastAnim;
                case RIGHT -> rightCastAnim;
                default -> frontCastAnim;
            };
        }
        else if (isAttacking) {
            anim = switch (direction) {
                case UP -> backAtkAnim;
                case LEFT -> leftAtkAnim;
                case RIGHT -> rightAtkAnim;
                default -> frontAtkAnim;
            };
        }
        else {
            anim = switch (direction) {
                case UP -> backAnim;
                case LEFT -> leftAnim;
                case RIGHT -> rightAnim;
                default -> frontAnim;
            };
        }

        TextureRegion frame = anim.getKeyFrame(
                isCasting ? castAnimTimer :
                        isAttacking ? attackAnimTimer :
                                stateTime,
                !isCasting && !isAttacking
        );

        float baseScale = (float) GameConstants.CELL_SIZE / frame.getRegionHeight();
        float scale = baseScale * VISUAL_SCALE;
        float drawW = frame.getRegionWidth() * scale;
        float drawH = frame.getRegionHeight() * scale;
        float drawX = worldX * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f - drawW / 2f;
        float drawY = worldY * GameConstants.CELL_SIZE;

        if (!isCasting) {
            if (hitFlash && hitFlashTimer % 0.1f > 0.05f) {
                batch.setColor(1, 1, 1, 0.6f);
            }
            else if (dashInvincible && dashInvincibleTimer % 0.1f > 0.05f) {
                batch.setColor(0.8f, 0.9f, 1f, 0.7f);
            }
            else {
                batch.setColor(1, 1, 1, 1);
            }
        } else {
            batch.setColor(1, 1, 1, 1);
        }

        batch.draw(frame, drawX, drawY, drawW, drawH);
        batch.setColor(1, 1, 1, 1);
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

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

        if (abilityManager != null) {
            abilityManager.reset();
        }
        Logger.debug("Player reset complete");
    }

    public String getPositionString() {
        return "(" + x + ", " + y + ")";
    }
    public Direction getDirection() {
        return direction;
    }

    public boolean isDashing(){
        return dashInvincible;
    }

    // ==========================================
    // BUFF -> 小字 BLUE (已移除 showNotification)
    // ==========================================

    public void activateAttackBuff() {
        if (!buffAttack) {
            buffAttack = true;
            if (gameManager != null && gameManager.getCombatEffectManager() != null) {
                gameManager.getCombatEffectManager().spawnStatusText(
                        this.worldX * GameConstants.CELL_SIZE,
                        this.worldY * GameConstants.CELL_SIZE + 50,
                        "ATK UP",
                        Color.BLUE
                );
            }
            Logger.gameEvent("acquire ATK Buff");
        }
    }

    public void activateRegenBuff() {
        if (!buffRegen) {
            buffRegen = true;
            if (gameManager != null && gameManager.getCombatEffectManager() != null) {
                gameManager.getCombatEffectManager().spawnStatusText(
                        this.worldX * GameConstants.CELL_SIZE,
                        this.worldY * GameConstants.CELL_SIZE + 50,
                        "REGEN UP",
                        Color.BLUE
                );
            }
            Logger.gameEvent("acquire HP Buff");
        }
    }

    public void activateManaBuff() {
        if (!buffManaEfficiency) {
            buffManaEfficiency = true;
            if (gameManager != null && gameManager.getCombatEffectManager() != null) {
                gameManager.getCombatEffectManager().spawnStatusText(
                        this.worldX * GameConstants.CELL_SIZE,
                        this.worldY * GameConstants.CELL_SIZE + 50,
                        "MANA UP",
                        Color.BLUE
                );
            }
            Logger.gameEvent("acquire Mana Buff");
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

    public float getDamageMultiplier() {
        return buffAttack ? 1.5f : 1.0f;
    }
    public float getMoveSpeed() {
        return Math.max(0.01f, 1f / MOVE_COOLDOWN);
    }
    public void setWorldPosition(float worldX, float worldY) {
        this.worldX = worldX;
        this.worldY = worldY;
    }
    public void setMovingAnim(boolean moving) {
        this.isMovingAnim = moving;
    }

    public void startCasting() {
        if (isDead) return;
        isCasting = true;
        castAnimTimer = 0f;
    }

    // 🔥 新增接口：允许外部访问 GameManager (用于 Treasure 等实体调用特效)
    public GameManager getGameManager() {
        return gameManager;
    }
}