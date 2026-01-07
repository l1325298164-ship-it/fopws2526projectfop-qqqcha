package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;
import java.util.Iterator;

public class TrapEffectManager {
    private Array<EnvironmentEffect> effects;
    private EnvironmentParticleSystem particleSystem;

    public TrapEffectManager() {
        this.effects = new Array<>();
        this.particleSystem = new EnvironmentParticleSystem();
    }

    // === 生成接口 ===

    public void spawnMudTrap(float x, float y) {
        effects.add(new MudTrapEffect(x, y));
    }

    public void spawnGeyser(float x, float y) {
        effects.add(new GeyserTrapEffect(x, y));
    }

    public void spawnPearlMine(float x, float y) {
        effects.add(new PearlMineEffect(x, y));
    }

    public void spawnTeaShards(float x, float y) {
        effects.add(new TeaShardsEffect(x, y));
    }

    // === 核心循环 ===

    public void update(float delta) {
        Iterator<EnvironmentEffect> it = effects.iterator();
        while (it.hasNext()) {
            EnvironmentEffect effect = it.next();
            effect.update(delta, particleSystem);
            if (effect.isFinished()) it.remove();
        }
        particleSystem.update(delta);
    }

    public void render(ShapeRenderer sr) {
        // 开启混合模式以支持透明度和光效
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.begin(ShapeRenderer.ShapeType.Filled);

        for (EnvironmentEffect effect : effects) {
            effect.render(sr);
        }

        // 渲染粒子系统
        particleSystem.render(sr);

        sr.end();
    }

    public void dispose() {
        effects.clear();
        particleSystem.clear();
    }
}