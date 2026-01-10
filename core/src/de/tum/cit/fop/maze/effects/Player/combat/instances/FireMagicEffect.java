package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

public class FireMagicEffect extends CombatEffect {
    // 类似于喷火器
    public FireMagicEffect(float x, float y) {
        super(x, y, 1.5f); // 持续 1.5 秒
    }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {
        // 每帧生成大量火焰粒子
        for (int i = 0; i < 4; i++) {
            float angle = MathUtils.random(0, 360); // 暂时向四周喷
            float speed = MathUtils.random(50, 150);

            // 颜色从黄到红随机
            Color fireColor = new Color(1f, MathUtils.random(0.2f, 0.8f), 0f, 1f);

            // 开启 gravity=true (向上飘) 和 friction=true (会有滞空感)
            ps.spawn(x, y, fireColor,
                    MathUtils.cosDeg(angle)*speed, MathUtils.sinDeg(angle)*speed,
                    MathUtils.random(6, 12), // 大粒子
                    0.8f, true, true);
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) { // 修正：重命名为 renderShape
        // 主体不需要画东西，全靠粒子表现
        // 为了视觉中心明显，画个淡淡的底座
        float p = timer / maxDuration;
        sr.setColor(1f, 0.4f, 0f, (1-p) * 0.3f);
        sr.circle(x, y, 15 + MathUtils.sin(timer*15) * 5);
    }
}