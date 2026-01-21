package de.tum.cit.fop.maze.effects.Player.combat.instances;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffect;
import de.tum.cit.fop.maze.effects.Player.combat.CombatParticleSystem;

/**
 * 怪物发现玩家时的“气浪爆发”特效。
 * 表现为一圈向四周急剧扩散的透明气浪 (白色/淡灰色)。
 */
public class AggroPulseEffect extends CombatEffect {

    private boolean spawned = false;

    public AggroPulseEffect(float x, float y) {
        super(x, y, 0.6f); // 气浪扩散很快，0.6秒就够了，更有爆发感
    }

    @Override
    protected void onUpdate(float delta, CombatParticleSystem ps) {
        if (!spawned) {
            spawned = true;

            // 增加粒子数量到 32 个，让气浪这一圈更密实
            int particleCount = 32;
            for (int i = 0; i < particleCount; i++) {
                float angle = MathUtils.random(0, 360);
                // 速度加快 (80-140)，模拟冲击波的爆发速度
                float speed = MathUtils.random(80, 140);

                // 🎨 颜色调整：极淡的青白色 (模拟空气扰动)
                // RGB: 0.9, 0.95, 1.0 (接近纯白但带一点冷色调)
                // Alpha: 0.25 (高透明，像气流)
                Color waveColor = new Color(0.9f, 0.95f, 1.0f, 0.25f);

                ps.spawn(
                        x, // 从中心点爆发，不要随机偏移太多，这样更像一个整圆扩散
                        y,
                        waveColor,
                        MathUtils.cosDeg(angle) * speed,
                        MathUtils.sinDeg(angle) * speed,
                        MathUtils.random(25, 50),      // 粒子很大，形成连片的气浪感
                        MathUtils.random(0.3f, 0.5f),  // 寿命短，瞬间消失
                        false, // 无重力
                        true   // 有摩擦力 (快速喷出后减速)
                );
            }
        }
    }

    @Override
    public void renderShape(ShapeRenderer sr) {}

    @Override
    public void renderSprite(SpriteBatch batch) {}
}