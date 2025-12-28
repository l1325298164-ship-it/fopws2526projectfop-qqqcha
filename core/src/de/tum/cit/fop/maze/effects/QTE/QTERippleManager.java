package de.tum.cit.fop.maze.effects.QTE;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool; // 👈 确保这行 Import 存在

/**
 * QTE 波纹特效管理器
 */
public class QTERippleManager {
    // 活跃的波纹列表
    private final Array<QTERipple> activeRipples = new Array<>();

    // 🔥【修改点】使用内部类来实例化 Pool，解决匿名类标红问题
    private final Pool<QTERipple> ripplePool = new Pool<QTERipple>() {
        @Override
        protected QTERipple newObject() {
            return new QTERipple();
        }
    };

    // 如果上面的还标红，请尝试下面这个备选写法（显式内部类）：
    /*
    private final RipplePool ripplePool = new RipplePool();
    private class RipplePool extends Pool<QTERipple> {
        @Override
        protected QTERipple newObject() {
            return new QTERipple();
        }
    }
    */

    public QTERippleManager() {
        // 构造函数留空即可，Pool 已经在成员变量里初始化了
    }

    /**
     * 生成一个水波纹
     */
    public void spawnRipple(float centerX, float centerY) {
        // 从池中拿一个对象
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
                // ♻️ 回收对象进池子
                ripplePool.free(ripple);
            }
        }
    }

    public void render(ShapeRenderer shapeRenderer) {
        if (activeRipples.isEmpty()) return;

        // ✨ 开启混合模式 (Additive Blending) 实现发光叠加效果
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // 设置线宽
        Gdx.gl.glLineWidth(3f);

        for (QTERipple ripple : activeRipples) {
            shapeRenderer.setColor(ripple.color);
            shapeRenderer.circle(ripple.x, ripple.y, ripple.radius, 32);
        }

        shapeRenderer.end();

        // 恢复默认设置
        Gdx.gl.glLineWidth(1f);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void dispose() {
        ripplePool.freeAll(activeRipples);
        activeRipples.clear();
        ripplePool.clear();
    }
}