package de.tum.cit.fop.maze.entities.trap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
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

    // ❌ 删除这个单参数的 update，因为它不会被调用，容易造成误导
    // @Override
    // public void update(float delta) { ... }

    @Override
    public void update(float delta) {

    }

    // ✅ 将逻辑移到这里
    @Override
    public void update(float delta, GameManager gameManager) {
        if (!active) return;

        // 如果陷阱处于“已激活”状态，开始倒计时
        if (state == State.ARMED) {
            timer += delta;
            // 震动效果的随机数可以在这里每帧更新，或者在 draw 里生成

            if (timer >= EXPLODE_DELAY) {
                explode();
            }
        }
    }

    @Override
    public void onPlayerStep(Player player) {
        if (state != State.IDLE) return;
        state = State.ARMED;
        timer = 0f;
        // 可以在这里播放一个“滴滴”声
    }

    /** 爆炸逻辑 */
    private void explode() {
        state = State.EXPLODED;
        active = false; // 爆炸后陷阱本身消失（但特效会生成）

        int cx = x;
        int cy = y;

        // 🔥 触发爆炸特效
        // 注意：这里我们使用成员变量 gm，或者使用传入 update 的 gameManager 都可以
        if (gm.getTrapEffectManager() != null) {
            float effectX = (x + 0.5f) * GameConstants.CELL_SIZE;
            float effectY = (y + 0.5f) * GameConstants.CELL_SIZE;
            gm.getTrapEffectManager().spawnPearlMine(effectX, effectY);
        }

        // ===== 伤害判定 =====
        Player player = gm.getPlayer();
        // 简单的距离判定 (爆炸半径1格)
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

        float radius = size / 5f;

        // 🔥 震动效果
        float shakeX = 0;
        float shakeY = 0;
        if (state == State.ARMED) {
            // 随着时间推移震动越来越剧烈
            float intensity = (timer / EXPLODE_DELAY) * 5f;
            shakeX = MathUtils.random(-intensity, intensity);
            shakeY = MathUtils.random(-intensity, intensity);
        }

        // 绘制三个小芋圆
        sr.setColor(TARO_PURPLE);
        sr.circle(centerX - radius + shakeX, centerY - radius + shakeY, radius);

        sr.setColor(POTATO_ORANGE);
        sr.circle(centerX + radius + shakeX, centerY - radius + shakeY, radius);

        sr.setColor(RICE_WHITE);
        sr.circle(centerX + shakeX, centerY + radius + shakeY, radius);
    }

    @Override
    public void drawSprite(SpriteBatch batch) {}

    @Override
    public RenderType getRenderType() {
        return RenderType.SHAPE;
    }
}