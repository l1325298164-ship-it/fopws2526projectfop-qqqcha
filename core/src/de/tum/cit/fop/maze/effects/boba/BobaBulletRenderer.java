package de.tum.cit.fop.maze.effects.boba;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.entities.BobaBullet;
import de.tum.cit.fop.maze.game.GameConstants;

/**
 * 适配格子坐标的珍珠子弹渲染器
 */
public class BobaBulletRenderer {

    private float effectIntensity = 1.0f;

    /**
     * 渲染珍珠子弹
     */
    public void render(BobaBullet bullet, SpriteBatch batch) {
        if (bullet == null || !bullet.isActive()) return;

        // 获取子弹的真实像素坐标
        float screenX = bullet.getRealX() - GameConstants.CELL_SIZE/2;
        float screenY = bullet.getRealY() - GameConstants.CELL_SIZE/2;

        // 计算视觉效果参数
        float currentSize = bullet.getCurrentRenderSize() * effectIntensity;
        float wobbleX = bullet.getWobbleOffsetX() * effectIntensity;
        float wobbleY = bullet.getWobbleOffsetY() * effectIntensity;
        float rotation = bullet.getRotation();

        // 获取子弹颜色
        float[] color = bullet.getColor();

        // 保存原始颜色
        com.badlogic.gdx.graphics.Color originalColor = batch.getColor();

        // 应用子弹颜色
        batch.setColor(color[0], color[1], color[2], 1f);

        // 绘制子弹
        // 注意：这里假设子弹纹理已经在 BobaBullet 类中加载
        // 如果有纹理，使用纹理绘制，否则使用简单的圆形

        // 暂时使用简单的圆形绘制
        // 在实际项目中，你应该使用子弹的纹理
        com.badlogic.gdx.graphics.glutils.ShapeRenderer shape =
                new com.badlogic.gdx.graphics.glutils.ShapeRenderer();

        batch.end(); // 结束 SpriteBatch

        shape.setProjectionMatrix(batch.getProjectionMatrix());
        shape.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        shape.setColor(color[0], color[1], color[2], 1f);
        shape.circle(screenX + wobbleX, screenY + wobbleY, currentSize/2);
        shape.end();

        batch.begin(); // 重新开始 SpriteBatch

        // 恢复原始颜色
        batch.setColor(originalColor);
    }

    /**
     * 设置特效强度
     */
    public void setEffectIntensity(float intensity) {
        this.effectIntensity = MathUtils.clamp(intensity, 0.1f, 2.0f);
    }

    public void dispose() {
        // 清理资源
    }
}