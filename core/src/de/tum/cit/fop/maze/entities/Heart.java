package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.Color;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;

/**
 * 红心实体 (编译修复纯净版)
 * 逻辑已移交 GameManager
 */
public class Heart extends GameObject {
    private boolean active = true;
    private Texture texture;

    // ✅ Fix 1: 构造函数参数匹配 GameObject(int, int)
    public Heart(int x, int y) {
        super(x, y);
        loadTexture();
    }

    // 兼容浮点数调用的重载（如果有地方用了float）
    public Heart(float x, float y) {
        super((int) x, (int) y);
        loadTexture();
    }

    private void loadTexture() {
        try {
            texture = new Texture(Gdx.files.internal("imgs/Items/heart.png"));
        } catch (Exception e) {
            Logger.error("Heart texture missing");
        }
    }

    @Override
    public void onInteract(Player player) {
        // GameManager 会处理加血，这里只需要把自己标记为非活跃（移除）
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    // ✅ Fix 2: 实现 GameObject 的抽象方法
    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (active && texture != null) {
            // 简单的浮动动画
            float bob = (float) Math.sin(System.currentTimeMillis() / 200.0) * 3;
            batch.draw(texture,
                    x * GameConstants.CELL_SIZE + 8,
                    y * GameConstants.CELL_SIZE + 8 + bob,
                    32, 32);
        }
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        if (!active) return;
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.circle(
                x * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f,
                y * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f,
                10
        );
    }

    public void dispose() {
        if (texture != null) texture.dispose();
    }
}