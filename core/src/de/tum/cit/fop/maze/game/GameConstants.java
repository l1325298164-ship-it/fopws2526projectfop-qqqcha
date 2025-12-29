// GameConstants.java
package de.tum.cit.fop.maze.game;

import com.badlogic.gdx.graphics.Color;

public class GameConstants {
    public static final int MAZE_WIDTH = 30;      // 必须是奇数
    public static final int MAZE_HEIGHT = 30;     // 必须是奇数
    public static final int CELL_SIZE = 30;
    // 新增：出口数量
    public static final int EXIT_COUNT = 3;  // 出口数量


    public static final float MOVE_DELAY_NORMAL = 0.1f;
    public static final float MOVE_DELAY_FAST = 0.05f;

    //enemy数量在这里

    public static final int ENEMY_E01_PEARL_COUNT = 3;
    public static final int ENEMY_E02_COFFEE_BEAN_COUNT = 5;
    public static final int ENEMY_E03_CARAMEL_COUNT = 5;



    // 相机相关常量
    public static final float VIEWPORT_WIDTH = 800;  // 视口宽度
    public static final float VIEWPORT_HEIGHT = 600; // 视口高度

    // 地图边界（用于限制相机移动）
    public static final float MIN_CAMERA_X = VIEWPORT_WIDTH / 2;
    public static final float MIN_CAMERA_Y = VIEWPORT_HEIGHT / 2;
    public static final float MAX_CAMERA_X = MAZE_WIDTH * CELL_SIZE - VIEWPORT_WIDTH / 2;
    public static final float MAX_CAMERA_Y = MAZE_HEIGHT * CELL_SIZE - VIEWPORT_HEIGHT / 2;

    // 颜色常量
    public static final Color FLOOR_COLOR = new Color(0f, 200f/255f, 0f, 1f);
    public static final Color WALL_COLOR = new Color(100f/255f, 100f/255f, 100f/255f, 1f);
    public static final Color PLAYER_COLOR = Color.RED;
    public static final Color KEY_COLOR = Color.YELLOW;
    public static final Color DOOR_COLOR = Color.BLUE;
    public static final Color LOCKED_DOOR_COLOR = Color.RED;

    // 游戏相关
    public static final int INITIAL_PLAYER_LIVES = 99999;
    public static final Color HEART_COLOR = Color.RED;

    // 游戏事件
    public static final float INVINCIBLE_TIME = 1.0f;

    // 游戏规则
    public static final int MAX_LIVES = 3;
    public static final int MAX_LEVELS = 5;
    public static final int TRAP_COUNT = 100;
    public static final int ENEMY_COUNT =10 ;

    private GameConstants() {} // 防止实例化
}
