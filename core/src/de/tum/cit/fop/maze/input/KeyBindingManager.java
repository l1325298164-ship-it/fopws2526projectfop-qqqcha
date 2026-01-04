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
        MOVE_UP,
        MOVE_DOWN,
        MOVE_LEFT,
        MOVE_RIGHT,
        USE_ABILITY,
        INTERACT,
        CONSOLE // 👈 后面我们会用到这个来开控制台
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
        // 默认按键设置
        loadBinding(GameAction.MOVE_UP, Input.Keys.W);
        loadBinding(GameAction.MOVE_DOWN, Input.Keys.S);
        loadBinding(GameAction.MOVE_LEFT, Input.Keys.A);
        loadBinding(GameAction.MOVE_RIGHT, Input.Keys.D);
        loadBinding(GameAction.USE_ABILITY, Input.Keys.SPACE);
        loadBinding(GameAction.INTERACT, Input.Keys.E);
        loadBinding(GameAction.CONSOLE, Input.Keys.GRAVE); // ` 键
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
        return Gdx.input.isKeyPressed(getKey(action));
    }

    /**
     * 检测某个动作的键是否刚刚被按下 (用于技能/交互)
     */
    public boolean isJustPressed(GameAction action) {
        return Gdx.input.isKeyJustPressed(getKey(action));
    }
    /**
     * 🔥 新增：恢复默认设置
     * 根据你的要求，移动键恢复为 上/下/左/右
     */
    public void resetToDefaults() {
        setBinding(GameAction.MOVE_UP, Input.Keys.UP);
        setBinding(GameAction.MOVE_DOWN, Input.Keys.DOWN);
        setBinding(GameAction.MOVE_LEFT, Input.Keys.LEFT);
        setBinding(GameAction.MOVE_RIGHT, Input.Keys.RIGHT);

        // 其他功能键恢复默认
        setBinding(GameAction.USE_ABILITY, Input.Keys.SPACE);
        setBinding(GameAction.INTERACT, Input.Keys.E);
        setBinding(GameAction.CONSOLE, Input.Keys.F1);
    }
}
