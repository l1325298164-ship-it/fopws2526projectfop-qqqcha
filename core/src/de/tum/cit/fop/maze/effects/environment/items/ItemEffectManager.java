package de.tum.cit.fop.maze.effects.environment.items;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;
import java.util.Iterator;

public class ItemEffectManager {
    private Array<EnvironmentEffect> effects;
    private EnvironmentParticleSystem particleSystem;

    public ItemEffectManager() {
        this.effects = new Array<>();
        this.particleSystem = new EnvironmentParticleSystem();
    }

    public void spawnTreasure(float x, float y) {
        effects.add(new TreasureEffect(x, y));
    }

    public void spawnHeart(float x, float y) {
        effects.add(new HeartEffect(x, y));
    }

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
        // 🔥 开启发光混合模式 (Additive)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        sr.begin(ShapeRenderer.ShapeType.Filled);

        for (EnvironmentEffect effect : effects) {
            effect.render(sr);
        }
        particleSystem.render(sr);

        sr.end();

        // 恢复默认
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void dispose() { effects.clear(); particleSystem.clear(); }
}