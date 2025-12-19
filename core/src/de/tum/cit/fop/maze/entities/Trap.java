package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.TextureManager;

public class Trap extends GameObject {

    private boolean isActive = true;
    private int damage = 1;

    private Texture trapTexture;
    private TextureManager textureManager;
    private boolean needsTextureUpdate = true;

    // 动画相关
    private boolean triggered = false;
    private float animationTimer = 0f;

    private static final float ANIMATION_DURATION = 0.4f;

    public Trap(int x, int y) {
        super(x, y);
        textureManager = TextureManager.getInstance();
        updateTexture();
    }

    /* ================== 逻辑 ================== */

    public void onPlayerStep(Player player) {
        if (!isActive) return;

        player.takeDamage(damage);

        // 启动被踩动画
        triggered = true;
        animationTimer = 0f;

        isActive = false; // 只触发一次
    }


    public void update(float delta) {
        if (triggered) {
            animationTimer += delta;
            if (animationTimer >= ANIMATION_DURATION) {
                triggered = false; // 动画结束
            }
        }
    }

    /* ================== 纹理 ================== */

    private void updateTexture() {
        trapTexture = textureManager.getTrapTexture(); // TextureManager 中已有
        needsTextureUpdate = false;
    }

    private boolean shouldBeVisible() {
        // 被踩动画期间一定可见
        if (triggered) return true;

        // 平时只在 COLOR 模式可见
        return textureManager.getCurrentMode() == TextureManager.TextureMode.COLOR;
    }

    /* ================== 渲染 ================== */

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        if (!active || trapTexture != null) return;
        if (!shouldBeVisible()) return;

        float progress = triggered
                ? animationTimer / ANIMATION_DURATION
                : 0f;
        progress = Math.min(progress, 1f);

        float scale = triggered
                ? 0.6f + 0.4f * progress
                : 0.8f;

        float alpha = triggered
                ? 1f - progress
                : 0.8f;

        float size = GameConstants.CELL_SIZE * scale;
        float offset = (GameConstants.CELL_SIZE - size) / 2f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(1f, 0f, 0f, alpha));

        shapeRenderer.rect(
                x * GameConstants.CELL_SIZE + offset,
                y * GameConstants.CELL_SIZE + offset,
                size,
                size
        );

        shapeRenderer.end();
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active || trapTexture == null) return;
        if (!shouldBeVisible()) return;

        if (needsTextureUpdate) {
            updateTexture();
        }

        float progress = triggered
                ? animationTimer / ANIMATION_DURATION
                : 0f;
        progress = Math.min(progress, 1f);

        float scale = triggered
                ? 0.6f + 0.4f * progress
                : 1f;

        float alpha = triggered
                ? 1f - progress
                : 1f;

        float size = GameConstants.CELL_SIZE * scale;
        float offset = (GameConstants.CELL_SIZE - size) / 2f;

        batch.setColor(1f, 1f, 1f, alpha);

        batch.draw(
                trapTexture,
                x * GameConstants.CELL_SIZE + offset,
                y * GameConstants.CELL_SIZE + offset,
                size,
                size
        );

        batch.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public RenderType getRenderType() {
        if (textureManager.getCurrentMode() == TextureManager.TextureMode.COLOR ||
                textureManager.getCurrentMode() == TextureManager.TextureMode.MINIMAL ||
                trapTexture == null) {
            return RenderType.SHAPE;
        }
        return RenderType.SPRITE;
    }
}
