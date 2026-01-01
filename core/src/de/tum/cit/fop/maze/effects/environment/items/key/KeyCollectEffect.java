package de.tum.cit.fop.maze.effects.environment.items.key;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;

public class KeyCollectEffect {
    private Vector2 position;
    private Texture texture;
    private float timer;
    private float duration = 1.0f; // 动画总时长
    private boolean finished = false;

    // 动画参数
    private float startY;
    private float jumpHeight = 64f; // 跳起高度（像素）

    public KeyCollectEffect(float x, float y, Texture texture) {
        this.position = new Vector2(x, y);
        this.startY = y;
        this.texture = texture;
        this.timer = 0;
    }

    public void update(float delta) {
        timer += delta;
        if (timer >= duration) {
            finished = true;
        }
    }

    public void render(SpriteBatch batch) {
        if (finished) return;

        float progress = Math.min(1.0f, timer / duration);

        // 1. 运动插值
        // SwingOut 让物体冲出去一点再弹回，很有弹性
        float currentY = startY + Interpolation.swingOut.apply(0, jumpHeight, progress);

        // 放大：从 1.0 变大到 2.0
        float scale = Interpolation.smooth.apply(1.0f, 2.0f, progress);

        // 旋转：转两圈 (720度)
        float rotation = Interpolation.pow2In.apply(0f, 720f, progress);

        // 透明度：最后 20% 的时间快速淡出
        float alpha = 1.0f;
        if (progress > 0.8f) {
            alpha = 1.0f - (progress - 0.8f) / 0.2f;
        }

        // 保存之前的混合模式和颜色
        int srcFunc = batch.getBlendSrcFunc();
        int dstFunc = batch.getBlendDstFunc();
        Color oldColor = new Color(batch.getColor());

        float width = 32; // 假设大小，或者用 texture.getWidth()
        float height = 32;
        float originX = width / 2;
        float originY = height / 2;

        // --- 第一层：金色光晕 (加法混合) ---
        // GL_ONE, GL_ONE 会让颜色叠加变亮，产生发光感
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        // 设置为金色，带一点透明度
        batch.setColor(1f, 0.9f, 0.2f, alpha * 0.6f);

        // 光晕画得比本体大一点
        batch.draw(texture,
                position.x, currentY,
                originX, originY,
                width, height,
                scale * 1.5f, scale * 1.5f, // 光晕放大1.5倍
                rotation,
                0, 0, texture.getWidth(), texture.getHeight(), false, false);

        // --- 第二层：钥匙本体 (正常混合) ---
        batch.setBlendFunction(srcFunc, dstFunc);
        batch.setColor(1f, 1f, 1f, alpha); // 恢复原色，但应用透明度

        batch.draw(texture,
                position.x, currentY,
                originX, originY,
                width, height,
                scale, scale,
                rotation,
                0, 0, texture.getWidth(), texture.getHeight(), false, false);

        // 恢复环境颜色
        //batch.setColor(oldColor);
        // 🟢 改为强制重置为纯白色：
        batch.setColor(Color.WHITE);
    }

    public boolean isFinished() {
        return finished;
    }
}