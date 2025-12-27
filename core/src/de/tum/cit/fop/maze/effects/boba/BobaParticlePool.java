package de.tum.cit.fop.maze.effects.boba;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/**
 * 粒子池（简化版）
 */
public class BobaParticlePool {

    private static class Particle {
        float x, y;
        float vx, vy;
        float size;
        float alpha;
        float lifetime;
        boolean active;
    }

    private Pool<Particle> particlePool;
    private Array<Particle> activeParticles;

    public BobaParticlePool() {
        particlePool = new Pool<Particle>() {
            @Override
            protected Particle newObject() {
                return new Particle();
            }
        };

        activeParticles = new Array<>();
    }

    public void createBurstEffect(float x, float y) {
        // 创建多个粒子模拟爆开效果
        int particleCount = 8 + (int)(Math.random() * 8);

        for (int i = 0; i < particleCount; i++) {
            Particle p = particlePool.obtain();
            p.x = x;
            p.y = y;

            // 随机方向
            float angle = (float)(Math.random() * Math.PI * 2);
            float speed = 50 + (float)(Math.random() * 100);
            p.vx = (float)Math.cos(angle) * speed;
            p.vy = (float)Math.sin(angle) * speed;

            // 随机大小
            p.size = 2 + (float)(Math.random() * 6);
            p.alpha = 1.0f;
            p.lifetime = 0.5f + (float)(Math.random() * 0.5f);
            p.active = true;

            activeParticles.add(p);
        }
    }

    public void update(float delta) {
        for (int i = activeParticles.size - 1; i >= 0; i--) {
            Particle p = activeParticles.get(i);

            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.lifetime -= delta;

            // 重力效果
            p.vy -= 200 * delta;

            // 更新透明度
            p.alpha = Math.max(0, p.lifetime);

            if (p.lifetime <= 0) {
                particlePool.free(p);
                activeParticles.removeIndex(i);
            }
        }
    }

    public void render(SpriteBatch batch) {
        // 绘制粒子
        // 在实际项目中，你需要使用合适的渲染方式
    }

    public void clearAllParticles() {
        for (Particle p : activeParticles) {
            particlePool.free(p);
        }
        activeParticles.clear();
    }

    public int getActiveParticleCount() {
        return activeParticles.size;
    }

    public void resetStats() {
        // 统计重置
    }

    public void dispose() {
        clearAllParticles();
    }
}