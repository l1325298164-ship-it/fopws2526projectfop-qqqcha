package de.tum.cit.fop.maze.entities.enemy;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
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

    /* ================== 2x2格子属性 ================== */
    private static final int GRID_SIZE = 2; // 🔥 占据2x2格子
    private float sizeMultiplier = 2.0f;    // 🔥 绘制尺寸是2倍

    /* ================== 外壳效果 ================== */
    private float crystalGlowTimer = 0f;
    private float crystalRotation = 0f;
    private static final float CRYSTAL_ROTATION_SPEED = 45f; // 度/秒

    /* ================== 构造 ================== */

    public EnemyE04_CrystallizedCaramelShell(int x, int y) {
        super(x, y);

        size = 2.0f; // 🔥 改为2.0，表示2x2格子

        hp = 50; // 🔥 因为是2x2大怪，血量更高
        collisionDamage = 8;
        attack = 8;

        moveSpeed = 1.5f;           // 🔥 更慢的移动速度
        moveInterval = 0.8f;        // 🔥 移动间隔更长
        changeDirInterval = 1.8f;
        detectRange = 8f;           // 🔥 检测范围更大

        // 初始化连续坐标
        this.worldX = x;
        this.worldY = y;

        updateTexture();

        Logger.debug("=== E04 2x2结晶焦糖壳创建于 (" + x + "," + y + ") ===");
    }

    /* ================== 🔥 2x2格子特殊方法 ================== */

    public boolean occupiesCell(int cellX, int cellY) {
        if (!active) return false;

        // 🔥 如果是2x2敌人，占据4个格子
        return (cellX >= x && cellX < x + GRID_SIZE &&
                cellY >= y && cellY < y + GRID_SIZE);
    }

    // 🔥 检查移动是否有效（2x2敌人需要检查4个格子）
    protected boolean canMoveTo(int targetX, int targetY, GameManager gm) {
        for (int dx = 0; dx < GRID_SIZE; dx++) {
            for (int dy = 0; dy < GRID_SIZE; dy++) {
                int checkX = targetX + dx;
                int checkY = targetY + dy;

                if (!gm.isEnemyValidMove(checkX, checkY)) {
                    return false;
                }

                // 🔥 额外检查：不能与其他E04重叠
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
        // Dash 命中：直接破壳 → 死亡
        if (isHitByDash()) {
            dieByShellBreak();
            resetDashHit();
            return;
        }

        // 🔥 因为是2x2大怪，普通攻击效果更差
        int reduced = Math.max(1, dmg / 8);
        super.takeDamage(reduced);

        // 🔥 被攻击时外壳闪烁
        isHitFlash = true;
        hitFlashTimer = 0f;

        // 🔥 轻微抖动效果
        shellShakeTimer = SHELL_SHAKE_DURATION;

        Logger.debug("E04(2x2) 受到伤害: " + reduced + " (原始: " + dmg + ")");
    }

    private void dieByShellBreak() {
        isShellBroken = true;
        shellBreakTimer = 0f;
        active = false;
        hp = 0;

        Logger.debug("🔨 E04 2x2结晶焦糖壳被Dash击碎！");
    }

    /* ================== 🔥 关键：保持不可通过特性 ================== */

    @Override
    public boolean isPassable() {
        // 🔥 结晶焦糖壳是固体障碍物，玩家不可通过
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
        Logger.debug("=== E04(2x2) updateTexture 调用 ===");

        try {
            // 🔥 尝试加载单动画Atlas
            TextureAtlas atlas = textureManager.getEnemyE04Atlas();

            if (atlas == null) {
                Logger.warning("E04 Atlas 为空，使用静态贴图");
                texture = textureManager.getEnemy4ShellTexture();
                singleAnim = null;
            } else {
                // 查找动画帧
                var regions = atlas.findRegions("E04");

                if (regions == null || regions.size == 0) {
                    Logger.debug("尝试其他可能的动画名称...");
                    String[] possibleNames = {"E04", "shell", "crystal", "caramel", "anim"};
                    for (String name : possibleNames) {
                        regions = atlas.findRegions(name);
                        if (regions != null && regions.size > 0) {
                            Logger.debug("找到动画名称: " + name);
                            break;
                        }
                    }
                }

                if (regions != null && regions.size > 0) {
                    Logger.debug("✅ 找到 " + regions.size + " 个 E04 动画帧");

                    singleAnim = new Animation<>(
                            0.3f,  // 🔥 更慢的帧间隔，符合大型敌人
                            regions,
                            Animation.PlayMode.LOOP
                    );

                    Logger.debug("✅ E04 2x2单动画创建成功");
                    texture = null;
                } else {
                    Logger.warning("❌ E04 Atlas 中没有找到动画帧，使用静态贴图");
                    texture = textureManager.getEnemy4ShellTexture();
                    singleAnim = null;
                }
            }
        } catch (Exception e) {
            Logger.error("❌ E04 加载动画时出错: " + e.getMessage());
            e.printStackTrace();
            texture = textureManager.getEnemy4ShellTexture();
            singleAnim = null;
        }

        needsTextureUpdate = false;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active && !isShellBroken) return;

        // 🔥 如果是破碎动画状态
        if (isShellBroken) {
            drawShellBreakEffect(batch);
            return;
        }

        // 🔥 正常绘制敌人
        super.drawSprite(batch);

        // 🔥 绘制外壳晶体特效
        if (singleAnim != null && active) {
            drawCrystalGlowEffect(batch);
        }
    }

    // 🔥 绘制外壳晶体光效
    private void drawCrystalGlowEffect(SpriteBatch batch) {
        if (singleAnim == null) return;

        crystalGlowTimer += 0.016f;
        crystalRotation += CRYSTAL_ROTATION_SPEED * 0.016f;
        if (crystalRotation > 360f) crystalRotation -= 360f;

        float glowAlpha = 0.3f + 0.2f * (float)Math.sin(crystalGlowTimer * 2f);

        TextureRegion frame = singleAnim.getKeyFrame(animTime, true);
        if (frame == null) return;

        // 🔥 2x2尺寸计算
        float drawW = GameConstants.CELL_SIZE * sizeMultiplier;
        float drawH = GameConstants.CELL_SIZE * sizeMultiplier;

        float drawX = worldX * GameConstants.CELL_SIZE;
        float drawY = worldY * GameConstants.CELL_SIZE;

        // 设置光效颜色
        batch.setColor(0.6f, 0.8f, 1.0f, glowAlpha);

        // 绘制旋转的光效
        batch.draw(frame, drawX, drawY,
                drawW / 2f, drawH / 2f,
                drawW, drawH,
                1f, 1f,
                crystalRotation);

        batch.setColor(1, 1, 1, 1);
    }

    // 🔥 绘制外壳破碎效果
    private void drawShellBreakEffect(SpriteBatch batch) {
        if (singleAnim == null) return;

        shellBreakTimer += 0.016f;
        if (shellBreakTimer >= SHELL_BREAK_DURATION) {
            isShellBroken = false;
            return;
        }

        float breakProgress = shellBreakTimer / SHELL_BREAK_DURATION;
        TextureRegion frame = singleAnim.getKeyFrame(singleAnim.getAnimationDuration() * 0.9f, true);
        if (frame == null) return;

        // 🔥 2x2破碎尺寸
        float breakScale = 1.0f - breakProgress * 0.5f;
        float drawW = GameConstants.CELL_SIZE * sizeMultiplier * breakScale;
        float drawH = GameConstants.CELL_SIZE * sizeMultiplier * breakScale;

        float drawX = worldX * GameConstants.CELL_SIZE +
                (GameConstants.CELL_SIZE * sizeMultiplier - drawW) / 2f;
        float drawY = worldY * GameConstants.CELL_SIZE +
                (GameConstants.CELL_SIZE * sizeMultiplier - drawH) / 2f;

        float flashAlpha = 0.8f * (1.0f - breakProgress);
        batch.setColor(1.0f, 1.0f, 1.0f, flashAlpha);

        float breakRotation = breakProgress * 360f;

        batch.draw(frame, drawX, drawY,
                drawW / 2f, drawH / 2f,
                drawW, drawH,
                1f, 1f,
                breakRotation);

        batch.setColor(1, 1, 1, 1);
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        // 暂不需要
    }

    /* ================== 行为 ================== */

    @Override
    public void update(float delta, GameManager gm) {
        // 🔥 更新动画时间
        animTime += delta;

        // 🔥 如果是破碎状态，只更新破碎动画
        if (isShellBroken) {
            shellBreakTimer += delta;
            if (shellBreakTimer >= SHELL_BREAK_DURATION) {
                isShellBroken = false;
            }
            return;
        }

        if (!active) return;

        if (shellShakeTimer > 0f) {
            shellShakeTimer -= delta;
        }

        updateHitFlash(delta);

        Player player = gm.getPlayer();
        float dist = distanceTo(player);

        // 🔥 简单的AI
        if (dist <= detectRange) {
            chasePlayer(gm, player);
        } else {
            tryMoveRandom(delta, gm);
        }

        moveContinuously(delta);

        // 🔥 更新连续坐标
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
        }
    }

    /* ================== 行为辅助 ================== */

    private void chasePlayer(GameManager gm, Player player) {
        if (isMoving) return;

        int dx = Integer.compare(player.getX(), x);
        int dy = Integer.compare(player.getY(), y);

        // 只走正交
        if (Math.abs(dx) > Math.abs(dy)) {
            dy = 0;
        } else {
            dx = 0;
        }

        int nx = x + dx;
        int ny = y + dy;

        // 🔥 使用2x2的移动检查
        if (canMoveTo(nx, ny, gm)) {
            startMoveTo(nx, ny);
        }
    }

    private float distanceTo(Player p) {
        // 🔥 使用2x2的中心位置计算距离
        float centerX = x + GRID_SIZE / 2f;
        float centerY = y + GRID_SIZE / 2f;
        float dx = p.getX() + 0.5f - centerX;
        float dy = p.getY() + 0.5f - centerY;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // 🔥 覆盖父类的绘制方法
    @Override
    protected void drawSingleAnimation(SpriteBatch batch) {
        if (singleAnim == null) {
            // 🔥 如果没有动画，绘制2x2的静态贴图
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

                if (isHitFlash) {
                    batch.setColor(1, 1, 1, 1);
                }
            }
            return;
        }

        // 如果有外壳抖动效果
        float shakeOffsetX = 0f;
        float shakeOffsetY = 0f;
        if (shellShakeTimer > 0f) {
            float shakeIntensity = shellShakeTimer / SHELL_SHAKE_DURATION;
            shakeOffsetX = (MathUtils.random() - 0.5f) * 6f * shakeIntensity; // 🔥 更大的抖动
            shakeOffsetY = (MathUtils.random() - 0.5f) * 6f * shakeIntensity;
        }

        TextureRegion frame = singleAnim.getKeyFrame(animTime, true);
        if (frame == null) return;

        // 🔥 2x2尺寸绘制
        float drawW = GameConstants.CELL_SIZE * sizeMultiplier;
        float drawH = GameConstants.CELL_SIZE * sizeMultiplier;

        float drawX = worldX * GameConstants.CELL_SIZE + shakeOffsetX;
        float drawY = worldY * GameConstants.CELL_SIZE + shakeOffsetY;

        if (isHitFlash) {
            float flashAlpha = 0.5f + 0.5f * (float) Math.sin(hitFlashTimer * 20f);
            batch.setColor(1, 1, 1, flashAlpha);
        }

        batch.draw(frame, drawX, drawY, drawW, drawH);

        if (isHitFlash) {
            batch.setColor(1, 1, 1, 1);
        }
    }

    // 🔥 获取世界坐标
    public float getWorldX() {
        return worldX;
    }

    public float getWorldY() {
        return worldY;
    }

    // 🔥 获取2x2格子的右边界
    public int getRightBound() {
        return x + GRID_SIZE - 1;
    }

    // 🔥 获取2x2格子的上边界
    public int getTopBound() {
        return y + GRID_SIZE - 1;
    }
}