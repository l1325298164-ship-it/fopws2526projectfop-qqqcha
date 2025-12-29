package de.tum.cit.fop.maze.entities.trap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

public class TrapT02_PearlMine extends Trap {

    private enum State {
        IDLE,
        ARMED,
        EXPLODED
    }

    private State state = State.IDLE;
    private float timer = 0f;

    /* ===== 参数 ===== */
    private static final float EXPLODE_DELAY = 0.8f;
    private static final int DAMAGE = 15;

    private final GameManager gm;   // 用于获取敌人

    public TrapT02_PearlMine(int x, int y, GameManager gm) {
        super(x, y);
        this.gm = gm;
    }

    @Override
    public void update(float delta) {
        if (!active) return;

        if (state == State.ARMED) {
            timer += delta;
            if (timer >= EXPLODE_DELAY) {
                explode();
            }
        }
    }

    @Override
    public void onPlayerStep(Player player) {
        // 只能被主角触发 & 只能触发一次
        if (state != State.IDLE) return;

        state = State.ARMED;
        timer = 0f;
    }

    /** 爆炸逻辑 */
    private void explode() {
        state = State.EXPLODED;
        active = false;

        int cx = x;
        int cy = y;

        // ===== 伤害玩家 =====
        Player player = gm.getPlayer();
        if (Math.abs(player.getX() - cx) <= 1 &&
                Math.abs(player.getY() - cy) <= 1) {
            player.takeDamage(DAMAGE);
        }

        // ===== 伤害范围内所有小怪 =====
        for (Enemy enemy : gm.getEnemies()) {
            if (Math.abs(enemy.getX() - cx) <= 1 &&
                    Math.abs(enemy.getY() - cy) <= 1) {
                enemy.takeDamage(DAMAGE);
            }
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
            case IDLE -> sr.setColor(new Color(0.6f, 0.6f, 0.6f, 1f));
            case ARMED -> sr.setColor(Color.RED);
        }

        sr.rect(px, py, size, size);
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        // 暂时不需要贴图
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.SHAPE;
    }
}
