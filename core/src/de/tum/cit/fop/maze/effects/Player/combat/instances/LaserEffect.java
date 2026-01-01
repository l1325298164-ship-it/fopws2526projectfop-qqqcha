package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

public class LaserEffect extends CombatEffect {
    private final float endX, endY;

    public LaserEffect(float startX, float startY, float endX, float endY) {
        super(startX, startY, 0.4f); // 激光瞬间爆发
        this.endX = endX;
        this.endY = endY;
    }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {
        // 只在击中点 (End Point) 产生溅射火花
        if (timer < 0.2f) { // 前0.2秒溅射
            for(int i=0; i<3; i++) {
                ps.spawn(endX, endY, Color.CYAN,
                        MathUtils.random(-100, 100), MathUtils.random(-100, 100),
                        4, 0.3f, true, false);
            }
        }
    }

    @Override
    public void render(ShapeRenderer sr) {
        float p = timer / maxDuration;

        // 1. 核心亮白线
        sr.setColor(1, 1, 1, 1f - p);
        sr.rectLine(x, y, endX, endY, 3);

        // 2. 外围光晕 (脉冲闪烁)
        float pulse = MathUtils.sin(timer * 40f); // 快速闪烁
        float width = 10 + pulse * 5;

        sr.setColor(0f, 0.8f, 1f, (1f - p) * 0.6f);
        sr.rectLine(x, y, endX, endY, width);

        // 3. 击中点光斑
        sr.circle(endX, endY, width * 1.5f);
    }
}