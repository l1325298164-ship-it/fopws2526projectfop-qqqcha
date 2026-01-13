package de.tum.cit.fop.maze.effects.environment;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public abstract class EnvironmentEffect {
    protected float x, y;
    protected float timer;
    protected float maxDuration;
    protected boolean isFinished;

    public EnvironmentEffect(float x, float y, float duration) {
        this.x = x;
        this.y = y;
        this.maxDuration = duration;
        this.timer = 0;
    }

    public void update(float delta, EnvironmentParticleSystem ps) {
        timer += delta;
        if (timer >= maxDuration) isFinished = true;
        onUpdate(delta, ps);
    }

    protected abstract void onUpdate(float delta, EnvironmentParticleSystem ps);

    /**
     * 🟢 几何/粒子层渲染 (使用 ShapeRenderer)
     * 适合：光圈、几何图形、粒子
     */
    public abstract void renderShape(ShapeRenderer sr);

    /**
     * 🔵 贴图/文字层渲染 (使用 SpriteBatch)
     * 适合：复杂的物品贴图、文字
     */
    public abstract void renderSprite(SpriteBatch batch);

    public boolean isFinished() { return isFinished; }
}