package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class MudTrapEffect extends EnvironmentEffect {
    // 泥点颜色
    private final Color mudColor = new Color(0.25f, 0.15f, 0.1f, 1f);

    // 触发特效通常在玩家踩上去时生成，持续时间不需太长
    public MudTrapEffect(float x, float y) {
        super(x, y, 1.0f);
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 第一帧生成泥浆飞溅 (Splatter)
        if (timer < delta * 2) {
            for (int i = 0; i < 6; i++) {
                ps.spawn(x, y, mudColor,
                        MathUtils.random(-40, 40), MathUtils.random(30, 70),
                        MathUtils.random(3, 5),
                        0.8f,
                        true, true); // 重力+阻力=黏在地上不弹跳的感觉
            }
        }
    }

    @Override
    public void render(ShapeRenderer sr) {
        // 绘制向内收缩的波纹 (Inward Ripple)
        // 配合 Entity 的贴图，增强吸入感
        float p = timer / maxDuration;
        float rippleRadius = 25 * (1f - p); // 从外向内收缩

        // 半透明深色环
        sr.setColor(0.1f, 0.05f, 0.0f, 0.4f * (1f - p));
        sr.circle(x, y, rippleRadius);

        // 挖空中间 (实际上是画一个稍微小一点的圆覆盖，但这需要混合模式支持 '减法' 或遮罩)
        // 由于 ShapeRenderer 功能有限，我们用画细圆环模拟：
        // (注：Filled 模式下无法画空心环，这里简单用画圆代替，依靠透明度叠加)
    }
}