package de.tum.cit.fop.maze.effects.environment.items.traps;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
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

    // === 渲染逻辑 (分层) ===

    /**
     * 🟢 阶段 1: 渲染陷阱的光效、几何体和粒子 (ShapeRenderer)
     * 在 GameScreen 中，应在 batch.end() 之后调用
     */
    public void renderShapes(ShapeRenderer sr) {
        // 开启混合模式以支持透明度和光效
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.begin(ShapeRenderer.ShapeType.Filled);

        for (EnvironmentEffect effect : effects) {
            // 🔴 修正：调用新的 renderShape 方法
            effect.renderShape(sr);
        }

        // 渲染粒子系统
        particleSystem.render(sr);

        sr.end();
    }

    /**
     * 🔵 阶段 2: 渲染陷阱的贴图/文字 (SpriteBatch)
     * 在 GameScreen 中，应在 batch.begin() 和 batch.end() 之间调用
     * (虽然目前的陷阱子类里这个方法是空的，但必须调用以保证接口完整)
     */
    public void renderSprites(SpriteBatch batch) {
        for (EnvironmentEffect effect : effects) {
            effect.renderSprite(batch);
        }
    }

    public void dispose() {
        effects.clear();
        particleSystem.clear();
    }
}