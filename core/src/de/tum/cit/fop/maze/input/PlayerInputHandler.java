// PlayerInputHandler.java - 更新版本
package de.tum.cit.fop.maze.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;

public class PlayerInputHandler {
    private float moveTimer = 0;
    private float currentMoveDelay = GameConstants.MOVE_DELAY_NORMAL;
    private float abilityCooldownTimer = 0;
    private static final float ABILITY_COOLDOWN = 0.1f; // 防止连续按键过快

    public PlayerInputHandler() {
        Logger.debug("PlayerInputHandler initialized");
    }

    public void update(float deltaTime, InputHandlerCallback callback) {
        moveTimer += deltaTime;
        abilityCooldownTimer -= deltaTime;

        // 处理移动输入
        handleMovementInput(deltaTime, callback);

        // 处理能力输入（独立于移动冷却）
        handleAbilityInput(callback);

        // 处理其他动作输入
        handleActionInput(callback);
    }

    private void handleMovementInput(float deltaTime, InputHandlerCallback callback) {
        boolean isRunning = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) ||
                Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        currentMoveDelay = (isRunning ?
                GameConstants.MOVE_DELAY_FAST :
                GameConstants.MOVE_DELAY_NORMAL)
                * callback.getMoveDelayMultiplier();

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

    private void handleAbilityInput(InputHandlerCallback callback) {
        // 检查能力冷却
        if (abilityCooldownTimer > 0) return;

        boolean abilityUsed = false;

        // 空格键 - 主攻击（对应槽位0）
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            abilityUsed = callback.onAbilityInput(0);
        }
        // 数字键1-4
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            abilityUsed = callback.onAbilityInput(0);
        }
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
            abilityUsed = callback.onAbilityInput(1);
        }
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
            abilityUsed = callback.onAbilityInput(2);
        }
        else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) {
            abilityUsed = callback.onAbilityInput(3);
        }
        // Q键 - 备用能力
        else if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) {
            abilityUsed = callback.onAbilityInput(0);
        }
        // E键 - 特殊能力
        else if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            abilityUsed = callback.onAbilityInput(1);
        }
        // R键 - 终极能力
        else if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            abilityUsed = callback.onAbilityInput(2);
        }

        // 如果使用了能力，设置短暂冷却防止连续触发
        if (abilityUsed) {
            abilityCooldownTimer = ABILITY_COOLDOWN;
        }
    }

    private void handleActionInput(InputHandlerCallback callback) {
        // 交互键（F键）
        if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            callback.onInteractInput();
        }

        // 菜单/暂停键（ESC键）
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            callback.onMenuInput();
        }

        // 切换跑步状态（Shift键状态在移动处理中已使用）
        // 可以在这里添加其他动作，如切换武器等
    }

    public boolean isRunning() {
        return currentMoveDelay == GameConstants.MOVE_DELAY_FAST;
    }

    public interface InputHandlerCallback {
        void onMoveInput(int dx, int dy);
        float getMoveDelayMultiplier();

        // 新增：能力输入回调
        boolean onAbilityInput(int slot);

        // 新增：交互输入回调
        void onInteractInput();

        // 新增：菜单输入回调
        void onMenuInput();
    }
}