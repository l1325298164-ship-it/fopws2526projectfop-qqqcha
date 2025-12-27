package de.tum.cit.fop.maze.effects.boba;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

/**
 * 粒子池 - 奶茶雾气与飞溅特效
 */
public class BobaParticlePool {

    public enum ParticleType {
        MIST,   // ⭐ 新增：奶茶雾气 (向上蒸发)
        DROPLET // ⭐ 新增：奶茶液滴 (受重力下落)
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

    /**
     * 创建奶茶雾气蒸发效果 (替代原来的 Burst)
     */
    public void createMistEffect(float x, float y) {
        // 产生 5-8 个雾气粒子
        for (int i = 0; i < 8; i++) {
            Particle p = particlePool.obtain();
            initParticle(p, x, y, ParticleType.MIST);
            activeParticles.add(p);
        }
    }

    /**
     * 创建奶茶液滴飞溅效果
     */
    public void createSplashEffect(float x, float y) {
        // 产生 6-10 个液滴
        for (int i = 0; i < 10; i++) {
            Particle p = particlePool.obtain();
            initParticle(p, x, y, ParticleType.DROPLET);
            activeParticles.add(p);
        }
    }

    private void initParticle(Particle p, float x, float y, ParticleType type) {
        p.x = x;
        p.y = y;
        p.type = type;
        p.active = true;

        if (type == ParticleType.MIST) {
            // 雾气：向上的速度为主，左右随机漂浮
            float angle = MathUtils.random(45f, 135f) * MathUtils.degreesToRadians; // 朝上
            float speed = MathUtils.random(10f, 40f);
            p.vx = MathUtils.cos(angle) * speed;
            p.vy = MathUtils.sin(angle) * speed + 20f; // 基础上升速度

            p.size = MathUtils.random(6f, 12f); // 雾气比较大
            p.lifetime = MathUtils.random(0.5f, 0.8f); // 消失得慢一点
            p.maxLifetime = p.lifetime;
        } else {
            // 液滴：向四周炸开，速度快
            float angle = MathUtils.random(0f, 360f) * MathUtils.degreesToRadians;
            float speed = MathUtils.random(50f, 150f);
            p.vx = MathUtils.cos(angle) * speed;
            p.vy = MathUtils.sin(angle) * speed;

            p.size = MathUtils.random(3f, 5f); // 液滴比较小
            p.lifetime = MathUtils.random(0.3f, 0.5f);
            p.maxLifetime = p.lifetime;
        }
    }

    public void update(float delta) {
        for (int i = activeParticles.size - 1; i >= 0; i--) {
            Particle p = activeParticles.get(i);

            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.lifetime -= delta;

            if (p.type == ParticleType.DROPLET) {
                // 液滴受重力下落
                p.vy -= 400f * delta;
            } else {
                // 雾气缓慢上升且变大
                p.vy += 10f * delta;
                p.size += 15f * delta; // 扩散感
            }

            p.alpha = p.lifetime / p.maxLifetime; // 慢慢消失

            if (p.lifetime <= 0) {
                activeParticles.removeIndex(i);
                particlePool.free(p);
            }
        }
    }

    public void render(SpriteBatch batch) {
        if (activeParticles.size == 0) return;

        batch.end();
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.Gdx.gl.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(com.badlogic.gdx.Gdx.gl.GL_SRC_ALPHA, com.badlogic.gdx.Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Particle p : activeParticles) {
            if (p.type == ParticleType.MIST) {
                // 奶茶色雾气 (米白/浅棕，半透明)
                shapeRenderer.setColor(0.95f, 0.9f, 0.8f, p.alpha * 0.6f);
            } else {
                // 奶茶液滴 (稍微深一点的棕色，不透明度高)
                shapeRenderer.setColor(0.85f, 0.75f, 0.65f, p.alpha);
            }
            shapeRenderer.circle(p.x, p.y, p.size / 2);
        }

        shapeRenderer.end();
        com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.Gdx.gl.GL_BLEND);
        batch.begin();
    }

    public void dispose() {
        shapeRenderer.dispose();
    }
}