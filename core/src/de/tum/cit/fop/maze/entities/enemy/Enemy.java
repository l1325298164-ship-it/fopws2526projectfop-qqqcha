package de.tum.cit.fop.maze.entities.enemy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.entities.GameObject;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

public abstract class Enemy extends GameObject {

    /* ================= 坐标 ================= */

    // 逻辑格子坐标：用于碰撞 & 地图判断（x, y 在 GameObject 里）
    protected float worldX;
    protected float worldY;

    /* ================= 属性 ================= */

    protected int hp;
    public int attack;
    protected int collisionDamage;
    protected float moveSpeed;
    protected float detectRange;

    // 行为节奏（实例级）
    protected float moveInterval = 0.25f;
    protected float changeDirInterval = 1.5f;
    /* ================= 尺寸 ================= */

    // 敌人占用的“格子大小比例”（1.0 = 正好一格）
    protected float size = 1.0f;

    /* ================= 移动状态 ================= */

    protected boolean isMoving = false;
    protected float targetX;
    protected float targetY;

    protected float moveCooldown = 0f;
    protected float dirCooldown = 0f;

    protected int dirX = 0;
    protected int dirY = 0;

    /* ================= 渲染 ================= */

    protected TextureManager textureManager;
    protected Texture texture;
    protected boolean needsTextureUpdate = true;

    /* ================= 受击闪烁 ================= */

    protected boolean isHitFlash = false;
    protected float hitFlashTimer = 0f;
    protected static final float HIT_FLASH_TIME = 0.25f;
    //dash 相关
    private boolean hitByDash = false;

    public boolean isHitByDash() {
        return hitByDash;
    }

    public void markHitByDash() {
        hitByDash = true;
    }

    public void resetDashHit() {
        hitByDash = false;
    }
    /* ================= 方向（只允许上下左右） ================= */

    protected static final int[][] CARDINAL_DIRS = {
            { 1, 0 },   // 右
            {-1, 0 },   // 左
            { 0, 1 },   // 上
            { 0,-1 }    // 下
    };

    /* ================= 构造 ================= */

    public Enemy(int x, int y) {
        super(x, y);
        this.worldX = x;
        this.worldY = y;
        textureManager = TextureManager.getInstance();
    }

    /* ================= 抽象 ================= */

    protected abstract void updateTexture();
    public abstract void update(float delta, GameManager gm);

    /* ================= 受伤 ================= */

    public void takeDamage(int dmg) {
        if (!active) return;

        hp -= dmg;
        AudioManager.getInstance().play(AudioType.ENEMY_ATTACKED);

        isHitFlash = true;
        hitFlashTimer = 0f;

        if (hp <= 0) {
            active = false;
            //添加死亡效果
            onDeath();
        }
        Logger.debug(getClass().getSimpleName() + " took " + dmg + " damage, HP: " + hp);
    }

    private void onDeath() {
    }

    protected void updateHitFlash(float delta) {
        if (!isHitFlash) return;

        hitFlashTimer += delta;
        if (hitFlashTimer >= HIT_FLASH_TIME) {
            isHitFlash = false;
            hitFlashTimer = 0f;
        }
    }

    /* ================= 渲染 ================= */

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;

        if (needsTextureUpdate) {
            updateTexture();
        }

        Texture tex = (texture != null)
                ? texture
                : TextureManager.getInstance().getColorTexture(Color.PURPLE);

        if (isHitFlash && hitFlashTimer % 0.1f > 0.05f) {
            batch.setColor(1f, 1f, 1f, 0.6f);
        } else {
            batch.setColor(1f, 1f, 1f, 1f);
        }

        float cell = GameConstants.CELL_SIZE;
        float drawSize = cell * size;

// 居中对齐（关键）
        float drawX = worldX * cell + (cell - drawSize) * 0.5f;
        float drawY = worldY * cell + (cell - drawSize) * 0.5f;

        batch.draw(
                tex,
                drawX,
                drawY,
                drawSize,
                drawSize
        );

    }

    /* ================= 连续移动（安全版） ================= */

    protected void moveContinuously(float delta) {
        if (!isMoving) return;

        float dx = targetX - worldX;
        float dy = targetY - worldY;
        float dist2 = dx * dx + dy * dy;

        if (dist2 < 1e-6f) {
            worldX = targetX;
            worldY = targetY;
            isMoving = false;
            return;
        }

        float dist = (float) Math.sqrt(dist2);
        float step = moveSpeed * delta;

        if (step >= dist) {
            worldX = targetX;
            worldY = targetY;
            isMoving = false;
            return;
        }

        worldX += (dx / dist) * step;
        worldY += (dy / dist) * step;
    }

    protected void startMoveTo(int nx, int ny) {
        x = nx;
        y = ny;
        targetX = nx;
        targetY = ny;
        isMoving = true;
    }

    /* ================= 随机移动（最终稳定版） ================= */

    protected void tryMoveRandom(float delta, GameManager gm) {

        if (isMoving) return;

        moveCooldown -= delta;
        dirCooldown -= delta;

        if (dirCooldown <= 0f) {
            pickRandomDir();
        }

        if (moveCooldown > 0f) return;

        // 最多尝试 4 个正交方向
        for (int i = 0; i < 4; i++) {
            int nx = x + dirX;
            int ny = y + dirY;

            if (gm.isEnemyValidMove(nx, ny)) {
                startMoveTo(nx, ny);
                moveCooldown = moveInterval;
                dirCooldown = changeDirInterval;
                return;
            }
            pickRandomDir();
        }
        // 4 次都失败：本帧直接结束，不进 cooldown
    }

    protected void pickRandomDir() {
        int[] dir = CARDINAL_DIRS[MathUtils.random(0, CARDINAL_DIRS.length - 1)];
        dirX = dir[0];
        dirY = dir[1];
    }
    // ==================判断是不是可以交互或者可以通过
    @Override
    public boolean isInteractable() {
        // 敌人通常不可交互（除非有特殊设计）
        return false;
    }

    @Override
    public boolean isPassable() {
        // 敌人不可通过
        return true;
    }
    /* ================= Getter ================= */

    public int getCollisionDamage() {
        return collisionDamage;
    }

    public int getAttackDamage() {
        return attack;
    }

    public boolean isDead() {
        return !active;
    }
}
