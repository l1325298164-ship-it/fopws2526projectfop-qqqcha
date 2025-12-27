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

        // 如果已经触发过且动画结束，不显示
        if (!isActive) return false;

        // 修改：在 IMAGE 模式也可见
        TextureManager.TextureMode mode = textureManager.getCurrentMode();
        return mode == TextureManager.TextureMode.IMAGE ||
                mode == TextureManager.TextureMode.COLOR;
    }

    /* ================== 渲染 ================== */

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        // 修改：不检查 trapTexture != null，因为 SHAPE 模式本就不需要纹理
        if (!active) return;  // 只检查 active，不检查 isActive
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
                : (isActive ? 0.8f : 0.3f); // 未激活的陷阱半透明

        float size = GameConstants.CELL_SIZE * scale;
        float offset = (GameConstants.CELL_SIZE - size) / 2f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // 根据状态改变颜色：已激活红色，已触发灰色
        if (isActive) {
            shapeRenderer.setColor(new Color(1f, 0f, 0f, alpha)); // 红色
        } else if (triggered) {
            shapeRenderer.setColor(new Color(1f, 0.5f, 0f, alpha)); // 触发中橙色
        } else {
            shapeRenderer.setColor(new Color(0.5f, 0.5f, 0.5f, alpha)); // 已触发灰色
        }

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
        // 修改：不检查 isActive，因为触发后仍然要显示
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
                : (isActive ? 1f : 0.4f); // 未激活的陷阱半透明

        float size = GameConstants.CELL_SIZE * scale;
        float offset = (GameConstants.CELL_SIZE - size) / 2f;

        // 根据状态调整颜色
        if (isActive) {
            batch.setColor(1f, 1f, 1f, alpha); // 正常颜色
        } else if (triggered) {
            batch.setColor(1f, 0.6f, 0.2f, alpha); // 触发中偏橙色
        } else {
            batch.setColor(0.5f, 0.5f, 0.5f, alpha); // 已触发灰色
        }

        batch.draw(
                trapTexture,
                x * GameConstants.CELL_SIZE + offset,
                y * GameConstants.CELL_SIZE + offset,
                size,
                size
        );

        batch.setColor(1f, 1f, 1f, 1f); // 恢复默认颜色
    }

    @Override
    public RenderType getRenderType() {
        TextureManager.TextureMode mode = textureManager.getCurrentMode();

        // 修改：IMAGE 模式使用 SPRITE 渲染
        if (mode == TextureManager.TextureMode.IMAGE && trapTexture != null) {
            return RenderType.SPRITE;
        }

        if (mode == TextureManager.TextureMode.COLOR ||
                mode == TextureManager.TextureMode.MINIMAL ||
                trapTexture == null) {
            return RenderType.SHAPE;
        }

        // 默认使用 SPRITE
        return RenderType.SPRITE;
    }

    /* ================== Getter 方法 ================== */

    public boolean isActive() {
        return isActive;
    }

    public boolean isTriggered() {
        return triggered;
    }
}