package de.tum.cit.fop.maze.effects.boba;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.EnemyBoba.BobaBullet;
import de.tum.cit.fop.maze.game.GameConstants;

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
            // 定义路径
            String path = "effects/boba-bullet.png";
            com.badlogic.gdx.files.FileHandle file = Gdx.files.internal(path);

            // 1. 检查文件是否存在
            if (!file.exists()) {
                System.err.println("❌ [BobaError] 找不到图片文件! 请检查路径: assets/" + path);
                System.err.println("   当前工作目录: " + Gdx.files.getLocalStoragePath());
                this.bulletTexture = null;
                return;
            }

            // 2. 尝试加载
            Texture tex = new Texture(file);
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            this.bulletTexture = new TextureRegion(tex);

            System.out.println("✅ [BobaSuccess] 成功加载子弹贴图: " + path);

        } catch (Exception e) {
            System.err.println("❌ [BobaError] 图片加载崩溃: " + e.getMessage());
            e.printStackTrace();
            this.bulletTexture = null;
        }
    }

    /*private void loadTexture() {
        try {
            Texture tex = new Texture(Gdx.files.internal("effects/boba-bullet.png"));
            tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            this.bulletTexture = new TextureRegion(tex);
        } catch (Exception e) {
            System.err.println("Boba texture load failed: " + e.getMessage());
            this.bulletTexture = null;
        }
    }
*/

    public void render(BobaBullet bullet, SpriteBatch batch) {
        if (bullet == null || !bullet.isActive()) return;

        //  修复：将格子坐标转换为像素坐标
        float x = bullet.getRealX() * GameConstants.CELL_SIZE;
        float y = bullet.getRealY() * GameConstants.CELL_SIZE;

        float radius = GameConstants.CELL_SIZE * 0.25f;
        float size = radius * 2;

        float scaleX = bullet.getScaleX() * effectIntensity;
        float scaleY = bullet.getScaleY() * effectIntensity;
        float rotation = bullet.getRotation();

        if (bulletTexture != null) {
            batch.setColor(1f, 1f, 1f, 1f);

            // 使用修正后的 x, y 计算绘制位置
            // 我们希望子弹中心对齐格子中心，所以加上半个格子的偏移
            float centerX = x + GameConstants.CELL_SIZE / 2f;
            float centerY = y + GameConstants.CELL_SIZE / 2f;

            batch.draw(
                    bulletTexture,
                    centerX - radius, centerY - radius, // 绘制起点
                    radius, radius,                     // 旋转中心
                    size, size,                         // 宽高
                    scaleX, scaleY,                     // 缩放
                    rotation                            // 旋转
            );
        } else {
            // ... (备用渲染代码也最好同步修正)
            batch.end();
            shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

            float centerX = x + GameConstants.CELL_SIZE / 2f;
            float centerY = y + GameConstants.CELL_SIZE / 2f;

            shapeRenderer.setColor(Color.ORANGE);
            shapeRenderer.circle(centerX, centerY, radius * Math.min(scaleX, scaleY));

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