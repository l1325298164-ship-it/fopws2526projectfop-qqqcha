package de.tum.cit.fop.maze.effects.environment.portal;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.game.GameConstants;

/**
 * 传送门特效总管
 * 负责：呼吸灯、门体悬浮、过关龙卷风动画、玩家消失逻辑
 */
public class PortalEffectManager {
    private enum State { IDLE, ACTIVE, FINISHED }
    public enum PortalOwner {
        DOOR,
        PLAYER
    }
    private PortalOwner owner = PortalOwner.DOOR;


    private State currentState = State.IDLE;
    private PortalParticlePool particlePool;
    private Texture glowTexture; // ✅ 现在这个是代码生成的

    // 动画参数
    private float timer = 0f;
    private float animationDuration = 2.0f;
    private float playerVanishTime = 1.0f;

    // 目标位置
    private float targetX, targetY;
    private boolean playerHidden = false;

    public PortalEffectManager() {
        this.particlePool = new PortalParticlePool();
        createGlowTexture(); // ✅ 初始化时直接生成光晕图
    }
    public PortalEffectManager(PortalOwner owner) {
        this.owner = owner;
        this.particlePool = new PortalParticlePool();
        createGlowTexture();
    }

    /**
     * 动态生成一个柔和的圆形光晕纹理
     * 纯代码生成，无需外部图片
     */
    private void createGlowTexture() {
        int size = 64; // 纹理分辨率，64x64足够了，反正会放大
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        // 中心点
        float centerX = size / 2f;
        float centerY = size / 2f;
        float maxRadius = size / 2f;

        // 遍历像素画径向渐变
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // 计算距离中心的距离
                float dx = x - centerX;
                float dy = y - centerY;
                float distance = (float)Math.sqrt(dx * dx + dy * dy);

                if (distance <= maxRadius) {
                    // 归一化距离 (0.0 = 中心, 1.0 = 边缘)
                    float t = distance / maxRadius;

                    // 核心算法：让中心很亮，边缘快速衰减
                    // 使用 pow(3) 让光晕更聚拢，不会像个大饼
                    float alpha = 1.0f - t;
                    alpha = (float)Math.pow(alpha, 3.0);

                    // 纯白色，Alpha通道控制透明度 (渲染时再染色)
                    pixmap.setColor(1f, 1f, 1f, alpha);
                    pixmap.drawPixel(x, y);
                }
            }
        }

        this.glowTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    public void update(float delta) {
// 🔥 PLAYER 模式：中心点必须持续更新（防止偏移）
        if (owner == PortalOwner.PLAYER) {
            // targetX / targetY 必须被视为“外部实时提供的中心”
            // 不在这里改值，交由 GameManager 每帧传入
        }



        timer += delta;

        if (currentState == State.ACTIVE) {
            if (timer < animationDuration * 0.8f) {
                particlePool.spawnTornadoParticles(targetX, targetY, GameConstants.CELL_SIZE * 0.4f);
            }
            particlePool.update(delta, targetX, targetY);

            if(owner== PortalOwner.DOOR) {
                if (!playerHidden && timer >= playerVanishTime) {
                    playerHidden = true;
                }
            }

            if (timer >= animationDuration) {
                currentState = State.FINISHED;
            }
        } else {
            particlePool.update(delta, targetX, targetY);
        }
    }

    public void startExitAnimation(float x, float y) {
        this.targetX = x;
        this.targetY = y;
        this.currentState = State.ACTIVE;
        this.timer = 0f;
        this.playerHidden = false;
    }

    /**
     * 渲染门背后的呼吸光晕
     */
    public void renderBack(SpriteBatch batch, float doorX, float doorY) {
        if (glowTexture == null) return;

        // 基础呼吸: 1.0 ~ 1.2 倍
        float breath = MathUtils.sin(timer * 2.5f);
        float scale = 1.1f + breath * 0.15f;
        float alpha = 0.4f + breath * 0.15f;

        if (currentState == State.ACTIVE) {
            scale = 1.5f + MathUtils.sin(timer * 15f) * 0.1f;
            alpha = 0.8f;
        }

        int srcFunc = batch.getBlendSrcFunc();
        int dstFunc = batch.getBlendDstFunc();
        Color oldColor = batch.getColor();

        // ✅ 关键：加法混合让光晕通透发亮
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        // ✅ 颜色：魔法蓝 (0.1, 0.6, 1.0)
        batch.setColor(0.1f, 0.6f, 1.0f, alpha);

        float size = GameConstants.CELL_SIZE * 2.0f; // 光晕大一点，覆盖两格

        batch.draw(glowTexture,
                doorX - size/2 + GameConstants.CELL_SIZE/2,
                doorY - size/2 + GameConstants.CELL_SIZE/2,
                size/2, size/2,
                size, size,
                scale, scale,
                0, // 光晕是圆的，不用旋转
                0, 0, glowTexture.getWidth(), glowTexture.getHeight(), false, false
        );

        batch.setColor(Color.WHITE);

        batch.setBlendFunction(srcFunc, dstFunc);
    }

    public void renderFront(SpriteBatch batch) {
        particlePool.render(batch);
        // 🟢 加一道保险，防止粒子特效污染后续渲染
        batch.setColor(Color.WHITE);
    }

    public float getDoorFloatOffset() {
        if (currentState == State.ACTIVE) {
            return MathUtils.random(-2f, 2f);
        }
        return MathUtils.sin(timer * 2.0f) * 4.0f;
    }

    public boolean shouldHidePlayer() {
        if (owner == PortalOwner.PLAYER) return false;
        return currentState == State.ACTIVE && playerHidden;
    }

    public boolean isFinished() {
        return currentState == State.FINISHED;
    }

    public boolean isActive() {
        return currentState == State.ACTIVE;
    }

    public void dispose() {
        if (glowTexture != null) glowTexture.dispose();
        particlePool.dispose();
    }
    /**
     * 玩家脚下的常驻特效 留着以后用
     */
    public void updatePlayerIdle(float delta, float x, float y, boolean isLevelTransition) {
        this.targetX = x;
        this.targetY = y;

        timer += delta;

        // 关卡过渡时 → 升级为 ACTIVE
        if (isLevelTransition) {
            currentState = State.ACTIVE;
        } else {
            currentState = State.IDLE;
        }

        // 少量粒子，避免太吵
        if (MathUtils.randomBoolean(0.15f)) {
            particlePool.spawnTornadoParticles(
                    targetX,
                    targetY,
                    GameConstants.CELL_SIZE * 0.25f
            );
        }

        particlePool.update(delta, targetX, targetY);
    }
    /**
     * 玩家进入迷宫 / 出生时的短暂传送阵（一次性，约 1 秒）
     */
    public void startPlayerSpawnEffect(float x, float y) {
        this.owner = PortalOwner.PLAYER;

        this.targetX = x;
        this.targetY = y;

        this.currentState = State.ACTIVE;
        this.timer = 0f;

        // 玩家特效：不隐藏玩家
        this.playerHidden = false;

        // 🔥 关键：玩家只播 1 秒
        this.animationDuration = 2.0f;
        this.playerVanishTime = 999f; // 永远不会触发
    }

    // 🔥【新增】重置状态方法
    public void reset() {
        this.currentState = State.IDLE;
        this.timer = 0f;
        this.playerHidden = false;
    }
    public void setCenter(float x, float y) {
        this.targetX = x;
        this.targetY = y;
    }
}