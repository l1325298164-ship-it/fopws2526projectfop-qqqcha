package de.tum.cit.fop.maze.effects.Player.combat;

import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public abstract class CombatEffect {
    protected float x, y;
    protected float timer;
    protected float maxDuration;
    protected boolean isFinished;

    public CombatEffect(float x, float y, float duration) {
        this.x = x;
        this.y = y;
        this.maxDuration = duration;
        this.timer = 0;
    }

    public void update(float delta, CombatParticleSystem ps) {
        timer += delta;
        if (timer >= maxDuration) isFinished = true;
        onUpdate(delta, ps);
    }

    // 子类必须实现：每一帧的逻辑（如生成粒子）
    protected abstract void onUpdate(float delta, CombatParticleSystem ps);

    // 子类必须实现：绘制几何主体
    public abstract void render(ShapeRenderer sr);

    public boolean isFinished() { return isFinished; }
}