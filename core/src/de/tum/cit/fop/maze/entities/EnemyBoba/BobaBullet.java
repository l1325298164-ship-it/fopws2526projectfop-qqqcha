package de.tum.cit.fop.maze.entities.EnemyBoba;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Disposable;
import de.tum.cit.fop.maze.entities.GameObject;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

/**
 * 珍珠奶茶子弹类 - 继承 GameObject 使用格子坐标
 */
public class BobaBullet extends GameObject implements Disposable {

    // 真实像素坐标（用于渲染和光滑移动）
    private float realX, realY;

    // 移动速度（格子/秒）
    private float vx, vy;
    private float speed = 8f; // 比EnemyBullet快一点

    // 子弹属性
    private int damage = 10;
    private float traveled = 0f;
    private float maxRange = 12f; // 最大射程（格子）

    // 视觉特效
    private TextureRegion texture;
    private float rotation = 0f;
    private float rotationSpeed = 180f;

    // 特效参数
    private float wobbleAmount = 0.05f;
    private float wobbleSpeed = 8f;
    private float wobbleTime = 0f;

    private float pulseAmount = 0.1f;
    private float pulseSpeed = 3f;

    // 颜色
    private float colorR = 0.4f; // 奶茶棕色
    private float colorG = 0.2f;
    private float colorB = 0.1f;

    // 是否由特效管理器管理
    private boolean managedByEffectManager = false;

    /**
     * 构造函数 - 与EnemyBullet保持一致
     * @param x 起始格子X坐标
     * @param y 起始格子Y坐标
     * @param dx 方向X分量（格子单位）
     * @param dy 方向Y分量（格子单位）
     * @param damage 伤害值
     */
    public BobaBullet(float x, float y, float dx, float dy, int damage) {
        super((int)x, (int)y); // 初始化格子坐标

        // 将格子坐标转换为像素坐标
        this.realX = x * GameConstants.CELL_SIZE;
        this.realY = y * GameConstants.CELL_SIZE;
        this.damage = damage;

        // 计算速度方向（单位向量）
        float len = (float) Math.sqrt(dx*dx + dy*dy);
        if (len > 0) {
            this.vx = dx / len * speed;
            this.vy = dy / len * speed;
        } else {
            this.vx = speed;
            this.vy = 0;
        }

        // 加载贴图
        loadTexture();

        // 初始化随机特效参数
        initRandomProperties();
    }

    /**
     * 更新方法 - 与GameManager兼容
     */
    public void update(float delta, GameManager gm) {
        if (!active) return;

        // 更新真实像素坐标
        realX += vx * delta * GameConstants.CELL_SIZE;
        realY += vy * delta * GameConstants.CELL_SIZE;
        traveled += Math.sqrt(vx*vx + vy*vy) * delta; // 已移动的格子数

        // 同步到格子坐标（用于碰撞检测）
        this.x = (int)(realX / GameConstants.CELL_SIZE);
        this.y = (int)(realY / GameConstants.CELL_SIZE);

        // 更新视觉特效
        wobbleTime += delta;
        rotation += rotationSpeed * delta;
        if (rotation > 360f) rotation -= 360f;

        // 碰撞检测（与EnemyBullet相同逻辑）
        // 1. 撞墙
        if (gm.getMazeCell(x, y) == 0) {
            active = false;
            // 可以在这里添加撞击特效
            return;
        }

        // 2. 射程限制
        if (traveled >= maxRange) {
            active = false;
            return;
        }

        // 3. 命中玩家
        Player player = gm.getPlayer();
        if (player != null && player.collidesWith(this)) {
            player.takeDamage(damage);
            active = false;
        }
    }

    /**
     * 绘制形状（GameObject要求实现）
     */
    @Override
    public void drawShape(com.badlogic.gdx.graphics.glutils.ShapeRenderer shapeRenderer) {
        // 可以选择不实现，因为我们将使用贴图渲染
    }

    /**
     * 绘制精灵（使用贴图）
     */
    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;

        // 计算视觉效果
        float currentSize = calculateCurrentSize();
        float wobbleX = calculateWobbleX();
        float wobbleY = calculateWobbleY();

        // 获取屏幕位置（像素坐标）
        float screenX = realX - GameConstants.CELL_SIZE/2;
        float screenY = realY - GameConstants.CELL_SIZE/2;

        // 保存原始颜色
        com.badlogic.gdx.graphics.Color originalColor = batch.getColor();

        // 应用颜色
        batch.setColor(colorR, colorG, colorB, 1f);

        if (texture != null) {
            // 绘制贴图（带摇晃效果）
            batch.draw(texture,
                    screenX + wobbleX,
                    screenY + wobbleY,
                    GameConstants.CELL_SIZE/2, GameConstants.CELL_SIZE/2, // 旋转中心
                    currentSize, currentSize,
                    1, 1,
                    rotation);
        } else {
            // 贴图加载失败，使用备用绘制（红色方块）
            batch.draw(
                    de.tum.cit.fop.maze.utils.TextureManager.getInstance().getColorTexture(
                            com.badlogic.gdx.graphics.Color.RED),
                    screenX,
                    screenY,
                    currentSize, currentSize
            );
        }

        // 恢复颜色
        batch.setColor(originalColor);
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE; // 使用精灵渲染
    }

    // ============ 特效相关方法 ============

    private void loadTexture() {
        try {
            Texture tex = new Texture(Gdx.files.internal("effects/boba-pearl-bullet.png"));
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            this.texture = new TextureRegion(tex);
        } catch (Exception e) {
            System.err.println("无法加载珍珠子弹贴图: " + e.getMessage());
            this.texture = null;
        }
    }

    private void initRandomProperties() {
        // 随机旋转速度
        this.rotationSpeed = MathUtils.random(120f, 240f);

        // 随机摇晃效果
        this.wobbleAmount = MathUtils.random(0.02f, 0.05f) * GameConstants.CELL_SIZE;
        this.wobbleSpeed = MathUtils.random(6f, 10f);

        // 随机脉动效果
        this.pulseAmount = MathUtils.random(0.05f, 0.15f);
        this.pulseSpeed = MathUtils.random(2f, 4f);

        // 随机颜色变化（奶茶色系）
        float colorVariation = MathUtils.random(-0.1f, 0.1f);
        this.colorR = MathUtils.clamp(0.4f + colorVariation, 0.3f, 0.5f);
        this.colorG = MathUtils.clamp(0.2f + colorVariation, 0.1f, 0.3f);
        this.colorB = MathUtils.clamp(0.1f + colorVariation, 0.05f, 0.15f);

        // 随机初始旋转
        this.rotation = MathUtils.random(0f, 360f);
    }

    private float calculateCurrentSize() {
        float pulse = 1.0f + (float)Math.sin(wobbleTime * pulseSpeed * 2 * Math.PI) * pulseAmount;
        return GameConstants.CELL_SIZE * 0.3f * pulse; // 基于格子大小的30%
    }

    private float calculateWobbleX() {
        return (float)Math.sin(wobbleTime * wobbleSpeed * 2 * Math.PI) * wobbleAmount;
    }

    private float calculateWobbleY() {
        return (float)Math.cos(wobbleTime * wobbleSpeed * 1.5f * 2 * Math.PI) * wobbleAmount * 0.7f;
    }

    // ============ Getter 方法（供特效管理器使用） ============


    public float getRealX() { return realX; }
    public float getRealY() { return realY; }

    public float getSpeed() { return speed; }
    public void setSpeed(float speed) { this.speed = speed; }

    public float getRotation() { return rotation; }
    public float getRotationSpeed() { return rotationSpeed; }
    public void setRotationSpeed(float speed) { this.rotationSpeed = speed; }

    public float getWobbleAmount() { return wobbleAmount; }
    public float getWobbleSpeed() { return wobbleSpeed; }

    public float getPulseAmount() { return pulseAmount; }
    public float getPulseSpeed() { return pulseSpeed; }

    public float[] getColor() { return new float[]{colorR, colorG, colorB, 1f}; }

    public float getCurrentRenderSize() {
        return calculateCurrentSize();
    }

    public float getWobbleOffsetX() {
        return calculateWobbleX();
    }

    public float getWobbleOffsetY() {
        return calculateWobbleY();
    }

    public boolean isManagedByEffectManager() {
        return managedByEffectManager;
    }

    public void setManagedByEffectManager(boolean managed) {
        this.managedByEffectManager = managed;
    }

    @Override
    public void dispose() {
        if (texture != null && texture.getTexture() != null) {
            texture.getTexture().dispose();
        }
    }
}