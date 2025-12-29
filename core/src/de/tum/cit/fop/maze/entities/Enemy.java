package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.TextureManager;

public abstract class Enemy extends GameObject {
// ===== 逻辑格子坐标（原本的 x, y）=====
    // x, y 仍然存在，用于碰撞 & 地图判断

    // ===== 连续世界坐标（新增）=====
    protected float worldX;
    protected float worldY;

    protected int hp;
    public int attack;
    protected int collisionDamage; // 近战碰撞伤害
    protected float moveSpeed;
    protected float detectRange;
    // ===== 默认值（相当于以前的常量）=====
    protected float moveInterval = 0.25f;      // 走一步的节奏
    protected float changeDirInterval = 1.5f;  // 换方向节奏



    protected boolean isMoving = false;
    protected float targetX;
    protected float targetY;

    // 巡逻相关
    protected float moveCooldown = 0f;
    protected float dirCooldown = 0f;      // 控制“换方向”

    protected int dirX = 0;
    protected int dirY = 0;



    protected TextureManager textureManager;
    protected Texture texture;
    protected boolean needsTextureUpdate = true;

    // ===== 受击闪烁相关 =====
    protected boolean isHitFlash = false;
    protected float hitFlashTimer = 0f;
    // 闪烁总时长
    protected static final float HIT_FLASH_TIME = 0.25f;

    protected static final int[][] CARDINAL_DIRS = {
            { 1, 0 },   // 右
            {-1, 0 },   // 左
            { 0, 1 },   // 上
            { 0,-1 }    // 下
    };


    public Enemy(int x, int y) {
        super(x, y);
        // ⭐ 初始世界坐标 = 格子中心
        this.worldX = x;
        this.worldY = y;

        textureManager = TextureManager.getInstance();
    }

    protected abstract void updateTexture();

    public abstract void update(float delta, GameManager gm);

    public void takeDamage(int dmg) {
        if (!active) return;

        hp -= dmg;

        // 🔊 敌人受伤音效
        AudioManager.getInstance().play(AudioType.ENEMY_ATTACKED);

        // ✨ 触发受击闪烁
        isHitFlash = true;
        hitFlashTimer = 0f;

        if (hp <= 0) {
            die();
        }
    }
    protected void updateHitFlash(float delta) {
        if (isHitFlash) {
            hitFlashTimer += delta;
            if (hitFlashTimer >= HIT_FLASH_TIME) {
                isHitFlash = false;
                hitFlashTimer = 0f;
            }
        }
    }


    private void die() {
        active = false;
        // 以后可以加：
        // AudioManager.getInstance().play(AudioType.ENEMY_DIE);
        // 掉落物
        // 计分
    }


    public boolean isDead() {
        return !active;
    }

    public void onTextureModeChanged() {
        needsTextureUpdate = true;
    }

    /* ================== 渲染（对齐 Trap / Player） ================== */


    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;

        if (needsTextureUpdate) {
            updateTexture();
        }

        Texture tex = (texture != null)
                ? texture
                : TextureManager.getInstance().getColorTexture(Color.PURPLE);

        // ✨ 受击闪烁效果（和 Player 一致）
        if (isHitFlash && hitFlashTimer % 0.1f > 0.05f) {
            batch.setColor(1f, 1f, 1f, 0.6f);
        } else {
            batch.setColor(1f, 1f, 1f, 1f);
        }

        batch.draw(
                tex,
                worldX * GameConstants.CELL_SIZE,
                worldY * GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE
        );
    }
    protected void moveContinuously(float delta) {
        if (!isMoving) return;

        float dx = targetX - worldX;
        float dy = targetY - worldY;

        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        // ⭐ 已到达目标
        if (dist < 0.01f) {
            worldX = targetX;
            worldY = targetY;
            isMoving = false;
            return;
        }

        // ⭐ 连续移动
        float step = moveSpeed * delta;

        worldX += (dx / dist) * step;
        worldY += (dy / dist) * step;
    }
    protected void startMoveTo(int nx, int ny) {
        // ⭐ 地图合法性仍然用格子判断
        x = nx;
        y = ny;

        targetX = nx;
        targetY = ny;
        isMoving = true;
    }




    public void drawShape(ShapeRenderer shapeRenderer) {
        if (!active || texture != null) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.PURPLE);

        shapeRenderer.rect(
                x * GameConstants.CELL_SIZE + 4,
                y * GameConstants.CELL_SIZE + 4,
                GameConstants.CELL_SIZE - 8,
                GameConstants.CELL_SIZE - 8
        );
        shapeRenderer.end();
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    protected void tryMoveRandom(float delta, GameManager gm) {
        if (isMoving) return;
        // 1️⃣ 冷却计时
        moveCooldown -= delta;
        dirCooldown -= delta;

        // 2️⃣ 定期换方向
        if (dirCooldown <= 0f) {
            int[] dir = CARDINAL_DIRS[MathUtils.random(0, CARDINAL_DIRS.length - 1)];
            dirX = dir[0];
            dirY = dir[1];
            dirCooldown = changeDirInterval;
        }

        // 3️⃣ 没到移动时间 → 不走
        if (moveCooldown > 0f) return;

        int nx = x + dirX;
        int ny = y + dirY;

        // 4️⃣ 敌人专用移动规则
        boolean moved = false;

// 最多尝试 4 次（防止死循环）
        for (int i = 0; i < 4; i++) {
            nx = x + dirX;
            ny = y + dirY;

            if (gm.isEnemyValidMove(nx, ny)) {
                startMoveTo(nx, ny);
                moved = true;
                break;
            }

            // ❌ 走不了 → 立刻换方向再试
            dirX = MathUtils.random(-1, 1);
            dirY = MathUtils.random(-1, 1);

            if (dirX == 0 && dirY == 0) {
                dirX = 1;
            }
        }

// 如果 4 次都走不了，就这帧不动（极少发生）

        // 5️⃣ 重置移动冷却
        moveCooldown = moveInterval;
    }

    protected void moveToward(int targetX, int targetY, GameManager gm) {
        if (moveCooldown > 0f) return;

        int dx = Integer.compare(targetX, x);
        int dy = Integer.compare(targetY, y);

        boolean moved = false;

        // 先尝试 X 方向
        if (dx != 0 && gm.isEnemyValidMove(x + dx, y)) {
            x += dx;
            moved = true;
        }
        // 再尝试 Y 方向
        else if (dy != 0 && gm.isEnemyValidMove(x, y + dy)) {
            y += dy;
            moved = true;
        }

        if (moved) {
            moveCooldown = moveInterval;
        }
    }


    protected void moveAwayFrom(int targetX, int targetY, GameManager gm) {
        if (moveCooldown > 0f) return;

        int dx = Integer.compare(x, targetX);
        int dy = Integer.compare(y, targetY);

        boolean moved = false;

        if (dx != 0 && gm.isEnemyValidMove(x + dx, y)) {
            x += dx;
            moved = true;
        } else if (dy != 0 && gm.isEnemyValidMove(x, y + dy)) {
            y += dy;
            moved = true;
        }

        if (moved) {
            moveCooldown = moveInterval;
        }
    }

    public int getCollisionDamage() {
        return collisionDamage;
    }

    public int getAttackDamage() {
        return attack;
    }


}


