package de.tum.cit.fop.maze.entities.enemy;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;

/**
 * 敌人 E02：小包咖啡豆
 * 行为：只会乱窜，不会攻击
 */
public class EnemyE02_SmallCoffeeBean extends Enemy {
    // 🔥 动画相关
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

    /* ================== 渲染 ================== */

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        super.drawSprite(batch); // 直接复用 Enemy 的渲染
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

        // 🔥 更新动画时间（即使不移动也播放动画）
        animTime += delta;

        // 🔥 受击闪烁
        updateHitFlash(delta);

        // 🔥 纯随机移动（乱窜）
        tryMoveRandom(delta, gm);
        moveContinuously(delta);

        // 🔥 调试日志（只在开始时显示几次）
        if (animTime < 0.5f && animTime - delta < 0.5f) {
            Logger.debug("E02 动画时间: " + animTime +
                    ", 动画: " + (singleAnim != null) +
                    ", 移动: " + isMoving);
        }
    }

}
