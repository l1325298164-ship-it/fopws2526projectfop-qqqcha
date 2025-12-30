// GameManager.java
package de.tum.cit.fop.maze.game;

import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.entities.enemy.EnemyBoba.EnemyCorruptedBoba;
import de.tum.cit.fop.maze.entities.enemy.EnemyBullet;
import de.tum.cit.fop.maze.entities.enemy.EnemyE02_SmallCoffeeBean;
import de.tum.cit.fop.maze.entities.enemy.EnemyE03_CaramelJuggernaut;
import de.tum.cit.fop.maze.entities.trap.*;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeGenerator;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GameManager implements PlayerInputHandler.InputHandlerCallback  {
    private GameState gameState = GameState.PLAYING;
    private Player player;
    private PlayerInputHandler inputHandler;
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



    // === 新增：游戏状态控制 ===
    private boolean isPaused = false;
    private boolean canInteract = false;
    private GameObject interactableObject = null;


    public GameManager() {
        Logger.debug("GameManager initialized");

        exitDoors = new ArrayList<>();
        traps = new ArrayList<>();
        enemies = new ArrayList<>();
        bullets = new ArrayList<>();

        inputHandler = new PlayerInputHandler();

        initNewGame();
    }

    private void initNewGame() {
        mazeGenerator = new MazeGenerator();
        maze = mazeGenerator.generateMaze();

        currentLevel = 1;
        gameState = GameState.PLAYING;
        isGameComplete = false;
        keyCollected = false;
        compassActive = false;
        isPaused = false;

        exitDoors.clear();
        enemies.clear();
        traps.clear();
        bullets.clear();
        key = null;
        compass = null;

        int[] pos = findRandomPathPosition();
        startX = pos[0];
        startY = pos[1];

        if (player == null) {
            player = new Player(startX, startY, this);
        } else {
            player.reset();
            player.setPosition(startX, startY);
        }

        generateLevelElements();

        Logger.debug("New game initialized");
    }


    // ========== 输入回调实现 ==========

    @Override
    public void onMoveInput(int dx, int dy) {
        if (player.isDead() || isPaused) return;

        // 如果正在使用能力，不能移动
        if (!player.getAbilityManager().getActiveAbilities().isEmpty()) {
            return;
        }

        int newX = player.getX() + dx;
        int newY = player.getY() + dy;

        if (isMoveValid(newX, newY)) {
            player.move(dx, dy);
            onPlayerMoved(newX, newY);
        } else {
            Logger.debug("移动无效: (" + newX + ", " + newY + ")");
        }
    }

    @Override
    public float getMoveDelayMultiplier() {
        return player.getMoveDelayMultiplier();
    }

    @Override
    public boolean onAbilityInput(int slot) {
        if (player.isDead() || isPaused) {
            Logger.debug("无法使用能力: 玩家死亡或游戏暂停");
            return false;
        }

        Logger.debug("尝试使用能力槽位: " + slot);
        player.useAbility(slot);
        return true;
    }

    @Override
    public void onInteractInput() {
        if (player.isDead() || isPaused) {
            Logger.debug("无法交互: 玩家死亡或游戏暂停");
            return;
        }

        handlePlayerInteraction();
    }

    @Override
    public void onMenuInput() {
        togglePause();
    }

    private void togglePause() {
        isPaused = !isPaused;
        if (isPaused) {
            gameState = GameState.PAUSED;
            Logger.gameEvent("游戏暂停");
        } else {
            gameState = GameState.PLAYING;
            Logger.gameEvent("游戏继续");
        }
    }

    private boolean isMoveValid(int x, int y) {
        // 边界检查
        if (x < 0 || x >= GameConstants.MAZE_WIDTH ||
                y < 0 || y >= GameConstants.MAZE_HEIGHT) {
            return false;
        }

        // 墙壁检查 (0 = 墙, 1 = 通路)
        if (maze[y][x] == 0) {
            return false;
        }

        // 检查陷阱是否可以穿过
        for (Trap trap : traps) {
            if (trap.getX() == x && trap.getY() == y && !trap.isPassable()) {
                return false;
            }
        }

        // 检查门是否解锁
        for (ExitDoor door : exitDoors) {
            if (door.getX() == x && door.getY() == y) {
                return !door.isLocked() || player.hasKey();
            }
        }

        return true;
    }

    private void onPlayerMoved(int newX, int newY) {
        // 触发移动后的事件
        checkKeyCollection();
        checkTrapCollision();
        checkEnemyCollision();
        checkExit();

        // 更新可交互对象
        updateInteractableObject();
    }

    private void handlePlayerInteraction() {
        updateInteractableObject(); // 确保有最新的交互对象

        if (interactableObject != null) {
            if (interactableObject.isInteractable()) {
                Logger.gameEvent("与对象交互: " + interactableObject.getClass().getSimpleName());
                interactableObject.onInteract(player);
            } else {
                Logger.debug("对象不可交互: " + interactableObject.getClass().getSimpleName());
            }
        } else {
            Logger.debug("没有可交互的对象");
        }
    }


    private void updateInteractableObject() {
        canInteract = false;
        interactableObject = null;

        int playerX = player.getX();
        int playerY = player.getY();

        // 先检查玩家当前位置
        checkTileForInteraction(playerX, playerY);

        if (canInteract) return; // 如果当前位置有交互对象，优先处理

        // 检查玩家面对的方向
        Player.Direction dir = player.getDirection();
        int checkX = playerX;
        int checkY = playerY;

        switch (dir) {
            case UP:
                checkY += 1;
                break;
            case DOWN:
                checkY -= 1;
                break;
            case LEFT:
                checkX -= 1;
                break;
            case RIGHT:
                checkX += 1;
                break;
        }

        checkTileForInteraction(checkX, checkY);
    }

    private void checkTileForInteraction(int x, int y) {
        // 检查钥匙
        if (key != null && key.isActive() &&
                key.getX() == x && key.getY() == y) {
            canInteract = true;
            interactableObject = key;
            Logger.debug("发现可交互钥匙");
            return;
        }

        // 检查门
        for (ExitDoor door : exitDoors) {
            if (door.getX() == x && door.getY() == y) {
                canInteract = true;
                interactableObject = door;
                Logger.debug("发现可交互门");
                return;
            }
        }
    }

    private void handleExitDoor(ExitDoor door) {
        if (!door.isLocked()) {
            if (currentLevel < GameConstants.MAX_LEVELS) {
                // 进入下一关
                isExitingLevel = true;
                Logger.gameEvent("进入下一关...");
                // 这里可以触发关卡切换动画
                completeLevelTransition();
            } else {
                // 游戏通关
                gameState = GameState.LEVEL_COMPLETE;
                isGameComplete = true;
                Logger.gameEvent("游戏通关!");
            }
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
        int attempts = 0;
        int maxAttempts = 500;

        while (exitDoors.size() < GameConstants.EXIT_COUNT && attempts < maxAttempts) {
            int x = MathUtils.random(4, GameConstants.MAZE_WIDTH - 5);
            int y = MathUtils.random(4, GameConstants.MAZE_HEIGHT - 5);
            attempts++;

            if (!isValidInternalDoorSpot(x, y)) continue;
            if (isTooCloseToOtherExit(x, y, exitDoors.size())) continue;

            exitDoors.add(new ExitDoor(x, y, exitDoors.size() + 1));
            Logger.debug("Internal exit door generated at (" + x + ", " + y + ")");
        }

        Logger.gameEvent("Generated " + exitDoors.size() + " internal exit doors");
    }

    private boolean isValidInternalDoorSpot(int x, int y) {
        // 1. 必须是墙
        if (maze[y][x] != 0) return false;

        // 2. 不能靠外 4 层（给主题墙留空间）
        int B = 4;
        if (x < B || y < B ||
                x >= GameConstants.MAZE_WIDTH - B ||
                y >= GameConstants.MAZE_HEIGHT - B) {
            return false;
        }

        // 3. 至少一侧是通路
        if (maze[y + 1][x] == 1) return true;
        if (maze[y - 1][x] == 1) return true;
        if (maze[y][x + 1] == 1) return true;
        if (maze[y][x - 1] == 1) return true;

        return false;
    }


    private void generateTraps() {

        traps.clear();

        int t01Count = GameConstants.TRAP_T01_GEYSER_COUNT;
        int t02Count = GameConstants.TRAP_T02_PEARL_MINE_COUNT;
        int t03Count = GameConstants.TRAP_T03_TEA_SHARDS_COUNT;
        int t04Count = GameConstants.TRAP_T04_MUD_COUNT;

        // ===== 防御性校验（防止调参炸游戏）=====
        if (t01Count + t02Count != GameConstants.TRAP_COUNT) {
            Logger.warning(
                    "Trap count mismatch! T01(" + t01Count +
                            ") + T02(" + t02Count +
                            ") != TRAP_COUNT(" + GameConstants.TRAP_COUNT + ")"
            );
        }

        int attempts = 0;
        int maxAttempts = 500;

        // ===== 1️⃣ 生成 T02 地雷 =====
        while (t02Count > 0 && attempts < maxAttempts) {
            attempts++;

            int x = MathUtils.random(1, GameConstants.MAZE_WIDTH - 2);
            int y = MathUtils.random(1, GameConstants.MAZE_HEIGHT - 2);

            if (!isValidTrapPosition(x, y)) continue;

            traps.add(new TrapT02_PearlMine(x, y, this));
            t02Count--;
        }

        // ===== 2️⃣ 生成 T01 喷泉 =====
        attempts = 0;

        while (t01Count > 0 && attempts < maxAttempts) {
            attempts++;

            int x = MathUtils.random(1, GameConstants.MAZE_WIDTH - 2);
            int y = MathUtils.random(1, GameConstants.MAZE_HEIGHT - 2);

            if (!isValidTrapPosition(x, y)) continue;

            traps.add(new TrapT01_Geyser(x, y, 4f));
            t01Count--;
        }

        Logger.gameEvent(
                "Generated traps: " +
                        (GameConstants.TRAP_T01_GEYSER_COUNT - t01Count) + " Geyser, " +
                        (GameConstants.TRAP_T02_PEARL_MINE_COUNT - t02Count) + " PearlMine"
        );
        // ===== 生成 T03 茶叶碎 =====
        attempts = 0;
        while (t03Count > 0 && attempts < maxAttempts) {
            attempts++;

            int x = MathUtils.random(1, GameConstants.MAZE_WIDTH - 2);
            int y = MathUtils.random(1, GameConstants.MAZE_HEIGHT - 2);

            if (!isValidTrapPosition(x, y)) continue;

            traps.add(new TrapT03_TeaShards(x, y));
            t03Count--;
        }
        // ===== 生成 T04 泥潭 =====
        generateMudPatches(GameConstants.TRAP_T04_MUD_COUNT);


    }
    private void generateMudPatches(int totalMudCount) {

        int remaining = totalMudCount;
        int maxAttempts = 500;
        int attempts = 0;

        while (remaining > 0 && attempts < maxAttempts) {
            attempts++;

            // 随机决定这一块泥潭大小
            int patchSize = MathUtils.random(
                    GameConstants.MUD_PATCH_MIN_SIZE,
                    GameConstants.MUD_PATCH_MAX_SIZE
            );
            patchSize = Math.min(patchSize, remaining);

            // 随机一个起点
            int x = MathUtils.random(1, GameConstants.MAZE_WIDTH - 2);
            int y = MathUtils.random(1, GameConstants.MAZE_HEIGHT - 2);

            if (!isValidTrapPosition(x, y)) continue;

            // 用 BFS / 扩散方式生成这一块
            List<int[]> patchCells = new ArrayList<>();
            patchCells.add(new int[]{x, y});

            int index = 0;
            while (patchCells.size() < patchSize && index < patchCells.size()) {
                int[] cell = patchCells.get(index++);
                int cx = cell[0];
                int cy = cell[1];

                int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1}};
                shuffleDirections(dirs);

                for (int[] d : dirs) {
                    if (patchCells.size() >= patchSize) break;

                    int nx = cx + d[0];
                    int ny = cy + d[1];

                    if (!isValidTrapPosition(nx, ny)) continue;

                    boolean alreadyUsed = false;
                    for (int[] p : patchCells) {
                        if (p[0] == nx && p[1] == ny) {
                            alreadyUsed = true;
                            break;
                        }
                    }
                    if (alreadyUsed) continue;

                    patchCells.add(new int[]{nx, ny});
                }
            }

            // 真正生成 Trap
            for (int[] cell : patchCells) {
                traps.add(new TrapT04_Mud(cell[0], cell[1]));
                remaining--;
                if (remaining <= 0) break;
            }
        }

        Logger.gameEvent("Generated mud patches, total tiles: " +
                (totalMudCount - remaining));
    }

    private void shuffleDirections(int[][] dirs) {
        for (int i = dirs.length - 1; i > 0; i--) {
            int j = MathUtils.random(i);
            int[] tmp = dirs[i];
            dirs[i] = dirs[j];
            dirs[j] = tmp;
        }
    }



    private boolean isValidTrapPosition(int x, int y) {

        // 1. 必须是通路
        if (maze[y][x] != 1) return false;

        // 2. 不能离玩家太近
        if (Math.abs(x - player.getX()) +
                Math.abs(y - player.getY()) < 3) return false;

        // 3. 不能和 Key 重叠
        if (key != null && x == key.getX() && y == key.getY()) return false;

        // 4. 不能和 Door 重叠
        for (ExitDoor door : exitDoors) {
            if (x == door.getX() && y == door.getY()) {
                return false;
            }
        }

        // 5. 不能和已有 Trap 重叠
        for (Trap trap : traps) {
            if (x == trap.getX() && y == trap.getY()) {
                return false;
            }
        }

        return true;
    }


    private void generateEnemies() {

        // EnemyCorruptedBoba（会射 BobaBullet 的敌人）
        generateEnemyType(
                GameConstants.ENEMY_E01_PEARL_COUNT,
                (x, y) -> new EnemyCorruptedBoba(x, y)
        );

        generateEnemyType(
                GameConstants.ENEMY_E02_COFFEE_BEAN_COUNT,
                (x, y) -> new EnemyE02_SmallCoffeeBean(x, y)
        );

        generateEnemyType(
                GameConstants.ENEMY_E03_CARAMEL_COUNT,
                (x, y) -> new EnemyE03_CaramelJuggernaut(x, y)
        );

        Logger.gameEvent("Generated " + enemies.size() + " enemies");
    }



    @FunctionalInterface
    private interface EnemyFactory {
        Enemy create(int x, int y);
    }

    private void generateEnemyType(int count, EnemyFactory factory) {
        int attempts = 0;
        int maxAttempts = 200;

        while (count > 0 && attempts < maxAttempts) {
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

            enemies.add(factory.create(x, y));
            count--;
        }
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
        maze = mazeGenerator.generateMaze();

        exitDoors.clear();
        enemies.clear();
        traps.clear();
        bullets.clear();
        key = null;
        compass = null;

        int[] pos = findRandomPathPosition();
        startX = pos[0];
        startY = pos[1];

        player.reset();
        player.setPosition(startX, startY);

        generateLevelElements();

        currentLevel++;

        Logger.gameEvent("Level " + currentLevel + " started");
    }

    public void update(float deltaTime) {
        inputHandler.update(deltaTime, this);

        if (gameState != GameState.PLAYING|| isPaused) return;
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

        // 更新可交互对象检测
        updateInteractableObject();
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
            if (player.collidesWith(exitDoor) && !exitDoor.isLocked()) {

                if (currentLevel < GameConstants.MAX_LEVELS) {
                    initializeLevel();
                } else {
                    gameState = GameState.LEVEL_COMPLETE;
                    isGameComplete = true;
                }
                return;
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
    public boolean isPaused() { return isPaused; }
    public boolean canInteract() { return canInteract; }
    public GameObject getInteractableObject() { return interactableObject; }
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
            player = new Player(spawnX, spawnY,this);
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
    public List<Enemy> getEnemiesAt(int x, int y) {
        List<Enemy> enemiesAtPosition = new ArrayList<>();

        for (Enemy enemy : enemies) {
            if (enemy != null && !enemy.isDead()) {
                // 检查敌人的逻辑位置是否在指定格子
                if (enemy.getX() == x && enemy.getY() == y) {
                    enemiesAtPosition.add(enemy);
                }
            }
        }

        return enemiesAtPosition;
    }
    public void resetGame() {
        Logger.debug("Resetting game");

        maze = mazeGenerator.generateMaze();

        currentLevel = 1;
        gameState = GameState.PLAYING;
        isGameComplete = false;
        keyCollected = false;
        compassActive = false;
        isPaused = false;

        exitDoors.clear();
        enemies.clear();
        traps.clear();
        bullets.clear();
        key = null;
        compass = null;

        int[] pos = findRandomPathPosition();
        startX = pos[0];
        startY = pos[1];

        player.reset();
        player.setPosition(startX, startY);

        generateLevelElements();
    }





}