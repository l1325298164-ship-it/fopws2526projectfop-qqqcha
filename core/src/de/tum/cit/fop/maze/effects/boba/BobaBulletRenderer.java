package de.tum.cit.fop.maze.effects.boba;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.EnemyBoba.BobaBullet;
import de.tum.cit.fop.maze.game.GameConstants;

/**
 * 适配格子坐标的珍珠子弹渲染器
 * 支持形变 (Squash & Stretch) 和旋转
 */
public class BobaBulletRenderer {

    private float effectIntensity = 1.0f;
    private TextureRegion bulletTexture;
    private final ShapeRenderer shapeRenderer;

    public BobaBulletRenderer() {
        shapeRenderer = new ShapeRenderer();
        loadTexture();
    }

    private void loadTexture() {
        try {
            // 尝试加载资源，文件名需确保正确
            Texture tex = new Texture(Gdx.files.internal("effects/boba-pearl-bullet.png"));
            // 线性过滤让旋转更平滑
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            this.bulletTexture = new TextureRegion(tex);
        } catch (Exception e) {
            // 资源不存在时不报错，静默失败，稍后使用 ShapeRenderer 备用
            this.bulletTexture = null;
        }
    }

    public void render(BobaBullet bullet, SpriteBatch batch) {
        if (bullet == null || !bullet.isActive()) return;

        // 1. 获取基础渲染参数
        float x = bullet.getRealX();
        float y = bullet.getRealY();
        float radius = GameConstants.CELL_SIZE * 0.25f; // 基础半径
        float size = radius * 2;

        // 屏幕绘制坐标 (左下角)
        float drawX = x - radius;
        float drawY = y - radius;

        // 2. 获取特效参数 (来自 BobaBullet 的物理计算)
        float scaleX = bullet.getScaleX() * effectIntensity;
        float scaleY = bullet.getScaleY() * effectIntensity;
        float rotation = bullet.getRotation();

        // 3. 绘制
        if (bulletTexture != null) {
            // 使用纹理绘制 (支持旋转和缩放)
            // originX/Y 设置为中心点，以便围绕中心旋转和缩放
            batch.setColor(1f, 1f, 1f, 1f);
            batch.draw(
                    bulletTexture,
                    drawX, drawY,               // 绘制位置
                    radius, radius,             // 旋转/缩放中心 (origin)
                    size, size,                 // 基础宽高
                    scaleX, scaleY,             // 缩放比例
                    rotation                    // 旋转角度
            );
        } else {
            // 备用绘制方案：简单的深色圆形
            batch.end();
            shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            // 珍珠黑
            shapeRenderer.setColor(0.2f, 0.1f, 0.05f, 1f);

            // 注意：ShapeRenderer 不直接支持带旋转的椭圆，
            // 这里简单模拟缩放，不处理旋转以保持简单
            shapeRenderer.ellipse(
                    x - (radius * scaleX),
                    y - (radius * scaleY),
                    size * scaleX,
                    size * scaleY
            );

            shapeRenderer.end();
            batch.begin();
        }
    }

    public void setEffectIntensity(float intensity) {
        this.effectIntensity = Math.max(0.1f, Math.min(2.0f, intensity));
    }

    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (bulletTexture != null && bulletTexture.getTexture() != null) {
            bulletTexture.getTexture().dispose();
        }
    }
}