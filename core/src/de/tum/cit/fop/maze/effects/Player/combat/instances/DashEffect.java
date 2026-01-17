package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

/**
 * 冲刺特效：在冲刺起点生成一团消散的烟尘/气流
 * <p>
 * 修改记录：
 * - 增加 level 参数，实现视觉分级：
 * Lv1-2: 白色烟尘 (基础)
 * Lv3-4: 青色电光 + 小型冲击波 (充能感)
 * Lv5:   金色光辉 + 强力冲击波 (无敌感)
 */
public class DashEffect extends CombatEffect {

    private final int particleCount; // 粒子数量根据等级动态决定
    private final float[] particlesX;
    private final float[] particlesY;
    private final float[] particlesVX;
    private final float[] particlesVY;
    private final float[] particlesLife;

    // 新增：视觉属性
    private final Color effectColor;
    private final boolean hasShockwave;
    private float shockwaveRadius = 0f;
    private final float maxShockwaveRadius;

    /**
     * @param level 技能等级，决定特效的华丽程度
     */
    public DashEffect(float x, float y, float directionAngle, int level) {
        super(x, y, 0.5f); // 持续 0.5 秒

        // 1. 根据等级配置视觉参数
        if (level >= 5) {
            // Lv5: 金色光辉，粒子密集，范围大
            this.particleCount = 20;
            this.effectColor = new Color(1f, 0.85f, 0.2f, 1f); // 金色
            this.hasShockwave = true;
            this.maxShockwaveRadius = 40f;
        } else if (level >= 3) {
            // Lv3-4: 青色电光，粒子中等
            this.particleCount = 12;
            this.effectColor = new Color(0.2f, 1f, 1f, 1f); // 青色 (Cyan)
            this.hasShockwave = true;
            this.maxShockwaveRadius = 25f; // 较小的冲击波
        } else {
            // Lv1-2: 白色烟尘，朴实无华
            this.particleCount = 8;
            this.effectColor = new Color(0.9f, 0.9f, 0.9f, 1f); // 灰白色
            this.hasShockwave = false;
            this.maxShockwaveRadius = 0f;
        }

        // 2. 初始化粒子数组
        particlesX = new float[particleCount];
        particlesY = new float[particleCount];
        particlesVX = new float[particleCount];
        particlesVY = new float[particleCount];
        particlesLife = new float[particleCount];

        // 冲刺方向 (角度转弧度)
        float rad = directionAngle * MathUtils.degRad;
        // 粒子生成的方向应该是冲刺的"反方向" (向后喷射)
        float backAngle = rad + MathUtils.PI;

        for (int i = 0; i < particleCount; i++) {
            particlesX[i] = x;
            particlesY[i] = y;

            // 在反方向上加一点随机散布 (+/- 45度)
            float spread = MathUtils.random(-0.8f, 0.8f);

            // 高等级粒子喷射速度更快，更有力度
            float baseSpeed = (level >= 3) ? 50f : 30f;
            float speed = MathUtils.random(baseSpeed, baseSpeed + 50f);

            particlesVX[i] = MathUtils.cos(backAngle + spread) * speed;
            particlesVY[i] = MathUtils.sin(backAngle + spread) * speed;

            particlesLife[i] = MathUtils.random(0.3f, 0.5f); // 粒子存活时间
        }
    }

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

        // 更新冲击波 (快速扩散)
        if (hasShockwave) {
            float expansionSpeed = maxShockwaveRadius / 0.25f; // 0.25秒内扩散完毕
            shockwaveRadius += expansionSpeed * delta;
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        // 1. 绘制粒子 (Filled)
        sr.set(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < particleCount; i++) {
            if (particlesLife[i] > 0) {
                // 透明度随寿命衰减
                float lifeRatio = particlesLife[i] / 0.5f;
                sr.setColor(effectColor.r, effectColor.g, effectColor.b, lifeRatio);

                // 大小随时间变小 (等级越高粒子初始略大)
                float baseSize = (particleCount > 10) ? 5f : 4f;
                float size = baseSize * lifeRatio;
                sr.circle(particlesX[i], particlesY[i], size);
            }
        }

        // 2. 绘制冲击波 (Line)
        if (hasShockwave && shockwaveRadius < maxShockwaveRadius) {
            sr.set(ShapeRenderer.ShapeType.Line);

            // 冲击波随扩散变淡
            float waveAlpha = 1.0f - (shockwaveRadius / maxShockwaveRadius);

            if (waveAlpha > 0) {
                sr.setColor(effectColor.r, effectColor.g, effectColor.b, waveAlpha);
                // 绘制两个半径相近的圆，模拟粗线条
                sr.circle(x, y, shockwaveRadius);
                sr.circle(x, y, shockwaveRadius * 0.9f);
            }
        }
    }

    @Override
    public void renderSprite(SpriteBatch batch) {
        // 如果没有贴图需求，留空即可
    }
}