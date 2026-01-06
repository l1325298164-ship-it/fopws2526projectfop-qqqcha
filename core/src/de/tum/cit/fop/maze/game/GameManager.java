package de.tum.cit.fop.maze.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffectManager;
import de.tum.cit.fop.maze.effects.environment.items.ItemEffectManager;
import de.tum.cit.fop.maze.effects.environment.items.key.KeyEffectManager;
import de.tum.cit.fop.maze.effects.environment.items.traps.TrapEffectManager;
import de.tum.cit.fop.maze.effects.environment.portal.PortalEffectManager;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.enemy.*;
import de.tum.cit.fop.maze.entities.enemy.EnemyBoba.BobaBullet;
import de.tum.cit.fop.maze.entities.enemy.EnemyBoba.EnemyCorruptedBoba;
import de.tum.cit.fop.maze.entities.trap.*;
import de.tum.cit.fop.maze.maze.MazeGenerator;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.effects.Enemy.boba.BobaBulletManager;
import de.tum.cit.fop.maze.utils.LeaderboardManager;
import de.tum.cit.fop.maze.utils.SaveManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.badlogic.gdx.math.MathUtils.random;
import static de.tum.cit.fop.maze.maze.MazeGenerator.BORDER_THICKNESS;

public class GameManager {
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

    private Compass compass;
    private MazeGenerator generator = new MazeGenerator();

    // ===== 特效管理器 =====
    private KeyEffectManager keyEffectManager;
    private ItemEffectManager itemEffectManager;
    private TrapEffectManager trapEffectManager;
    private CombatEffectManager combatEffectManager;
    private BobaBulletManager bobaBulletEffectManager = new BobaBulletManager();

    // ===== Keys =====
    private final List<Key> keys = new ArrayList<>();
    private boolean keyProcessed = false;

    // ===== Reset Control =====
    private boolean pendingReset = false;
    private boolean gameOverProcessed = false;

    // 🔥 动画状态管理
    private boolean levelTransitionInProgress = false;
    private ExitDoor currentExitDoor = null;
    private float levelTransitionTimer = 0f;
    private static final float LEVEL_TRANSITION_DELAY = 0.5f;

    private int currentLevel = 1;
    private PortalEffectManager playerSpawnPortal;

    // ==========================================
    // 🔥 [Phase 1 & 2] 统计与数据
    // ==========================================
    private int sessionDamageTaken = 0; // 本局受伤次数
    private GameSaveData careerStats;   // 生涯数据缓存

    // 🔥 让 VictoryScreen 能访问
    public GameSaveData victoryData = null;

    public boolean isGameWon() {
        return victoryData != null;
    }

    /* ================= 生命周期 ================= */
    public GameManager(DifficultyConfig difficultyConfig) {
        if (difficultyConfig == null) {
            throw new IllegalArgumentException("difficultyConfig must not be null");
        }
        this.difficultyConfig = difficultyConfig;

        // 初始化生涯数据
        this.careerStats = new GameSaveData();

        resetGame();
    }

    private void resetGame() {
        maze = generator.generateMaze();

        enemies.clear();
        traps.clear();
        hearts.clear();
        treasures.clear();
        for (ExitDoor door : exitDoors) {
            if (door != null) door.resetDoor();
        }
        keys.clear();

        int[] spawn = randomEmptyCell();

        if (player == null) {
            player = new Player(spawn[0], spawn[1], this);
        } else {
            player.reset();
            player.setPosition(spawn[0], spawn[1]);
        }

        // 玩家出生特效
        float px = player.getX() * GameConstants.CELL_SIZE;
        float py = player.getY() * GameConstants.CELL_SIZE;
        playerSpawnPortal = new PortalEffectManager(PortalEffectManager.PortalOwner.PLAYER);
        playerSpawnPortal.startPlayerSpawnEffect(px, py);

        generateLevel();

        compass = new Compass(player);
        bullets.clear();
        bobaBulletEffectManager.clearAllBullets(false);

        // 初始化所有特效管理器
        if (keyEffectManager != null) keyEffectManager.dispose();
        keyEffectManager = new KeyEffectManager();

        if (itemEffectManager != null) itemEffectManager.dispose();
        itemEffectManager = new ItemEffectManager();

        if (trapEffectManager != null) trapEffectManager.dispose();
        trapEffectManager = new TrapEffectManager();

        if (combatEffectManager != null) combatEffectManager.dispose();
        combatEffectManager = new CombatEffectManager();

        levelTransitionInProgress = false;
        currentExitDoor = null;
        levelTransitionTimer = 0f;
        gameOverProcessed = false;
        victoryData = null;

        // 重置局内统计
        sessionDamageTaken = 0;

        Logger.gameEvent("Game reset complete");
    }

    public void loadFromSave(GameSaveData data) {
        if (data == null) return;
        this.currentLevel = data.currentLevel;

        // 同步生涯数据
        this.careerStats = data;

        resetGame();
        if (player != null) {
            player.setScore(data.score);
            player.setHealthStatus(data.lives, data.maxLives);
            player.setMana(data.mana);
            player.setHasKey(data.hasKey);
            player.setBuffs(data.buffAttack, data.buffRegen, data.buffManaEfficiency);
            if (data.hasKey) unlockAllExitDoors();
        }
        Logger.gameEvent("Game Loaded from Save: Level " + currentLevel + ", Score: " + data.score);
    }

    // ==========================================
    // 🔥 [Phase 1 & 4] 扣分系统 + 飘字
    // ==========================================
    public void deductScore(int amount, String reason) {
        if (player == null) return;

        int oldScore = player.getScore();
        int newScore = Math.max(0, oldScore - amount); // 确保不为负

        player.setScore(newScore);

        // 记录受伤
        if (amount > 0) {
            sessionDamageTaken++;

            // 🔥 [Phase 4] 飘红字特效
            if (combatEffectManager != null) {
                float px = player.getX() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f;
                float py = player.getY() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE;
                combatEffectManager.spawnFloatingText(px, py, "-" + amount, Color.RED);
            }
        }

        Logger.info("Score deducted: " + amount + " (" + reason + ") | Current: " + newScore);
    }

    public GameSaveData getCareerStats() {
        return careerStats;
    }

    // ==========================================
    // 🔥 [Phase 3] 成就系统核心逻辑
    // ==========================================

    public void unlockAchievement(String id, String name) {
        GameSaveData stats = getCareerStats();

        if (stats.unlockedAchievements.containsKey(id) && stats.unlockedAchievements.get(id)) {
            return;
        }

        stats.unlockedAchievements.put(id, true);

        // 1. 屏幕顶部弹窗
        if (player != null) {
            player.showNotification("ACHIEVEMENT: " + name);
        }

        // 2. 立即保存
        SaveManager.saveGame(this);

        Logger.info(">>> ACHIEVEMENT UNLOCKED: " + name + " (" + id + ") <<<");
    }

    private void checkCombatAchievements() {
        GameSaveData stats = getCareerStats();

        if (stats.totalKills_E01 >= 60) unlockAchievement("ACH_04", "Scrap Filtering");
        if (stats.totalKills_E02 >= 40) unlockAchievement("ACH_05", "Grinding Time");
        if (stats.totalKills_E03 >= 50) unlockAchievement("ACH_06", "Melting Sugar");
        if (stats.totalKills_E04 >= 50) unlockAchievement("ACH_07", "Shell Breaker");
        if (stats.totalKills_Global >= 200) unlockAchievement("ACH_08", "Order Overload");
    }

    private void checkProgressionAchievements() {
        if (currentLevel > 1) unlockAchievement("ACH_02", "First Order Served");
    }

    public void unlockPVAchievement() {
        unlockAchievement("ACH_01", "Recipe Memorized");
    }

    // ==========================================
    // 🔥 [Phase 2, 3, 4] 埋点：杀怪、计分、飘字
    // ==========================================
    private void handleEnemyDeath(Enemy e) {
        if (player == null) return;

        GameSaveData stats = getCareerStats();
        int scoreToAdd = 0;

        // 计分规则
        if (e instanceof EnemyE01_CorruptedPearl) {
            scoreToAdd = 150;
            stats.totalKills_E01++;
        } else if (e instanceof EnemyE02_SmallCoffeeBean) {
            scoreToAdd = 100;
            stats.totalKills_E02++;
        } else if (e instanceof EnemyE03_CaramelJuggernaut) {
            scoreToAdd = 600;
            stats.totalKills_E03++;
        }
//        else if (e instanceof EnemyE04_CrystallizedCaramelShell) {
//            scoreToAdd = 600;
//            stats.totalKills_E04++;
//        }
        else {
            scoreToAdd = 100;
        }

        player.addScore(scoreToAdd);
        stats.totalKills_Global++;

        // 🔥 [Phase 4] 飘金字特效 (在怪物尸体上方)
        if (combatEffectManager != null) {
            float ex = e.getX() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f;
            float ey = e.getY() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE;
            combatEffectManager.spawnFloatingText(ex, ey, "+" + scoreToAdd, Color.GOLD);
        }

        Logger.info("Enemy Defeated: " + e.getClass().getSimpleName() + " | Total Kills: " + stats.totalKills_Global);

        // 检查成就
        checkCombatAchievements();
    }

    private void applyDamageWithPenalty(int damage, int scorePenalty, String sourceName) {
        if (player.isInvincible() || player.isDashInvincible()) return;

        player.takeDamage(damage);
        deductScore(scorePenalty, sourceName);
        sessionDamageTaken++;
    }

    /* ================= 游戏循环与逻辑 ================= */

    public void update(float delta) {
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

        if (levelTransitionInProgress) {
            if (currentExitDoor != null) currentExitDoor.update(delta, this);
            levelTransitionTimer += delta;
            if (levelTransitionTimer >= LEVEL_TRANSITION_DELAY) {
                levelTransitionInProgress = false;
                levelTransitionTimer = 0f;
                currentExitDoor = null;
                nextLevel();
            }
            return;
        }

        player.update(delta);

        if (player.isDead() && !gameOverProcessed) {
            handleGameOver(false);
        }

        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy e = enemyIterator.next();
            e.update(delta, this);

            if (e.isDead() || !e.isActive()) {
                if (e.isDead()) {
                    handleEnemyDeath(e);
                }
                enemyIterator.remove();
            }
        }

        for (ExitDoor door : exitDoors) door.update(delta, this);
        for (Trap trap : traps) trap.update(delta, this);

        checkExitReached();
        updateCompass();
        updateBullets(delta);

        bobaBulletEffectManager.addBullets(bullets);
        bobaBulletEffectManager.update(delta);

        handlePlayerEnemyCollision();
        handleDashHitEnemies();
        checkAutoPickup();

        if (keyEffectManager != null) keyEffectManager.update(delta);
        if (itemEffectManager != null) itemEffectManager.update(delta);
        if (trapEffectManager != null) trapEffectManager.update(delta);
        if (combatEffectManager != null) combatEffectManager.update(delta);

        handleKeyLogic();

        if (pendingReset) {
            pendingReset = false;
            resetGame();
        }
    }

    private void handleGameOver(boolean victory) {
        gameOverProcessed = true;

        // 准备结算数据
        GameSaveData finalData = new GameSaveData();
        finalData.score = player.getScore();
        finalData.lives = player.getLives();
        finalData.unlockedAchievements = getCareerStats().unlockedAchievements;

        Logger.gameEvent(victory ? "Game Completed!" : "Player Died!");

        if (victory) {
            // 🎉 胜利：通知 GameScreen 跳转
            this.victoryData = finalData;
        } else {
            // 💀 失败：简单记录
            LeaderboardManager leaderboard = new LeaderboardManager();
            if (leaderboard.isHighScore(finalData.score)) {
                leaderboard.addScore("Player-Died", finalData.score);
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
        Logger.gameEvent("Level transition started");
    }

    public void nextLevel() {
        currentLevel++;

        checkProgressionAchievements();

        if (currentLevel > GameConstants.MAX_LEVELS) {
            Logger.gameEvent("Game completed!");
            handleGameOver(true);
            return;
        }

        SaveManager.saveGame(this);
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
            if (door.isLocked()) door.unlock();
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
            if (door.getX() == x && door.getY() == y) return true;
        }
        return false;
    }

    private void updateBullets(float delta) {
        for (int i = bullets.size - 1; i >= 0; i--) {
            BobaBullet bullet = bullets.get(i);
            bullet.update(delta, this);
            if (!bullet.isActive()) bullets.removeIndex(i);
        }
    }

    private void generateLevel() {
        if (exitDoors.isEmpty()) generateExitDoors();
        generateEnemies();
        generateTraps();
        generateHearts();
        generateTreasures();
        generateKeys();
    }

    private void generateKeys() {
        int keyCount = GameConstants.KEYCOUNT;
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
            exitDoors.add(new ExitDoor(p[0], p[1], direction));
        }
    }

    private ExitDoor.DoorDirection determineDoorDirection(int x, int y) {
        int[][] maze = getMaze();
        int w = maze[0].length;
        int h = maze.length;
        boolean up = y + 1 < h && maze[y + 1][x] == 1;
        boolean down = y - 1 >= 0 && maze[y - 1][x] == 1;
        boolean left = x - 1 >= 0 && maze[y][x - 1] == 1;
        boolean right = x + 1 < w && maze[y][x + 1] == 1;
        List<ExitDoor.DoorDirection> possible = new ArrayList<>();
        if (up) possible.add(ExitDoor.DoorDirection.UP);
        if (down) possible.add(ExitDoor.DoorDirection.DOWN);
        if (left) possible.add(ExitDoor.DoorDirection.LEFT);
        if (right) possible.add(ExitDoor.DoorDirection.RIGHT);
        if (!possible.isEmpty()) return possible.get(random.nextInt(possible.size()));
        return ExitDoor.DoorDirection.UP;
    }

    private boolean isValidDoorPosition(int x, int y) {
        int[][] maze = getMaze();
        int w = maze[0].length;
        int h = maze.length;
        if (maze[y][x] != 0) return false;
        if (y + 1 < h && maze[y + 1][x] == 1) return true;
        if (y - 1 >= 0 && maze[y - 1][x] == 1) return true;
        if (x - 1 >= 0 && maze[y][x - 1] == 1) return true;
        if (x + 1 < w && maze[y][x + 1] == 1) return true;
        return false;
    }

    private int[] randomWallCell() {
        int[][] maze = getMaze();
        int w = maze[0].length;
        int h = maze.length;
        for (int i = 0; i < 1000; i++) {
            int x = BORDER_THICKNESS + random.nextInt(w - BORDER_THICKNESS * 2);
            int y = BORDER_THICKNESS + random.nextInt(h - BORDER_THICKNESS * 2);
            if (maze[y][x] != 0 || isExitDoorAt(x, y)) continue;
            if ((y + 1 < h && maze[y + 1][x] == 1) || (y - 1 >= 0 && maze[y - 1][x] == 1) ||
                    (x - 1 >= 0 && maze[y][x - 1] == 1) || (x + 1 < w && maze[y][x + 1] == 1)) {
                return new int[]{x, y};
            }
        }
        return new int[]{BORDER_THICKNESS, BORDER_THICKNESS};
    }

    private void generateEnemies() {
        for (int i = 0; i < difficultyConfig.enemyE01PearlCount; i++) {
            int[] p = randomEmptyCell();
            enemies.add(new EnemyCorruptedBoba(p[0], p[1]));
        }
        for (int i = 0; i < difficultyConfig.enemyE02CoffeeBeanCount; i++) {
            int[] p = randomEmptyCell();
            enemies.add(new EnemyE02_SmallCoffeeBean(p[0], p[1]));
        }
        for (int i = 0; i < difficultyConfig.enemyE03CaramelCount; i++) {
            int[] p = randomEmptyCell();
            enemies.add(new EnemyE03_CaramelJuggernaut(p[0], p[1]));
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
        for (int i = 0; i < 10; i++) {
            int[] p = randomEmptyCell();
            hearts.add(new Heart(p[0], p[1]));
        }
    }

    private void generateTreasures() {
        int count = 0;
        int attempts = 0;
        while (count < 3 && attempts < 200) {
            attempts++;
            int[] p = randomEmptyCell();
            if (isOccupied(p[0], p[1])) continue;
            treasures.add(new Treasure(p[0], p[1]));
            count++;
        }
    }

    private int[] randomEmptyCell() {
        int x, y;
        do {
            x = random(1, maze[0].length - 2);
            y = random(1, maze.length - 2);
        } while (maze[y][x] == 0);
        return new int[]{x, y};
    }

    public boolean canPlayerMoveTo(int x, int y) {
        if (x < 0 || y < 0 || y >= maze.length || x >= maze[0].length) return false;
        for (ExitDoor door : exitDoors) if (door.getX() == x && door.getY() == y) return !door.isLocked();
        return maze[y][x] == 1;
    }

    public Player getPlayer() {
        return player;
    }

    public int[][] getMaze() {
        return maze;
    }

    public List<Enemy> getEnemies() {
        return enemies;
    }

    public List<Trap> getTraps() {
        return traps;
    }

    public List<Heart> getHearts() {
        return hearts;
    }

    public List<Treasure> getTreasures() {
        return treasures;
    }

    public List<ExitDoor> getExitDoors() {
        return exitDoors;
    }

    public Compass getCompass() {
        return compass;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public List<Key> getKeys() {
        return keys;
    }

    public boolean isLevelTransitionInProgress() {
        return levelTransitionInProgress;
    }

    public BobaBulletManager getBobaBulletEffectManager() {
        return bobaBulletEffectManager;
    }

    public KeyEffectManager getKeyEffectManager() {
        return keyEffectManager;
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

    public CombatEffectManager getCombatEffectManager() {
        return combatEffectManager;
    }

    public void onMoveInput(int dx, int dy) {
        if (player == null || levelTransitionInProgress) return;
        int nx = player.getX() + dx;
        int ny = player.getY() + dy;
        if (canPlayerMoveTo(nx, ny)) player.move(dx, dy);
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
        for (Treasure t : treasures) {
            if (t.isInteractable() && t.getX() == px && t.getY() == py) {
                t.onInteract(player);
                return;
            }
        }
        for (Heart h : hearts) {
            if (h.isActive() && h.getX() == px && h.getY() == py) {
                h.onInteract(player);
                return;
            }
        }
    }

    private void checkAutoPickup() {
        if (levelTransitionInProgress) return;
        int px = player.getX();
        int py = player.getY();
        Iterator<Key> keyIt = keys.iterator();
        while (keyIt.hasNext()) {
            Key k = keyIt.next();
            if (k.isActive() && k.getX() == px && k.getY() == py) {
                if (keyEffectManager != null)
                    keyEffectManager.spawnKeyEffect(k.getX() * GameConstants.CELL_SIZE, k.getY() * GameConstants.CELL_SIZE, k.getTexture());
                k.onInteract(player);
                keyIt.remove();
                onKeyCollected();
                break;
            }
        }
        Iterator<Heart> heartIt = hearts.iterator();
        while (heartIt.hasNext()) {
            Heart h = heartIt.next();
            if (h.isActive() && h.getX() == px && h.getY() == py) {
                if (itemEffectManager != null)
                    itemEffectManager.spawnHeart((h.getX() + 0.5f) * GameConstants.CELL_SIZE, (h.getY() + 0.5f) * GameConstants.CELL_SIZE);
                h.onInteract(player);

                player.addScore(50);

                // 🔥 [Phase 4] 爱心飘字 +50 (绿色)
                if (combatEffectManager != null) {
                    float fx = (h.getX() + 0.5f) * GameConstants.CELL_SIZE;
                    float fy = (h.getY() + 1f) * GameConstants.CELL_SIZE;
                    combatEffectManager.spawnFloatingText(fx, fy, "+50", Color.GREEN);
                }

                GameSaveData stats = getCareerStats();
                if (!stats.hasHealedOnce) {
                    stats.hasHealedOnce = true;
                    unlockAchievement("ACH_03", "Emergency Topping");
                }

                heartIt.remove();
            }
        }
        for (Treasure t : treasures) {
            if (t.isInteractable() && t.getX() == px && t.getY() == py) {
                if (itemEffectManager != null)
                    itemEffectManager.spawnTreasure((t.getX() + 0.5f) * GameConstants.CELL_SIZE, (t.getY() + 0.5f) * GameConstants.CELL_SIZE);
                t.onInteract(player);

                player.addScore(800);

                // 🔥 [Phase 4] 宝箱飘字 +800 (金色)
                if (combatEffectManager != null) {
                    float tx = (t.getX() + 0.5f) * GameConstants.CELL_SIZE;
                    float ty = (t.getY() + 1f) * GameConstants.CELL_SIZE;
                    combatEffectManager.spawnFloatingText(tx, ty, "+800", Color.GOLD);
                }
            }
        }
    }

    public boolean isEnemyValidMove(int x, int y) {
        if (x < 0 || y < 0 || x >= maze[0].length || y >= maze.length) return false;
        if (maze[y][x] == 0) return false;
        for (ExitDoor door : exitDoors) if (door.getX() == x && door.getY() == y) return false;
        for (Trap trap : traps) if (trap.getX() == x && trap.getY() == y && !trap.isPassable()) return false;
        return true;
    }

    public List<Enemy> getEnemiesAt(int x, int y) {
        List<Enemy> result = new ArrayList<>();
        for (Enemy e : enemies) if (e.isActive() && !e.isDead() && e.getX() == x && e.getY() == y) result.add(e);
        return result;
    }

    public int getMazeCell(int x, int y) {
        if (x < 0 || y < 0 || y >= maze.length || x >= maze[0].length) return 0;
        return maze[y][x];
    }

    public void spawnProjectile(BobaBullet bullet) {
        if (bullet != null) bullets.add(bullet);
    }

    public void spawnProjectile(EnemyBullet bullet) {
        if (bullet != null) bullets.add((BobaBullet) bullet);
    }

    private void handlePlayerEnemyCollision() {
        if (levelTransitionInProgress || player == null || player.isDead()) return;
        for (Enemy e : enemies) {
            if (e.isActive() && !e.isDead() && e.getX() == player.getX() && e.getY() == player.getY()) {

                if (!player.isDashInvincible()) {
                    int damage = e.getAttackDamage();
                    int penalty = 50;

                    if (e instanceof EnemyE03_CaramelJuggernaut || e instanceof EnemyE04_CrystallizedCaramelShell) {
                        penalty = 100;
                    }

                    applyDamageWithPenalty(damage, penalty, e.getClass().getSimpleName());
                }
            }
        }
    }

    private void handleDashHitEnemies() {
        if (levelTransitionInProgress || player == null || !player.isDashing()) return;
        for (Enemy e : enemies) {
            if (e.isActive() && !e.isDead() && e.getX() == player.getX() && e.getY() == player.getY()) {
                e.takeDamage(2);
                if (combatEffectManager != null) {
                    float ex = e.getX() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f;
                    float ey = e.getY() * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2f;
                    combatEffectManager.spawnSlash(ex, ey, 0, 1);
                }
            }
        }
    }

    public void dispose() {
        if (keyEffectManager != null) keyEffectManager.dispose();
        if (itemEffectManager != null) itemEffectManager.dispose();
        if (trapEffectManager != null) trapEffectManager.dispose();
        if (combatEffectManager != null) combatEffectManager.dispose();
        for (ExitDoor door : exitDoors) door.dispose();
        for (Treasure t : treasures) t.dispose();

        for (Trap trap : traps) {
            if (trap instanceof TrapT02_PearlMine) {
                ((TrapT02_PearlMine) trap).dispose();
            }
        }
    }
}