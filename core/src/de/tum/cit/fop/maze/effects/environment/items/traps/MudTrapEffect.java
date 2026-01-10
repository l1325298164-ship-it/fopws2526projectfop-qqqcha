package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class MudTrapEffect extends EnvironmentEffect {
    // 波纹参数
    private static final float MAX_RIPPLE_RADIUS = 50f; // 最大波纹半径
    private static final int RIPPLE_COUNT = 3; // 同时存在的波纹数量

    public MudTrapEffect(float x, float y) {
        super(x, y, 1.0f);
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 移除粒子效果，只使用波纹扩散（变大效果）
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        // 绘制向外扩散的波纹 (Outward Ripple) - 变大效果
        // 注意：ShapeRenderer 已经在 Filled 模式下，我们只使用 Filled 模式
        float p = timer / maxDuration;
        
        // 绘制多层波纹，营造扩散效果
        for (int i = 0; i < RIPPLE_COUNT; i++) {
            float offset = i * 0.33f; // 每层波纹的延迟
            float adjustedP = Math.max(0f, Math.min(1f, p - offset));
            
            if (adjustedP > 0 && adjustedP < 1f) {
                // 波纹从中心向外扩散
                float outerRadius = MAX_RIPPLE_RADIUS * adjustedP;
                float innerRadius = outerRadius * 0.6f; // 内圈半径，形成圆环效果
                
                // 透明度随距离和时间衰减
                float alpha = (1f - adjustedP) * 0.5f;
                
                // 外层波纹：深棕色，半透明
                sr.setColor(0.25f, 0.15f, 0.1f, alpha);
                sr.circle(x, y, outerRadius);
                
                // 内层（更暗，形成层次感）
                sr.setColor(0.2f, 0.12f, 0.08f, alpha * 0.5f);
                sr.circle(x, y, innerRadius);
            }
        }
        
        // 中心点（初始位置，逐渐变大）
        float centerAlpha = (1f - p) * 0.9f;
        float centerSize = 8f + 12f * p; // 从8逐渐变大到20
        sr.setColor(0.3f, 0.2f, 0.15f, centerAlpha);
        sr.circle(x, y, centerSize);
        
        // 中心核心（更亮的小点，也变大）
        float coreSize = 5f + 6f * p;
        sr.setColor(0.35f, 0.25f, 0.2f, centerAlpha * 0.9f);
        sr.circle(x, y, coreSize);
    }

    @Override
    public void renderSprite(SpriteBatch batch) {
        // 不需要贴图
    }
}
