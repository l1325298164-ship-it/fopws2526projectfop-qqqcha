package de.tum.cit.fop.maze.entities.trap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;

public class TrapT01_Geyser extends Trap {

    public TrapT01_Geyser(int x, int y) {
        super(x, y);
    }

    private enum State {
        IDLE,
        WARNING,
        ERUPTING
    }


    private State state = State.IDLE;

    private float timer = 0f;
    private float damageTickTimer = 0f;

    /* ===== 可调参数 ===== */
    private final float warningDuration = 1f;
    private final float eruptDuration = 1f;
    private  float cycleDuration;   // 整个周期
    private final int damagePerTick = 10;
    private final float damageInterval = 0.5f;

    public TrapT01_Geyser(int x, int y, float cycleDuration) {
        super(x, y);
        this.cycleDuration = cycleDuration;
    }

    @Override
    public void update(float delta) {
        timer += delta;

        switch (state) {
            case IDLE -> {
                if (timer >= cycleDuration - warningDuration - eruptDuration) {
                    state = State.WARNING;
                    timer = 0f;
                }
            }

            case WARNING -> {
                if (timer >= warningDuration) {
                    state = State.ERUPTING;
                    timer = 0f;
                    damageTickTimer = 0f;
                }
            }

            case ERUPTING -> {
                damageTickTimer -= delta;

                if (timer >= eruptDuration) {
                    state = State.IDLE;
                    timer = 0f;
                    damageTickTimer = 0f;
                }
            }
        }
    }

    @Override
    public void onPlayerStep(Player player) {
        if (state != State.ERUPTING) return;

        // 在喷射阶段，每 0.5s 扣一次血
        if (damageTickTimer <= 0f) {
            player.takeDamage(damagePerTick);
            damageTickTimer = damageInterval;
        }
    }

    /* ================= 渲染（Shape） ================= */

    @Override
    public void drawShape(ShapeRenderer sr) {
        if (!active) return;

        float size = GameConstants.CELL_SIZE;
        float px = x * size;
        float py = y * size;

        switch (state) {
            case IDLE -> sr.setColor(new Color(0.4f, 0.25f, 0.1f, 0.4f));
            case WARNING -> sr.setColor(Color.RED);
            case ERUPTING -> sr.setColor(new Color(1f, 0.5f, 0f, 1f));
        }

        sr.rect(px, py, size, size);
    }


    @Override
    public void drawSprite(SpriteBatch batch) {
        // 暂时不做贴图，Shape 已足够
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.SHAPE;
    }
}
