package de.tum.cit.fop.maze.effects.boba;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/**
 * 粒子池 - 奶茶特效专用
 * 包含：雾气蒸发 (Mist) 和 液滴飞溅 (Droplet)
 */
public class BobaParticlePool {

    // 定义粒子类型
    public enum ParticleType {
        DEFAULT, // 旧效果
        MIST,    // 雾气：向上飘散，变大
        DROPLET  // 液滴：受重力下落
    }

    private static class Particle {
        float x, y;
        float vx, vy;
        float size;
        float alpha;
        float lifetime;
        float maxLifetime;
        boolean active;
        ParticleType type;
    }

    private final Pool<Particle> particlePool;
    private final Array<Particle> activeParticles;
    private final ShapeRenderer shapeRenderer;

    public BobaParticlePool() {
        particlePool = new Pool<Particle>() {
            @Override
            protected Particle newObject() {
                return new Particle();
            }
        };
        activeParticles = new Array<>();
        shapeRenderer = new ShapeRenderer();
    }

    // ==========================================
    // ⭐ 缺失的方法补全在这里
    // ==========================================

    /**
     * 雾气效果：向上飘散 (Mist)
     */
    public void createMistEffect(float x, float y) {
        int count = MathUtils.random(6, 10);
        for (int i = 0; i < count; i++) {
            Particle p = particlePool.obtain();
            initParticle(p, x, y, ParticleType.MIST);
            activeParticles.add(p);
        }
    }

    /**
     * 飞溅效果：受重力下落 (Splash)
     */
    public void createSplashEffect(float x, float y) {
        int count = MathUtils.random(8, 14);
        for (int i = 0; i < count; i++) {
            Particle p = particlePool.obtain();
            initParticle(p, x, y, ParticleType.DROPLET);
            activeParticles.add(p);
        }
    }

    /**
     * 兼容旧代码：普通爆开 (Burst)
     */
    public void createBurstEffect(float x, float y) {
        // 默认产生一些随机粒子
        int count = 8;
        for (int i = 0; i < count; i++) {
            Particle p = particlePool.obtain();
            initParticle(p, x, y, ParticleType.DEFAULT);
            activeParticles.add(p);
        }
    }

    // ==========================================
    // 核心逻辑
    // ==========================================

    private void initParticle(Particle p, float x, float y, ParticleType type) {
        p.x = x;
        p.y = y;
        p.type = type;
        p.active = true;

        if (type == ParticleType.MIST) {
            // 雾气：角度向上 (45~135度)，速度慢
            float angle = MathUtils.random(45f, 135f) * MathUtils.degreesToRadians;
            float speed = MathUtils.random(15f, 40f);

            p.vx = MathUtils.cos(angle) * speed;
            p.vy = MathUtils.sin(angle) * speed + 15f;
            p.size = MathUtils.random(6f, 10f);
            p.lifetime = MathUtils.random(0.6f, 1.0f);

        } else if (type == ParticleType.DROPLET) {
            // 液滴：四散炸开，速度快
            float angle = MathUtils.random(0f, 360f) * MathUtils.degreesToRadians;
            float speed = MathUtils.random(60f, 160f);

            p.vx = MathUtils.cos(angle) * speed;
            p.vy = MathUtils.sin(angle) * speed;
            p.size = MathUtils.random(3f, 5f);
            p.lifetime = MathUtils.random(0.4f, 0.6f);

        } else {
            // Default
            float angle = MathUtils.random(0f, 360f) * MathUtils.degreesToRadians;
            float speed = MathUtils.random(50f, 100f);
            p.vx = MathUtils.cos(angle) * speed;
            p.vy = MathUtils.sin(angle) * speed;
            p.size = MathUtils.random(2f, 5f);
            p.lifetime = 0.5f;
        }
        p.maxLifetime = p.lifetime;
        p.alpha = 1.0f;
    }

    public void update(float delta) {
        for (int i = activeParticles.size - 1; i >= 0; i--) {
            Particle p = activeParticles.get(i);

            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.lifetime -= delta;

            if (p.type == ParticleType.MIST) {
                p.vy += 20f * delta;   // 向上浮
                p.size += 15f * delta; // 变大
            } else if (p.type == ParticleType.DROPLET) {
                p.vy -= 500f * delta;  // 重力下坠
            } else {
                p.vy -= 200f * delta;
            }

            p.alpha = Math.max(0, p.lifetime / p.maxLifetime);

            if (p.lifetime <= 0) {
                activeParticles.removeIndex(i);
                particlePool.free(p);
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (activeParticles.size == 0) return;

        batch.end(); // 暂停 SpriteBatch 以使用 ShapeRenderer
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Particle p : activeParticles) {
            if (p.type == ParticleType.MIST) {
                // 米白色雾气
                shapeRenderer.setColor(0.95f, 0.92f, 0.85f, p.alpha * 0.6f);
            } else if (p.type == ParticleType.DROPLET) {
                // 奶茶棕色
                shapeRenderer.setColor(0.85f, 0.75f, 0.65f, p.alpha);
            } else {
                shapeRenderer.setColor(0.2f, 0.2f, 0.2f, p.alpha);
            }
            shapeRenderer.circle(p.x, p.y, p.size / 2);
        }

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin(); // 恢复 SpriteBatch
    }

    public void clearAllParticles() {
        particlePool.freeAll(activeParticles);
        activeParticles.clear();
    }

    public int getActiveParticleCount() {
        return activeParticles.size;
    }

    public void resetStats() {
        // 可选：重置统计数据
    }

    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}