// GameManager.java
package de.tum.cit.fop.maze.game;

import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.EnemyBoba.EnemyCorruptedBoba;
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

    // 玩家初始位置
    private int startX, startY;

    // 游戏完成状态
    private boolean isGameComplete = false;
    private float gameCompleteTime = 0;
    private boolean keyCollected = false;
    private boolean compassActive = false;
    // 等待通关特效
    private boolean isExitingLevel = false;

    public GameManager() {
        Logger.debug("GameManager initialized");
        exitDoors = new ArrayList<>();
        traps = new ArrayList<>();
        initializeGame();

    }

    private void initializeGame() {
        Logger.debug("初始化游戏...");
        // 清空之前的出口
        exitDoors.clear();
        traps.clear();
        enemies.clear();
        bullets.clear();


        // 生成迷宫

        mazeGenerator = new MazeGenerator();
        maze = mazeGenerator.generateMaze();

        // 生成玩家位置
        int[] randomPos = findRandomPathPosition();
        startX = randomPos[0];
        startY = randomPos[1];

        if (player == null) {
            player = new Player(startX, startY);
        } else {
            player.setPosition(startX, startY);
            player.reset(); // 重置玩家状态
        }

        Logger.debug("Player spawned at (" + startX + ", " + startY + ")");

        // 生成游戏元素
        generateLevelElements();

        // 打印迷宫用于调试
        if (Logger.isDebugEnabled()) {
            mazeGenerator.printMazeForDebug(maze);
        }
    }

    private void generateLevelElements() {
        // 生成钥匙
        generateKey();

        // 生成出口
        generateExitDoors();

        // 生成陷阱
        generateTraps();

        // 生成敌人
        generateEnemies();

        // 重置玩家钥匙状态
        player.setHasKey(false);
        keyCollected = false;

        // 创建指南针
        compass = new Compass(player);
        compassActive = true;
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

            // enemies.add(new EnemyE01_CorruptedPearl(x, y));
            //Logger.debug("EnemyE01_CorruptedPearl generated at (" + x + ", " + y + ")");

// ✅ 生成新的 Boba 敌人
            enemies.add(new EnemyCorruptedBoba(x, y));
            Logger.debug("EnemyCorruptedBoba generated at (" + x + ", " + y + ")");
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

    /**
     * 查找最近的出口
     */
    private ExitDoor findNearestExit() {
        if (exitDoors.isEmpty()) {
            return null;
        }

        ExitDoor nearest = null;
        float minDistance = Float.MAX_VALUE;

        for (ExitDoor door : exitDoors) {
            float distance = calculateDistance(player.getX(), player.getY(),
                    door.getX(), door.getY());

            if (distance < minDistance) {
                minDistance = distance;
                nearest = door;
            }
        }

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

        // 🔥【新增】清空旧敌人和子弹
        enemies.clear();
        bullets.clear();

        // 重新生成迷宫和物品
        maze = mazeGenerator.generateMaze();

        // 生成玩家
        int[] randomPos = findRandomPathPosition();
        startX = randomPos[0];
        startY = randomPos[1];
        player.setPosition(startX, startY);
        player.setHasKey(false);
        player.reset();
        Logger.debug("Level " + currentLevel + ": Player spawned at (" +
                startX + ", " + startY + ")");

        // 重新生成游戏元素
        generateLevelElements();

        currentLevel++;


        Logger.gameEvent("Level " + currentLevel + " started");
    }
    public void update(float deltaTime) {
        if (gameState != GameState.PLAYING) return;
        if (player.isDead()) {
            gameState = GameState.GAME_OVER;
            Logger.gameEvent("Game Over - Player died");
            return;
        }

        // 更新玩家
        player.update(deltaTime);

        // 更新陷阱
        for (Trap trap : traps) {
            trap.update(deltaTime);
        }

        // 检查碰撞
        checkKeyCollection();
        checkTrapCollision();

        // 更新敌人
        for (Enemy e : enemies) {
            e.update(deltaTime, this);
        }

        // 检查敌人碰撞
        checkEnemyCollision();

        // 更新子弹
        for (EnemyBullet b : bullets) {
            b.update(deltaTime, this);
        }

        // 移除无效的敌人和子弹
        enemies.removeIf(e -> e == null || e.isDead());
        bullets.removeIf(b -> b == null || !b.isActive());

        // 更新指南针
        ExitDoor nearestExit = findNearestExit();
        if (compass != null) {
            compass.update(nearestExit);
        }

        // 检查出口
        checkExit();
    }

    private void checkTrapCollision() {
        for (Trap trap : traps) {
            if (trap.isActive() && player.collidesWith(trap)) {
                trap.onPlayerStep(player);
                Logger.gameEvent("Player stepped on a trap at " + trap.getPositionString());
            }
        }
    }

    private void generateKey() {
        int keyX, keyY;
        int attempts = 0;
        int maxAttempts = 100;

        do {
            keyX = MathUtils.random(1, GameConstants.MAZE_WIDTH - 2);
            keyY = MathUtils.random(1, GameConstants.MAZE_HEIGHT - 2);
            attempts++;

            if (attempts >= maxAttempts) {
                Logger.error("Failed to generate key after " + maxAttempts + " attempts");
                // 寻找第一个可用的通路位置
                outer:
                for (int y = 1; y < GameConstants.MAZE_HEIGHT - 1; y++) {
                    for (int x = 1; x < GameConstants.MAZE_WIDTH - 1; x++) {
                        if (maze[y][x] == 1 &&
                                Math.abs(x - player.getX()) + Math.abs(y - player.getY()) >= 3) {
                            keyX = x;
                            keyY = y;
                            break outer;
                        }
                    }
                }
                break;
            }
        } while (maze[keyY][keyX] != 1 ||
                Math.abs(keyX - player.getX()) + Math.abs(keyY - player.getY()) < 3);

        key = new Key(keyX, keyY);
        Logger.debug("Key generated at " + key.getPositionString() + " after " + attempts + " attempts");
    }

    private void checkKeyCollection() {
        if (key != null && key.isActive() && player.collidesWith(key)) {
            key.collect();
            player.setHasKey(true);
            keyCollected = true;

            // 解锁所有出口门
            for (ExitDoor door : exitDoors) {
                door.unlock();
            }
            Logger.gameEvent("Key collected, all " + exitDoors.size() + " exit doors unlocked");
        }
    }

    private void checkExit() {
        for (ExitDoor exitDoor : exitDoors) {
            if (player.collidesWith(exitDoor)) {
                if (!exitDoor.isLocked()) {
                    // 通过出口
                    if (currentLevel < GameConstants.MAX_LEVELS) {
                        // 进入下一关
                        initializeLevel();
                    } else {
                        // 游戏通关
                        gameState = GameState.LEVEL_COMPLETE;
                        isGameComplete = true;
                        Logger.gameEvent("Game completed!");
                    }
                    return;
                } else {
                    Logger.gameEvent("Exit door is locked, need key");
                }
            }
        }
    }

    // 新增方法：供 GameScreen 在动画播放完毕后调用
    public void completeLevelTransition() {
        isExitingLevel = false;
        if (currentLevel < GameConstants.MAX_LEVELS) {
            initializeLevel();
        } else {
            gameState = GameState.LEVEL_COMPLETE;
        }
    }

    public boolean isValidMove(int x, int y) {
        if (x < 0 || x >= GameConstants.MAZE_WIDTH ||
                y < 0 || y >= GameConstants.MAZE_HEIGHT) {
            return false;
        }

        // 检查是否是出口
        for (ExitDoor exitDoor : exitDoors) {
            if (x == exitDoor.getX() && y == exitDoor.getY()) {
                return !exitDoor.isLocked() || player.hasKey();
            }
        }

        return maze[y][x] == 1;
    }

    public boolean isEnemyValidMove(int x, int y) {
        if (x < 0 || x >= GameConstants.MAZE_WIDTH ||
                y < 0 || y >= GameConstants.MAZE_HEIGHT) {
            return false;
        }
        return maze[y][x] == 1;
    }

    public int[][] getMaze() {
        int[][] copy = new int[maze.length][];
        for (int i = 0; i < maze.length; i++) {
            copy[i] = Arrays.copyOf(maze[i], maze[i].length);
        }
        return copy;
    }

    public int getMazeCell(int x, int y) {
        if (isValidCoordinate(x, y)) {
            return maze[y][x];
        }
        return 0;
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
    public boolean isGameComplete() { return isGameComplete; }
    public int getCurrentLevel() { return currentLevel; }
    public int getLives() { return lives; }

    public List<ExitDoor> getExitDoors() {
        return exitDoors;
    }

    public Compass getCompass() {
        return compass;
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

    public void restart() {
        Logger.gameEvent("Game restarted");
        currentLevel = 1;
        initializeGame();
        gameState = GameState.PLAYING;
        isGameComplete = false;
    }

    public void onTextureModeChanged() {
        if (player != null) {
            player.onTextureModeChanged();
        }

        if (key != null) {
            key.onTextureModeChanged();
        }

        for (ExitDoor door : exitDoors) {
            door.onTextureModeChanged();
        }

        for (Trap trap : traps) {
            trap.onTextureModeChanged();
        }

        Logger.gameEvent("Texture mode changed to: " +
                TextureManager.getInstance().getCurrentMode());
    }

    public void spawnEnemy(Enemy enemy) {
        enemies.add(enemy);
    }

    public void spawnProjectile(EnemyBullet bullet) {
        bullets.add(bullet);
    }

    private void checkEnemyCollision() {
        for (Enemy enemy : enemies) {
            if (enemy == null || enemy.isDead()) continue;

            // 更精确的碰撞检测（使用网格坐标）
            if (player.getX() == enemy.getX() &&
                    player.getY() == enemy.getY()) {

                // 获取敌人的碰撞伤害
                int damage = enemy.getCollisionDamage();

                // 对玩家造成伤害
                player.takeDamage(damage);

                // 添加击退效果（可选）
//                applyKnockbackFromEnemy(enemy);

                Logger.gameEvent(
                        "Player hit by enemy at (" +
                                enemy.getX() + ", " + enemy.getY() +
                                ") for " + damage + " damage"
                );

                break; // 一次只处理一个敌人的碰撞
            }
        }
    }

    public void setMaze(int[][] qteMaze) {
        Logger.debug("GameManager.setMaze() - using fixed QTE maze");

        // 深拷贝迷宫
        this.maze = new int[qteMaze.length][];
        for (int i = 0; i < qteMaze.length; i++) {
            this.maze[i] = Arrays.copyOf(qteMaze[i], qteMaze[i].length);
        }

        // 清空内容
        exitDoors.clear();
        traps.clear();
        enemies.clear();
        bullets.clear();
        key = null;
        compass = null;

        // 创建/重置玩家
        int spawnX = 1;
        int spawnY = 1;

        outer:
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                if (maze[y][x] == 1) {
                    spawnX = x;
                    spawnY = y;
                    break outer;
                }
            }
        }

        if (player == null) {
            player = new Player(spawnX, spawnY);
        } else {
            player.setPosition(spawnX, spawnY);
            player.reset();
        }

        // 强制状态为 PLAYING
        gameState = GameState.PLAYING;
        isGameComplete = false;

        Logger.debug("QTE maze loaded, player spawned at (" +
                spawnX + ", " + spawnY + ")");
    }

    public void resetGame() {
        Logger.debug("GameManager 重置游戏状态");

        // 重置迷宫
        if (mazeGenerator == null) {
            mazeGenerator = new MazeGenerator();
        }
        maze = mazeGenerator.generateMaze();

        // 重置游戏状态
        currentLevel = 1;
        gameState = GameState.PLAYING;
        isGameComplete = false;
        gameCompleteTime = 0;
        keyCollected = false;
        compassActive = false;

        // 清空所有实体
        exitDoors.clear();
        enemies.clear();
        traps.clear();
        bullets.clear();
        key = null;
        compass = null;

        // 重新生成玩家
        int[] randomPos = findRandomPathPosition();
        startX = randomPos[0];
        startY = randomPos[1];

        if (player == null) {
            player = new Player(startX, startY);
        } else {
            player.reset();
            player.setPosition(startX, startY);
            player.setHasKey(false);
        }

        // 重新生成游戏元素
        generateLevelElements();

        Logger.debug("游戏状态已重置");
    }

    // 添加的辅助方法
    public boolean isKeyCollected() {
        return keyCollected;
    }

    public boolean isCompassActive() {
        return compassActive && compass != null;
    }

    // Getter methods
    public ExitDoor getExitDoor() { return exitDoors.isEmpty() ? null : exitDoors.get(0);  }
    public int[][] getMazeForRendering() {
        return maze; // 直接返回引用，因为MazeRenderer只需要读取
    }






}