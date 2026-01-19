package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

public class DashEffect extends CombatEffect {

    private final int particleCount;
    private final float[] particlesX;
    private final float[] particlesY;
    private final float[] particlesVX;
    private final float[] particlesVY;
    private final float[] particlesLife;

    private final Color effectColor;
    private final boolean hasShockwave;
    private float shockwaveRadius = 0f;
    private final float maxShockwaveRadius;

    /**
     * ✅ [兼容修复] 3参数构造函数，默认等级为 1
     * 供 CombatEffectManager 调用
     */
    public DashEffect(float x, float y, float directionAngle) {
        this(x, y, directionAngle, 1);
    }

    /**
     * 全参数构造函数
     */
    public DashEffect(float x, float y, float directionAngle, int level) {
        super(x, y, 0.5f);

        // 1. 根据等级配置视觉参数
        if (level >= 5) {
            this.particleCount = 20;
            this.effectColor = new Color(1f, 0.85f, 0.2f, 1f); // 金色
            this.hasShockwave = true;
            this.maxShockwaveRadius = 40f;
        } else if (level >= 3) {
            this.particleCount = 12;
            this.effectColor = new Color(0.2f, 1f, 1f, 1f); // 青色
            this.hasShockwave = true;
            this.maxShockwaveRadius = 25f;
        } else {
            this.particleCount = 8;
            this.effectColor = new Color(0.9f, 0.9f, 0.9f, 1f); // 白色
            this.hasShockwave = false;
            this.maxShockwaveRadius = 0f;
        }

        // 2. 初始化粒子
        particlesX = new float[particleCount];
        particlesY = new float[particleCount];
        particlesVX = new float[particleCount];
        particlesVY = new float[particleCount];
        particlesLife = new float[particleCount];

        float rad = directionAngle * MathUtils.degRad;
        float backAngle = rad + MathUtils.PI;

        for (int i = 0; i < particleCount; i++) {
            particlesX[i] = x;
            particlesY[i] = y;

            float spread = MathUtils.random(-0.8f, 0.8f);
            float baseSpeed = (level >= 3) ? 50f : 30f;
            float speed = MathUtils.random(baseSpeed, baseSpeed + 50f);

            particlesVX[i] = MathUtils.cos(backAngle + spread) * speed;
            particlesVY[i] = MathUtils.sin(backAngle + spread) * speed;
            particlesLife[i] = MathUtils.random(0.3f, 0.5f);
        }
    }

    // ✅ [核心修复] 实现 onUpdate 而不是 override update
    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {
        // 更新粒子位置
        for (int i = 0; i < particleCount; i++) {
            if (particlesLife[i] > 0) {
                particlesX[i] += particlesVX[i] * delta;
                particlesY[i] += particlesVY[i] * delta;
                particlesLife[i] -= delta;
            }
        }

        // 更新冲击波
        if (hasShockwave) {
            float expansionSpeed = maxShockwaveRadius / 0.25f;
            shockwaveRadius += expansionSpeed * delta;
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        sr.set(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < particleCount; i++) {
            if (particlesLife[i] > 0) {
                float lifeRatio = particlesLife[i] / 0.5f;
                sr.setColor(effectColor.r, effectColor.g, effectColor.b, lifeRatio);
                float baseSize = (particleCount > 10) ? 5f : 4f;
                float size = baseSize * lifeRatio;
                sr.circle(particlesX[i], particlesY[i], size);
            }
        }

        if (hasShockwave && shockwaveRadius < maxShockwaveRadius) {
            sr.set(ShapeRenderer.ShapeType.Line);
            float waveAlpha = 1.0f - (shockwaveRadius / maxShockwaveRadius);
            if (waveAlpha > 0) {
                sr.setColor(effectColor.r, effectColor.g, effectColor.b, waveAlpha);
                sr.circle(x, y, shockwaveRadius);
                sr.circle(x, y, shockwaveRadius * 0.9f);
            }
        }
    }

    @Override
    public void renderSprite(SpriteBatch batch) {
        // 不需要贴图
    }
}

/**
 * 冲刺特效：在冲刺起点生成一团消散的烟尘/气流
 * <p>
 * 修改记录：
 * - 增加 level 参数，实现视觉分级：
 * Lv1-2: 白色烟尘 (基础)
 * Lv3-4: 青色电光 + 小型冲击波 (充能感)
 * Lv5:   金色光辉 + 强力冲击波 (无敌感)
 */