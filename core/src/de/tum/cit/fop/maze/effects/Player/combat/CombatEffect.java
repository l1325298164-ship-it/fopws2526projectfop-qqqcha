package de.tum.cit.fop.maze.effects.Player.combat;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

    /**
     * 更新逻辑
     * @param delta 时间增量
     * @param ps 粒子系统，允许特效在 update 时自己生成粒子
     */
    public void update(float delta, CombatParticleSystem ps) {
        timer += delta;
        if (timer >= maxDuration) isFinished = true;
        onUpdate(delta, ps);
    }

    protected abstract void onUpdate(float delta, CombatParticleSystem ps);

    /**
     * 🟢 形状/粒子层渲染 (使用 ShapeRenderer)
     * 适合：刀光几何体、火花、圆环、线条
     * 注意：这一层通常开启 GL_BLEND 混合模式以实现发光效果
     */
    public abstract void renderShape(ShapeRenderer sr);

    /**
     * 🔵 贴图/文字层渲染 (使用 SpriteBatch)
     * 适合：伤害数字、复杂的魔法阵图片、图标
     */
    public void renderSprite(SpriteBatch batch) {
        // 默认留空，子类按需覆盖
    }

    public boolean isFinished() { return isFinished; }
}