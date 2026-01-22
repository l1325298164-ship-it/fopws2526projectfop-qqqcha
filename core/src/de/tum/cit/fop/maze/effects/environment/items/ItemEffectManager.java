package de.tum.cit.fop.maze.effects.environment.items;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;
import de.tum.cit.fop.maze.utils.Logger;

import java.util.Iterator;

public class ItemEffectManager {
    private static final int MAX_EFFECTS = 100; // 物品特效通常较少
    private Array<EnvironmentEffect> effects;
    private EnvironmentParticleSystem particleSystem;

    public ItemEffectManager() {
        this.effects = new Array<>();
        this.particleSystem = new EnvironmentParticleSystem();
    }

    // === 生成接口 ===

    /**
     * 安全添加特效，防止列表无限膨胀
     */
    private void safeAddEffect(EnvironmentEffect effect) {
        if (effect == null) {
            Logger.warning("Attempted to add null effect to ItemEffectManager");
            return;
        }
        if (effects.size >= MAX_EFFECTS) {
            if (effects.size > 0) {
                effects.removeIndex(0); // 移除最旧的特效
                Logger.debug("ItemEffectManager reached max effects limit, removed oldest effect");
            }
        }
        effects.add(effect);
    }

    public void spawnTreasure(float x, float y) {
        safeAddEffect(new TreasureEffect(x, y));
    }

    public void spawnHeart(float x, float y) {
        safeAddEffect(new HeartEffect(x, y));
    }

    // 新增：接管钥匙特效
    public void spawnKeyEffect(float x, float y, Texture texture) {
        safeAddEffect(new KeyCollectEffect(x, y, texture));
    }

    // === 更新逻辑 ===

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
     * 阶段 1: 渲染光效和粒子 (ShapeRenderer)
     * 在 GameScreen 中，应该在 batch.end() 之后，单独调用
     */
    public void renderShapes(ShapeRenderer sr) {
        if (sr == null) {
            Logger.warning("ShapeRenderer is null, cannot render item effect shapes");
            return;
        }
        // 开启混合模式 (让光效叠加更好看，且不过曝)
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        sr.begin(ShapeRenderer.ShapeType.Filled);

        for (EnvironmentEffect effect : effects) {
            effect.renderShape(sr);
        }
        particleSystem.render(sr);

        sr.end();
    }

    /**
     * 阶段 2: 渲染物品贴图 (SpriteBatch)
     * 在 GameScreen 中，应该在 batch.begin() 和 batch.end() 之间调用
     */
    public void renderSprites(SpriteBatch batch) {
        if (batch == null) {
            Logger.warning("SpriteBatch is null, cannot render item effect sprites");
            return;
        }
        for (EnvironmentEffect effect : effects) {
            effect.renderSprite(batch);
        }
    }

    public void dispose() {
        effects.clear();
        particleSystem.clear();
    }
}