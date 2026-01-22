package de.tum.cit.fop.maze.entities.enemy;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.entities.GameObject;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public abstract class Enemy extends GameObject {

    // ... (Variables omitted) ...
    protected float worldX;
    protected float worldY;
    protected float stateTime = 0f;
    protected int hp;
    public int attack;
    protected int collisionDamage;
    protected float moveSpeed;
    protected float detectRange;
    protected EnemyState state = EnemyState.IDLE;
    protected EnemyState lastState = EnemyState.IDLE;
    protected float moveInterval = 0.35f;
    protected float changeDirInterval = 1.5f;
    protected float size = 1.0f;

    // Animation fields
    protected Animation<TextureRegion> frontAnim;
    protected Animation<TextureRegion> backAnim;
    protected Animation<TextureRegion> leftAnim;
    protected Animation<TextureRegion> rightAnim;
    protected Animation<TextureRegion> singleAnim;
    protected float animTime = 0f;
    protected Direction direction = Direction.DOWN;
    protected boolean isMoving = false;
    protected float targetX, targetY;
    protected float moveCooldown = 0f, dirCooldown = 0f;
    protected int dirX = 0, dirY = 0;
    protected TextureManager textureManager;
    protected Texture texture;
    protected boolean needsTextureUpdate = true;
    protected boolean isHitFlash = false;
    protected float hitFlashTimer = 0f;
    protected static final float HIT_FLASH_TIME = 0.25f;
    private boolean hitByDash = false;
    protected static final int[][] CARDINAL_DIRS = {{1,0}, {-1,0}, {0,1}, {0,-1}};
    protected GameManager gameManager;

    public Enemy(int x, int y) {
        super(x, y);
        this.worldX = x;
        this.worldY = y;
        textureManager = TextureManager.getInstance();
        needsTextureUpdate = true;
        texture = null;
    }

    protected abstract void updateTexture();
    public abstract void update(float delta, GameManager gm);

    // ... (updateAggroPulse, takeDamage, etc. omitted) ...
    protected void updateAggroPulse(GameManager gm) {
        if (state == EnemyState.CHASING && lastState != EnemyState.CHASING) {
            if (stateTime > 1.0f && gm.getCombatEffectManager() != null) {
                float cx = this.getWorldX() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f;
                float cy = this.getWorldY() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f;
                gm.getCombatEffectManager().spawnAggroPulse(cx, cy);
            }
        }
        lastState = state;
    }

    public void takeDamage(int dmg) {
        if (!active) return;
        hp -= dmg;
        AudioManager.getInstance().play(AudioType.ENEMY_ATTACKED);
        isHitFlash = true;
        hitFlashTimer = 0f;
        if (gameManager != null) {
            float cx = this.worldX * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f;
            float cy = this.worldY * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f;
            if (gameManager.getCombatEffectManager() != null) {
                gameManager.getCombatEffectManager().spawnHitSpark(cx, cy);
            }
            gameManager.triggerHitFeedback(1.0f);
        }
        if (hp <= 0) {
            active = false;
            AudioManager.getInstance().play(AudioType.ENEMY_DEATH);
            if (gameManager != null && gameManager.getCombatEffectManager() != null) {
                float cx = this.worldX * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f;
                float cy = this.worldY * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f;
                gameManager.getCombatEffectManager().spawnEnemyDeathEffect(cx, cy);
            }
            onDeath();
        }
    }

    private void onDeath() {
        if (gameManager != null) gameManager.onEnemyKilled(this);
    }

    protected void updateHitFlash(float delta) {
        if (!isHitFlash) return;
        hitFlashTimer += delta;
        if (hitFlashTimer >= HIT_FLASH_TIME) {
            isHitFlash = false;
            hitFlashTimer = 0f;
        }
    }

    public void setGameManager(GameManager gm) { this.gameManager = gm; }
    public enum Direction { UP, DOWN, LEFT, RIGHT }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;
        if (hasSingleAnimation()) {
            drawSingleAnimation(batch);
            return;
        }
        if (hasFourDirectionAnimation()) {
            drawAnimated(batch);
            return;
        }
        drawStatic(batch);
    }

    // 🔥 [修复] 添加此方法
    protected boolean hasAnimation() {
        return hasSingleAnimation() || hasFourDirectionAnimation();
    }

    boolean hasSingleAnimation() { return singleAnim != null; }

    protected boolean hasFourDirectionAnimation() {
        return leftAnim != null || rightAnim != null || frontAnim != null || backAnim != null;
    }

    // ... (Remaining draw methods and movement logic omitted) ...
    protected void drawSingleAnimation(SpriteBatch batch) {
        if (singleAnim == null) { drawStatic(batch); return; }
        TextureRegion frame = singleAnim.getKeyFrame(animTime, true);
        if (frame == null) { drawStatic(batch); return; }
        float baseScale = (float) GameConstants.CELL_SIZE / frame.getRegionHeight();
        float scale = baseScale * size;
        float drawW = frame.getRegionWidth() * scale;
        float drawH = frame.getRegionHeight() * scale;
        float drawX = x * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f - drawW / 2f;
        float drawY = y * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f - drawH / 2f;
        if (isHitFlash) {
            float flashAlpha = 0.5f + 0.5f * (float) Math.sin(hitFlashTimer * 20f);
            batch.setColor(1, 1, 1, flashAlpha);
        }
        batch.draw(frame, drawX, drawY, drawW, drawH);
        if (isHitFlash) batch.setColor(1, 1, 1, 1);
    }

    protected void drawAnimated(SpriteBatch batch) {
        Animation<TextureRegion> anim;
        switch (direction) {
            case LEFT -> anim = leftAnim;
            case RIGHT -> anim = rightAnim;
            case UP -> anim = backAnim;
            case DOWN -> anim = frontAnim;
            default -> anim = rightAnim;
        }
        if (anim == null) { drawStatic(batch); return; }
        TextureRegion frame = anim.getKeyFrame(stateTime, true);
        float baseScale = (float) GameConstants.CELL_SIZE / frame.getRegionHeight();
        float scale = baseScale * 2.5f;
        float drawW = frame.getRegionWidth() * scale;
        float drawH = frame.getRegionHeight() * scale;
        float drawX = x * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f - drawW / 2f;
        float drawY = y * GameConstants.CELL_SIZE;
        if (isHitFlash) {
            float flashAlpha = 0.5f + 0.5f * (float) Math.sin(hitFlashTimer * 20f);
            batch.setColor(1, 1, 1, flashAlpha);
        }
        batch.draw(frame, drawX, drawY, drawW, drawH);
        if (isHitFlash) batch.setColor(1, 1, 1, 1);
    }

    protected void drawStatic(SpriteBatch batch) {
        if (texture == null) return;
        float size = GameConstants.CELL_SIZE;
        batch.draw(texture, x * size, y * size, size, size);
    }

    protected void moveContinuously(float delta) {
        if (!isMoving) return;
        float dx = targetX - worldX;
        float dy = targetY - worldY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist < 1e-4f) {
            worldX = targetX;
            worldY = targetY;
            isMoving = false;
            return;
        }
        float speed = 1f / moveInterval;
        float step = speed * delta;
        if (step >= dist) {
            worldX = targetX;
            worldY = targetY;
            isMoving = false;
        } else {
            worldX += (dx / dist) * step;
            worldY += (dy / dist) * step;
        }
    }
    // Enemy.java
    protected AudioType getAttackSound() {
        return AudioType.ENEMY_ATTACK_DEFAULT;
    }

    protected void startMoveTo(int nx, int ny) {
        x = nx;
        y = ny;
        targetX = nx;
        targetY = ny;
        isMoving = true;
    }

    protected void tryMoveRandom(float delta, GameManager gm) {
        if (isMoving) return;
        moveCooldown -= delta;
        dirCooldown -= delta;
        if (dirCooldown <= 0f) {
            pickRandomDir();
            dirCooldown = changeDirInterval;
        }
        if (moveCooldown > 0f) return;
        for (int i = 0; i < 4; i++) {
            int nx = x + dirX;
            int ny = y + dirY;
            if (gm.isEnemyValidMove(nx, ny)) {
                startMoveTo(nx, ny);
                moveCooldown = moveInterval;
                return;
            }
            pickRandomDir();
        }
    }

    protected void pickRandomDir() {
        int[] dir = CARDINAL_DIRS[MathUtils.random(0, CARDINAL_DIRS.length - 1)];
        dirX = dir[0];
        dirY = dir[1];
    }

    @Override
    public boolean isInteractable() { return false; }
    @Override
    public boolean isPassable() { return true; }
    public int getCollisionDamage() { return collisionDamage; }
    public int getAttackDamage() { return attack; }
    public boolean isDead() { return !active; }
    public float getWorldX() { return worldX; }
    public float getWorldY() { return worldY; }
    public boolean isHitByDash() { return hitByDash; }
    public void markHitByDash() { hitByDash = true; }
    public void resetDashHit() { hitByDash = false; }
    public boolean occupiesCell(int cellX, int cellY) { return active && cellX == x && cellY == y; }
}