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

    protected int hp;
    public int attack;
    protected float moveSpeed;
    protected float detectRange;
    protected static final float MOVE_INTERVAL = 0.25f; // 0.25 秒走一步


    // 巡逻相关
    protected float moveCooldown = 0f;
    protected float dirCooldown = 0f;      // 控制“换方向”

    protected int dirX = 0;
    protected int dirY = 0;

    // 控制多久换一次方向（秒）
    protected static final float CHANGE_DIR_INTERVAL = 1.5f;


    protected TextureManager textureManager;
    protected Texture texture;
    protected boolean needsTextureUpdate = true;

    // ===== 受击闪烁相关 =====
    protected boolean isHitFlash = false;
    protected float hitFlashTimer = 0f;
    // 闪烁总时长
    protected static final float HIT_FLASH_TIME = 0.25f;



    public Enemy(int x, int y) {
        super(x, y);
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
                x * GameConstants.CELL_SIZE,
                y * GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE
        );
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

        // 1️⃣ 冷却计时
        moveCooldown -= delta;
        dirCooldown -= delta;

        // 2️⃣ 定期换方向
        if (dirCooldown <= 0f) {
            dirX = MathUtils.random(-1, 1);
            dirY = MathUtils.random(-1, 1);

            // 防止完全不动
            if (dirX == 0 && dirY == 0) {
                dirX = 1;
            }

            dirCooldown = CHANGE_DIR_INTERVAL;
        }

        // 3️⃣ 没到移动时间 → 不走
        if (moveCooldown > 0f) return;

        int nx = x + dirX;
        int ny = y + dirY;

        // 4️⃣ 敌人专用移动规则
        if (gm.isEnemyValidMove(nx, ny)) {
            x = nx;
            y = ny;
        }

        // 5️⃣ 重置移动冷却
        moveCooldown = MOVE_INTERVAL;
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
            moveCooldown = MOVE_INTERVAL;
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
            moveCooldown = MOVE_INTERVAL;
        }
    }




}


