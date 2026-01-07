package de.tum.cit.fop.maze.entities.enemy;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;

public class EnemyE03_CaramelJuggernaut extends Enemy {

    private EnemyState state = EnemyState.IDLE;

    /* ================== AOE ================== */

    private float aoeCooldown = 0f;
    private static final float AOE_INTERVAL = 1.5f;
    private static final int AOE_DAMAGE = 10;

    private Texture aoeTexture;
    private Texture redCircleTexture;
    /* ================== AOE 动画 ================== */
    private boolean isAoeActive = false;
    private float aoeAnimTime = 0f;
    private static final float AOE_ANIM_DURATION = 0.3f; // AOE动画持续时间

    public EnemyE03_CaramelJuggernaut(int x, int y) {
        super(x, y);
        size = 1.8f;
        hp = 28;
        collisionDamage = 8;
        attack = AOE_DAMAGE;

        moveSpeed = 1.8f;
        moveInterval = 0.4f;
        changeDirInterval = 999f; // 基本不用随机
        detectRange = 7f;

        aoeTexture = textureManager.getEnemy3AOETexture();
        redCircleTexture = createRedCircleTexture();
        updateTexture();

        direction = Direction.DOWN;
    }

    // 🔥 创建红色圆形贴图的方法
    private Texture createRedCircleTexture() {
        int size = 64; // 纹理大小
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        // 设置红色
        pixmap.setColor(1.0f, 0.2f, 0.2f, 1.0f); // 深红色
        pixmap.fillCircle(size/2, size/2, size/2 - 2); // 画圆形

        // 添加半透明边缘
        pixmap.setColor(1.0f, 0.4f, 0.4f, 0.5f); // 浅红色半透明
        pixmap.drawCircle(size/2, size/2, size/2 - 2);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();

        Logger.debug("✅ 创建红色圆形AOE贴图");
        return texture;
    }

    // 🔥 修改AOE效果绘制方法 - 脉冲版本
    private void drawAoeEffect(SpriteBatch batch) {
        if (redCircleTexture == null) return;

        // 计算脉冲效果
        float pulse = (float) (Math.sin(aoeAnimTime * 20f) * 0.2f + 0.8f); // 20Hz脉冲
        float alpha = 0.7f * (1.0f - aoeAnimTime / AOE_ANIM_DURATION); // 逐渐消失

        // 绘制多层红色圆形，创建光晕效果

        // 外层光晕（最浅）
        batch.setColor(1.0f, 0.2f, 0.2f, alpha * 0.3f);
        float outerSize = size * 1.8f * pulse;
        float outerWidth = 2 * GameConstants.CELL_SIZE * outerSize;
        float outerHeight = 2 * GameConstants.CELL_SIZE * outerSize;
        float outerX = worldX * GameConstants.CELL_SIZE +
                (GameConstants.CELL_SIZE - outerWidth) / 2f;
        float outerY = worldY * GameConstants.CELL_SIZE +
                (GameConstants.CELL_SIZE - outerHeight) / 2f;
        batch.draw(redCircleTexture, outerX, outerY, outerWidth, outerHeight);

        // 中间层（中等）
        batch.setColor(1.0f, 0.1f, 0.1f, alpha * 0.6f);
        float middleSize = size * 1.6f;
        float middleWidth = 2 * GameConstants.CELL_SIZE * middleSize;
        float middleHeight = 2 * GameConstants.CELL_SIZE * middleSize;
        float middleX = worldX * GameConstants.CELL_SIZE +
                (GameConstants.CELL_SIZE - middleWidth) / 2f;
        float middleY = worldY * GameConstants.CELL_SIZE +
                (GameConstants.CELL_SIZE - middleHeight) / 2f;
        batch.draw(redCircleTexture, middleX, middleY, middleWidth, middleHeight);

        // 内层（最实心）
        batch.setColor(1.0f, 0.0f, 0.0f, alpha * 0.9f);
        float innerSize = size * 1.4f * (1.0f - pulse * 0.2f); // 内层反向脉冲
        float innerWidth = 2 * GameConstants.CELL_SIZE * innerSize;
        float innerHeight = 2 * GameConstants.CELL_SIZE * innerSize;
        float innerX = worldX * GameConstants.CELL_SIZE +
                (GameConstants.CELL_SIZE - innerWidth) / 2f;
        float innerY = worldY * GameConstants.CELL_SIZE +
                (GameConstants.CELL_SIZE - innerHeight) / 2f;
        batch.draw(redCircleTexture, innerX, innerY, innerWidth, innerHeight);

        // 恢复颜色
        batch.setColor(1, 1, 1, 1);
    }


    //------------------承伤-----------------
    @Override
    public void takeDamage(int dmg) {
        // 焦糖重装兵可能有护甲
        int armor = 0; // 减伤0点
        int actualDamage = Math.max(0, dmg - armor);

        super.takeDamage(actualDamage);
    }
    /* ================== 渲染 ================== */

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;

        // 🔥 直接调用父类的绘制逻辑
        super.drawSprite(batch);

        // 🔥 2. AOE 效果（如果有）
        if (isAoeActive) {
            drawAoeEffect(batch);
        }
    }




    
    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        // 不用 Shape
    }

    @Override
    protected void updateTexture() {
        Logger.debug("=== E03 updateTexture 调用 ===");

        try {
            // 🔥 加载动画资源
            TextureAtlas sideAtlas = textureManager.getEnemyE03Atla();
            TextureAtlas frontAtlas = textureManager.getEnemyE03Atla();
            TextureAtlas backAtlas = textureManager.getEnemyE03Atla();

            // 1️⃣ 左右动画
            if (sideAtlas != null) {
                var leftRegions = sideAtlas.findRegions("E03_left");
                if (leftRegions != null && leftRegions.size > 0) {
                    leftAnim = new Animation<>(0.15f, leftRegions, Animation.PlayMode.LOOP);
                    Logger.debug("✅ E03 左动画创建: " + leftRegions.size + "帧");
                }

                var rightRegions = sideAtlas.findRegions("E03_right");
                if (rightRegions != null && rightRegions.size > 0) {
                    rightAnim = new Animation<>(0.15f, rightRegions, Animation.PlayMode.LOOP);
                    Logger.debug("✅ E03 右动画创建: " + rightRegions.size + "帧");
                }
            }

            // 2️⃣ 前动画（向下）
            if (frontAtlas != null) {
                var frontRegions = frontAtlas.findRegions("E03_front");
                if (frontRegions != null && frontRegions.size > 0) {
                    frontAnim = new Animation<>(0.15f, frontRegions, Animation.PlayMode.LOOP);
                    Logger.debug("✅ E03 前动画创建: " + frontRegions.size + "帧");
                }
            }

            // 3️⃣ 后动画（向上）
            if (backAtlas != null) {
                var backRegions = backAtlas.findRegions("E03_back");
                if (backRegions != null && backRegions.size > 0) {
                    backAnim = new Animation<>(0.15f, backRegions, Animation.PlayMode.LOOP);
                    Logger.debug("✅ E03 后动画创建: " + backRegions.size + "帧");
                }
            }

            // 🔥 如果所有动画都加载失败，回退到静态贴图
            if (!hasAnimation()) {
                Logger.warning("❌ E03 动画加载失败，使用静态贴图");
                texture = textureManager.getEnemy3Texture();
            } else {
                texture = null; // 有动画就不需要静态贴图
                Logger.debug("✅ E03 动画系统就绪");
            }

        } catch (Exception e) {
            Logger.error("❌ E03 加载动画时出错: " + e.getMessage());
            e.printStackTrace();
            // 出错时回退到静态贴图
            texture = textureManager.getEnemy3Texture();
        }

        needsTextureUpdate = false;
        Logger.debug("=== E03 updateTexture 完成 ===");
    }



    /* ================== 行为 ================== */

    @Override
    public void update(float delta, GameManager gm) {
        if (!active) return;

        // 🔥 更新动画时间（即使不移动也播放待机动画）
        if (state == EnemyState.IDLE) {
            // 待机时慢速播放动画
            stateTime += delta * 0.5f;
        }

        updateHitFlash(delta);

        // 🔥 更新AOE动画时间
        if (isAoeActive) {
            aoeAnimTime += delta;
            if (aoeAnimTime >= AOE_ANIM_DURATION) {
                isAoeActive = false;
                aoeAnimTime = 0f;
            }
        }

        Player player = gm.getPlayer();
        float dist = distanceTo(player);

        aoeCooldown -= delta;

        // 激活逻辑
        boolean canSeePlayer =
                dist <= detectRange &&
                        !hasWallBetween(player, gm);

        if (canSeePlayer) {
            state = EnemyState.ATTACK;
            // 🔥 面向玩家
            updateDirection(player);
        } else {
            state = EnemyState.IDLE;
        }

        if (state == EnemyState.ATTACK) {
            chasePlayer(delta, gm, player);
            tryAOEAttack(player, gm);
        }

        moveContinuously(delta);
    }

    private void updateDirection(Player player) {
        int dx = player.getX() - x;
        int dy = player.getY() - y;

        if (Math.abs(dx) > Math.abs(dy)) {
            // 水平方向为主
            direction = (dx > 0) ? Direction.RIGHT : Direction.LEFT;
        } else {
            // 垂直方向为主
            direction = (dy > 0) ? Direction.UP : Direction.DOWN;
        }
    }
    private boolean hasWallBetween(Player player, GameManager gm) {

        int px = player.getX();
        int py = player.getY();

        // 只处理同一行或同一列（正交视线）
        if (x == px) {
            int minY = Math.min(y, py);
            int maxY = Math.max(y, py);
            for (int ty = minY + 1; ty < maxY; ty++) {
                if (gm.getMazeCell(x, ty) == 0) {
                    return true; // 有墙
                }
            }
        } else if (y == py) {
            int minX = Math.min(x, px);
            int maxX = Math.max(x, px);
            for (int tx = minX + 1; tx < maxX; tx++) {
                if (gm.getMazeCell(tx, y) == 0) {
                    return true; // 有墙
                }
            }
        }

        return false; // 没被墙挡住
    }

    private void tryAOEAttack(Player player, GameManager gm) {

        if (aoeCooldown > 0f) return;

        if (isPlayerInAOE(player) && !hasWallBetween(player, gm)) {
            // 🔥 触发AOE攻击
            player.takeDamage(AOE_DAMAGE);

            // 🔥 激活AOE动画
            isAoeActive = true;
            aoeAnimTime = 0f;

            // 🔥 可以在这里添加音效
            // AudioManager.getInstance().play(AudioType.ENEMY_AOE);
        }

        aoeCooldown = AOE_INTERVAL;
    }

    /* ================== 追击 ================== */

    private void chasePlayer(float delta, GameManager gm, Player player) {

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

        if (gm.isEnemyValidMove(nx, ny)) {
            startMoveTo(nx, ny);
        }
    }

    /* ================== AOE ================== */

    private boolean isPlayerInAOE(Player player) {
        int px = player.getX();
        int py = player.getY();

        return Math.abs(px - x) <= 1 &&
                Math.abs(py - y) <= 1;
    }


    private float distanceTo(Player p) {
        float dx = p.getX() - x;
        float dy = p.getY() - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    @Override
    protected void drawAnimated(SpriteBatch batch) {
        if (!hasAnimation()) {
            drawStatic(batch);
            return;
        }

        Animation<TextureRegion> anim = getCurrentAnimation();
        if (anim == null) {
            drawStatic(batch);
            return;
        }

        TextureRegion frame = anim.getKeyFrame(stateTime, true);
        if (frame == null) {
            drawStatic(batch);
            return;
        }

        // 🔥 计算大尺寸绘制
        float baseScale = (float) GameConstants.CELL_SIZE / frame.getRegionHeight();
        float scale = baseScale * size; // 使用E03的size(1.8f)

        float drawW = frame.getRegionWidth() * scale;
        float drawH = frame.getRegionHeight() * scale;

        // 🔥 使用连续坐标实现平滑
        float drawX = worldX * GameConstants.CELL_SIZE +
                GameConstants.CELL_SIZE / 2f - drawW / 2f;
        float drawY = worldY * GameConstants.CELL_SIZE +
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

    // 🔥 获取当前方向的动画
    private Animation<TextureRegion> getCurrentAnimation() {
        switch (direction) {
            case LEFT -> {
                if (leftAnim != null) return leftAnim;
                if (rightAnim != null) return rightAnim;
            }
            case RIGHT -> {
                if (rightAnim != null) return rightAnim;
                if (leftAnim != null) return leftAnim;
            }
            case UP -> {
                if (backAnim != null) return backAnim;
                if (frontAnim != null) return frontAnim;
            }
            case DOWN -> {
                if (frontAnim != null) return frontAnim;
                if (backAnim != null) return backAnim;
            }
        }

        // 如果指定方向的动画不存在，尝试返回任何可用的动画
        if (frontAnim != null) return frontAnim;
        if (backAnim != null) return backAnim;
        if (leftAnim != null) return leftAnim;
        if (rightAnim != null) return rightAnim;

        return null;
    }

}
