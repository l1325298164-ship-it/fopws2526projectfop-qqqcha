package de.tum.cit.fop.maze.effects.environment.items;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import de.tum.cit.fop.maze.effects.environment.EnvironmentEffect;
import de.tum.cit.fop.maze.effects.environment.EnvironmentParticleSystem;

public class KeyCollectEffect extends EnvironmentEffect {
    private final Texture texture;
    private final float startY;
    private final float jumpHeight = 64f;

    public KeyCollectEffect(float x, float y, Texture texture) {
        super(x, y, 1.0f); // 持续 1.0 秒
        this.startY = y;
        this.texture = texture;
    }

    @Override
    protected void onUpdate(float delta, EnvironmentParticleSystem ps) {
        // 不需要粒子
    }

    @Override
    public void renderShape(ShapeRenderer sr) {
        // 不需要几何绘制
    }

    @Override
    public void renderSprite(SpriteBatch batch) {
        float progress = Math.min(1.0f, timer / maxDuration);

        // 动画计算
        float currentY = startY + Interpolation.swingOut.apply(0, jumpHeight, progress);
        float scale = Interpolation.smooth.apply(1.0f, 2.0f, progress);
        float rotation = Interpolation.pow2In.apply(0f, 720f, progress);

        float alpha = 1.0f;
        if (progress > 0.8f) {
            alpha = 1.0f - (progress - 0.8f) / 0.2f;
        }

        float width = 32;
        float height = 32;
        float originX = width / 2;
        float originY = height / 2;

        int srcFunc = batch.getBlendSrcFunc();
        int dstFunc = batch.getBlendDstFunc();

        // Layer 1: 金色光晕 (Additive)
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        batch.setColor(1f, 0.9f, 0.2f, alpha * 0.5f);
        batch.draw(texture, x, currentY, originX, originY, width, height,
                scale * 1.5f, scale * 1.5f, rotation, 0, 0,
                texture.getWidth(), texture.getHeight(), false, false);

        // Layer 2: 本体 (Normal)
        batch.setBlendFunction(srcFunc, dstFunc);
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(texture, x, currentY, originX, originY, width, height,
                scale, scale, rotation, 0, 0,
                texture.getWidth(), texture.getHeight(), false, false);

        batch.setColor(Color.WHITE);
    }
}