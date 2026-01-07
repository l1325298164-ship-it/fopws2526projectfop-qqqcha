package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;
import de.tum.cit.fop.maze.game.GameConstants;

/**
 * 冲刺特效：在冲刺起点生成一团消散的烟尘/气流
 */
public class DashEffect extends CombatEffect {

    private final int particleCount = 8;
    private final float[] particlesX;
    private final float[] particlesY;
    private final float[] particlesVX;
    private final float[] particlesVY;
    private final float[] particlesLife;

    public DashEffect(float x, float y, float directionAngle) {
        super(x, y, 0.5f); // 持续 0.5 秒

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
            float speed = MathUtils.random(30f, 80f); // 粒子速度

            particlesVX[i] = MathUtils.cos(backAngle + spread) * speed;
            particlesVY[i] = MathUtils.sin(backAngle + spread) * speed;

            particlesLife[i] = MathUtils.random(0.3f, 0.5f); // 粒子存活时间
        }
    }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {

    }

    @Override
    public void render(ShapeRenderer sr) {

    }

    @Override
    public void update(float delta) {
        super.update(delta);
        for (int i = 0; i < particleCount; i++) {
            if (particlesLife[i] > 0) {
                particlesX[i] += particlesVX[i] * delta;
                particlesY[i] += particlesVY[i] * delta;
                particlesLife[i] -= delta;
            }
        }
    }

    @Override
    public void draw(SpriteBatch batch) {
        // 如果你有烟尘的 Texture，用 batch.draw()
        // 这里为了通用，暂时不需要 Texture
    }

    @Override
    public void drawDebug(ShapeRenderer shapeRenderer) {
        // 使用 ShapeRenderer 绘制白色的气流圆点 (类似塞尔达的烟尘)
        shapeRenderer.set(ShapeRenderer.ShapeType.Filled);

        for (int i = 0; i < particleCount; i++) {
            if (particlesLife[i] > 0) {
                // 颜色随时间变透明：白色 -> 透明
                float alpha = particlesLife[i] / 0.5f;
                shapeRenderer.setColor(1f, 1f, 1f, alpha);

                // 大小随时间变小
                float size = 4f * (particlesLife[i] / 0.5f);

                shapeRenderer.circle(particlesX[i], particlesY[i], size);
            }
        }
    }
}