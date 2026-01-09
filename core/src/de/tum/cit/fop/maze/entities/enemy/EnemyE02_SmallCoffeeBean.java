package de.tum.cit.fop.maze.entities.enemy;

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

/**
 * 敌人 E02：小包咖啡豆
 * 行为：只会乱窜，不会攻击
 */
public class EnemyE02_SmallCoffeeBean extends Enemy {

    // 🔥 新增：连续移动相关变量
    private float targetWorldX;  // 连续移动目标坐标
    private float targetWorldY;
    private boolean isMovingContinuously = false;
    private float moveSpeedMultiplier = 1.0f;

    private float rotation = 0f;
    private float rotationSpeed = 180f; // 度/秒
    // 🔥 动画相关
    public int getCollisionDamage() {
        return collisionDamage;
    }

    private Animation<TextureRegion> anim;
    private float animTime = 0f;
    public EnemyE02_SmallCoffeeBean(int x, int y) {
        super(x, y);
        size = 0.8f;

        hp = 3;
        collisionDamage = 5;

        moveSpeed = 6.0f;          // 连续移动速度（格/秒）
        moveInterval = 0.2f;      // 走得频繁
        changeDirInterval = 0.2f;  // 疯狂换方向
        // 🔥 初始化连续移动坐标
        this.worldX = x;
        this.worldY = y;
        this.targetWorldX = x;
        this.targetWorldY = y;
        updateTexture();
    }
    @Override
    public void takeDamage(int dmg) {
        int actualDamage = dmg;

        // 你可以在这里改伤害
        // actualDamage = dmg / 2;

        super.takeDamage(actualDamage); // ⭐ 关键
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {

    }
    public boolean collidesWithPlayer(Player p) {
        float dx = (p.getX() + 0.5f) - worldX;
        float dy = (p.getY() + 0.5f) - worldY;

        float distSq = dx * dx + dy * dy;

        // 碰撞半径（你可以调）
        float radius = 0.6f;
        return distSq <= radius * radius;
    }

    /* ================== 渲染 ================== */

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        // 🔥 覆盖父类的绘制方法，使用连续坐标而不是格子坐标
        if (!active) return;

        // 🔥 更新渲染坐标（使用连续坐标）
        if (hasSingleAnimation()) {
            drawSingleAnimation(batch);
            return;
        }

        if (hasFourDirectionAnimation()) {
            drawAnimated(batch);
            return;
        }

        // 回退到静态贴图
        if (texture != null) {
            float scale = size;
            float drawSize = GameConstants.CELL_SIZE * scale;

            // 🔥 使用连续坐标渲染
            float drawX = worldX * GameConstants.CELL_SIZE +
                    (GameConstants.CELL_SIZE - drawSize) / 2f;
            float drawY = worldY * GameConstants.CELL_SIZE +
                    (GameConstants.CELL_SIZE - drawSize) / 2f;

            batch.draw(texture, drawX, drawY, drawSize, drawSize);
        }
    }
    @Override
    protected void drawSingleAnimation(SpriteBatch batch) {
        if (singleAnim == null) {
            super.drawSingleAnimation(batch);
            return;
        }

        TextureRegion frame = singleAnim.getKeyFrame(animTime, true);

        if (frame == null) {
            super.drawSingleAnimation(batch);
            return;
        }

        float baseScale = (float) GameConstants.CELL_SIZE / frame.getRegionHeight();
        float scale = baseScale * size;

        float drawW = frame.getRegionWidth() * scale;
        float drawH = frame.getRegionHeight() * scale;

        // 🔥 使用连续坐标渲染，实现平滑移动
        float drawX = worldX * GameConstants.CELL_SIZE +
                GameConstants.CELL_SIZE / 2f - drawW / 2f;
        float drawY = worldY * GameConstants.CELL_SIZE +
                GameConstants.CELL_SIZE / 2f - drawH / 2f;

        // 🔥 受击闪烁效果
        if (isHitFlash) {
            float flashAlpha = 0.5f + 0.5f * (float) Math.sin(hitFlashTimer * 20f);
            batch.setColor(1, 1, 1, flashAlpha);
        }

        batch.draw(frame, drawX, drawY,
                drawW / 2f, drawH / 2f,  // 旋转中心
                drawW, drawH,
                1f, 1f,
                rotation);  // 旋转角度
        // 恢复颜色
        if (isHitFlash) {
            batch.setColor(1, 1, 1, 1);
        }
    }



    @Override
    protected void updateTexture() {
        Logger.debug("=== E02 updateTexture 调用 ===");

        try {
            // 🔥 尝试加载动画 Atlas
            TextureAtlas atlas = textureManager.getEnemyE02Atla();

            if (atlas == null) {
                Logger.warning("E02 Atlas 为空，使用静态贴图");
                texture = textureManager.getEnemy2Texture();
                singleAnim = null;  // 🔥 设置为 null，让基类使用静态贴图
            } else {
                // 查找动画帧（尝试多个可能的名称）
                var regions = atlas.findRegions("E02_anim");

                if (regions == null || regions.size == 0) {
                    // 如果找不到指定名称，尝试其他可能的名称
                    Logger.debug("尝试其他可能的动画名称...");
                    String[] possibleNames = {"E02", "coffee", "bean", "anim"};
                    for (String name : possibleNames) {
                        regions = atlas.findRegions(name);
                        if (regions != null && regions.size > 0) {
                            Logger.debug("找到动画名称: " + name);
                            break;
                        }
                    }
                }

                if (regions != null && regions.size > 0) {
                    Logger.debug("✅ 找到 " + regions.size + " 个 E02 动画帧");

                    // 🔥 创建动画并赋值给 singleAnim
                    singleAnim = new Animation<>(
                            0.1f,  // 帧间隔（秒）
                            regions,
                            Animation.PlayMode.LOOP
                    );

                    Logger.debug("✅ E02 动画创建成功");

                    // 验证动画帧
                    for (int i = 0; i < Math.min(regions.size, 3); i++) {
                        Logger.debug("  帧 " + i + ": " +
                                regions.get(i).getRegionWidth() + "x" +
                                regions.get(i).getRegionHeight());
                    }

                    // 不再需要静态贴图
                    texture = null;
                } else {
                    Logger.warning("❌ E02 Atlas 中没有找到动画帧，使用静态贴图");
                    texture = textureManager.getEnemy2Texture();
                    singleAnim = null;
                }
            }
        } catch (Exception e) {
            Logger.error("❌ E02 加载动画时出错: " + e.getMessage());
            e.printStackTrace();
            // 出错时回退到静态贴图
            texture = textureManager.getEnemy2Texture();
            singleAnim = null;
        }

        needsTextureUpdate = false;
        Logger.debug("=== E02 updateTexture 完成 ===");
    }

    @Override
    public void update(float delta, GameManager gm) {
        if (!active) return;
        rotation += rotationSpeed * delta;
        if (rotation > 360f) rotation -= 360f;
        // 🔥 更新动画时间（即使不移动也播放动画）
        animTime += delta;

        // 🔥 受击闪烁
        updateHitFlash(delta);
        updateContinuousMovement(delta, gm);}

    // 🔥 连续平滑移动逻辑
    private void updateContinuousMovement(float delta, GameManager gm) {
        // 如果正在移动，先更新当前位置
        if (isMovingContinuously) {
            updateContinuousPosition(delta);
        }

        // 检查是否需要选择新方向
        if (!isMovingContinuously || hasReachedTarget()) {
            chooseNewDirection(gm);
        }
    }

    // 🔥 更新连续位置

    private void updateContinuousPosition(float delta) {
        if (!isMovingContinuously) return;

        float dx = targetWorldX - worldX;
        float dy = targetWorldY - worldY;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance < 0.01f) {
            // 到达目标
            worldX = targetWorldX;
            worldY = targetWorldY;
            isMovingContinuously = false;

            // 🔥 更新格子坐标
            x = Math.round(worldX);
            y = Math.round(worldY);
            return;
        }

        // 计算移动步长
        float moveStep = moveSpeed * delta * moveSpeedMultiplier;

        if (moveStep >= distance) {
            // 这一步会超过目标
            worldX = targetWorldX;
            worldY = targetWorldY;
            isMovingContinuously = false;

            // 🔥 更新格子坐标
            x = Math.round(worldX);
            y = Math.round(worldY);
        } else {
            // 正常移动
            worldX += (dx / distance) * moveStep;
            worldY += (dy / distance) * moveStep;
        }
    }

    // 🔥 检查是否到达目标
    private boolean hasReachedTarget() {
        float dx = targetWorldX - worldX;
        float dy = targetWorldY - worldY;
        return Math.sqrt(dx * dx + dy * dy) < 0.01f;
    }

    // 🔥 选择新方向并开始移动
    private void chooseNewDirection(GameManager gm) {
        // 尝试随机方向
        for (int attempt = 0; attempt < 4; attempt++) {
            int[] dir = CARDINAL_DIRS[MathUtils.random(0, CARDINAL_DIRS.length - 1)];
            float newTargetX = worldX + dir[0];
            float newTargetY = worldY + dir[1];

            int gridX = Math.round(newTargetX);
            int gridY = Math.round(newTargetY);

            // 检查目标位置是否可通行
            if (gm.isEnemyValidMove(gridX, gridY)) {
                // 设置目标位置
                targetWorldX = newTargetX;
                targetWorldY = newTargetY;
                isMovingContinuously = true;

                // 🔥 随机速度变化，让移动更有趣
                moveSpeedMultiplier = MathUtils.random(0.8f, 1.2f);

                // 更新格子坐标
                x = Math.round(worldX);
                y = Math.round(worldY);

                Logger.debug("E02 新方向: (" + dir[0] + "," + dir[1] +
                        "), 速度倍率: " + moveSpeedMultiplier);
                return;
            }
        }

        // 如果没有可行方向，停止移动
        isMovingContinuously = false;
    }

    // 🔥 覆盖父类的移动方法，防止冲突
    @Override
    protected void startMoveTo(int nx, int ny) {
        // 不执行父类的格子跳跃移动
    }

    @Override
    protected void moveContinuously(float delta) {
        // 不执行父类的移动逻辑，使用我们的连续移动
    }

    @Override
    protected void tryMoveRandom(float delta, GameManager gm) {
        // 不执行父类的随机移动，使用我们的连续移动
    }

}
