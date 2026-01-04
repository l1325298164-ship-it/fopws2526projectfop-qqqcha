package de.tum.cit.fop.maze.effects.environment.items;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class HeartEffect extends EnvironmentEffect {
    // 嫩粉色配置
    private final Color coreColor = new Color(1.0f, 0.4f, 0.7f, 0.9f); // 亮粉
    private final Color glowColor = new Color(1.0f, 0.7f, 0.85f, 0.4f); // 柔粉

    public HeartEffect(float x, float y) {
        super(x, y, 1.5f); // 持续 1.5 秒，比较慢，体现治愈感
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 产生治愈气泡 (大小不一的圆点)
        if (timer < maxDuration * 0.9f) {
            if (MathUtils.randomBoolean(0.4f)) { // 频率适中
                float offsetX = MathUtils.random(-15, 15);
                // 气泡飘起
                ps.spawn(x + offsetX, y,
                        glowColor,
                        MathUtils.random(-5, 5), MathUtils.random(40, 100), // 慢慢飘
                        MathUtils.random(3, 8), // 大小不一，模拟气泡
                        1.2f, // 寿命长一点
                        false, false);
            }
        }

        // 爆发瞬间：产生几个大圆点向外散开
        if (timer == 0) {
            for(int i=0; i<6; i++) {
                float angle = MathUtils.random(0, 360) * MathUtils.degRad;
                float speed = MathUtils.random(50, 80);
                ps.spawn(x, y, coreColor,
                        MathUtils.cos(angle)*speed, MathUtils.sin(angle)*speed,
                        8, 0.5f, false, false);
            }
        }
    }

    @Override
    public void render(ShapeRenderer sr) {
        float p = timer / maxDuration;
        float fade = 1f - p;

        // === 柔和的底部光晕 ===
        // 不画十字架了，画柔和的圆形力场

        // 内核 (高亮)
        sr.setColor(coreColor.r, coreColor.g, coreColor.b, 0.5f * fade);
        float r1 = 15 + MathUtils.sin(timer * 3) * 3;
        sr.circle(x, y, r1);

        // 外层 (大光环)
        sr.setColor(glowColor.r, glowColor.g, glowColor.b, 0.3f * fade);
        float r2 = r1 + 15;
        sr.circle(x, y, r2);

        // 最外层 (淡淡的波动)
        sr.setColor(glowColor.r, glowColor.g, glowColor.b, 0.1f * fade);
        sr.circle(x, y, r2 + 10 + MathUtils.sin(timer * 8) * 5);
    }
}