package de.tum.cit.fop.maze.entities.enemy;

import com.badlogic.gdx.graphics.Color;
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

    /* ================= 坐标 ================= */

    // 逻辑格子坐标：用于碰撞 & 地图判断（x, y 在 GameObject 里）
    protected float worldX;
    protected float worldY;

    /* ================= 属性 ================= */
    protected float stateTime = 0f;

    protected int hp;
    public int attack;
    protected int collisionDamage;
    protected float moveSpeed;
    protected float detectRange;

    // 行为节奏（实例级）
    protected float moveInterval = 0.35f;
    protected float changeDirInterval = 1.5f;
    /* ================= 尺寸 ================= */

    // 敌人占用的“格子大小比例”（1.0 = 正好一格）
    protected float size = 1.0f;

    /* ================= 移动状态 ================= */
    protected Animation<TextureRegion> frontAnim;
    protected Animation<TextureRegion> backAnim;
    protected Animation<TextureRegion> leftAnim;
    protected Animation<TextureRegion> rightAnim;
    protected Animation<TextureRegion> singleAnim;  // 用于单动画敌人
    protected float animTime = 0f;  // 动画时间


    protected Direction direction = Direction.DOWN;
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
        needsTextureUpdate = true;
        texture = null; // ⬅️ 非常重要

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
    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }
    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;

        // 🔥 优先级：单动画 > 四方向动画 > 静态贴图
        if (hasSingleAnimation()) {
            drawSingleAnimation(batch);
            return;
        }

        if (hasFourDirectionAnimation()) {
            drawAnimated(batch);
            return;
        }

        // 否则回退到旧贴图（兼容老怪）
        drawStatic(batch);
    }

    boolean hasSingleAnimation() {
        return singleAnim != null;
    }

    protected boolean hasFourDirectionAnimation() {
        return leftAnim != null || rightAnim != null
                || frontAnim != null || backAnim != null;
    }
    protected void drawSingleAnimation(SpriteBatch batch) {
        if (singleAnim == null) {
            Logger.error("单动画为空，回退静态渲染");
            drawStatic(batch);
            return;
        }

        TextureRegion frame = singleAnim.getKeyFrame(animTime, true);

        if (frame == null) {
            Logger.error("单动画帧为空");
            drawStatic(batch);
            return;
        }

        float baseScale = (float) GameConstants.CELL_SIZE / frame.getRegionHeight();
        float scale = baseScale * size;  // 使用敌人的 size 属性

        float drawW = frame.getRegionWidth() * scale;
        float drawH = frame.getRegionHeight() * scale;

        // 🔥 居中绘制（对于小尺寸敌人很重要）
        float drawX = x * GameConstants.CELL_SIZE +
                GameConstants.CELL_SIZE / 2f - drawW / 2f;
        float drawY = y * GameConstants.CELL_SIZE +
                GameConstants.CELL_SIZE / 2f - drawH / 2f;

        // 🔥 受击闪烁效果
        if (isHitFlash) {
            float flashAlpha = 0.5f + 0.5f * (float) Math.sin(hitFlashTimer * 20f);
            batch.setColor(1, 1, 1, flashAlpha);
        }

        batch.draw(frame, drawX, drawY, drawW, drawH);

        // 恢复颜色
        if (isHitFlash) {
            batch.setColor(1, 1, 1, 1);
        }
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

        if (anim == null){  Logger.error("Current animation is NULL! Falling back to static.");return;};

        TextureRegion frame = anim.getKeyFrame(stateTime, true);

        float baseScale = (float) GameConstants.CELL_SIZE / frame.getRegionHeight();
        float scale = baseScale * 2.5f;

        float drawW = frame.getRegionWidth() * scale;
        float drawH = frame.getRegionHeight() * scale;

        float drawX = x * GameConstants.CELL_SIZE
                + GameConstants.CELL_SIZE / 2f - drawW / 2f;
        float drawY = y * GameConstants.CELL_SIZE;

        batch.draw(frame, drawX, drawY, drawW, drawH);
    }

    protected void drawStatic(SpriteBatch batch) {
        if (texture == null) return;

        float size = GameConstants.CELL_SIZE;
        batch.draw(texture,
                x * size,
                y * size,
                size,
                size
        );
    }

    /* ================= 连续移动（安全版） ================= */

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

        // ⭐ 用 moveInterval 反推速度
        float speed = 1f / moveInterval; // 格 / 秒
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

        // 只在这里统一减少冷却
        moveCooldown -= delta;
        dirCooldown -= delta;

        // 到时间才换方向（一次）
        if (dirCooldown <= 0f) {
            pickRandomDir();
            dirCooldown = changeDirInterval;
        }

        // 还在移动冷却中，直接返回
        if (moveCooldown > 0f) return;

        // 尝试当前方向 + 最多 3 次备用方向
        for (int i = 0; i < 4; i++) {

            int nx = x + dirX;
            int ny = y + dirY;

            if (gm.isEnemyValidMove(nx, ny)) {
                startMoveTo(nx, ny);

                // ✅ 只有成功移动才进入 cooldown
                moveCooldown = moveInterval;
                return;
            }

            // 当前方向不通 → 换方向再试
            pickRandomDir();
        }

        // ❗ 4 次都失败：什么都不做
        // 不进 moveCooldown，不重置 dirCooldown
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


    // 🔥 添加获取世界坐标的方法
    public float getWorldX() {
        return worldX;
    }

    public float getWorldY() {
        return worldY;
    }
    protected boolean hasAnimation() {
        return hasSingleAnimation() || hasFourDirectionAnimation();
    }

    public boolean occupiesCell(int cellX, int cellY) {
        // 默认1x1敌人只占据一个格子
        return active && cellX == x && cellY == y;
    }



}
