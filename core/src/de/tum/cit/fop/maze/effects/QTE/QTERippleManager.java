package de.tum.cit.fop.maze.effects.QTE;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;

public class QTERippleManager {
    private final Array<QTERipple> activeRipples = new Array<>();

    // 内部类 Pool
    private final Pool<QTERipple> ripplePool = new Pool<QTERipple>() {
        @Override
        protected QTERipple newObject() {
            return new QTERipple();
        }
    };

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

    public void render(ShapeRenderer shapeRenderer) {
        if (activeRipples.isEmpty()) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // 手动画 5 层圆，模拟 5px 线宽
        int steps = 5;
        for (QTERipple ripple : activeRipples) {
            shapeRenderer.setColor(ripple.color);
            for (int i = 0; i < steps; i++) {
                shapeRenderer.circle(ripple.x, ripple.y, ripple.radius + i * 0.8f, 64);
            }
        }

        shapeRenderer.end();
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    public void dispose() {
        ripplePool.freeAll(activeRipples);
        activeRipples.clear();
        ripplePool.clear();
    }
}