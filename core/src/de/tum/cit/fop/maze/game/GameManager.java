// GameManager.java
package de.tum.cit.fop.maze.game;

import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.entities.ExitDoor;
import de.tum.cit.fop.maze.entities.Key;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.Test2;
import de.tum.cit.fop.maze.maze.MazeGenerator;
import de.tum.cit.fop.maze.utils.Logger;

import java.util.Arrays;

public class GameManager {
    private GameState gameState = GameState.PLAYING;
    private Player player;
    private Test2 test2;
    //将test2实装 并且撰写行动逻辑以及攻击逻辑
    //TODO 新增test3 作为静止陷阱

    private Key key;
    private ExitDoor exitDoor;
    //maze
    private MazeGenerator mazeGenerator;
    private int[][] maze;
    private int lives = GameConstants.MAX_LIVES;
    private int currentLevel = 1;

    public GameManager() {
        Logger.debug("GameManager initialized");
        initializeGame();
    }

    private void initializeGame() {
        // 生成迷宫
        mazeGenerator = new MazeGenerator();
        maze = mazeGenerator.generateMaze();

        // 创建玩家
        player = new Player(1, 1);
        test2 = new Test2(1, 1);

        // 生成钥匙
        generateKey();

        // 生成出口
        generateExitDoor();

        // 打印迷宫用于调试 跟之前一样 没有设置就不会进行打印
        if (Logger.isDebugEnabled()) {
            mazeGenerator.printMazeForDebug(maze);
        }




    }
    // ✅ 添加这个方法，确保返回副本
    public int[][] getMaze() {
        // 创建深拷贝，防止外部修改
        int[][] copy = new int[maze.length][];
        for (int i = 0; i < maze.length; i++) {
            copy[i] = Arrays.copyOf(maze[i], maze[i].length);
        }
        return copy;
    }

    public void update() {
        //用于切换其他游戏状态
        if (gameState != GameState.PLAYING) return;
        //时刻检查钥匙拿了没 出去了没
        checkKeyCollection();
        checkExit();
    }



    private void generateKey() {
        int keyX, keyY;
        int attempts = 0;

        do {
            keyX = MathUtils.random(1, GameConstants.MAZE_WIDTH - 2);
            keyY = MathUtils.random(1, GameConstants.MAZE_HEIGHT - 2);
            attempts++;
            if (attempts > 100) {
                Logger.error("Failed to generate key after 100 attempts");
                // 强制生成在可用位置，该算法还可以改进，比如生成在距离玩家30steps的位置
                for (int y = 1; y < GameConstants.MAZE_HEIGHT - 1; y++) {
                    for (int x = 1; x < GameConstants.MAZE_WIDTH - 1; x++) {
                            keyX = x;
                            keyY = y;
                        }
                    }
                break;
                }
            } while (maze[keyY][keyX] == 0);

        key = new Key(keyX, keyY);
        Logger.debug("Key generated at " + key.getPositionString() + " after " + attempts + " attempts");
    }


    public void update(float deltaTime) {
        if (gameState != GameState.PLAYING) return;

        // 更新玩家状态
        player.update(deltaTime);

        // 更新敌人
//        for (Enemy enemy : enemies) {
//            enemy.update(deltaTime);
//        }

        checkKeyCollection();
//        checkEnemyCollision();
        checkExit();
//        checkGameOver();
    }

    private void generateExitDoor() {
        int doorX, doorY;
        int attempts = 0;
        boolean doorIsAccessible = false;

        do {
            int side = MathUtils.random(0, 3);
            switch (side) {
                case 0: // 上边界
                    doorX = MathUtils.random(1, GameConstants.MAZE_WIDTH - 2);
                    doorY = GameConstants.MAZE_HEIGHT - 1;
                    break;
                case 1: // 右边界
                    doorX = GameConstants.MAZE_WIDTH - 1;
                    doorY = MathUtils.random(1, GameConstants.MAZE_HEIGHT - 2);
                    break;
                case 2: // 下边界
                    doorX = MathUtils.random(1, GameConstants.MAZE_WIDTH - 2);
                    doorY = 0;
                    break;
                default: // 左边界
                    doorX = 0;
                    doorY = MathUtils.random(1, GameConstants.MAZE_HEIGHT - 2);
                    break;
            }
            attempts++;

            // ✅ 简单的可进入性检查：门必须有一边是通路
            doorIsAccessible = false;
            if (doorX == 0 && maze[doorY][1] == 1) { // 左边界，检查右边
                doorIsAccessible = true;
            } else if (doorX == GameConstants.MAZE_WIDTH - 1 && maze[doorY][GameConstants.MAZE_WIDTH - 2] == 1) { // 右边界，检查左边
                doorIsAccessible = true;
            } else if (doorY == 0 && maze[1][doorX] == 1) { // 下边界，检查上边
                doorIsAccessible = true;
            } else if (doorY == GameConstants.MAZE_HEIGHT - 1 && maze[GameConstants.MAZE_HEIGHT - 2][doorX] == 1) { // 上边界，检查下边
                doorIsAccessible = true;
            }

            if (attempts > 100) {
                Logger.error("Failed to generate accessible exit door after 100 attempts");
                // 强制在(0,0)生成，并确保它是可进入的
                doorX = 0;
                doorY = 0;
                maze[0][1] = 1; // 确保右边是通路
                maze[1][0] = 1; // 确保下边是通路
                doorIsAccessible = true;
            }

            // ❌ 旧的：只检查是否是墙
            // } while (maze[doorY][doorX] != 0);

            // ✅ 新的：检查是否是墙 AND 是否可进入
        } while (maze[doorY][doorX] != 0 || !doorIsAccessible);

        exitDoor = new ExitDoor(doorX, doorY);
        Logger.debug("Exit door generated at " + exitDoor.getPositionString() +
            " after " + attempts + " attempts");
    }

    private boolean isNearPlayer(int x, int y) {
        int distance = Math.abs(x - 1) + Math.abs(y - 1);
        return distance < 3;
    }



    private void checkKeyCollection() {
        if (key != null && key.isActive() && player.collidesWith(key)) {
            key.collect();
            player.setHasKey(true);
            if (exitDoor != null) {
                exitDoor.unlock();
            }
        }
    }


    public int getLives() {
        return lives;
    }

    public int getMaxLives() {
        return GameConstants.MAX_LIVES;
    }


    private void checkExit() {
        if (exitDoor != null && player.collidesWith(exitDoor)) {
            if (player.hasKey()) {
                if (currentLevel < GameConstants.MAX_LEVELS) {
                    // 进入下一关
                    initializeLevel();
                } else {
                    // 游戏通关
                    gameState = GameState.LEVEL_COMPLETE;
                    Logger.gameEvent("Game completed!");
                }
            } else {
                Logger.gameEvent("Player tried to exit without key");
            }
        }
    }

    private void initializeLevel() {
        // 重新生成迷宫和物品
        maze = mazeGenerator.generateMaze();
        generateKey();
        generateExitDoor();
        player.setPosition(1, 1);
        player.setHasKey(false);
    }

    public boolean isValidMove(int x, int y) {
        if (x < 0 || x >= GameConstants.MAZE_WIDTH ||
            y < 0 || y >= GameConstants.MAZE_HEIGHT) {
            return false;
        }

        // 检查是否是出口
        if (exitDoor != null && x == exitDoor.getX() && y == exitDoor.getY()) {
            return player.hasKey();
        }

        return maze[y][x] == 1;
    }
    // ✅ 添加这个方法：提供只读访问
    public int getMazeCell(int x, int y) {
        if (isValidCoordinate(x, y)) {
            return maze[y][x];
        }
        return 0; // 越界返回墙
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < GameConstants.MAZE_WIDTH &&
            y >= 0 && y < GameConstants.MAZE_HEIGHT;
    }


    public boolean isPlayerAtEdge() {
        Player player = getPlayer();
        if (player == null) return false;

        // 检查玩家是否靠近地图边缘
        int edgeThreshold = 3;
        return player.getX() <= edgeThreshold ||
            player.getX() >= GameConstants.MAZE_WIDTH - edgeThreshold - 1 ||
            player.getY() <= edgeThreshold ||
            player.getY() >= GameConstants.MAZE_HEIGHT - edgeThreshold - 1;
    }
    public float[] getCameraBounds() {
        // 返回相机应该限制的范围
        return new float[] {
            GameConstants.MIN_CAMERA_X,
            GameConstants.MAX_CAMERA_X,
            GameConstants.MIN_CAMERA_Y,
            GameConstants.MAX_CAMERA_Y
        };
    }


    // Getter methods
    public GameState getGameState() { return gameState; }
    public void setGameState(GameState state) { this.gameState = state; }
    public Player getPlayer() { return player; }
    public Test2 getTest2() {
        return test2;
    }
    public void setTest2(Test2 test2) {}
    public Key getKey() { return key; }
    public ExitDoor getExitDoor() { return exitDoor; }
    public boolean isGameComplete() { return gameState == GameState.LEVEL_COMPLETE; }
    public int[][] getMazeForRendering() {
        return maze; // 直接返回引用，因为MazeRenderer只需要读取
    }

    public void restart() {
        Logger.gameEvent("Game restarted");
        initializeGame();
        gameState = GameState.PLAYING;
    }

    public Object getCurrentLevel() {
        return currentLevel;
    }


}
