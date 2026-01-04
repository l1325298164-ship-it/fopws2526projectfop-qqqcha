package de.tum.cit.fop.maze.game;

import com.badlogic.gdx.graphics.Color;

public final class GameConstants {

    /* ===== 地图基础 ===== */
    public static final int CELL_SIZE = 45;

    /* ===== 相机 ===== */
    public static final float VIEWPORT_WIDTH  = 800;
    public static final float VIEWPORT_HEIGHT = 600;

    /* ===== 玩家 ===== */
    public static final int MAX_LIVES = 999;
    public static final float INVINCIBLE_TIME = 3.0f;
    public static final float MOVE_DELAY_NORMAL = 0.06f;
    public static final float MOVE_DELAY_FAST = 0.1f;

    /* ===== 游戏进程 ===== */
    public static final int MAX_LEVELS = 5;
    public static final int KEYCOUNT = 1;

    /* ===== 颜色（纯表现 DEBUG用） ===== */
    public static final Color FLOOR_COLOR = new Color(0f, 200f/255f, 0f, 1f);
    public static final Color WALL_COLOR  = new Color(100f/255f, 100f/255f, 100f/255f, 1f);
    public static final Color PLAYER_COLOR = Color.RED;
    public static final Color KEY_COLOR = Color.YELLOW;
    public static final Color DOOR_COLOR = Color.BLUE;
    public static final Color LOCKED_DOOR_COLOR = Color.RED;
    public static final Color HEART_COLOR = Color.RED;

    /* ===== Debug ===== */
    public static final boolean DEBUG_MODE = false;

    private GameConstants() {}
}
