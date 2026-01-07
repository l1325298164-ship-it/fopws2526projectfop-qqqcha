package de.tum.cit.fop.maze.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import java.util.HashMap;
import java.util.Map;

/**
 * 按键绑定管理员
 * 你的报错是因为缺少这个文件，或者这个文件里的 GameAction 没有定义好。
 */
public class KeyBindingManager {

    private static KeyBindingManager instance;
    private final Preferences prefs;
    private static final String PREFS_NAME = "maze_controls_settings";

    // 🔥 报错的核心原因：必须在这里定义 GameAction
    public enum GameAction {

        // ===== P1 =====
        P1_MOVE_UP,
        P1_MOVE_DOWN,
        P1_MOVE_LEFT,
        P1_MOVE_RIGHT,
        P1_USE_ABILITY,
        P1_DASH,
        P1_INTERACT,

        // ===== P2 =====
        P2_MOVE_UP,
        P2_MOVE_DOWN,
        P2_MOVE_LEFT,
        P2_MOVE_RIGHT,
        P2_USE_ABILITY,
        P2_DASH,
        P2_INTERACT,

        // ===== SYSTEM =====
        CONSOLE
    }
    private final Map<GameAction, Integer> keyBindings;

    private KeyBindingManager() {
        keyBindings = new HashMap<>();
        prefs = Gdx.app.getPreferences(PREFS_NAME);
        load();
    }

    public static KeyBindingManager getInstance() {
        if (instance == null) {
            instance = new KeyBindingManager();
        }
        return instance;
    }

    private void load() {

        // ===== P1 默认键位 =====
        loadBinding(GameAction.P1_MOVE_UP, Input.Keys.W);
        loadBinding(GameAction.P1_MOVE_DOWN, Input.Keys.S);
        loadBinding(GameAction.P1_MOVE_LEFT, Input.Keys.A);
        loadBinding(GameAction.P1_MOVE_RIGHT, Input.Keys.D);

        loadBinding(GameAction.P1_USE_ABILITY, Input.Keys.SPACE);
        loadBinding(GameAction.P1_DASH, Input.Keys.SHIFT_LEFT);
        loadBinding(GameAction.P1_INTERACT, Input.Keys.E);

        // ===== P2 默认键位 =====
        loadBinding(GameAction.P2_MOVE_UP, Input.Keys.UP);
        loadBinding(GameAction.P2_MOVE_DOWN, Input.Keys.DOWN);
        loadBinding(GameAction.P2_MOVE_LEFT, Input.Keys.LEFT);
        loadBinding(GameAction.P2_MOVE_RIGHT, Input.Keys.RIGHT);

        loadBinding(GameAction.P2_USE_ABILITY, Input.Buttons.LEFT);
        loadBinding(GameAction.P2_DASH, Input.Buttons.RIGHT);
        loadBinding(GameAction.P2_INTERACT, Input.Keys.NUM_1);

        // ===== SYSTEM =====
        loadBinding(GameAction.CONSOLE, Input.Keys.GRAVE);
    }


    private void loadBinding(GameAction action, int defaultKey) {
        int keyCode = prefs.getInteger(action.name(), defaultKey);
        keyBindings.put(action, keyCode);
    }

    public void setBinding(GameAction action, int newKeyCode) {
        keyBindings.put(action, newKeyCode);
        prefs.putInteger(action.name(), newKeyCode);
        prefs.flush();
    }

    public int getKey(GameAction action) {
        return keyBindings.getOrDefault(action, Input.Keys.UNKNOWN);
    }

    public String getKeyName(GameAction action) {
        return Input.Keys.toString(getKey(action));
    }
    // ==========================================
    // 🔥 把这两个方法加到 KeyBindingManager.java 的最底下
    // ==========================================

    /**
     * 检测某个动作的键是否正被按住 (用于移动)
     */
    public boolean isPressed(GameAction action) {
        int code = getKey(action);

        if (code == Input.Buttons.LEFT || code == Input.Buttons.RIGHT) {
            return Gdx.input.isButtonPressed(code);
        }

        return Gdx.input.isKeyPressed(code);
    }

    public boolean isJustPressed(GameAction action) {
        int code = getKey(action);

        if (code == Input.Buttons.LEFT || code == Input.Buttons.RIGHT) {
            return Gdx.input.isButtonJustPressed(code);
        }

        return Gdx.input.isKeyJustPressed(code);
    }



    /**
     * 🔥 双人模式默认键位
     */
    public void resetToDefaults() {

        // ======================
        // P1 - 键盘 WASD
        // ======================
        setBinding(GameAction.P1_MOVE_UP,    Input.Keys.W);
        setBinding(GameAction.P1_MOVE_DOWN,  Input.Keys.S);
        setBinding(GameAction.P1_MOVE_LEFT,  Input.Keys.A);
        setBinding(GameAction.P1_MOVE_RIGHT, Input.Keys.D);

        setBinding(GameAction.P1_USE_ABILITY, Input.Keys.SPACE);
        setBinding(GameAction.P1_DASH,        Input.Keys.SHIFT_LEFT);
        setBinding(GameAction.P1_INTERACT,    Input.Keys.E);

        // ======================
        // P2 - 方向键 + 鼠标
        // ======================
        setBinding(GameAction.P2_MOVE_UP,    Input.Keys.UP);
        setBinding(GameAction.P2_MOVE_DOWN,  Input.Keys.DOWN);
        setBinding(GameAction.P2_MOVE_LEFT,  Input.Keys.LEFT);
        setBinding(GameAction.P2_MOVE_RIGHT, Input.Keys.RIGHT);

        setBinding(GameAction.P2_USE_ABILITY, Input.Buttons.LEFT);   // 鼠标左键
        setBinding(GameAction.P2_DASH,        Input.Buttons.RIGHT);  // 鼠标右键
        setBinding(GameAction.P2_INTERACT,    Input.Keys.NUM_1);

        // ======================
        // 通用
        // ======================
        setBinding(GameAction.CONSOLE, Input.Keys.GRAVE); // `
    }

}
