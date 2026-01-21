package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

/**
 * 怪物发现玩家时的“气浪冲击”特效。
 * 修正版：高速向外扩散的圆环，去除摩擦力，避免像雾气一样堆积。
 */
public class AggroPulseEffect extends CombatEffect {

    private boolean spawned = false;

    public AggroPulseEffect(float x, float y) {
        super(x, y, 0.5f); // 冲击波速度很快，0.5秒就扩散出去了
    }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {
        if (!spawned) {
            spawned = true;

            // 增加粒子数量以形成连续的圆环
            int particleCount = 40;
            float angleStep = 360f / particleCount;

            for (int i = 0; i < particleCount; i++) {
                // 角度均匀分布 + 少量随机抖动，保证圆环完整
                float angle = i * angleStep + MathUtils.random(-5f, 5f);

                // 🚀 [修改1] 极高的初速度，模拟空气爆破
                float speed = MathUtils.random(280, 350);

                // 🎨 颜色：亮青白色，透明度适中
                Color waveColor = new Color(0.85f, 0.95f, 1.0f, 0.5f);

                // ⭕ [修改2] 初始位置偏移：直接从一个小圆圈开始，而不是从一个点
                // 这样中间是空的，不会糊在一起
                float startOffset = 15f;
                float startX = x + MathUtils.cosDeg(angle) * startOffset;
                float startY = y + MathUtils.sinDeg(angle) * startOffset;

                ps.spawn(
                        startX,
                        startY,
                        waveColor,
                        MathUtils.cosDeg(angle) * speed,
                        MathUtils.sinDeg(angle) * speed,
                        MathUtils.random(15, 25),      // 粒子稍微调小一点，显得更锐利
                        0.4f,  // 寿命短
                        false, // 无重力
                        false  // 🔥 [修改3] 关键：关闭摩擦力！让它一直向外飞！
                );
            }
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) {}

    @Override
    public void renderSprite(SpriteBatch batch) {}
}