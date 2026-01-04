package de.tum.cit.fop.maze.effects.QTE;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

public class QTERippleManager {
    private final Array<QTERipple> activeRipples = new Array<>();
    private Texture rippleTexture; // 🔥 程序化生成的发光纹理

    // 内部类 Pool
    private final Pool<QTERipple> ripplePool = new Pool<QTERipple>() {
        @Override
        protected QTERipple newObject() {
            return new QTERipple();
        }
    };

    public QTERippleManager() {
        // 🔥 初始化时生成一张柔和的圆环光晕图
        createRippleTexture();
    }

    /**
     * 程序化生成一张 128x128 的柔和圆环纹理
     * 这样就不需要找美工要素材了
     */
    private void createRippleTexture() {
        int size = 128;
        int center = size / 2;
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        // 遍历每个像素，画一个柔和的圆环
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                double dx = x - center;
                double dy = y - center;
                double dist = Math.sqrt(dx * dx + dy * dy);
                double normalizedDist = dist / (size / 2.0);

                // 核心算法：在半径 0.7 处最亮，向两侧衰减
                // 模拟一个宽度约为 0.3 的光环
                float alpha = 0f;
                if (normalizedDist > 0.5 && normalizedDist < 1.0) {
                    // 距离中心 0.75 处 alpha 为 1，边缘为 0
                    float delta = (float) Math.abs(normalizedDist - 0.75);
                    // 0.25 是半宽
                    if (delta < 0.25f) {
                        alpha = 1f - (delta / 0.25f);
                        // 让衰减更平滑 (三次缓动)
                        alpha = alpha * alpha * (3 - 2 * alpha);
                    }
                }

                // 写入白色，透明度由 alpha 控制
                // 渲染时我们会用 setColor 染成粉/黄/青色
                pixmap.setColor(1f, 1f, 1f, alpha);
                pixmap.drawPixel(x, y);
            }
        }

        rippleTexture = new Texture(pixmap);
        pixmap.dispose();
    }

    public void spawnRipple(float centerX, float centerY) {
        QTERipple ripple = ripplePool.obtain();
        ripple.init(centerX, centerY);
        activeRipples.add(ripple);
    }

    public void update(float delta) {
        for (int i = activeRipples.size - 1; i >= 0; i--) {
            QTERipple ripple = activeRipples.get(i);
            ripple.update(delta);
            if (!ripple.active) {
                activeRipples.removeIndex(i);
                ripplePool.free(ripple);
            }
        }
    }

    // 🔥 注意：这里改用了 SpriteBatch 而不是 ShapeRenderer
    public void render(SpriteBatch batch) {
        if (activeRipples.isEmpty()) return;

        // 保存旧的混合模式
        int srcFunc = batch.getBlendSrcFunc();
        int dstFunc = batch.getBlendDstFunc();
        Color oldColor = batch.getColor();

        // 🔥 开启加法混合 (Additive Blending) -> 发光效果
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        for (QTERipple ripple : activeRipples) {
            // 设置波纹颜色 (带透明度)
            batch.setColor(ripple.color);

            float size = ripple.radius * 2; // 直径

            // 绘制纹理，居中
            batch.draw(rippleTexture,
                    ripple.x - ripple.radius,
                    ripple.y - ripple.radius,
                    size, size);
        }

        // 恢复默认混合模式
        batch.setBlendFunction(srcFunc, dstFunc);
        batch.setColor(oldColor);
    }

    public void dispose() {
        if (rippleTexture != null) {
            rippleTexture.dispose();
        }
        ripplePool.freeAll(activeRipples);
        activeRipples.clear();
        ripplePool.clear();
    }
}