// GameManager.java
package de.tum.cit.fop.maze.game;

import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.maze.MazeGenerator;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameManager  {
    private GameState gameState = GameState.PLAYING;
    private Player player;
    private Key key;
    private List<ExitDoor> exitDoors; // 改为出口列表
    private List<Trap> traps;
    private List<Enemy> enemies = new ArrayList<>();
    private List<EnemyBullet> bullets = new ArrayList<>();

    private Compass compass;
    //maze
    private MazeGenerator mazeGenerator;
    private int[][] maze;
    private int lives = GameConstants.MAX_LIVES;
    private int currentLevel = 1;

    public GameManager() {
        Logger.debug("GameManager initialized");
        exitDoors = new ArrayList<>();
        traps = new ArrayList<>();
        initializeGame();

    }

    private void initializeGame() {
        // 清空之前的出口
        exitDoors.clear();
        traps.clear();
        enemies.clear();
        bullets.clear();


        // 生成迷宫

        mazeGenerator = new MazeGenerator();
        maze = mazeGenerator.generateMaze();


        // 100%概率：随机出现在迷宫中
        int[] randomPos = findRandomPathPosition();
        player = new Player(randomPos[0], randomPos[1]);
        Logger.debug("Player randomly spawned at (" + randomPos[0] + ", " + randomPos[1] + ")");


        // 生成钥匙
        generateKey();

        // 生成出口
        generateExitDoors();
        generateTraps();
        generateEnemies();


        compass = new Compass(player);

        // 打印迷宫用于调试
        if (Logger.isDebugEnabled()) {
            mazeGenerator.printMazeForDebug(maze);
        }



    }

    private void generateExitDoors() {
        int doorsToGenerate = GameConstants.EXIT_COUNT;
        int attempts = 0;
        int maxAttempts = 200;

        for (int i = 0; i < doorsToGenerate; i++) {
            boolean doorPlaced = false;
            int doorX = 0, doorY = 0;

            while (!doorPlaced && attempts < maxAttempts) {
                // 随机选择一个边界
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

                // 检查位置是否可用
                boolean positionAvailable =
                        maze[doorY][doorX] == 0 && // 必须是墙
                                isAccessibleFromInside(doorX, doorY) && // 必须可从内部进入
                                !isTooCloseToOtherExit(doorX, doorY, i); // 不能太靠近其他出口

                if (positionAvailable) {
                    // 创建新的出口
                    ExitDoor exitDoor = new ExitDoor(doorX, doorY, i + 1);
                    exitDoors.add(exitDoor);
                    doorPlaced = true;
                    Logger.debug("Exit door " + (i + 1) + " generated at (" +
                            doorX + ", " + doorY + ")");
                }
            }

            if (!doorPlaced) {
                Logger.warning("Failed to generate exit door " + (i + 1) +
                        " after " + attempts + " attempts");
            }
        }

        Logger.gameEvent("Generated " + exitDoors.size() + " exit doors");
    }
    private void generateTraps() {
        int trapCount = GameConstants.TRAP_COUNT; // 你可以自己加这个常量
        int attempts = 0;
        int maxAttempts = 200;

        while (traps.size() < trapCount && attempts < maxAttempts) {
            int x = MathUtils.random(1, GameConstants.MAZE_WIDTH - 2);
            int y = MathUtils.random(1, GameConstants.MAZE_HEIGHT - 2);
            attempts++;

            // 1. 必须是通路
            if (maze[y][x] != 1) continue;

            // 2. 不能生成在玩家起点附近
            if (Math.abs(x - player.getX()) + Math.abs(y - player.getY()) < 3) continue;

            // 3. 不能和 Key / Door 重叠
            if (key != null && x == key.getX() && y == key.getY()) continue;

            boolean overlapsDoor = false;
            for (ExitDoor door : exitDoors) {
                if (x == door.getX() && y == door.getY()) {
                    overlapsDoor = true;
                    break;
                }
            }
            if (overlapsDoor) continue;

            // 4. 不能和已有 Trap 重叠
            boolean overlapsTrap = false;
            for (Trap trap : traps) {
                if (x == trap.getX() && y == trap.getY()) {
                    overlapsTrap = true;
                    break;
                }
            }
            if (overlapsTrap) continue;

            traps.add(new Trap(x, y));
            Logger.debug("Trap generated at (" + x + ", " + y + ")");
        }

        Logger.gameEvent("Generated " + traps.size() + " traps");
    }
    private void generateEnemies() {
        int enemyCount = GameConstants.ENEMY_COUNT;
        int attempts = 0;
        int maxAttempts = 200;

        // ✅ 用 enemies.size()
        while (enemies.size() < enemyCount && attempts < maxAttempts) {
            int x = MathUtils.random(1, GameConstants.MAZE_WIDTH - 2);
            int y = MathUtils.random(1, GameConstants.MAZE_HEIGHT - 2);
            attempts++;

            if (maze[y][x] != 1) continue;
            if (Math.abs(x - player.getX()) + Math.abs(y - player.getY()) < 3) continue;
            if (key != null && x == key.getX() && y == key.getY()) continue;

            boolean overlapsDoor = false;
            for (ExitDoor door : exitDoors) {
                if (x == door.getX() && y == door.getY()) {
                    overlapsDoor = true;
                    break;
                }
            }
            if (overlapsDoor) continue;

            enemies.add(new EnemyE01_CorruptedPearl(x, y));
            Logger.debug("EnemyE01_CorruptedPearl generated at (" + x + ", " + y + ")");
        }

        Logger.gameEvent("Generated " + enemies.size() + " enemies");
    }



    /**
     * 检查出口是否可以从内部进入
     */
    private boolean isAccessibleFromInside(int doorX, int doorY) {
        if (doorX == 0 && maze[doorY][1] == 1) { // 左边界
            return true;
        } else if (doorX == GameConstants.MAZE_WIDTH - 1 &&
                maze[doorY][GameConstants.MAZE_WIDTH - 2] == 1) { // 右边界
            return true;
        } else if (doorY == 0 && maze[1][doorX] == 1) { // 下边界
            return true;
        } else if (doorY == GameConstants.MAZE_HEIGHT - 1 &&
                maze[GameConstants.MAZE_HEIGHT - 2][doorX] == 1) { // 上边界
            return true;
        }
        return false;
    }

    /**
     * 检查是否太靠近其他出口
     */
    private boolean isTooCloseToOtherExit(int x, int y, int currentDoorIndex) {
        int minDistance = 5; // 最小距离

        for (int i = 0; i < currentDoorIndex; i++) {
            ExitDoor existingDoor = exitDoors.get(i);
            int distance = Math.abs(existingDoor.getX() - x) +
                    Math.abs(existingDoor.getY() - y);
            if (distance < minDistance) {
                return true;
            }
        }
        return false;
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
    /**
     * 查找最近的出口
     */
    private ExitDoor findNearestExit() {
        Logger.debug("=== findNearestExit START ===");

        if (exitDoors.isEmpty()) {
            Logger.debug("exitDoors list is empty!");
            return null;
        }

        Logger.debug("Checking " + exitDoors.size() + " exit doors (including locked):");

        ExitDoor nearest = null;
        float minDistance = Float.MAX_VALUE;

        for (int i = 0; i < exitDoors.size(); i++) {
            ExitDoor door = exitDoors.get(i);

            // 不再跳过锁定的门！即使锁定也计算距离
            float distance = calculateDistance(player.getX(), player.getY(),
                    door.getX(), door.getY());

            Logger.debug("Door " + door.getDoorId() +
                    " at (" + door.getX() + "," + door.getY() + ")" +
                    " - locked: " + door.isLocked() +
                    " - distance: " + distance);

            if (distance < minDistance) {
                minDistance = distance;
                nearest = door;
                Logger.debug("  This is now the nearest door");
            }
        }

        Logger.debug("Nearest door: " +
                (nearest != null ?
                        "Door " + nearest.getDoorId() + " (locked: " + nearest.isLocked() + ")" :
                        "null"));
        Logger.debug("=== findNearestExit END ===");

        return nearest;
    }

    /**
     * 计算两点间的距离
     */
    private float calculateDistance(int x1, int y1, int x2, int y2) {
        return (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    /**
     * 查找随机路径位置
     */
    private int[] findRandomPathPosition() {
        int width = maze[0].length;
        int height = maze.length;
        int attempts = 0;
        int maxAttempts = 100;

        while (attempts < maxAttempts) {
            int x = MathUtils.random(1, width - 2);
            int y = MathUtils.random(1, height - 2);

            // 检查是否是通路且不是靠近边界（避免出生在死胡同）
            if (maze[y][x] == 1) {
                // 检查周围是否有至少2个方向可走
                int possibleDirections = 0;
                if (x > 0 && maze[y][x-1] == 1) possibleDirections++;
                if (x < width-1 && maze[y][x+1] == 1) possibleDirections++;
                if (y > 0 && maze[y-1][x] == 1) possibleDirections++;
                if (y < height-1 && maze[y+1][x] == 1) possibleDirections++;

                if (possibleDirections >= 2) {
                    return new int[]{x, y};
                }
            }
            attempts++;
        }

        // 如果找不到合适位置，返回默认位置
        Logger.warning("Could not find suitable random position, using default");
        return new int[]{1, 1};
    }

    private void initializeLevel() {
        // 清空出口列表
        exitDoors.clear();
        traps.clear();   // ⭐ 必须

        // 重新生成迷宫和物品
        maze = mazeGenerator.generateMaze();

        // 生成玩家
        int[] randomPos = findRandomPathPosition();
        player.setPosition(randomPos[0], randomPos[1]);
        Logger.debug("Level " + currentLevel + ": Player spawned at (" +
                randomPos[0] + ", " + randomPos[1] + ")");

        // 重新生成钥匙和出口
        generateKey();
        generateExitDoors();
        generateTraps(); //

        // 重置玩家钥匙状态
        player.setHasKey(false);

        // 更新指南针
        compass = new Compass(player);

        currentLevel++;

        Logger.gameEvent("Level " + currentLevel + " started");
    }
    public void update(float deltaTime) {
        if (gameState != GameState.PLAYING) return;
        if (player.isDead()) {
            gameState = GameState.GAME_OVER;
            return;
        }


        player.update(deltaTime);

        // ⭐ 更新 Trap（动画在这里走）
        for (Trap trap : traps) {
            if (trap != null) {
                trap.update(deltaTime);
            }
        }
        checkKeyCollection();
        checkTrapCollision();

        for (Enemy e : enemies) {
            e.update(deltaTime, this);
        }

        for (EnemyBullet b : bullets) {
            b.update(deltaTime, this);
        }



        // 总是查找最近的出口（包括锁定的）
        ExitDoor nearestExit = findNearestExit();

        Logger.debug("GameManager.update() - nearestExit: " +
                (nearestExit != null ?
                        "Door " + nearestExit.getDoorId() +
                                " (locked: " + nearestExit.isLocked() + ")" :
                        "null"));

        // 更新指南针
        if (compass != null) {
            compass.update(nearestExit);
            Logger.debug("Compass active: " + compass.isActive());
        }

        checkExit();

        enemies.removeIf(e -> e == null || e.isDead());
        bullets.removeIf(b -> b == null || !b.isActive());

    }

    private void checkTrapCollision() {
        for (Trap trap : traps) {
            if (trap != null && trap.isActive() && player.collidesWith(trap)) {
                trap.onPlayerStep(player);
                Logger.gameEvent("Player stepped on a trap at " + trap.getPositionString());
            }
        }
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




    private boolean isNearPlayer(int x, int y) {
        int distance = Math.abs(x - 1) + Math.abs(y - 1);
        return distance < 3;
    }



    private void checkKeyCollection() {
        if (key != null && key.isActive() && player.collidesWith(key)) {
            key.collect();
            player.setHasKey(true);
            // 解锁所有出口门
            for (ExitDoor door : exitDoors) {
                if (door != null) {
                    door.unlock();
                }
            }
            Logger.gameEvent("Key collected, all " + exitDoors.size() + " exit doors unlocked");
        }
    }






    private void checkExit() {
        for (ExitDoor exitDoor : exitDoors) {
            if (exitDoor != null && player.collidesWith(exitDoor)) {
                if (player.hasKey()) {
                    if (exitDoor.isLocked()) {
                        // 解锁当前出口
                        exitDoor.unlock();
                        Logger.gameEvent("Exit door " + exitDoor.getDoorId() + " unlocked");
                    } else {
                        // 通过出口
                        if (currentLevel < GameConstants.MAX_LEVELS) {
                            // 进入下一关
                            initializeLevel();
                        } else {
                            // 游戏通关
                            gameState = GameState.LEVEL_COMPLETE;
                            Logger.gameEvent("Game completed!");
                        }
                        return;
                        // 退出循环，因为已经进入下一关或游戏结束
                    }
                } else {
                    Logger.gameEvent("Player tried to exit without key");
                }
            }
        }
    }

    public boolean isValidMove(int x, int y) {
        if (x < 0 || x >= GameConstants.MAZE_WIDTH ||
                y < 0 || y >= GameConstants.MAZE_HEIGHT) {
            return false;
        }

        // 检查是否是出口


        // 检查是否是任意出口
        for (ExitDoor exitDoor : exitDoors) {
            if (x == exitDoor.getX() && y == exitDoor.getY()) {
                return player.hasKey() || !exitDoor.isLocked();
            }
        }

        return maze[y][x] == 1;
    }
    public boolean isEnemyValidMove(int x, int y) {
        if (x < 0 || x >= GameConstants.MAZE_WIDTH ||
                y < 0 || y >= GameConstants.MAZE_HEIGHT) {
            return false;
        }

        // 敌人只能走通路
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





    // Getter methods
    public GameState getGameState() { return gameState; }
    public void setGameState(GameState state) { this.gameState = state; }
    public Player getPlayer() { return player; }
    public Key getKey() { return key; }
    public ExitDoor getExitDoor() { return exitDoors.isEmpty() ? null : exitDoors.get(0);  }
    public boolean isGameComplete() { return gameState == GameState.LEVEL_COMPLETE; }
    public int[][] getMazeForRendering() {
        return maze; // 直接返回引用，因为MazeRenderer只需要读取
    }
    // ✅ 新增：获取所有出口
    public ArrayList<ExitDoor> getExitDoors() {
        return (ArrayList<ExitDoor>) exitDoors;
    }

    // ✅ 新增：获取指南针
    public Compass getCompass() {
        return compass;
    }

    public void restart() {
        Logger.gameEvent("Game restarted");
        initializeGame();
        gameState = GameState.PLAYING;
    }

    public Object getCurrentLevel() {
        return currentLevel;
    }
    public void onTextureModeChanged() {
        // 通知玩家
        if (player != null) {
            player.onTextureModeChanged();
        }

        // 通知钥匙
        if (key != null) {
            key.onTextureModeChanged();
        }

        // 通知所有出口
        for (ExitDoor door : exitDoors) {
            door.onTextureModeChanged();
        }

        for (Trap trap : traps) {
            if (trap != null) {
                trap.onTextureModeChanged();
            }
        }

        Logger.gameEvent("Texture mode changed to: " +
                TextureManager.getInstance().getCurrentMode());
    }
    public List<Trap> getTraps() {
        return traps;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<EnemyBullet> getBullets() {
        return bullets;
    }
    public void spawnEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    public void spawnProjectile(EnemyBullet bullet) {
        bullets.add(bullet);
    }


}
