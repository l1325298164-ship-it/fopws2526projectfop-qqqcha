package de.tum.cit.fop.maze.effects.environment;

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
    public abstract void render(ShapeRenderer sr);
    public boolean isFinished() { return isFinished; }
}