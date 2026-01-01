package de.tum.cit.fop.maze.effects.Player.combat;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.effects.combat.instances.*;
import de.tum.cit.fop.maze.effects.Player.combat.instances.*;

import java.util.Iterator;

public class CombatEffectManager {

    private Array<CombatEffect> effects;
    private CombatParticleSystem particleSystem; // 独立的战斗粒子系统

    public CombatEffectManager() {
        this.effects = new Array<>();
        this.particleSystem = new CombatParticleSystem();
    }

    // === 外部调用接口 ===

    /**
     * 挥剑攻击
     * @param level 1=普通, 2=进阶, 3=炫彩大招
     */
    public void spawnSlash(float x, float y, float angle, int level) {
        effects.add(new SlashEffect(x, y, angle, level));
    }

    public void spawnFire(float x, float y) {
        effects.add(new FireMagicEffect(x, y));
    }

    public void spawnHeal(float x, float y) {
        effects.add(new HealEffect(x, y));
    }

    public void spawnLaser(float startX, float startY, float endX, float endY) {
        effects.add(new LaserEffect(startX, startY, endX, endY));
    }

    public void spawnDebuff(float x, float y) {
        effects.add(new DebuffEffect(x, y));
    }

    // === 核心循环 ===

    public void update(float delta) {
        // 1. 更新特效逻辑 (生成粒子)
        Iterator<CombatEffect> it = effects.iterator();
        while (it.hasNext()) {
            CombatEffect e = it.next();
            e.update(delta, particleSystem);
            if (e.isFinished()) it.remove();
        }

        // 2. 更新粒子物理
        particleSystem.update(delta);
    }

    public void render(ShapeRenderer sr) {
        // 🔥 关键：开启加法混合模式 (Additive Blending)
        // 这会让重叠的粒子变亮，产生发光感
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        sr.begin(ShapeRenderer.ShapeType.Filled);

        // 绘制特效主体
        for (CombatEffect e : effects) {
            e.render(sr);
        }

        // 绘制粒子
        particleSystem.render(sr);

        sr.end();

        // 恢复默认混合模式
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void dispose() {
        effects.clear();
        particleSystem.clear();
    }
}