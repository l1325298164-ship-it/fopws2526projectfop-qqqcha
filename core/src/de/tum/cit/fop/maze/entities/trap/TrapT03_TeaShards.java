package de.tum.cit.fop.maze.entities.trap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;

public class TrapT03_TeaShards extends Trap {

    private enum State {
        IDLE,
        DAMAGING
    }

    private State state = State.IDLE;

    /* ===== 参数 ===== */
    private static final int DAMAGE = 5;
    private static final float DAMAGE_INTERVAL = 0.5f; // 1 秒 2 次
    private static final float SLOW_DURATION = 2.0f;

    private float damageTimer = 0f;

    public TrapT03_TeaShards(int x, int y) {
        super(x, y);
    }

    @Override
    public void update(float delta) {
        if (state == State.DAMAGING) {
            damageTimer -= delta;
            if (damageTimer < 0f) {
                damageTimer = 0f;
            }
        }
    }

    @Override
    public void onPlayerStep(Player player) {

        // 只有主角有效（Enemy 不触发）
        state = State.DAMAGING;

        // ===== 扣血（按频率）=====
        if (damageTimer <= 0f) {
            player.takeDamage(DAMAGE);
            damageTimer = DAMAGE_INTERVAL;
        }

        // ===== 减速（不可叠加，但刷新时间）=====
        player.applySlow(SLOW_DURATION);
    }

    /* ================= 渲染 ================= */

    @Override
    public void drawShape(ShapeRenderer sr) {
        if (!active) return;

        float size = GameConstants.CELL_SIZE;
        float px = x * size;
        float py = y * size;

        // 地刺：深绿偏黄
        sr.setColor(new Color(0.1f, 0.6f, 0.2f, 1f));
        sr.rect(px, py, size, size);
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        // Shape 足够
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.SHAPE;
    }
}
