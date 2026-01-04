package de.tum.cit.fop.maze.entities.trap;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

public class TrapT03_TeaShards extends Trap {
    private final int damage = 1;

    public TrapT03_TeaShards(int x, int y) {
        super(x, y);
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {

    }

    @Override
    public void drawSprite(SpriteBatch batch) {

    }

    @Override
    public RenderType getRenderType() {
        return null;
    }

    @Override
    public void update(float delta, GameManager gameManager) {
        Player player = gameManager.getPlayer();
        if (player.getX() == x && player.getY() == y) {
            player.takeDamage(damage);

            // 🔥 触发飞溅特效 (概率触发以免过于密集)
            if (Math.random() < 0.3 && gameManager.getTrapEffectManager() != null) {
                float cx = (x + 0.5f) * GameConstants.CELL_SIZE;
                float cy = (y + 0.5f) * GameConstants.CELL_SIZE;
                gameManager.getTrapEffectManager().spawnTeaShards(cx, cy);
            }
        }
    }

    @Override
    public void onPlayerStep(Player player) {

    }
}