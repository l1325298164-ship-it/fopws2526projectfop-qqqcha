// PlayerInputHandler.java
package de.tum.cit.fop.maze.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;

public class PlayerInputHandler {
    private float moveTimer = 0;
    private float currentMoveDelay = GameConstants.MOVE_DELAY_NORMAL;

    public PlayerInputHandler() {
        Logger.debug("PlayerInputHandler initialized");
    }

    public void update(float deltaTime, InputHandlerCallback callback) {
        moveTimer += deltaTime;

        boolean isRunning = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ||
            Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        currentMoveDelay = isRunning ?
            GameConstants.MOVE_DELAY_FAST :
            GameConstants.MOVE_DELAY_NORMAL;

        if (moveTimer < currentMoveDelay) {
            return;
        }

        moveTimer -= currentMoveDelay;

        int dx = 0, dy = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            dy = 1;
        } else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            dy = -1;
        } else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            dx = -1;
        } else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            dx = 1;
        }

        if (dx != 0 || dy != 0) {
            callback.onMoveInput(dx, dy);
        }
    }

    public boolean isRunning() {
        return currentMoveDelay == GameConstants.MOVE_DELAY_FAST;
    }

    public interface InputHandlerCallback {
        void onMoveInput(int dx, int dy);
    }
}
