package de.tum.cit.fop.maze.effects.boba;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import de.tum.cit.fop.maze.game.GameConstants;

/**
 * 粒子池 - 支持奶茶飞溅特效
 */
public class BobaParticlePool {

    public enum ParticleType {
        DEFAULT, // 普通黑珍珠碎屑
        MILK     // 奶茶液滴
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
    private final ShapeRenderer shapeRenderer; // 用于绘制简单粒子

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
     * 创建普通爆开效果 (珍珠黑)
     */
    public void createBurstEffect(float x, float y) {
        createEffect(x, y, 8, ParticleType.DEFAULT);
    }

    /**
     * 创建奶茶飞溅效果 (奶茶色)
     */
    public void createSplashEffect(float x, float y) {
        createEffect(x, y, 12, ParticleType.MILK);
    }

    private void createEffect(float x, float y, int count, ParticleType type) {
        for (int i = 0; i < count; i++) {
            Particle p = particlePool.obtain();
            p.x = x;
            p.y = y;
            p.type = type;
            p.active = true;

            // 随机方向
            float angle = MathUtils.random(0f, MathUtils.PI2);
            float speed;

            if (type == ParticleType.MILK) {
                // 奶茶液滴：速度更快，范围更广
                speed = MathUtils.random(60f, 180f);
                p.size = MathUtils.random(2f, 5f);
                p.lifetime = MathUtils.random(0.4f, 0.8f);
            } else {
                // 普通碎屑
                speed = MathUtils.random(30f, 100f);
                p.size = MathUtils.random(3f, 6f);
                p.lifetime = MathUtils.random(0.3f, 0.6f);
            }

            p.maxLifetime = p.lifetime;
            p.vx = MathUtils.cos(angle) * speed;
            p.vy = MathUtils.sin(angle) * speed;
            p.alpha = 1.0f;

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
            if (p.type == ParticleType.MILK) {
                p.vy -= 400 * delta; // 液体重力感更强
            } else {
                p.vy -= 150 * delta; // 烟雾/碎屑飘得慢
            }

            // 更新透明度
            p.alpha = p.lifetime / p.maxLifetime;

            if (p.lifetime <= 0) {
                activeParticles.removeIndex(i);
                particlePool.free(p);
            }
        }
    }

    /**
     * 渲染所有粒子
     * 注意：我们需要暂时结束 SpriteBatch 来使用 ShapeRenderer，或者直接用 batch 画纹理
     * 这里为了简单且效果好，使用 ShapeRenderer 画圆
     */
    public void render(SpriteBatch batch) {
        if (activeParticles.size == 0) return;

        batch.end(); // 暂停 SpriteBatch

        // 开启混合模式以支持透明度
        com.badlogic.gdx.Gdx.gl.glEnable(com.badlogic.gdx.Gdx.gl.GL_BLEND);
        com.badlogic.gdx.Gdx.gl.glBlendFunc(com.badlogic.gdx.Gdx.gl.GL_SRC_ALPHA, com.badlogic.gdx.Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Particle p : activeParticles) {
            if (p.type == ParticleType.MILK) {
                // 奶茶色 (米白/浅棕)
                shapeRenderer.setColor(0.85f, 0.75f, 0.65f, p.alpha);
            } else {
                // 珍珠黑 (深紫黑)
                shapeRenderer.setColor(0.1f, 0.05f, 0.15f, p.alpha);
            }
            shapeRenderer.circle(p.x, p.y, p.size / 2);
        }

        shapeRenderer.end();
        com.badlogic.gdx.Gdx.gl.glDisable(com.badlogic.gdx.Gdx.gl.GL_BLEND);

        batch.begin(); // 恢复 SpriteBatch
    }

    public void clearAllParticles() {
        particlePool.freeAll(activeParticles);
        activeParticles.clear();
    }

    public int getActiveParticleCount() {
        return activeParticles.size;
    }

    public void resetStats() {}

    public void dispose() {
        shapeRenderer.dispose();
    }
}