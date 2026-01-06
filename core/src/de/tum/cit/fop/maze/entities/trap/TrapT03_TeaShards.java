package de.tum.cit.fop.maze.entities.trap;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

public class TrapT03_TeaShards extends Trap {
    private final int damage = 1;

    // 🔥 新增：冷却时间控制
    private float cooldownTimer = 0f;
    private static final float COOLDOWN = 1.0f; // 1秒冷却

    public TrapT03_TeaShards(int x, int y) {
        super(x, y);
    }

    @Override
    public void update(float delta) {}

    @Override
    public void update(float delta, GameManager gameManager) {
        // 更新冷却
        if (cooldownTimer > 0) {
            cooldownTimer -= delta;
        }

        Player player = gameManager.getPlayer();

        // 判定条件：位置重合 且 冷却结束
        if (player.getX() == x && player.getY() == y && cooldownTimer <= 0) {
            player.takeDamage(damage);
            cooldownTimer = COOLDOWN; // 重置冷却

            // 🔥 触发特效 (现在必然触发，不再依赖随机概率，因为有冷却限制了)
            if (gameManager.getTrapEffectManager() != null) {
                float cx = (x + 0.5f) * GameConstants.CELL_SIZE;
                float cy = (y + 0.5f) * GameConstants.CELL_SIZE;
                gameManager.getTrapEffectManager().spawnTeaShards(cx, cy);
            }
        }
    }

    @Override
    public void onPlayerStep(Player player) {}

    // 🔥 修复：让陷阱可见
    @Override
    public RenderType getRenderType() {
        return RenderType.SHAPE;
    }

    @Override
    public void drawShape(ShapeRenderer sr) {
        float cx = (x + 0.5f) * GameConstants.CELL_SIZE;
        float cy = (y + 0.5f) * GameConstants.CELL_SIZE;

        // 绘制几个尖锐的三角形，表示碎瓷片
        sr.setColor(0.7f, 0.9f, 0.7f, 1f); // 浅绿色瓷片

        // 碎片1
        sr.triangle(cx - 8, cy - 8, cx - 2, cy - 2, cx - 10, cy + 2);
        // 碎片2
        sr.triangle(cx + 5, cy + 5, cx + 12, cy, cx + 2, cy - 5);
        // 碎片3
        sr.triangle(cx - 2, cy + 8, cx + 4, cy + 12, cx + 2, cy + 2);
    }

    @Override
    public void drawSprite(SpriteBatch batch) {}
}