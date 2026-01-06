package de.tum.cit.fop.maze.entities.trap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture; // 引入 Texture
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

    // 🔥 新增：贴图纹理
    private Texture texture;

    /* ===== 参数 ===== */
    private static final float EXPLODE_DELAY = 0.8f;
    private static final int DAMAGE = 15;

    private final GameManager gm;

    public TrapT02_PearlMine(int x, int y, GameManager gm) {
        super(x, y);
        this.gm = gm;

        // ⚠️ 请修改这里的路径为你实际的图片路径
        // 建议图片大小为 16x16 或 32x32 像素
        this.texture = new Texture(Gdx.files.internal("traps/pearl_mine.png"));
    }

    @Override
    public boolean isPassable() {
        return true;
    }

    // ✅ 这里的 update 逻辑保留之前修复后的版本
    @Override
    public void update(float delta, GameManager gameManager) {
        if (!active) return;

        if (state == State.ARMED) {
            timer += delta;
            if (timer >= EXPLODE_DELAY) {
                explode();
            }
        }
    }

    // 屏蔽掉未使用的 update(float delta)
    @Override
    public void update(float delta) {}

    @Override
    public void onPlayerStep(Player player) {
        if (state != State.IDLE) return;
        state = State.ARMED;
        timer = 0f;
    }

    private void explode() {
        state = State.EXPLODED;
        active = false;

        int cx = x;
        int cy = y;

        // 触发特效
        if (gm.getTrapEffectManager() != null) {
            float effectX = (x + 0.5f) * GameConstants.CELL_SIZE;
            float effectY = (y + 0.5f) * GameConstants.CELL_SIZE;
            gm.getTrapEffectManager().spawnPearlMine(effectX, effectY);
        }

        // 伤害判定
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

    /* ================= 渲染修改 ================= */

    @Override
    public RenderType getRenderType() {
        // 🔥 修改为 SPRITE 模式，这样游戏才会调用 drawSprite
        return RenderType.SPRITE;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;

        float size = GameConstants.CELL_SIZE;
        float px = x * size;
        float py = y * size;

        // 🔥 保留震动效果：如果处于 ARMED 状态，计算随机偏移
        float shakeX = 0;
        float shakeY = 0;
        if (state == State.ARMED) {
            // 随着时间推移震动越来越剧烈
            float intensity = (timer / EXPLODE_DELAY) * 5f;
            shakeX = MathUtils.random(-intensity, intensity);
            shakeY = MathUtils.random(-intensity, intensity);
        }

        // 如果处于 ARMED 状态，还可以让贴图变红一点表示警告
        if (state == State.ARMED) {
            batch.setColor(1f, 0.5f, 0.5f, 1f); // 变红
        } else {
            batch.setColor(1f, 1f, 1f, 1f); // 原色
        }

        // 绘制贴图 (加上震动偏移)
        batch.draw(texture, px + shakeX, py + shakeY, size, size);

        // 记得把颜色改回来，以免影响后续绘制
        batch.setColor(1f, 1f, 1f, 1f);
    }

    @Override
    public void drawShape(ShapeRenderer sr) {
        // 不需要 Shape 绘制了
    }

    // 建议添加释放资源的方法
    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
    }
}