package de.tum.cit.fop.maze.entities.trap;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

public class TrapT01_Geyser extends Trap {
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

    private enum State { IDLE, WARN, ACTIVE }
    private State currentState = State.IDLE;
    private float stateTimer = 0f;
    private final float idleDuration;
    private final float warnDuration = 1.0f;
    private final float activeDuration = 1.0f;

    public TrapT01_Geyser(int x, int y, float idleTime) {
        super(x, y);
        this.idleDuration = idleTime;
    }

    @Override
    public void update(float delta) {

    }

    @Override
    public void update(float delta, GameManager gameManager) {
        stateTimer += delta;
        switch (currentState) {
            case IDLE:
                if (stateTimer >= idleDuration) {
                    currentState = State.WARN;
                    stateTimer = 0;
                }
                break;
            case WARN:
                if (stateTimer >= warnDuration) {
                    currentState = State.ACTIVE;
                    stateTimer = 0;
                    // 🔥 触发喷发特效
                    if (gameManager.getTrapEffectManager() != null) {
                        float cx = (x + 0.5f) * GameConstants.CELL_SIZE;
                        float cy = (y + 0.5f) * GameConstants.CELL_SIZE;
                        gameManager.getTrapEffectManager().spawnGeyser(cx, cy);
                    }
                }
                break;
            case ACTIVE:
                if (gameManager.getPlayer().getX() == x && gameManager.getPlayer().getY() == y) {
                    gameManager.getPlayer().takeDamage(1);
                }
                if (stateTimer >= activeDuration) {
                    currentState = State.IDLE;
                    stateTimer = 0;
                }
                break;
        }
    }

    @Override
    public void onPlayerStep(Player player) {

    }
}