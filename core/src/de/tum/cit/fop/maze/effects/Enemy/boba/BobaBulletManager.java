package de.tum.cit.fop.maze.effects.Enemy.boba;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import de.tum.cit.fop.maze.entities.enemy.EnemyBoba.BobaBullet;
import de.tum.cit.fop.maze.game.GameConstants;

/**
 * Boba 子弹特效管理器
 * 适配格子坐标系统，主要负责管理子弹的视觉特效
 * 注意：子弹本身的更新和碰撞检测仍由 GameManager 处理
 */
public class BobaBulletManager implements Disposable {
    // 组件
    private final BobaBulletRenderer bulletRenderer;
    private final BobaTrailSystem trailSystem;
    private final BobaParticlePool particlePool;

    // 状态管理
    private final Array<BobaBullet> managedBullets;
    private boolean isEnabled = true;
    private float effectScale = 1.0f;

    // 性能统计
    private int maxBulletsInFrame = 0;
    private int bulletsRendered = 0;

    // 渲染模式控制
    public enum RenderMode {
        MANAGED,    // 由特效管理器完全控制渲染
        ASSISTED    // 协助渲染，子弹自身也渲染
    }

    private RenderMode renderMode = RenderMode.MANAGED;

    /**
     * 构造函数
     */
    public BobaBulletManager() {
        this.bulletRenderer = new BobaBulletRenderer();
        this.trailSystem = new BobaTrailSystem();
        this.particlePool = new BobaParticlePool();
        this.managedBullets = new Array<>();

        // 默认配置
        setTrailIntensity(0.7f);

        System.out.println("🔥🔥🔥 BobaBulletManager Constructor executed!");
    }

    /**
     * 添加子弹到特效管理器
     */
    public void addBullet(BobaBullet bullet) {
        if (bullet == null) return;

        if (!managedBullets.contains(bullet, true)) {
            managedBullets.add(bullet);
            bullet.setManagedByEffectManager(true);
            trailSystem.trackBullet(bullet);
            maxBulletsInFrame = Math.max(maxBulletsInFrame, managedBullets.size);
        }
    }

    /**
     * 批量添加子弹
     */
    public void addBullets(Array<BobaBullet> bullets) {
        if (bullets == null) return;

        for (BobaBullet bullet : bullets) {
            addBullet(bullet);
        }
    }

    /**
     * 移除子弹（从特效管理器）
     */
    public void removeBullet(BobaBullet bullet) {
        if (bullet == null) return;

        if (managedBullets.removeValue(bullet, true)) {
            bullet.setManagedByEffectManager(false);
            trailSystem.untrackBullet(bullet);

            // 如果子弹被销毁（不活跃），创建销毁特效
            if (!bullet.isActive()) {
                createDestructionEffect(bullet);
            }
        }
    }

    /**
     * 更新特效管理器
     * 注意：需要在 GameManager.update() 之后调用
     */
    public void update(float deltaTime) {
        if (!isEnabled) return;

        // 更新拖尾系统
        trailSystem.update(deltaTime);

        // 清理不活跃的子弹
        cleanupInactiveBullets();

        // 更新粒子池
        particlePool.update(deltaTime);

        // 更新性能统计
        updatePerformanceStats();
    }

    /**
     * 渲染所有受管理的子弹及其特效
     * 注意：这个 render 方法应该在游戏的 SpriteBatch begin/end 块内调用
     */
    public void render(SpriteBatch batch) {
        if (!isEnabled) return;

        bulletsRendered = 0;

        // 渲染顺序：拖尾（在子弹后面）-> 子弹 -> 粒子效果
        if (renderMode == RenderMode.MANAGED) {
            // 先渲染拖尾
            trailSystem.render(batch);

            // 再渲染子弹本身（由特效管理器完全控制）
            for (BobaBullet bullet : managedBullets) {
                if (bullet.isActive()) {
                    bulletRenderer.render(bullet, batch);
                    bulletsRendered++;
                }
            }
        } else {
            // ASSISTED模式：只渲染拖尾，子弹由自身渲染
            trailSystem.render(batch);
        }

        // 最后渲染粒子效果（在最上层）
        particlePool.render(batch);
    }

    /**
     * 清理不活动的子弹
     */
    private void cleanupInactiveBullets() {
        for (int i = managedBullets.size - 1; i >= 0; i--) {
            BobaBullet bullet = managedBullets.get(i);

            if (!bullet.isActive()) {
                // 创建销毁特效
                createDestructionEffect(bullet);

                // 从管理器中移除
                removeBullet(bullet);
            }
        }
    }

    /**
     * 创建子弹销毁特效
     */
    private void createDestructionEffect(BobaBullet bullet) {
        // 获取子弹的实际像素位置
        float pixelX = bullet.getRealX() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f;
        float pixelY = bullet.getRealY() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f;

        // 同时播放两种特效，打造层次感
        // 1. 播放雾气 (原 Burst，现在是 Mist)
        particlePool.createMistEffect(pixelX, pixelY);

        // 2. 播放飞溅
        particlePool.createSplashEffect(pixelX, pixelY);

        System.out.println("Playing Mist & Splash effect at " + pixelX + "," + pixelY);
    }

    /**
     * 设置特效整体强度
     */
    public void setEffectIntensity(float intensity) {
        this.effectScale = Math.max(0.1f, Math.min(2.0f, intensity));
        trailSystem.setIntensity(intensity);
        bulletRenderer.setEffectIntensity(intensity);
    }

    /**
     * 设置拖尾强度
     */
    public void setTrailIntensity(float intensity) {
        trailSystem.setIntensity(intensity);
    }

    /**
     * 设置渲染模式
     */
    public void setRenderMode(RenderMode mode) {
        this.renderMode = mode;

        // 根据模式调整子弹的渲染状态
        for (BobaBullet bullet : managedBullets) {
            if (mode == RenderMode.MANAGED) {
                // MANAGED模式：子弹自身不渲染
                // 这需要 BobaBullet 有相应的方法来控制
            } else {
                // ASSISTED模式：子弹自身也渲染
            }
        }
    }

    /**
     * 启用/禁用特效管理器
     */
    public void setEnabled(boolean enabled) {
        this.isEnabled = enabled;
        if (!enabled) {
            trailSystem.clearAllTrails();
            particlePool.clearAllParticles();
        }
    }

    /**
     * 获取性能统计信息
     */
    public String getPerformanceStats() {
        return String.format(
                "Boba特效 - 管理子弹: %d, 本帧渲染: %d, 历史最大: %d, 拖尾粒子: %d, 活跃粒子: %d",
                managedBullets.size,
                bulletsRendered,
                maxBulletsInFrame,
                trailSystem.getActiveParticleCount(),
                particlePool.getActiveParticleCount()
        );
    }

    /**
     * 重置性能统计
     */
    public void resetPerformanceStats() {
        maxBulletsInFrame = 0;
        bulletsRendered = 0;
        trailSystem.resetStats();
        particlePool.resetStats();
    }

    // 🔥 修复：增加带参数的方法，控制是否显示特效
    /**
     * 清空所有受管理的子弹
     * @param showEffects 是否播放销毁特效
     */
    public void clearAllBullets(boolean showEffects) {
        if (showEffects) {
            // 为每个子弹创建销毁效果
            for (BobaBullet bullet : managedBullets) {
                createDestructionEffect(bullet);
            }
        }

        // 清空所有列表
        managedBullets.clear();
        trailSystem.clearAllTrails();
        particlePool.clearAllParticles();
    }

    /**
     * 清空所有受管理的子弹 (默认播放特效)
     */
    public void clearAllBullets() {
        clearAllBullets(true);
    }

    /**
     * 获取受管理的子弹数量
     */
    public int getManagedBulletCount() {
        return managedBullets.size;
    }

    /**
     * 获取所有受管理的子弹（只读）
     */
    public Array<BobaBullet> getManagedBullets() {
        return new Array<>(managedBullets);
    }

    /**
     * 检查子弹是否由特效管理器管理
     */
    public boolean isManagingBullet(BobaBullet bullet) {
        return managedBullets.contains(bullet, true);
    }

    /**
     * 更新性能统计
     */
    private void updatePerformanceStats() {
        maxBulletsInFrame = Math.max(maxBulletsInFrame, managedBullets.size);
    }

    @Override
    public void dispose() {
        // 🔥 修复：必须先清理子弹（且不播放特效），再销毁资源
        clearAllBullets(false);

        bulletRenderer.dispose();
        trailSystem.dispose();
        particlePool.dispose();
    }

    /**
     * 调试方法：绘制调试信息
     */
    public void drawDebug(SpriteBatch batch) {
        // 可以在这里绘制特效管理器的调试信息
        // 如子弹位置、拖尾点数量等
    }
}