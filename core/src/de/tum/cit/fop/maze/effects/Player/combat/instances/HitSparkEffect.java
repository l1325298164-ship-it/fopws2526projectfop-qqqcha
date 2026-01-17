package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

public class HitSparkEffect extends CombatEffect {
    private final float size;
    private boolean particlesSpawned = false;

    public HitSparkEffect(float x, float y) {
        super(x, y, 0.15f); // 极短的生存时间
        this.size = 20f;
    }

    @Override
    public void update(float delta, CombatParticleSystem particleSystem) {
        super.update(delta, particleSystem);

        // 只在第一帧生成粒子
        if (!particlesSpawned) {
            particlesSpawned = true;
            for (int i = 0; i < 5; i++) {
                particleSystem.spawn(
                        x, y,
                        new Color(1f, MathUtils.random(0.5f, 1f), 0f, 1f), // 黄色到橙色
                        MathUtils.random(-150, 150), // 速度
                        MathUtils.random(-150, 150),
                        MathUtils.random(3, 6), // 大小
                        MathUtils.random(0.2f, 0.4f), // 寿命
                        true, // 阻力
                        false // 重力
                );
            }
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        float alpha = 1.0f - (timer / maxDuration);
        sr.setColor(1f, 1f, 0.7f, alpha); // 亮黄色

        float s = size * (0.8f + 0.2f * (timer / maxDuration));

        // 绘制 X 形光
        // 线宽通过多次绘制模拟 (ShapeRenderer 默认线宽可能不支持直接改)
        sr.line(x - s, y + s, x + s, y - s);
        sr.line(x - s, y - s, x + s, y + s);

        // 中心高亮
        sr.setColor(1f, 1f, 1f, alpha);
        sr.circle(x, y, s * 0.3f);
    }

    @Override
    public void renderSprite(SpriteBatch batch) {}
}