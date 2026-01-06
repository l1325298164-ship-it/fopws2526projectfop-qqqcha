package de.tum.cit.fop.maze.game;

import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.effects.boba.BobaBulletManager;
import de.tum.cit.fop.maze.effects.fog.FogSystem;
import de.tum.cit.fop.maze.effects.key.KeyEffectManager;
import de.tum.cit.fop.maze.effects.portal.PortalEffectManager;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.Obstacle.DynamicObstacle;
import de.tum.cit.fop.maze.entities.Obstacle.MovingWall;
import de.tum.cit.fop.maze.entities.enemy.*;
import de.tum.cit.fop.maze.entities.enemy.EnemyBoba.BobaBullet;
import de.tum.cit.fop.maze.entities.trap.*;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeGenerator;
import de.tum.cit.fop.maze.utils.Logger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import static com.badlogic.gdx.math.MathUtils.random;
import static de.tum.cit.fop.maze.maze.MazeGenerator.BORDER_THICKNESS;

public class GameManager implements PlayerInputHandler.InputHandlerCallback {
    private final DifficultyConfig difficultyConfig;



    public DifficultyConfig getDifficultyConfig() {
        return difficultyConfig;
    }
    private int[][] maze;
    private Player player;

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Trap> traps = new ArrayList<>();
    private final List<Heart> hearts = new ArrayList<>();
    private final List<Treasure> treasures = new ArrayList<>();
    private final List<ExitDoor> exitDoors = new ArrayList<>();
    private final Array<BobaBullet> bullets = new Array<>();
    private List<DynamicObstacle> obstacles = new ArrayList<>();

    // GameManager.java
    private FogSystem fogSystem;
    private Compass compass;
    private MazeGenerator generator = new MazeGenerator();
    private KeyEffectManager keyEffectManager;
    private PlayerInputHandler inputHandler;
    // ===== Cat Follower =====
    private CatFollower cat;

    private Map<String, Float> gameVariables;

    // ===== Keys =====
    private final List<Key> keys = new ArrayList<>();
    private boolean keyProcessed = false;

    // ===== Reset Control =====
    private boolean pendingReset = false;
    private boolean justReset = false;

    // 🔥 新增：动画状态管理
    private boolean levelTransitionInProgress = false;
    private ExitDoor currentExitDoor = null;
    private float levelTransitionTimer = 0f;
    private static final float LEVEL_TRANSITION_DELAY = 0.5f; // 动画完成后延迟0.5秒

    private int currentLevel = 1;

    //effect to player
    private PortalEffectManager playerSpawnPortal;


    /* ================= 生命周期 ================= */
    public GameManager(DifficultyConfig difficultyConfig) {
        this.inputHandler = new PlayerInputHandler();
        if (difficultyConfig == null) {
            throw new IllegalArgumentException("difficultyConfig must not be null");
        }
        this.difficultyConfig = difficultyConfig;

        // ⚠️ 一定要在最后
        resetGame();
    }

    private void resetGame() {
        gameVariables = new HashMap<>();
        // 默认值：速度 1.0，受伤倍率 1.0，相机缩放 1.0
        gameVariables.put("speed_mult", 1.0f);
        gameVariables.put("dmg_taken", 1.0f);
        gameVariables.put("cam_zoom", 1.0f);
        gameVariables.put("time_scale", 1.0f);

        maze = generator.generateMaze(difficultyConfig);


        enemies.clear();
        traps.clear();
        hearts.clear();
        treasures.clear();
        // 🔥 注意：exitDoors 不清空，只重置状态
        for (ExitDoor door : exitDoors) {
            if (door != null) {
                door.resetDoor();
            }
        }
        keys.clear();

        int[] spawn = randomEmptyCell();

        if (player == null) {
            player = new Player(spawn[0], spawn[1], this);
        } else {
            player.reset();
            player.setPosition(spawn[0], spawn[1]);
        }
        cat = null;  // 默认没有小猫
        if (difficultyConfig.difficulty == Difficulty.HARD) {
            fogSystem = new FogSystem();
        } else {
            fogSystem = null;
        }
        // 🔥 玩家出生传送阵（一次性）
        float px = player.getX() * GameConstants.CELL_SIZE;
        float py = player.getY() * GameConstants.CELL_SIZE;


        playerSpawnPortal = new PortalEffectManager(PortalEffectManager.PortalOwner.PLAYER);
        playerSpawnPortal.startPlayerSpawnEffect(px, py);
        obstacles = new ArrayList<>();

        generateLevel();

        compass = new Compass(player);
        bullets.clear();
        bobaBulletEffectManager.clearAllBullets(false);
        keyEffectManager = new KeyEffectManager();

        // 🔥 重置动画状态
        levelTransitionInProgress = false;
        currentExitDoor = null;
        levelTransitionTimer = 0f;
        // 🔥 测试：强制在玩家旁边创建一个T01陷阱
        int testX = player.getX() + 1;
        int testY = player.getY();

        System.out.println("🔥🔥🔥 创建测试陷阱在玩家旁边: (" + testX + "," + testY + ")");
        Trap testTrap = new TrapT01_Geyser(testX, testY, 2.0f);
        traps.add(testTrap);
        Logger.gameEvent("Game reset complete");
    }

    public void update(float delta) {
        // 🔥 强制修正粒子中心
        if (playerSpawnPortal != null) {
            float cx = (player.getX() + 0.5f) * GameConstants.CELL_SIZE;
            float cy = (player.getY() + 0.15f) * GameConstants.CELL_SIZE;

            playerSpawnPortal.setCenter(cx, cy);
            playerSpawnPortal.update(delta);

            if (playerSpawnPortal.isFinished()) {
                playerSpawnPortal.dispose();
                playerSpawnPortal = null;
            }
        }
                // 🔥 如果关卡过渡正在进行，只更新相关逻辑
        if (levelTransitionInProgress) {
            if (currentExitDoor != null) {
                // 只更新当前触发的出口门
                currentExitDoor.update(delta, this);
            }

            // 更新关卡过渡计时器
            levelTransitionTimer += delta;
            if (levelTransitionTimer >= LEVEL_TRANSITION_DELAY) {
                // 延迟时间到，触发重置
                levelTransitionInProgress = false;
                levelTransitionTimer = 0f;
                currentExitDoor = null;
                nextLevel();
            }
            return;
        }

        // 正常游戏逻辑
        player.update(delta);
        boolean fogOn = fogSystem != null && fogSystem.isActive();

// Hard + 雾 → 启用猫
        if (difficultyConfig.difficulty == Difficulty.HARD) {
            if (fogOn) {
                if (cat == null)
                    cat = new CatFollower(player, this);
                cat.update(delta);   // ★ 必须添加
            } else {
                cat = null;
            }
        } else {
            cat = null;
        }
        if (fogSystem != null) {
            fogSystem.update(delta);
        }

        // ===== 🔥 新增：更新陷阱 =====
        for (Trap trap : traps) {
            if (trap.isActive()) {
                trap.update(delta);

                // 🔥 调试：输出T01陷阱状态
                if (trap instanceof TrapT01_Geyser) {
                    Logger.debug("T01陷阱更新: 位置(" + trap.getX() + "," + trap.getY() + ")");
                }
            }
        }




        // ===== 修复: 使用 Iterator 遍历敌人，避免并发修改异常 =====
        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy e = enemyIterator.next();
            e.update(delta, this);

            if (e.isDead() || !e.isActive()) {
                enemyIterator.remove();
            }
        }

        // 更新出口门
        for (ExitDoor door : exitDoors) {
            door.update(delta, this);
        }

        // 🔥 修改：检查玩家是否到达出口
        checkExitReached();
        updateCompass();
        updateBullets(delta);
        for (DynamicObstacle o : obstacles) {
            o.update(delta, this);
        }
        bobaBulletEffectManager.addBullets(bullets);
        bobaBulletEffectManager.update(delta);

        handlePlayerEnemyCollision();
        handleDashHitEnemies();
        checkAutoPickup();

        if (keyEffectManager != null) {
            keyEffectManager.update(delta);
        }
        handlePlayerTrapInteraction();
        handleKeyLogic();

        // ===== 🔥 统一重置执行点 =====
        if (pendingReset) {
            pendingReset = false;
            resetGame();
            justReset = true;
        }
    }

    private void handlePlayerTrapInteraction() {
        if (levelTransitionInProgress || player == null || player.isDead()) return;

        int px = player.getX();
        int py = player.getY();

        for (Trap trap : traps) {
            if (!trap.isActive()) continue;

            // 检查玩家是否在陷阱上
            if (trap.getX() == px && trap.getY() == py) {
                trap.onPlayerStep(player);

                // 🔥 调试：输出陷阱交互
                if (trap instanceof TrapT01_Geyser) {
                    Logger.debug("玩家在T01陷阱上: 位置(" + px + "," + py + ")");
                }
            }
        }
    }
    private void updateCompass() {
        if (compass == null) return;

        ExitDoor nearest = null;
        float bestDist = Float.MAX_VALUE;

        for (ExitDoor door : exitDoors) {
            if (!door.isActive()) continue;

            float dx = door.getX() - player.getX();
            float dy = door.getY() - player.getY();
            float dist = dx * dx + dy * dy; // 不开根号，性能好

            if (dist < bestDist) {
                bestDist = dist;
                nearest = door;
            }
        }

        compass.update(nearest);
    }

    // 🔥 新增：检查玩家是否到达出口
    private void checkExitReached() {
        Player p = player;

        for (ExitDoor door : exitDoors) {
            if (!door.isLocked() &&
                    door.isActive() &&
                    door.getX() == p.getX() &&
                    door.getY() == p.getY() &&
                    !levelTransitionInProgress) { // 🔥 防止重复触发

                // 触发门动画
                door.onPlayerStep(p);

                // 开始关卡过渡
                startLevelTransition(door);
                return;
            }
        }
    }

    // 🔥 新增：开始关卡过渡
    private void startLevelTransition(ExitDoor door) {
        levelTransitionInProgress = true;
        currentExitDoor = door;
        levelTransitionTimer = 0f;

        // 可选：禁用玩家输入
        Logger.gameEvent("Level transition started at door " + door.getPositionString());
    }

    public void nextLevel() {
        currentLevel++;

        if (currentLevel > GameConstants.MAX_LEVELS) {
            Logger.gameEvent("Game completed!");
            return;
        }

        requestReset();
    }
    public void requestReset() {
        pendingReset = true;
    }

    public void onKeyCollected() {
        player.setHasKey(true);
        unlockAllExitDoors();
        Logger.gameEvent("All exits unlocked");
    }
    private void unlockAllExitDoors() {
        for (ExitDoor door : exitDoors) {
            if (door.isLocked()) {
                door.unlock();
            }
        }
    }
    private void handleKeyLogic() {
        if (keyProcessed) return;

        for (Key key : keys) {
            if (key.isCollected()) {
                unlockAllExitDoors();
                keyProcessed = true;
                break;
            }
        }
    }
    public boolean isExitDoorAt(int x, int y) {
        for (ExitDoor door : exitDoors) {
            if (door.getX() == x && door.getY() == y) {
                return true;
            }
        }
        return false;
    }

    private void updateBullets(float delta) {
        for (int i = bullets.size - 1; i >= 0; i--) {
            BobaBullet bullet = bullets.get(i);
            bullet.update(delta, this);

            if (!bullet.isActive()) {
                bullets.removeIndex(i);
            }
        }
    }

    private void generateLevel() {
        // 🔥 只在第一次生成门
        if (exitDoors.isEmpty()) {
            generateExitDoors();
        }
        generateEnemies();
        generateTraps();
        generateHearts();
        generateTreasures();
        generateKeys();

        generateMovingWalls();
    }

    private void generateMovingWalls() {
        obstacles.clear();  // 确保清空旧的

        int sx, sy, ex, ey;

        // 找一个横向通路
        do {
            sx = random.nextInt(difficultyConfig.mazeWidth - 10);
            sy = random.nextInt(difficultyConfig.mazeHeight);
            ex = sx + 5;   // 让它向右走 5 格
            ey = sy;
        } while (!isWalkableLine(sx, sy, ex, ey));

        MovingWall wall = new MovingWall(sx, sy, ex, ey, MovingWall.WallType.SINGLE);
        obstacles.add(wall);

        // 添加调试日志
        Logger.debug("MovingWall created: (" + sx + "," + sy + ") -> (" + ex + "," + ey + ")");
    }

    private boolean isWalkableLine(int sx, int sy, int ex, int ey) {
        if (sy != ey) return false; // 只做水平路径
        for (int x = sx; x <= ex; x++) {
            if (maze[sy][x] != 1) return false;
        }
        return true;
    }


    private void generateKeys() {
        int keyCount = GameConstants.KEYCOUNT;

        for (int i = 0; i < keyCount; i++) {
            int x, y;
            do {
                x = random.nextInt(difficultyConfig.mazeWidth);
                y = random.nextInt(difficultyConfig.mazeHeight);
            } while (
                    getMazeCell(x, y) != 1 ||
                            isOccupied(x, y) ||
                            isExitDoorAt(x, y)
            );
            keys.add(new Key(x, y, this));
        }
    }

    private boolean isOccupied(int x, int y) {
        // 玩家
        if (player != null && player.getX() == x && player.getY() == y) {
            return true;
        }

        // 敌人
        for (Enemy e : enemies) {
            if (e.isActive() && e.getX() == x && e.getY() == y) {
                return true;
            }
        }

        // 宝箱
        for (Treasure t : treasures) {
            if (t.isActive() && t.getX() == x && t.getY() == y) {
                return true;
            }
        }

        // 爱心
        for (Heart h : hearts) {
            if (h.isActive() && h.getX() == x && h.getY() == y) {
                return true;
            }
        }

        for (Key k : keys) {
            if (k.isActive() && k.getX() == x && k.getY() == y) {
                return true;
            }
        }

        // 陷阱
        for (Trap trap : traps) {
            if (trap.isActive() && trap.getX() == x && trap.getY() == y) {
                return true;
            }
        }

        return false;
    }


    //============EXIT DOORS===============//
    private void generateExitDoors() {
        // 🔥 清空旧的门（第一次调用时应该是空的）
        exitDoors.clear();

        for (int i = 0; i < difficultyConfig.exitCount; i++) {
            int[] p = randomWallCell();
            int attempts = 0;

            // 🔥 确保门的位置是有效的
            while (!isValidDoorPosition(p[0], p[1]) && attempts < 50) {
                p = randomWallCell();
                attempts++;
            }

            // 🔥 关键修复：根据位置智能确定门的方向
            ExitDoor.DoorDirection direction = determineDoorDirection(p[0], p[1]);

            // 🔥 使用带方向的构造函数
            ExitDoor door = new ExitDoor(p[0], p[1], direction);
            exitDoors.add(door);
            Logger.debug("ExitDoor created at (" + p[0] + ", " + p[1] + ") facing " + direction);
        }
    }

    // 🔥 新增：根据迷宫结构智能确定门的方向
    private ExitDoor.DoorDirection determineDoorDirection(int x, int y) {
        int[][] maze = getMaze();
        int width = maze[0].length;
        int height = maze.length;

        // 统计四个方向的通路情况
        boolean up = y + 1 < height && maze[y + 1][x] == 1;
        boolean down = y - 1 >= 0 && maze[y - 1][x] == 1;
        boolean left = x - 1 >= 0 && maze[y][x - 1] == 1;
        boolean right = x + 1 < width && maze[y][x + 1] == 1;

        // 🔥 简化逻辑：优先选择有通路的方向
        List<ExitDoor.DoorDirection> possibleDirections = new ArrayList<>();

        if (up) possibleDirections.add(ExitDoor.DoorDirection.UP);
        if (down) possibleDirections.add(ExitDoor.DoorDirection.DOWN);
        if (left) possibleDirections.add(ExitDoor.DoorDirection.LEFT);
        if (right) possibleDirections.add(ExitDoor.DoorDirection.RIGHT);

        // 如果有可用的通路方向，随机选择一个
        if (!possibleDirections.isEmpty()) {
            return possibleDirections.get(random.nextInt(possibleDirections.size()));
        }

        // 🔥 如果没有相邻通路，根据位置决定（边缘的门应该有合理的朝向）
        if (y >= height - 3) return ExitDoor.DoorDirection.DOWN;    // 靠近底部，门朝下
        if (y <= 2) return ExitDoor.DoorDirection.UP;               // 靠近顶部，门朝上
        if (x >= width - 3) return ExitDoor.DoorDirection.LEFT;     // 靠近右边，门朝左
        if (x <= 2) return ExitDoor.DoorDirection.RIGHT;            // 靠近左边，门朝右

        // 默认向上
        return ExitDoor.DoorDirection.UP;
    }

    private boolean isValidDoorPosition(int x, int y) {
        int[][] maze = getMaze();
        int width = maze[0].length;
        int height = maze.length;

        // 必须是墙
        if (maze[y][x] != 0) return false;

        // 🔥 关键：检查相邻格子是否有通路
        boolean hasAdjacentPath = false;

        // 四个主要方向
        if (y + 1 < height && maze[y + 1][x] == 1) hasAdjacentPath = true;
        if (y - 1 >= 0 && maze[y - 1][x] == 1) hasAdjacentPath = true;
        if (x - 1 >= 0 && maze[y][x - 1] == 1) hasAdjacentPath = true;
        if (x + 1 < width && maze[y][x + 1] == 1) hasAdjacentPath = true;

        return hasAdjacentPath;
    }

    private int[] randomWallCell() {
        int[][] maze = getMaze();
        int width = maze[0].length;
        int height = maze.length;

        for (int attempt = 0; attempt < 1000; attempt++) {
            int x = BORDER_THICKNESS + random.nextInt(width - BORDER_THICKNESS * 2);
            int y = BORDER_THICKNESS + random.nextInt(height - BORDER_THICKNESS * 2);

            // 1️⃣ 必须是墙
            if (maze[y][x] != 0) continue;

            // 2️⃣ 不能已经有出口门
            if (isExitDoorAt(x, y)) continue;

            // 🔥 3️⃣ 关键修复：检查相邻格子是否有通路
            // 检查上下左右四个方向
            boolean hasAdjacentPath = false;

            // 上
            if (y + 1 < height && maze[y + 1][x] == 1) hasAdjacentPath = true;
            // 下
            if (y - 1 >= 0 && maze[y - 1][x] == 1) hasAdjacentPath = true;
            // 左
            if (x - 1 >= 0 && maze[y][x - 1] == 1) hasAdjacentPath = true;
            // 右
            if (x + 1 < width && maze[y][x + 1] == 1) hasAdjacentPath = true;

            if (!hasAdjacentPath) continue;

            return new int[]{x, y};
        }

        Logger.warning("randomWallCell fallback triggered");
        // 🔥 改进的 fallback：找一个至少有相邻通路的墙
        for (int y = BORDER_THICKNESS; y < height - BORDER_THICKNESS; y++) {
            for (int x = BORDER_THICKNESS; x < width - BORDER_THICKNESS; x++) {
                if (maze[y][x] != 0) continue;
                if (isExitDoorAt(x, y)) continue;

                // 检查相邻通路
                if ((y + 1 < height && maze[y + 1][x] == 1) ||
                        (y - 1 >= 0 && maze[y - 1][x] == 1) ||
                        (x - 1 >= 0 && maze[y][x - 1] == 1) ||
                        (x + 1 < width && maze[y][x + 1] == 1)) {
                    return new int[]{x, y};
                }
            }
        }

        return new int[]{BORDER_THICKNESS, BORDER_THICKNESS};
    }

    /* ---------- Enemies ---------- */
    private void generateEnemies() {
        for (int i = 0; i < difficultyConfig.enemyE01PearlCount; i++) {
            int[] p = randomEmptyCell();
            enemies.add(new EnemyE01_CorruptedPearl(p[0], p[1]));
            Logger.debug("创建动画珍珠敌人 #" + (i+1));
        }

        for (int i = 0; i < difficultyConfig.enemyE02CoffeeBeanCount; i++) {
            int[] p = randomEmptyCell();
            enemies.add(new EnemyE02_SmallCoffeeBean(p[0], p[1]));
        }

        for (int i = 0; i < difficultyConfig.enemyE03CaramelCount; i++) {
            int[] p = randomEmptyCell();
            enemies.add(new EnemyE03_CaramelJuggernaut(p[0], p[1]));
        }
//待会更改
        for (int i = 0; i < difficultyConfig.enemyE04ShellCount; i++) {
            int[] p = randomEmptyCell();
            enemies.add(new EnemyE04_CrystallizedCaramelShell(p[0], p[1]));
        }
    }

    /* ---------- Traps ---------- */
    private void generateTraps() {
        for (int i = 0; i < difficultyConfig.trapT01GeyserCount; i++) {
            int[] p = randomEmptyCell();
            traps.add(new TrapT01_Geyser(p[0], p[1], 3f));
        }

        for (int i = 0; i < difficultyConfig.trapT02PearlMineCount; i++) {
            int[] p = randomEmptyCell();
            traps.add(new TrapT02_PearlMine(p[0], p[1], this));
        }

        for (int i = 0; i < difficultyConfig.trapT03TeaShardCount; i++) {
            int[] p = randomEmptyCell();
            traps.add(new TrapT03_TeaShards(p[0], p[1]));
        }

        for (int i = 0; i < difficultyConfig.trapT04MudTileCount; i++) {
            int[] p = randomEmptyCell();
            traps.add(new TrapT04_Mud(p[0], p[1]));
        }
    }

    /* ---------- Hearts ---------- */
    private void generateHearts() {
        int count = 10;
        for (int i = 0; i < count; i++) {
            int[] p = randomEmptyCell();
            hearts.add(new Heart(p[0], p[1]));
        }
    }

    /* ---------- Treasures ---------- */
    /* ---------- Treasures ---------- */
    private void generateTreasures() {
        // 🔥 [Treasure] 智能生成 3 个宝箱
        int targetCount = 3;
        int spawned = 0;
        int attempts = 0;

        while (spawned < targetCount && attempts < 200) {
            attempts++;
            int[] p = randomEmptyCell(); // 获取一个空地坐标
            int tx = p[0];
            int ty = p[1];

            // 1. 检查是否已被占用 (isOccupied 已经包含了玩家、敌人、陷阱和其他宝箱)
            // randomEmptyCell 已经保证不是墙壁，所以只需要检查物体重叠
            if (isOccupied(tx, ty)) continue;

            treasures.add(new Treasure(tx, ty));
            spawned++;
        }
        Logger.debug("Generated " + spawned + " treasures.");
    }

    /* ================= 工具 ================= */
    private int[] randomEmptyCell() {
        int x, y;
        int width = maze[0].length;
        int height = maze.length;

        int attempts = 0;
        do {
            x = random(1, width - 2);
            y = random(1, height - 2);
            attempts++;

            // 防止无限循环
            if (attempts > 500) {
                Logger.warning("randomEmptyCell: Too many attempts, using fallback");
                // 回退：从中心开始搜索
                for (int offset = 0; offset < Math.max(width, height); offset++) {
                    for (int cx = Math.max(1, width/2 - offset); cx <= Math.min(width-2, width/2 + offset); cx++) {
                        for (int cy = Math.max(1, height/2 - offset); cy <= Math.min(height-2, height/2 + offset); cy++) {
                            if (maze[cy][cx] != 0 && !isOccupied(cx, cy)) {
                                Logger.debug("randomEmptyCell fallback: found (" + cx + ", " + cy + ")");
                                return new int[]{cx, cy};
                            }
                        }
                    }
                }
                // 终极回退：返回玩家位置（应该不会到这里）
                return new int[]{player.getX(), player.getY()};
            }
        } while (maze[y][x] == 0 || isOccupied(x, y)); // 🔥 新增 isOccupied 检查

        Logger.debug("randomEmptyCell: found (" + x + ", " + y + ") after " + attempts + " attempts");
        return new int[]{x, y};
    }

    public boolean canPlayerMoveTo(int x, int y) {
        // 1️⃣ 越界
        if (x < 0 || y < 0 || y >= maze.length || x >= maze[0].length) {
            return false;
        }


        // 2️⃣ 检查2x2敌人
        for (Enemy enemy : enemies) {
            if (enemy instanceof EnemyE04_CrystallizedCaramelShell) {
                EnemyE04_CrystallizedCaramelShell shell = (EnemyE04_CrystallizedCaramelShell) enemy;
                if (shell.isActive() && shell.occupiesCell(x, y)) {
                    return false;
                }
            }
        }

        // 2️⃣ 检查是否是门的位置
        for (ExitDoor door : exitDoors) {
            if (door.getX() == x && door.getY() == y) {
                return !door.isLocked();
            }
        }
        // ⭐ 新增检查：移动墙与所有动态障碍物
        for (DynamicObstacle o : obstacles) {
            if (o.getX() == x && o.getY() == y) {
                return false;  // 玩家不能走进移动的墙
            }
        }

        // 3️⃣ 普通墙体
        return maze[y][x] == 1;
    }

    /* ================= Getter ================= */
    public Player getPlayer() { return player; }
    public int[][] getMaze() { return maze; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<Trap> getTraps() { return traps; }
    public List<Heart> getHearts() { return hearts; }
    public List<Treasure> getTreasures() { return treasures; }
    public List<ExitDoor> getExitDoors() { return exitDoors; }
    public Compass getCompass() { return compass; }
    public int getCurrentLevel() { return currentLevel; }
    public List<Key> getKeys() { return keys; }

    // 🔥 新增：获取动画状态
    public boolean isLevelTransitionInProgress() {
        return levelTransitionInProgress;
    }

    /* ================= 输入 ================= */
    public void onMoveInput(int dx, int dy) {
        if (player == null) return;

        // 首先更新玩家的朝向（无论是否能移动）
        player.updateDirection(dx, dy);

        // 然后尝试移动
        int nx = player.getX() + dx;
        int ny = player.getY() + dy;

        if (canPlayerMoveTo(nx, ny)) {
            player.move(dx, dy);
        } else {
            // 即使不能移动，方向也已经更新了
            // 可以在这里播放一个"撞墙"的音效或动画
            Logger.debug("无法移动到 (" + nx + "," + ny + ")，但方向已更新");
        }
    }

    @Override
    public float getMoveDelayMultiplier() {
        return 1.0f;
    }

    public boolean onAbilityInput(int slot) {
        if (levelTransitionInProgress) return false;
        player.useAbility(slot);
        return true;
    }

    public void onInteractInput() {
        if (levelTransitionInProgress) return;

        int px = player.getX();
        int py = player.getY();

        // 宝箱
        for (Treasure t : treasures) {
            if (t.isInteractable() && t.getX() == px && t.getY() == py) {
                t.onInteract(player);
                return;
            }
        }

        // 爱心
        for (Heart h : hearts) {
            if (h.isActive() && h.getX() == px && h.getY() == py) {
                h.onInteract(player);
                return;
            }
        }
    }

    @Override
    public void onMenuInput() {

    }

    private void checkAutoPickup() {
        if (levelTransitionInProgress) return;

        int px = player.getX();
        int py = player.getY();

        // ===== 钥匙 =====
        Iterator<Key> keyIterator = keys.iterator();
        while (keyIterator.hasNext()) {
            Key key = keyIterator.next();
            if (!key.isActive()) continue;

            if (key.getX() == px && key.getY() == py) {
                float effectX = key.getX() * GameConstants.CELL_SIZE;
                float effectY = key.getY() * GameConstants.CELL_SIZE;

                if (key.getTexture() != null) {
                    keyEffectManager.spawnKeyEffect(effectX, effectY, key.getTexture());
                }

                key.onInteract(player);
                keyIterator.remove();
                onKeyCollected();
                break;
            }
        }

        // ===== 爱心 =====
        Iterator<Heart> heartIterator = hearts.iterator();
        while (heartIterator.hasNext()) {
            Heart h = heartIterator.next();
            if (h.isActive() && h.getX() == px && h.getY() == py) {
                h.onInteract(player);
                heartIterator.remove();
            }
        }

        // ===== 宝箱 (Treasure) =====
        Iterator<Treasure> treasureIterator = treasures.iterator();
        while (treasureIterator.hasNext()) {
            Treasure t = treasureIterator.next();

            // 只要玩家踩上去，并且宝箱还没开
            if (t.isInteractable() && t.getX() == px && t.getY() == py) {
                // 触发开箱逻辑 (Player.java 会获得 Buff)
                t.onInteract(player);

                // ⚠️ 注意：宝箱打开后不移除 (remove)，因为它要变成开箱状态留在原地
                // 所以这里不需要 treasureIterator.remove();
            }
        }
    }

    /**
     * Enemy 专用移动判定
     */
    public boolean isEnemyValidMove(int x, int y) {
        // 越界 = 不可走
        if (x < 0 || y < 0 || x >= maze[0].length || y >= maze.length) {
            return false;
        }

        // 墙 = 不可走
        if (maze[y][x] == 0) {
            return false;
        }

        // 🔥 出口门 = 不可走（无论是否解锁）
        for (ExitDoor door : exitDoors) {
            if (door.getX() == x && door.getY() == y) {
                return false;
            }
        }

        // Trap 是否阻挡
        for (var trap : traps) {
            if (trap.getX() == x && trap.getY() == y && !trap.isPassable()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取指定格子上的所有敌人
     */
    public List<Enemy> getEnemiesAt(int x, int y) {
        List<Enemy> result = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy == null) continue;
            if (enemy.isDead()) continue;
            if (enemy.getX() == x && enemy.getY() == y) {
                result.add(enemy);
            }
        }
        return result;
    }

    /**
     * 获取迷宫某一格的值
     */
    public int getMazeCell(int x, int y) {
        if (x < 0 || y < 0) {
            return 0;
        }

        if (y >= maze.length || x >= maze[0].length) {
            return 0;
        }

        return maze[y][x];
    }


    /**
     * 生成敌人子弹 / 投射物
     */
    public void spawnProjectile(EnemyBullet bullet) {
        if (bullet == null) return;
        bullets.add((BobaBullet) bullet);
    }

    public void spawnProjectile(BobaBullet bullet) {
        if (bullet == null) return;
        bullets.add(bullet);
    }

    // GameManager.java
    private BobaBulletManager bobaBulletEffectManager = new BobaBulletManager();
    public BobaBulletManager getBobaBulletEffectManager() {
        return bobaBulletEffectManager;
    }

    private void handlePlayerEnemyCollision() {
        if (levelTransitionInProgress) return;

        Player player = this.player;
        if (player == null || player.isDead()) return;

        for (Enemy enemy : enemies) {
            if (!enemy.isActive() || enemy.isDead()) continue;

            if (enemy.getX() == player.getX() && enemy.getY() == player.getY()) {
                if (player.isDashInvincible()) {
                    continue;
                }
                player.takeDamage(enemy.getAttackDamage());
            }
        }
    }

    private void handleDashHitEnemies() {
        if (levelTransitionInProgress) return;

        Player player = this.player;
        if (player == null) return;
        if (!player.isDashing()) return;

        for (Enemy enemy : enemies) {
            if (!enemy.isActive() || enemy.isDead()) continue;

            if (enemy.getX() == player.getX() && enemy.getY() == player.getY()) {
                enemy.markHitByDash();   // ⭐ E04关键
                enemy.takeDamage(2);
            }
        }
    }

    public KeyEffectManager getKeyEffectManager() {
        return keyEffectManager;
    }
    public PortalEffectManager getPlayerSpawnPortal() {
        return playerSpawnPortal;
    }


    public void dispose() {
        if (keyEffectManager != null) {
            keyEffectManager.dispose();
        }
        // 🔥 清理出口门资源
        for (ExitDoor door : exitDoors) {
            door.dispose();
        }
        // 🔥 [Treasure] 清理宝箱资源
        for (Treasure t : treasures) {
            t.dispose();
        }
    }
    /* ================= [Console] 变量操作 API ================= */

    /**
     * 设置游戏变量 (给控制台调用)
     * 例如: gm.setVariable("speed_mult", 2.0f);
     */
    public void setVariable(String key, float value) {
        if (gameVariables == null) gameVariables = new HashMap<>();
        gameVariables.put(key, value);
        Logger.debug("Console Variable Set: " + key + " = " + value);
    }

    /**
     * 获取游戏变量 (给 Player/Camera 调用)
     * 如果没有设置过，默认返回 1.0
     */
    public float getVariable(String key) {
        if (gameVariables == null) return 1.0f;
        return gameVariables.getOrDefault(key, 1.0f);
    }


    public String getScore() {
        return String.valueOf(player.getScore());
    }

    public PlayerInputHandler getInputHandler() {
        return  inputHandler;
    }
    //给教学用的
    private boolean tutorialMode = false;
    public void setTutorialMode(boolean tutorialMode) {
        this.tutorialMode = tutorialMode;
    }

    public boolean isTutorialMode() {
        return tutorialMode;
    }

    public boolean isPlayerDead() {
        return player != null && player.isDead();
    }

    public boolean isObstacleValidMove(int nx, int ny) {

        // ① 越界直接不行
        if (nx < 0 || ny < 0 ||
                ny >= maze.length ||
                nx >= maze[0].length) {
            return false;
        }

        // ② 静态迷宫墙不能进
        if (maze[ny][nx] == 0) {
            return false;
        }

        // ③ 出口门：障碍物不能进（防止堵死关卡）
        for (ExitDoor door : exitDoors) {
            if (door.getX() == nx && door.getY() == ny) {
                return false;
            }
        }

        // ④ 敌人不能被占格（包括 E04）
        for (Enemy e : enemies) {
            if (e.isActive() &&
                    e.getX() == nx &&
                    e.getY() == ny) {
                return false;
            }
        }

        // ⑤ 其他动态障碍物不能重叠
        for (DynamicObstacle o : obstacles) {
            if (o.getX() == nx && o.getY() == ny) {
                return false;
            }
        }

        /*
         * ⚠️ 注意：
         * 玩家不在这里拦截
         *
         * 因为：
         * - 玩家是否被“推走”
         * - 是否能让路
         * - 是否受伤 / 硬直
         *
         * 这些都属于【交互逻辑】
         * 而不是【占格合法性】
         */

        return true;
    }

    public List<DynamicObstacle> getObstacles() { return obstacles; }
    public CatFollower getCat() {
        return cat;
    }

}