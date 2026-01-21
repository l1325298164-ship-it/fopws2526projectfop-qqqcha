package de.tum.cit.fop.maze.entities.enemy;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;

public class EnemyE04_CrystallizedCaramelShell extends Enemy {

    /* ================== 状态 ================== */
    private boolean isShellBroken = false;
    private float shellShakeTimer = 0f;
    private static final float SHELL_SHAKE_DURATION = 0.5f;
    private float shellBreakTimer = 0f;
    private static final float SHELL_BREAK_DURATION = 0.8f;
    private boolean hasEnteredAttack = false;

    /* ================== 2x2格子属性 ================== */
    private static final int GRID_SIZE = 2;
    private float sizeMultiplier = 2.0f;

    /* ================== 外壳效果 ================== */
    private float crystalGlowTimer = 0f;
    private float crystalRotation = 0f;
    private static final float CRYSTAL_ROTATION_SPEED = 45f;
    @Override
    protected AudioType getAttackSound() {
        return AudioType.ENEMY_ATTACK_E04;
    }

    /* ================== 构造 ================== */

    public EnemyE04_CrystallizedCaramelShell(int x, int y) {
        super(x, y);

        size = 2.0f;

        hp = 50;
        collisionDamage = 8;
        attack = 8;

        moveSpeed = 1.0f;
        moveInterval = 0.8f;
        changeDirInterval = 1.8f;
        detectRange = 8f;

        this.worldX = x;
        this.worldY = y;

        updateTexture();

        Logger.debug("=== E04 2x2结晶焦糖壳创建于 (" + x + "," + y + ") ===");
    }

    /* ================== 2x2格子特殊方法 ================== */

    public boolean occupiesCell(int cellX, int cellY) {
        if (!active) return false;
        return (cellX >= x && cellX < x + GRID_SIZE &&
                cellY >= y && cellY < y + GRID_SIZE);
    }

    protected boolean canMoveTo(int targetX, int targetY, GameManager gm) {
        for (int dx = 0; dx < GRID_SIZE; dx++) {
            for (int dy = 0; dy < GRID_SIZE; dy++) {
                int checkX = targetX + dx;
                int checkY = targetY + dy;

                if (!gm.isEnemyValidMove(checkX, checkY)) {
                    return false;
                }
                for (Enemy other : gm.getEnemies()) {
                    if (other != this && other instanceof EnemyE04_CrystallizedCaramelShell) {
                        if (other.occupiesCell(checkX, checkY)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /* ================== 受伤逻辑 ================== */

    @Override
    public void takeDamage(int dmg) {
        if (isHitByDash()) {
            dieByShellBreak();
            resetDashHit();
            return;
        }

        int reduced = Math.max(1, dmg / 8);
        super.takeDamage(reduced);

        isHitFlash = true;
        hitFlashTimer = 0f;
        shellShakeTimer = SHELL_SHAKE_DURATION;

        Logger.debug("E04(2x2) 受到伤害: " + reduced + " (原始: " + dmg + ")");
    }

    private void dieByShellBreak() {
        if (isShellBroken) return; // 防止重复触发

        isShellBroken = true;
        shellBreakTimer = 0f;

        // 先不杀，等动画播完
        Logger.debug("🔨 E04 2x2结晶焦糖壳被Dash击碎！播放动画...");
    }

    /* ================== 行为 ================== */

    @Override
    public void update(float delta, GameManager gm) {

        animTime += delta;

        // ===== 破碎动画逻辑 =====
        if (isShellBroken) {
            shellBreakTimer += delta;

            // 动画播完后，执行处决
            if (shellBreakTimer >= SHELL_BREAK_DURATION) {
                isShellBroken = false;

                // 🔥 强制击杀：扣除巨量生命值，确保 isDead 变为 true
                super.takeDamage(this.hp + 9999);

                Logger.debug("💀 E04 动画结束，确认死亡，触发掉落");
            }
            return; // 破碎时不再移动
        }

        if (!active) return;

        if (shellShakeTimer > 0f) {
            shellShakeTimer -= delta;
        }

        updateHitFlash(delta);

        Player target = gm.getNearestAlivePlayer(x + GRID_SIZE / 2, y + GRID_SIZE / 2);

        if (target != null) {
            float dist = distanceTo(target);
            if (dist <= detectRange) {
                if (!hasEnteredAttack) {
                    hasEnteredAttack = true;
                    AudioManager.getInstance().play(AudioType.ENEMY_ATTACK_E04);
                }
                chaseTarget(gm, target);
            } else {
                hasEnteredAttack = false;
                tryMoveRandom(delta, gm);
            }
        } else {
            tryMoveRandom(delta, gm);
        }

        moveContinuously(delta);

        if (isMoving) {
            float dx = targetX - worldX;
            float dy = targetY - worldY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance < 0.01f) {
                worldX = targetX;
                worldY = targetY;
                isMoving = false;
            } else {
                float step = moveSpeed * delta;
                worldX += (dx / distance) * step;
                worldY += (dy / distance) * step;
            }

            // 🔥🔥🔥 关键修复：同步逻辑坐标！
            // 如果不更新 x, y，掉落物就会掉在出生点，而不是死亡点
            this.x = (int) worldX;
            this.y = (int) worldY;
        }
    }

    @Override
    public boolean isPassable() {
        return false;
    }

    @Override
    public boolean isInteractable() {
        return false;
    }

    /* ================== 渲染 ================== */

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    @Override
    protected void updateTexture() {
        try {
            TextureAtlas atlas = textureManager.getEnemyE04Atlas();
            if (atlas == null) {
                texture = textureManager.getEnemy4ShellTexture();
                singleAnim = null;
            } else {
                var regions = atlas.findRegions("E04");
                if (regions == null || regions.size == 0) {
                    regions = atlas.findRegions("shell");
                }

                if (regions != null && regions.size > 0) {
                    singleAnim = new Animation<>(0.3f, regions, Animation.PlayMode.LOOP);
                    texture = null;
                } else {
                    texture = textureManager.getEnemy4ShellTexture();
                    singleAnim = null;
                }
            }
        } catch (Exception e) {
            texture = textureManager.getEnemy4ShellTexture();
            singleAnim = null;
        }
        needsTextureUpdate = false;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active && !isShellBroken) return;

        if (isShellBroken) {
            drawShellBreakEffect(batch);
            return;
        }

        super.drawSprite(batch);

        if (singleAnim != null && active) {
            drawCrystalGlowEffect(batch);
        }
    }

    private void drawCrystalGlowEffect(SpriteBatch batch) {
        if (singleAnim == null) return;
        crystalGlowTimer += 0.016f;
        crystalRotation += CRYSTAL_ROTATION_SPEED * 0.016f;

        float glowAlpha = 0.3f + 0.2f * (float)Math.sin(crystalGlowTimer * 2f);
        TextureRegion frame = singleAnim.getKeyFrame(animTime, true);
        if (frame == null) return;

        float drawW = GameConstants.CELL_SIZE * sizeMultiplier;
        float drawH = GameConstants.CELL_SIZE * sizeMultiplier;
        float drawX = worldX * GameConstants.CELL_SIZE;
        float drawY = worldY * GameConstants.CELL_SIZE;

        batch.setColor(0.6f, 0.8f, 1.0f, glowAlpha);
        batch.draw(frame, drawX, drawY, drawW / 2f, drawH / 2f, drawW, drawH, 1f, 1f, crystalRotation);
        batch.setColor(1, 1, 1, 1);
    }

    private void drawShellBreakEffect(SpriteBatch batch) {
        if (singleAnim == null) return;

        float breakProgress = shellBreakTimer / SHELL_BREAK_DURATION;
        TextureRegion frame = singleAnim.getKeyFrame(0, true);
        if (frame == null) return;

        float breakScale = 1.0f - breakProgress * 0.5f;
        float drawW = GameConstants.CELL_SIZE * sizeMultiplier * breakScale;
        float drawH = GameConstants.CELL_SIZE * sizeMultiplier * breakScale;
        float drawX = worldX * GameConstants.CELL_SIZE + (GameConstants.CELL_SIZE * sizeMultiplier - drawW) / 2f;
        float drawY = worldY * GameConstants.CELL_SIZE + (GameConstants.CELL_SIZE * sizeMultiplier - drawH) / 2f;

        float flashAlpha = 0.8f * (1.0f - breakProgress);
        batch.setColor(1.0f, 1.0f, 1.0f, flashAlpha);

        float breakRotation = breakProgress * 360f;
        batch.draw(frame, drawX, drawY, drawW / 2f, drawH / 2f, drawW, drawH, 1f, 1f, breakRotation);
        batch.setColor(1, 1, 1, 1);
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {}

    /* ================== 行为辅助 ================== */
    private void chaseTarget(GameManager gm, Player target) {
        if (isMoving) return;
        int dx = Integer.compare(target.getX(), x);
        int dy = Integer.compare(target.getY(), y);
        if (Math.abs(dx) > Math.abs(dy)) dy = 0;
        else dx = 0;
        int nx = x + dx;
        int ny = y + dy;
        if (canMoveTo(nx, ny, gm)) startMoveTo(nx, ny);
    }

    private float distanceTo(Player p) {
        float centerX = x + GRID_SIZE / 2f;
        float centerY = y + GRID_SIZE / 2f;
        float dx = p.getX() + 0.5f - centerX;
        float dy = p.getY() + 0.5f - centerY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    protected void drawSingleAnimation(SpriteBatch batch) {
        if (singleAnim == null) {
            if (texture != null) {
                float drawW = GameConstants.CELL_SIZE * sizeMultiplier;
                float drawH = GameConstants.CELL_SIZE * sizeMultiplier;
                float drawX = worldX * GameConstants.CELL_SIZE;
                float drawY = worldY * GameConstants.CELL_SIZE;
                if (isHitFlash) {
                    float flashAlpha = 0.5f + 0.5f * (float) Math.sin(hitFlashTimer * 20f);
                    batch.setColor(1, 1, 1, flashAlpha);
                }
                batch.draw(texture, drawX, drawY, drawW, drawH);
                if (isHitFlash) batch.setColor(1, 1, 1, 1);
            }
            return;
        }

        float shakeOffsetX = 0f;
        float shakeOffsetY = 0f;
        if (shellShakeTimer > 0f) {
            float shakeIntensity = shellShakeTimer / SHELL_SHAKE_DURATION;
            shakeOffsetX = (MathUtils.random() - 0.5f) * 6f * shakeIntensity;
            shakeOffsetY = (MathUtils.random() - 0.5f) * 6f * shakeIntensity;
        }

        TextureRegion frame = singleAnim.getKeyFrame(animTime, true);
        if (frame == null) return;
        float drawW = GameConstants.CELL_SIZE * sizeMultiplier;
        float drawH = GameConstants.CELL_SIZE * sizeMultiplier;
        float drawX = worldX * GameConstants.CELL_SIZE + shakeOffsetX;
        float drawY = worldY * GameConstants.CELL_SIZE + shakeOffsetY;

        if (isHitFlash) {
            float flashAlpha = 0.5f + 0.5f * (float) Math.sin(hitFlashTimer * 20f);
            batch.setColor(1, 1, 1, flashAlpha);
        }
        batch.draw(frame, drawX, drawY, drawW, drawH);
        if (isHitFlash) batch.setColor(1, 1, 1, 1);
    }

    public float getWorldX() { return worldX; }
    public float getWorldY() { return worldY; }
    public int getRightBound() { return x + GRID_SIZE - 1; }
    public int getTopBound() { return y + GRID_SIZE - 1; }
}