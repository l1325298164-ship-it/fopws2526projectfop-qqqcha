package de.tum.cit.fop.maze.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;

public class PlayerInputHandler {

    private float moveTimer = 0f;
    private float abilityCooldownTimer = 0f;

    private static final float ABILITY_COOLDOWN = 0.1f;

    public PlayerInputHandler() {
        Logger.debug("PlayerInputHandler initialized");
    }

    public void update(float delta, InputHandlerCallback callback) {
        moveTimer += delta;
        abilityCooldownTimer -= delta;

        handleMovementInput(delta, callback);
        handleAbilityInput(callback);
        handleActionInput(callback);
    }

    /* ================= 移动 ================= */

    private void handleMovementInput(float delta, InputHandlerCallback callback) {

        boolean running =
                Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ||
                        Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        float moveDelay = (running
                ? GameConstants.MOVE_DELAY_FAST
                : GameConstants.MOVE_DELAY_NORMAL)
                * callback.getMoveDelayMultiplier();

        if (moveTimer < moveDelay) return;
        moveTimer -= moveDelay;

        int dx = 0, dy = 0;

        if (Gdx.input.isKeyPressed(Input.Keys.UP)) dy = 1;
        else if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) dy = -1;
        else if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) dx = -1;
        else if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) dx = 1;

        if (dx != 0 || dy != 0) {
            callback.onMoveInput(dx, dy);
        }
    }

    /* ================= 技能 ================= */

    private void handleAbilityInput(InputHandlerCallback callback) {
        if (abilityCooldownTimer > 0) return;

        boolean used = false;

        // Slot 0 - 普通攻击
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            used = callback.onAbilityInput(0);
        }
        // Slot 1 - Dash（Shift 单点）
        else if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT)
                || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_RIGHT)
                || Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            used = callback.onAbilityInput(1);
        }
        // Slot 2 / 3
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            used = callback.onAbilityInput(2);
        }
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            used = callback.onAbilityInput(3);
        }

        if (used) {
            abilityCooldownTimer = ABILITY_COOLDOWN;
        }
    }

    /* ================= 其他 ================= */

    private void handleActionInput(InputHandlerCallback callback) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            callback.onInteractInput();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            callback.onMenuInput();
        }
    }

    /* ================= 回调接口 ================= */

    public interface InputHandlerCallback {
        void onMoveInput(int dx, int dy);
        float getMoveDelayMultiplier();
        boolean onAbilityInput(int slot);
        void onInteractInput();
        void onMenuInput();
    }
}
