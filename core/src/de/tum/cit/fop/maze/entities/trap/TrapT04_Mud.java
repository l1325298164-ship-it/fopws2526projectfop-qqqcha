package de.tum.cit.fop.maze.entities.trap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;

public class TrapT04_Mud extends Trap {

    /* ===== 参数 ===== */
    private static final float SLOW_DURATION = 1.5f; // 每次踩刷新 1.5s

    public TrapT04_Mud(int x, int y) {
        super(x, y);
    }

    @Override
    public void update(float delta) {
        // 泥潭是“被动型”，不需要每帧更新
    }
    @Override
    public void onPlayerStep(Player player) {
        // 只减速，不扣血
        player.applySlow(SLOW_DURATION);
    }

    /* ================= 渲染 ================= */

    @Override
    public void drawShape(ShapeRenderer sr) {
        if (!active) return;

        float size = GameConstants.CELL_SIZE;
        float px = x * size;
        float py = y * size;

        // 泥潭颜色：深棕 / 暗绿
        sr.setColor(new Color(0.35f, 0.25f, 0.15f, 1f));
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
