package de.tum.cit.fop.maze.game;

import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.effects.Enemy.boba.BobaBulletManager;
import de.tum.cit.fop.maze.effects.environment.items.ItemEffectManager;
import de.tum.cit.fop.maze.effects.environment.items.traps.TrapEffectManager;
import de.tum.cit.fop.maze.effects.environment.portal.PortalEffectManager;
import de.tum.cit.fop.maze.effects.fog.FogSystem;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffectManager; // 🔥 [Fix] Import
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.badlogic.gdx.math.MathUtils.random;
import static de.tum.cit.fop.maze.maze.MazeGenerator.BORDER_THICKNESS;

public class GameManager implements PlayerInputHandler.InputHandlerCallback {
    private final DifficultyConfig difficultyConfig;
    private float debugTimer = 0f;

    public DifficultyConfig getDifficultyConfig() { return difficultyConfig; }

    private int[][] maze;
    private List<Player> players = new ArrayList<>();
    private boolean twoPlayerMode = true;
    private Player player;

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Trap> traps = new ArrayList<>();
    private final List<Heart> hearts = new ArrayList<>();
    private final List<Treasure> treasures = new ArrayList<>();
    private final List<ExitDoor> exitDoors = new ArrayList<>();
    private final Array<BobaBullet> bullets = new Array<>();
    private List<DynamicObstacle> obstacles = new ArrayList<>();

    private int mouseTileX = -1;
    private int mouseTileY = -1;

    // Managers & Systems
    private FogSystem fogSystem;
    private Compass compass;
    private MazeGenerator generator = new MazeGenerator();
    private PlayerInputHandler inputHandler;

    // 特效管理器
    private ItemEffectManager itemEffectManager;
    private TrapEffectManager trapEffectManager;
    // 🔥 [Fix] 新增战斗特效管理器
    private CombatEffectManager combatEffectManager;
    // 🔥 [Correction] 补回遗漏的 BobaBulletManager 声明
    private BobaBulletManager bobaBulletEffectManager = new BobaBulletManager();

    private CatFollower cat;
    private Map<String, Float> gameVariables;

    private final List<Key> keys = new ArrayList<>();
    private boolean keyProcessed = false;

    private boolean pendingReset = false;
    private boolean justReset = false;

    private boolean levelTransitionInProgress = false;
    private ExitDoor currentExitDoor = null;
    private float levelTransitionTimer = 0f;
    private static final float LEVEL_TRANSITION_DELAY = 0.5f;

    private int currentLevel = 1;
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
        players.clear();

        int[] spawn1 = randomEmptyCell();
        Player p1 = new Player(
                spawn1[0],spawn1[1],this,Player.PlayerIndex.P1
        );players.add(p1);

        if (twoPlayerMode) {
            int[] spawn2 = findNearbySpawn(p1);

            Player p2 = new Player(
                    spawn2[0],
                    spawn2[1],
                    this,
                    Player.PlayerIndex.P2
            );
            players.add(p2);
        }

        // 🔥 关键：同步旧 player 引用
        syncSinglePlayerRef();

        cat = null;
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

        // ✅ 初始化特效管理器
        itemEffectManager = new ItemEffectManager();
        trapEffectManager = new TrapEffectManager();
        combatEffectManager = new CombatEffectManager(); // 🔥 [Fix] Init

        // 🔥 重置动画状态
        levelTransitionInProgress = false;
        currentExitDoor = null;
        levelTransitionTimer = 0f;

        Logger.gameEvent("Game reset complete");
    }

    private int[] findNearbySpawn(Player p1) {
        int px = p1.getX();
        int py = p1.getY();
        // 8 个方向（顺时针）
        int[][] offsets = { {-1, -1}, {0, -1}, {1, -1}, {-1,  0}, {1,  0}, {-1,  1}, {0,  1}, {1,  1} };
        for (int[] o : offsets) {
            int nx = px + o[0];
            int ny = py + o[1];
            if (canPlayerMoveTo(nx, ny) && !isOccupied(nx, ny)) {
                return new int[]{nx, ny};
            }
        }
        return randomEmptyCell();
    }

    public void debugEnemiesAndBullets() {
        // ... debug info ...
    }

    public void update(float delta) {
        inputHandler.update(delta, this, Player.PlayerIndex.P1);

        if (twoPlayerMode) {
            inputHandler.update(delta, this, Player.PlayerIndex.P2);
        }

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
                currentExitDoor.update(delta, this);
            }
            levelTransitionTimer += delta;
            if (levelTransitionTimer >= LEVEL_TRANSITION_DELAY) {
                levelTransitionInProgress = false;
                levelTransitionTimer = 0f;
                currentExitDoor = null;
                nextLevel();
            }
            return;
        }

        // 正常游戏逻辑
        for (Player p : players) {
            if (!p.isDead()) {
                p.update(delta);
            }
        }

        boolean fogOn = fogSystem != null && fogSystem.isActive();
        if (difficultyConfig.difficulty == Difficulty.HARD) {
            if (fogOn) {
                if (cat == null) cat = new CatFollower(player, this);
                cat.update(delta);
            } else {
                cat = null;
            }
        } else {
            cat = null;
        }

        if (fogSystem != null) {
            fogSystem.update(delta);
        }

        // ===== 🔥 更新陷阱 =====
        for (Trap trap : traps) {
            if (trap.isActive()) {
                trap.update(delta);
            }
        }

        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy e = enemyIterator.next();
            e.update(delta, this);

            if (e.isDead() || !e.isActive()) {
                enemyIterator.remove();
            }
        }

        for (ExitDoor door : exitDoors) {
            door.update(delta, this);
        }

        checkExitReached();
        updateCompass();
        updateBullets(delta);
        for (DynamicObstacle o : obstacles) {
            o.update(delta, this);
        }

        // 正常调用 bobaBulletEffectManager
        bobaBulletEffectManager.addBullets(bullets);
        bobaBulletEffectManager.update(delta);

        handlePlayerEnemyCollision();
        handleDashHitEnemies();
        checkAutoPickup();

        // ✅ 更新特效管理器
        if (itemEffectManager != null) itemEffectManager.update(delta);
        if (trapEffectManager != null) trapEffectManager.update(delta);
        if (combatEffectManager != null) combatEffectManager.update(delta); // 🔥 [Fix] Update

        handlePlayerTrapInteraction();
        handleKeyLogic();

        if (pendingReset) {
            pendingReset = false;
            resetGame();
            justReset = true;
        }

        debugTimer += delta;
        if (debugTimer >= 2.0f) {
            debugEnemiesAndBullets();
            debugTimer = 0f;
        }
    }

    private void handlePlayerTrapInteraction() {
        if (levelTransitionInProgress || player == null || player.isDead()) return;
        int px = player.getX();
        int py = player.getY();

        Iterator<Trap> it = traps.iterator();
        while (it.hasNext()) {
            Trap trap = it.next();
            if (!trap.isActive()) continue;

            if (trap.getX() == px && trap.getY() == py) {
                trap.onPlayerStep(player);
                float effectX = (trap.getX() + 0.5f) * GameConstants.CELL_SIZE;
                float effectY = (trap.getY() + 0.5f) * GameConstants.CELL_SIZE;

                if (trapEffectManager != null) {
                    if (trap instanceof TrapT01_Geyser) trapEffectManager.spawnGeyser(effectX, effectY);
                    else if (trap instanceof TrapT02_PearlMine) trapEffectManager.spawnPearlMine(effectX, effectY);
                    else if (trap instanceof TrapT03_TeaShards) trapEffectManager.spawnTeaShards(effectX, effectY);
                    else if (trap instanceof TrapT04_Mud) trapEffectManager.spawnMudTrap(effectX, effectY);
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
            float dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                nearest = door;
            }
        }
        compass.update(nearest);
    }

    private void checkExitReached() {
        Player p = player;
        for (ExitDoor door : exitDoors) {
            if (!door.isLocked() && door.isActive() && door.getX() == p.getX() && door.getY() == p.getY() && !levelTransitionInProgress) {
                door.onPlayerStep(p);
                startLevelTransition(door);
                return;
            }
        }
    }

    private void startLevelTransition(ExitDoor door) {
        levelTransitionInProgress = true;
        currentExitDoor = door;
        levelTransitionTimer = 0f;
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
        obstacles.clear();
        int sx, sy, ex, ey;
        do {
            sx = random.nextInt(difficultyConfig.mazeWidth - 10);
            sy = random.nextInt(difficultyConfig.mazeHeight);
            ex = sx + 5;
            ey = sy;
        } while (!isWalkableLine(sx, sy, ex, ey));
        MovingWall wall = new MovingWall(sx, sy, ex, ey, MovingWall.WallType.SINGLE);
        obstacles.add(wall);
        Logger.debug("MovingWall created: (" + sx + "," + sy + ") -> (" + ex + "," + ey + ")");
    }

    private boolean isWalkableLine(int sx, int sy, int ex, int ey) {
        if (sy != ey) return false;
        for (int x = sx; x <= ex; x++) {
            if (maze[sy][x] != 1) return false;
        }
        return true;
    }

    private void generateKeys() {
        int keyCount = difficultyConfig.keyCount;
        for (int i = 0; i < keyCount; i++) {
            int x, y;
            do {
                x = random.nextInt(difficultyConfig.mazeWidth);
                y = random.nextInt(difficultyConfig.mazeHeight);
            } while (getMazeCell(x, y) != 1 || isOccupied(x, y) || isExitDoorAt(x, y));
            keys.add(new Key(x, y, this));
        }
    }

    private boolean isOccupied(int x, int y) {
        if (player != null && player.getX() == x && player.getY() == y) return true;
        for (Enemy e : enemies) if (e.isActive() && e.getX() == x && e.getY() == y) return true;
        for (Treasure t : treasures) if (t.isActive() && t.getX() == x && t.getY() == y) return true;
        for (Heart h : hearts) if (h.isActive() && h.getX() == x && h.getY() == y) return true;
        for (Key k : keys) if (k.isActive() && k.getX() == x && k.getY() == y) return true;
        for (Trap trap : traps) if (trap.isActive() && trap.getX() == x && trap.getY() == y) return true;
        return false;
    }

    private void generateExitDoors() {
        exitDoors.clear();
        for (int i = 0; i < difficultyConfig.exitCount; i++) {
            int[] p = randomWallCell();
            int attempts = 0;
            while (!isValidDoorPosition(p[0], p[1]) && attempts < 50) {
                p = randomWallCell();
                attempts++;
            }
            ExitDoor.DoorDirection direction = determineDoorDirection(p[0], p[1]);
            ExitDoor door = new ExitDoor(p[0], p[1], direction);
            exitDoors.add(door);
            Logger.debug("ExitDoor created at (" + p[0] + ", " + p[1] + ") facing " + direction);
        }
    }

    private ExitDoor.DoorDirection determineDoorDirection(int x, int y) {
        int[][] maze = getMaze();
        int width = maze[0].length;
        int height = maze.length;
        boolean up = y + 1 < height && maze[y + 1][x] == 1;
        boolean down = y - 1 >= 0 && maze[y - 1][x] == 1;
        boolean left = x - 1 >= 0 && maze[y][x - 1] == 1;
        boolean right = x + 1 < width && maze[y][x + 1] == 1;

        List<ExitDoor.DoorDirection> possibleDirections = new ArrayList<>();
        if (up) possibleDirections.add(ExitDoor.DoorDirection.UP);
        if (down) possibleDirections.add(ExitDoor.DoorDirection.DOWN);
        if (left) possibleDirections.add(ExitDoor.DoorDirection.LEFT);
        if (right) possibleDirections.add(ExitDoor.DoorDirection.RIGHT);

        if (!possibleDirections.isEmpty()) {
            return possibleDirections.get(random.nextInt(possibleDirections.size()));
        }
        if (y >= height - 3) return ExitDoor.DoorDirection.DOWN;
        if (y <= 2) return ExitDoor.DoorDirection.UP;
        if (x >= width - 3) return ExitDoor.DoorDirection.LEFT;
        if (x <= 2) return ExitDoor.DoorDirection.RIGHT;
        return ExitDoor.DoorDirection.UP;
    }

    private boolean isValidDoorPosition(int x, int y) {
        int[][] maze = getMaze();
        int width = maze[0].length;
        int height = maze.length;
        if (maze[y][x] != 0) return false;
        boolean hasAdjacentPath = false;
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
            if (maze[y][x] != 0) continue;
            if (isExitDoorAt(x, y)) continue;

            boolean hasAdjacentPath = false;
            if (y + 1 < height && maze[y + 1][x] == 1) hasAdjacentPath = true;
            if (y - 1 >= 0 && maze[y - 1][x] == 1) hasAdjacentPath = true;
            if (x - 1 >= 0 && maze[y][x - 1] == 1) hasAdjacentPath = true;
            if (x + 1 < width && maze[y][x + 1] == 1) hasAdjacentPath = true;

            if (!hasAdjacentPath) continue;
            return new int[]{x, y};
        }
        // Fallback
        for (int y = BORDER_THICKNESS; y < height - BORDER_THICKNESS; y++) {
            for (int x = BORDER_THICKNESS; x < width - BORDER_THICKNESS; x++) {
                if (maze[y][x] != 0) continue;
                if (isExitDoorAt(x, y)) continue;
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

    private void generateEnemies() {
        for (int i = 0; i < difficultyConfig.enemyE01PearlCount; i++) {
            int[] p = randomEmptyCell();
            enemies.add(new EnemyE01_CorruptedPearl(p[0], p[1]));
        }
        for (int i = 0; i < difficultyConfig.enemyE02CoffeeBeanCount; i++) {
            int[] p = randomEmptyCell();
            enemies.add(new EnemyE02_SmallCoffeeBean(p[0], p[1]));
        }
        for (int i = 0; i < difficultyConfig.enemyE03CaramelCount; i++) {
            int[] p = randomEmptyCell();
            enemies.add(new EnemyE03_CaramelJuggernaut(p[0], p[1]));
        }
        for (int i = 0; i < difficultyConfig.enemyE04ShellCount; i++) {
            int[] p = randomEmptyCell();
            enemies.add(new EnemyE04_CrystallizedCaramelShell(p[0], p[1]));
        }
    }

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

    private void generateHearts() {
        int count = 10;
        for (int i = 0; i < count; i++) {
            int[] p = randomEmptyCell();
            hearts.add(new Heart(p[0], p[1]));
        }
    }

    private void generateTreasures() {
        int targetCount = 3;
        int spawned = 0;
        int attempts = 0;
        while (spawned < targetCount && attempts < 200) {
            attempts++;
            int[] p = randomEmptyCell();
            int tx = p[0];
            int ty = p[1];
            if (isOccupied(tx, ty)) continue;
            treasures.add(new Treasure(tx, ty));
            spawned++;
        }
    }

    private int[] randomEmptyCell() {
        int x, y;
        int width = maze[0].length;
        int height = maze.length;
        int attempts = 0;
        do {
            x = random(1, width - 2);
            y = random(1, height - 2);
            attempts++;
            if (attempts > 500) {
                Logger.warning("randomEmptyCell: Too many attempts, using fallback");
                return new int[]{player.getX(), player.getY()};
            }
        } while (maze[y][x] == 0 || isOccupied(x, y));
        return new int[]{x, y};
    }

    public boolean canPlayerMoveTo(int x, int y) {
        if (x < 0 || y < 0 || y >= maze.length || x >= maze[0].length) return false;
        for (Enemy enemy : enemies) {
            if (enemy instanceof EnemyE04_CrystallizedCaramelShell) {
                EnemyE04_CrystallizedCaramelShell shell = (EnemyE04_CrystallizedCaramelShell) enemy;
                if (shell.isActive() && shell.occupiesCell(x, y)) return false;
            }
        }
        for (ExitDoor door : exitDoors) {
            if (door.getX() == x && door.getY() == y) return !door.isLocked();
        }
        for (DynamicObstacle o : obstacles) {
            if (o.getX() == x && o.getY() == y) return false;
        }
        return maze[y][x] == 1;
    }

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
    public boolean isLevelTransitionInProgress() { return levelTransitionInProgress; }

    public void onMoveInput(Player.PlayerIndex index, int dx, int dy) {
        Player p = getPlayerByIndex(index);
        if (p == null) return;
        p.updateDirection(dx, dy);
        int nx = p.getX() + dx;
        int ny = p.getY() + dy;
        if (canPlayerMoveTo(nx, ny)) {
            p.move(dx, dy);
        }
    }

    private Player getPlayerByIndex(Player.PlayerIndex index) {
        for (Player p : players) {
            if (p.getPlayerIndex() == index) return p;
        }
        return null;
    }

    @Override
    public float getMoveDelayMultiplier() { return 1.0f; }

    @Override
    public boolean onAbilityInput(Player.PlayerIndex index, int slot) {
        if (levelTransitionInProgress) return false;
        Player p = getPlayerByIndex(index);
        if (p == null || p.isDead()) return false;
        p.useAbility(slot);
        return true;
    }

    @Override
    public void onInteractInput(Player.PlayerIndex index) {
        if (levelTransitionInProgress) return;
        Player p = getPlayerByIndex(index);
        if (p == null || p.isDead()) return;
        int px = p.getX();
        int py = p.getY();
        for (Treasure t : treasures) {
            if (t.isInteractable() && t.getX() == px && t.getY() == py) {
                t.onInteract(p);
                return;
            }
        }
        for (Heart h : hearts) {
            if (h.isActive() && h.getX() == px && h.getY() == py) {
                h.onInteract(p);
                return;
            }
        }
    }

    @Override
    public void onMenuInput() {}

    private void checkAutoPickup() {
        if (levelTransitionInProgress) return;
        int px = player.getX();
        int py = player.getY();

        Iterator<Key> keyIterator = keys.iterator();
        while (keyIterator.hasNext()) {
            Key key = keyIterator.next();
            if (!key.isActive()) continue;
            if (key.getX() == px && key.getY() == py) {
                float effectX = key.getX() * GameConstants.CELL_SIZE;
                float effectY = key.getY() * GameConstants.CELL_SIZE;
                if (key.getTexture() != null && itemEffectManager != null) {
                    itemEffectManager.spawnKeyEffect(effectX, effectY, key.getTexture());
                }
                key.onInteract(player);
                keyIterator.remove();
                onKeyCollected();
                break;
            }
        }

        Iterator<Heart> heartIterator = hearts.iterator();
        while (heartIterator.hasNext()) {
            Heart h = heartIterator.next();
            if (h.isActive() && h.getX() == px && h.getY() == py) {
                float effectX = (h.getX() + 0.5f) * GameConstants.CELL_SIZE;
                float effectY = (h.getY() + 0.5f) * GameConstants.CELL_SIZE;
                if (itemEffectManager != null) {
                    itemEffectManager.spawnHeart(effectX, effectY);
                }
                h.onInteract(player);
                heartIterator.remove();
            }
        }

        Iterator<Treasure> treasureIterator = treasures.iterator();
        while (treasureIterator.hasNext()) {
            Treasure t = treasureIterator.next();
            if (t.isInteractable() && t.getX() == px && t.getY() == py) {
                float effectX = (t.getX() + 0.5f) * GameConstants.CELL_SIZE;
                float effectY = (t.getY() + 0.5f) * GameConstants.CELL_SIZE;
                if (itemEffectManager != null) {
                    itemEffectManager.spawnTreasure(effectX, effectY);
                }
                t.onInteract(player);
            }
        }
    }

    public boolean isEnemyValidMove(int x, int y) {
        if (x < 0 || y < 0 || x >= maze[0].length || y >= maze.length) return false;
        if (maze[y][x] == 0) return false;
        for (ExitDoor door : exitDoors) if (door.getX() == x && door.getY() == y) return false;
        for (var trap : traps) if (trap.getX() == x && trap.getY() == y && !trap.isPassable()) return false;
        return true;
    }

    public List<Enemy> getEnemiesAt(int x, int y) {
        List<Enemy> result = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy == null || enemy.isDead()) continue;
            if (enemy.getX() == x && enemy.getY() == y) result.add(enemy);
        }
        return result;
    }

    public int getMazeCell(int x, int y) {
        if (x < 0 || y < 0 || y >= maze.length || x >= maze[0].length) return 0;
        return maze[y][x];
    }

    public void spawnProjectile(EnemyBullet bullet) {
        if (bullet == null) return;
        if (bullet instanceof BobaBullet) {
            bullets.add((BobaBullet) bullet);
        } else {
            Logger.debug("Non-Boba bullet spawned: " + bullet.getClass().getSimpleName());
        }
    }

    public void spawnProjectile(BobaBullet bullet) {
        if (bullet == null) return;
        bullets.add(bullet);
    }

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
                if (player.isDashInvincible()) continue;
                player.takeDamage(enemy.getAttackDamage());
            }
        }
    }

    private void handleDashHitEnemies() {
        if (levelTransitionInProgress) return;
        Player player = this.player;
        if (player == null || !player.isDashing()) return;
        for (Enemy enemy : enemies) {
            if (!enemy.isActive() || enemy.isDead()) continue;
            if (enemy.getX() == player.getX() && enemy.getY() == player.getY()) {
                enemy.markHitByDash();
                enemy.takeDamage(2);
            }
        }
    }

    public PortalEffectManager getPlayerSpawnPortal() {
        return playerSpawnPortal;
    }

    public ItemEffectManager getItemEffectManager() {
        return itemEffectManager;
    }

    public TrapEffectManager getTrapEffectManager() {
        return trapEffectManager;
    }

    // 🔥 [Fix] 新增 Getter
    public CombatEffectManager getCombatEffectManager() {
        return combatEffectManager;
    }

    public void dispose() {
        if (itemEffectManager != null) {
            itemEffectManager.dispose();
        }
        if (trapEffectManager != null) {
            trapEffectManager.dispose();
        }
        if (combatEffectManager != null) {
            combatEffectManager.dispose(); // 🔥 [Fix] Dispose
        }
        if (players != null) {
            for (Player p : players) p.dispose(); // 🔥 [Fix] Dispose player trails
        }

        for (ExitDoor door : exitDoors) door.dispose();
        for (Treasure t : treasures) t.dispose();
        bobaBulletEffectManager.dispose();
        playerSpawnPortal.dispose();
    }

    public void setVariable(String key, float value) {
        if (gameVariables == null) gameVariables = new HashMap<>();
        gameVariables.put(key, value);
        Logger.debug("Console Variable Set: " + key + " = " + value);
    }
    public float getVariable(String key) {
        if (gameVariables == null) return 1.0f;
        return gameVariables.getOrDefault(key, 1.0f);
    }
    public String getScore() { return String.valueOf(player.getScore()); }
    public PlayerInputHandler getInputHandler() { return  inputHandler; }
    public boolean isPlayerDead() { return player != null && player.isDead(); }
    public boolean isObstacleValidMove(int nx, int ny) {
        if (nx < 0 || ny < 0 || ny >= maze.length || nx >= maze[0].length) return false;
        if (maze[ny][nx] == 0) return false;
        for (ExitDoor door : exitDoors) if (door.getX() == nx && door.getY() == ny) return false;
        for (Enemy e : enemies) if (e.isActive() && e.getX() == nx && e.getY() == ny) return false;
        for (DynamicObstacle o : obstacles) if (o.getX() == nx && o.getY() == ny) return false;
        return true;
    }
    public List<DynamicObstacle> getObstacles() { return obstacles; }
    public CatFollower getCat() { return cat; }
    public void setTwoPlayerMode(boolean enabled) { this.twoPlayerMode = enabled; }
    private void syncSinglePlayerRef() {
        if (!players.isEmpty()) player = players.get(0); else player = null;
    }
    public boolean isTwoPlayerMode() { return twoPlayerMode; }
    public List<Player> getPlayers() { return players; }
    public void setMouseTargetTile(int x, int y) { this.mouseTileX = x; this.mouseTileY = y; }
    public int getMouseTileX() { return mouseTileX; }
    public int getMouseTileY() { return mouseTileY; }
}