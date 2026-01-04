package de.tum.cit.fop.maze.entities.trap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils; // 引入数学工具用于震动
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

    // 🔥 芋圆三色定义
    private static final Color TARO_PURPLE = new Color(0.7f, 0.4f, 0.95f, 1f);
    private static final Color POTATO_ORANGE = new Color(1.0f, 0.65f, 0.3f, 1f);
    private static final Color RICE_WHITE = new Color(0.98f, 0.98f, 0.95f, 1f);

    private final GameManager gm;

    public TrapT02_PearlMine(int x, int y, GameManager gm) {
        super(x, y);
        this.gm = gm;
    }

    @Override
    public boolean isPassable() {
        return true;
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
    public void update(float delta, GameManager gameManager) {

    }

    @Override
    public void onPlayerStep(Player player) {
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

        // 🔥 触发爆炸特效 (无缝衔接)
        if (gm.getTrapEffectManager() != null) {
            float effectX = (x + 0.5f) * GameConstants.CELL_SIZE;
            float effectY = (y + 0.5f) * GameConstants.CELL_SIZE;
            gm.getTrapEffectManager().spawnPearlMine(effectX, effectY);
        }

        // ===== 伤害判定 =====
        Player player = gm.getPlayer();
        if (Math.abs(player.getX() - cx) <= 1 && Math.abs(player.getY() - cy) <= 1) {
            player.takeDamage(DAMAGE);
        }

        for (Enemy enemy : gm.getEnemies()) {
            if (Math.abs(enemy.getX() - cx) <= 1 && Math.abs(enemy.getY() - cy) <= 1) {
                enemy.takeDamage(DAMAGE);
            }
        }
    }

    /* ================= 渲染（Shape） ================= */

    @Override
    public void drawShape(ShapeRenderer sr) {
        if (!active) return;

        float size = GameConstants.CELL_SIZE;
        float centerX = x * size + size / 2;
        float centerY = y * size + size / 2;

        // 芋圆半径 (比之前的方块小，显得精致)
        float radius = size / 5f;

        // 🔥 震动效果：如果处于 ARMED (触发) 状态，让芋圆剧烈抖动
        float shakeX = 0;
        float shakeY = 0;
        if (state == State.ARMED) {
            shakeX = MathUtils.random(-3f, 3f);
            shakeY = MathUtils.random(-3f, 3f);
        }

        // 绘制三个挤在一起的小芋圆 (左紫、右橙、上白)

        // 1. 左下：芋头紫
        sr.setColor(TARO_PURPLE);
        sr.circle(centerX - radius + shakeX, centerY - radius + shakeY, radius);

        // 2. 右下：地瓜橙
        sr.setColor(POTATO_ORANGE);
        sr.circle(centerX + radius + shakeX, centerY - radius + shakeY, radius);

        // 3. 上方：糯米白
        sr.setColor(RICE_WHITE);
        sr.circle(centerX + shakeX, centerY + radius + shakeY, radius);
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        // 不需要贴图，使用 ShapeRenderer 绘制
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.SHAPE;
    }
}