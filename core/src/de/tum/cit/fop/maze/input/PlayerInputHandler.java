package de.tum.cit.fop.maze.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;

public class PlayerInputHandler {

    // ================= 教程用移动标记 =================
    private boolean movedUp = false;
    private boolean movedDown = false;
    private boolean movedLeft = false;
    private boolean movedRight = false;

    private float moveTimer = 0f;
    private float abilityCooldownP1 = 0f;
    private float abilityCooldownP2 = 0f;

    private static final float ABILITY_COOLDOWN = 0.1f;

    public PlayerInputHandler() {
        Logger.debug("PlayerInputHandler initialized");
    }

    /**
     * ⚠️ 这是 GameScreen 真正调用的方法
     */
    public void update(
            float delta,
            InputHandlerCallback callback,
            Player.PlayerIndex index
    ) {
        // ===== 移动 =====
        handleMovementInput(delta, callback, index);

        // ===== 技能 / Dash =====
        handleAbilityInput(delta, callback, index);

        // ===== 交互 =====
        handleActionInput(callback, index);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            callback.onMenuInput();
        }
    }

    /* ================= 移动 ================= */

    private void handleMovementInput(
            float delta,
            InputHandlerCallback callback,
            Player.PlayerIndex index
    ) {
        moveTimer += delta;

        float moveDelay =
                GameConstants.MOVE_DELAY_NORMAL * callback.getMoveDelayMultiplier();

        if (moveTimer < moveDelay) return;
        moveTimer -= moveDelay;

        int dx = 0;
        int dy = 0;

        var km = KeyBindingManager.getInstance();

        if (index == Player.PlayerIndex.P1) {
            if (km.isPressed(KeyBindingManager.GameAction.P1_MOVE_UP)) {
                dy = 1;
                movedUp = true;
            } else if (km.isPressed(KeyBindingManager.GameAction.P1_MOVE_DOWN)) {
                dy = -1;
                movedDown = true;
            } else if (km.isPressed(KeyBindingManager.GameAction.P1_MOVE_LEFT)) {
                dx = -1;
                movedLeft = true;
            } else if (km.isPressed(KeyBindingManager.GameAction.P1_MOVE_RIGHT)) {
                dx = 1;
                movedRight = true;
            }

        } else { // ===== P2 =====
            if (km.isPressed(KeyBindingManager.GameAction.P2_MOVE_UP)) {
                dy = 1;
            } else if (km.isPressed(KeyBindingManager.GameAction.P2_MOVE_DOWN)) {
                dy = -1;
            } else if (km.isPressed(KeyBindingManager.GameAction.P2_MOVE_LEFT)) {
                dx = -1;
            } else if (km.isPressed(KeyBindingManager.GameAction.P2_MOVE_RIGHT)) {
                dx = 1;
            }
        }

        if (dx != 0 || dy != 0) {
            callback.onMoveInput(index, dx, dy);
        }
    }

    /* ================= 技能 / Dash ================= */

    private void handleAbilityInput(
            float delta,
            InputHandlerCallback callback,
            Player.PlayerIndex index
    ){
        float cd = (index == Player.PlayerIndex.P1)
                ? abilityCooldownP1
                : abilityCooldownP2;

        if (cd > 0f) {
            if (index == Player.PlayerIndex.P1) {
                abilityCooldownP1 -= delta;
            } else {
                abilityCooldownP2 -= delta;
            }
            return;
        }

        boolean used = false;
        var km = KeyBindingManager.getInstance();

        if (index == Player.PlayerIndex.P1) {

            // P1：Space = 技能 / 近战
            if (km.isJustPressed(KeyBindingManager.GameAction.P1_USE_ABILITY)) {
                used = callback.onAbilityInput(index, 0);
            }

            // P1：Shift = Dash
            if (km.isJustPressed(KeyBindingManager.GameAction.P1_DASH)) {
                used = callback.onAbilityInput(index,1);
            }

        } else { // ===== P2 =====

            // P2：鼠标左键 = 魔法技能
            if (km.isJustPressed(KeyBindingManager.GameAction.P2_USE_ABILITY)) {
                used = callback.onAbilityInput(index, 1);
            }

            // P2：鼠标右键 = Dash
            if (km.isJustPressed(KeyBindingManager.GameAction.P2_DASH)) {
                used = callback.onAbilityInput(index,1);
            }
        }

        if (used) {
            if (index == Player.PlayerIndex.P1) {
                abilityCooldownP1 = ABILITY_COOLDOWN;
            } else {
                abilityCooldownP2 = ABILITY_COOLDOWN;
            }
        }
    }

    /* ================= 交互 ================= */

    private void handleActionInput(
            InputHandlerCallback callback,
            Player.PlayerIndex index
    ) {
        var km = KeyBindingManager.getInstance();

        if (index == Player.PlayerIndex.P1) {
            if (km.isJustPressed(KeyBindingManager.GameAction.P1_INTERACT)) {
                callback.onInteractInput(index);
            }
        } else {
            if (km.isJustPressed(KeyBindingManager.GameAction.P2_INTERACT)) {
                callback.onInteractInput(index);
            }
        }
    }

    /* ================= 回调接口 ================= */

    public interface InputHandlerCallback {
        void onMoveInput(Player.PlayerIndex index, int dx, int dy);
        float getMoveDelayMultiplier();
        boolean onAbilityInput(Player.PlayerIndex index, int slot);
        void onInteractInput(Player.PlayerIndex index);
        void onMenuInput();
    }

    // ================= 教程接口 =================

    public void resetTutorialMoveFlags() {
        movedUp = movedDown = movedLeft = movedRight = false;
    }

    public boolean hasMovedUp() { return movedUp; }
    public boolean hasMovedDown() { return movedDown; }
    public boolean hasMovedLeft() { return movedLeft; }
    public boolean hasMovedRight() { return movedRight; }
}
