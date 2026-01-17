package de.tum.cit.fop.maze.game;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.graphics.Color;
import de.tum.cit.fop.maze.abilities.Ability;
import de.tum.cit.fop.maze.effects.Enemy.boba.BobaBulletManager;
import de.tum.cit.fop.maze.effects.environment.items.ItemEffectManager;
import de.tum.cit.fop.maze.effects.environment.items.traps.TrapEffectManager;
import de.tum.cit.fop.maze.effects.environment.portal.PortalEffectManager;
import de.tum.cit.fop.maze.effects.fog.FogSystem;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffectManager;
import de.tum.cit.fop.maze.effects.key.KeyEffectManager;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.Obstacle.DynamicObstacle;
import de.tum.cit.fop.maze.entities.Obstacle.MovingWall;
import de.tum.cit.fop.maze.entities.chapter.Chapter1Relic;
import de.tum.cit.fop.maze.entities.chapter.ChapterContext;
import de.tum.cit.fop.maze.entities.chapter.ChapterDropType;
import de.tum.cit.fop.maze.entities.chapter.RelicData;
import de.tum.cit.fop.maze.entities.enemy.*;
import de.tum.cit.fop.maze.entities.enemy.EnemyBoba.BobaBullet;
import de.tum.cit.fop.maze.entities.trap.*;
import de.tum.cit.fop.maze.game.achievement.AchievementManager;
import de.tum.cit.fop.maze.game.achievement.CareerData;
import de.tum.cit.fop.maze.game.event.GameEventSource;
import de.tum.cit.fop.maze.game.save.GameSaveData;
import de.tum.cit.fop.maze.game.save.PlayerSaveData;
import de.tum.cit.fop.maze.game.score.*;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeGenerator;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.game.save.StorageManager;

import java.util.*;
import java.util.function.Consumer;

import static com.badlogic.gdx.math.MathUtils.random;
import static de.tum.cit.fop.maze.maze.MazeGenerator.BORDER_THICKNESS;

public class GameManager implements PlayerInputHandler.InputHandlerCallback {

    private boolean autoSaveEnabled = true;

    public void setAutoSaveEnabled(boolean enabled) {
        this.autoSaveEnabled = enabled;
    }

    // ===== 延迟恢复用 =====
    private GameSaveData pendingRestoreData = null;

    private DifficultyConfig difficultyConfig;
    private float debugTimer = 0f;

    // ===== Endless Co-op Revive =====
    private static final float REVIVE_DELAY = 10f;

    // ✨ 自动保存
    private boolean restoringFromSave = false;
    private float autoSaveTimer = 0f;
    private static final float AUTO_SAVE_INTERVAL = 30f;

    private boolean revivePending = false;
    private float reviveTimer = 0f;

    private int[][] maze;
    private final List<Player> players = new ArrayList<>();
    private boolean twoPlayerMode = true;
    private Player player;

    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Trap> traps = new ArrayList<>();
    private final List<Heart> hearts = new ArrayList<>();
    private final List<HeartContainer> heartContainers = new ArrayList<>();
    private final List<Treasure> treasures = new ArrayList<>();
    private final List<ExitDoor> exitDoors = new ArrayList<>();
    private final Array<BobaBullet> bullets = new Array<>();
    private List<DynamicObstacle> obstacles = new ArrayList<>();

    private int mouseTileX = -1;
    private int mouseTileY = -1;

    private FogSystem fogSystem;

    private Compass compass;
    private final MazeGenerator generator = new MazeGenerator();
    private KeyEffectManager keyEffectManager;
    private final PlayerInputHandler inputHandler;

    private ItemEffectManager itemEffectManager;
    private TrapEffectManager trapEffectManager;
    private CombatEffectManager combatEffectManager;
    private final BobaBulletManager bobaBulletEffectManager = new BobaBulletManager();

    private ScoreManager scoreManager;
    private AchievementManager achievementManager;
    private GameSaveData gameSaveData;

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

    private boolean levelCompletedPendingSettlement = false;
    private int currentLevel = 1;

    private PortalEffectManager playerSpawnPortal;
    private final ChapterContext chapterContext;
    private boolean chapterMode = false;
    private Chapter1Relic chapter1Relic;
    private final List<Chapter1Relic> chapterRelics = new ArrayList<>();
    private boolean viewingChapterRelic = false;
    private boolean restoreLock = false;

    //
    private StorageManager.SaveTarget currentSaveTarget = StorageManager.SaveTarget.AUTO;
    private Consumer<Enemy> enemyKillListener;

    public void setEnemyKillListener(Consumer<Enemy> listener) {
        this.enemyKillListener = listener;
    }

    public GameManager(DifficultyConfig difficultyConfig, boolean twoPlayerMode,ChapterContext chapterContext)  {
        this.chapterContext = chapterContext;
        this.inputHandler = new PlayerInputHandler();
        if (difficultyConfig == null) {
            throw new IllegalArgumentException("difficultyConfig must not be null");
        }
        this.difficultyConfig = difficultyConfig;

        GameEventSource eventSource = GameEventSource.getInstance();
        eventSource.clearListeners();

        this.gameSaveData = new GameSaveData();
        this.scoreManager = new ScoreManager(difficultyConfig);

        StorageManager storageManager = StorageManager.getInstance();
        CareerData careerData = storageManager.loadCareer();

        this.achievementManager = new AchievementManager(
                careerData,
                this.gameSaveData,
                storageManager,
                difficultyConfig.difficulty
        );

        eventSource.addListener(this.scoreManager);
        eventSource.addListener(this.achievementManager);

        this.twoPlayerMode = twoPlayerMode;
        this.chapterMode = (chapterContext != null);
    }
    public GameManager(DifficultyConfig difficultyConfig, boolean twoPlayerMode) {
        this(difficultyConfig,twoPlayerMode,null);
    }

    public void resetGame() {
        if (restoreLock) {
            Logger.error("⛔ resetGame blocked during restore");
            return;
        }
        Logger.error("🔥 RESET GAME CALLED");
        Logger.error("  pendingRestoreData=" + pendingRestoreData);
        Logger.error("  restoringFromSave=" + restoringFromSave);
        Logger.error("  stackTrace:");
        new Exception().printStackTrace();
        Logger.error(
                "RESET GAME | pendingRestoreData=" + (pendingRestoreData != null)
                        + " restoringFromSave=" + restoringFromSave
        );
        gameVariables = new HashMap<>();
        gameVariables.put("speed_mult", 1.0f);
        gameVariables.put("dmg_taken", 1.0f);
        gameVariables.put("cam_zoom", 1.0f);
        gameVariables.put("time_scale", 1.0f);

        if (!restoringFromSave) {
            maze = generator.generateMaze(difficultyConfig);
        } else {
            maze = deepCopyMaze(gameSaveData.maze);
        }

        enemies.clear();
        traps.clear();
        hearts.clear();
        heartContainers.clear(); // 确保清空
        treasures.clear();
        for (ExitDoor door : exitDoors) {
            if (door != null) {
                door.resetDoor();
            }
        }
        keys.clear();
        players.clear();

        int[] spawn1 = randomEmptyCell();
        Player p1 = new Player(spawn1[0], spawn1[1], this, Player.PlayerIndex.P1);
        players.add(p1);

        if (twoPlayerMode) {
            int[] spawn2 = findNearbySpawn(p1);
            if (spawn2 == null) spawn2 = spawn1;
            Player p2 = new Player(spawn2[0], spawn2[1], this, Player.PlayerIndex.P2);
            players.add(p2);

            revivePending = false;
            reviveTimer = 0f;
        }

        syncSinglePlayerRef();
        cat = null;  // 默认没有小猫
        if (difficultyConfig.difficulty == Difficulty.HARD) {
            fogSystem = new FogSystem();
        } else {
            fogSystem = null;
        }

        if (player == null) {
            Logger.error("Player is null after resetGame");
            return;
        }

        float px = player.getX() * GameConstants.CELL_SIZE;
        float py = player.getY() * GameConstants.CELL_SIZE;

        playerSpawnPortal = new PortalEffectManager(PortalEffectManager.PortalOwner.PLAYER);
        playerSpawnPortal.startPlayerSpawnEffect(px, py);
        obstacles = new ArrayList<>();

        generateLevel();

        compass = new Compass(player);

        scoreManager.reset();
        bullets.clear();
        bobaBulletEffectManager.clearAllBullets(false);

        keyEffectManager = new KeyEffectManager();
        itemEffectManager = new ItemEffectManager();
        trapEffectManager = new TrapEffectManager();
        combatEffectManager = new CombatEffectManager();

        levelTransitionInProgress = false;
        currentExitDoor = null;
        levelTransitionTimer = 0f;

        Logger.gameEvent("Game reset complete");
    }

    private StorageManager.SaveTarget pendingRestoreSource;
    public void restoreFromSaveData(GameSaveData saveData, StorageManager.SaveTarget source) {
        Logger.error("🔥 RESTORE START source=" + source);

        restoreLock = true;
        restoringFromSave = true;
        currentSaveTarget = source;

        this.gameSaveData = saveData;
        this.pendingRestoreData = saveData;

        this.currentLevel = saveData.currentLevel;
        this.twoPlayerMode = saveData.twoPlayerMode;
    }

    public void debugEnemiesAndBullets() {
        if (player == null) {
            Logger.debug("Player not initialized yet, skip debugEnemiesAndBullets");
            return;
        }
        Logger.debug("=== GameManager Debug ===");
        Logger.debug("Player at: (" + player.getX() + ", " + player.getY() + ")");
        Logger.debug("Total enemies: " + enemies.size());

        int shootingEnemies = 0;
        for (Enemy enemy : enemies) {
            String state = enemy.isActive() ? "Active" : "Inactive";
            String type = enemy.getClass().getSimpleName();
            String pos = "(" + enemy.getX() + ", " + enemy.getY() + ")";
            float dist = (float) Math.sqrt(
                    Math.pow(enemy.getX() - player.getX(), 2) +
                            Math.pow(enemy.getY() - player.getY(), 2)
            );

            Logger.debug("  " + type + " at " + pos + " - " + state + " | Dist: " + dist);

            if (enemy.isActive() && dist < 10) { // 假设射击距离为10
                shootingEnemies++;
            }
        }

        Logger.debug("Enemies in shooting range: " + shootingEnemies);
        Logger.debug("Active bullets: " + bullets.size);
        Logger.debug("=== End Debug ===");
    }

    private int[] findNearbySpawn(Player p1) {
        int px = p1.getX();
        int py = p1.getY();

        // 8 个方向（顺时针）
        int[][] offsets = {
                {-1, -1}, {0, -1}, {1, -1},
                {-1,  0},          {1,  0},
                {-1,  1}, {0,  1}, {1,  1}
        };

        for (int[] o : offsets) {
            int nx = px + o[0];
            int ny = py + o[1];

            // 必须：能走 + 没被占
            if (canPlayerMoveTo(nx, ny) && !isOccupied(nx, ny)) {
                return new int[]{nx, ny};
            }
        }

        // ⚠️ 如果 8 格全满，兜底：随机一个
        Logger.warning("No nearby spawn found for P2, fallback to random");
        return null;
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
            if (o instanceof MovingWall mw) {
                if (mw.occupiesCell(x, y)) {
                    return false;
                }
            } else {
                if (o.getX() == x && o.getY() == y) {
                    return false;
                }
            }
        }

        // 3️⃣ 普通墙体
        return maze[y][x] == 1;
    }

    public void update(float delta) {
        if (!viewingChapterRelic) {
            inputHandler.update(delta, this, Player.PlayerIndex.P1);
            if (twoPlayerMode) {
                inputHandler.update(delta, this, Player.PlayerIndex.P2);
            }
        }

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
            if (currentExitDoor != null) {
                currentExitDoor.update(delta, this);
            }
            levelTransitionTimer += delta;
            if (levelTransitionTimer >= LEVEL_TRANSITION_DELAY) {
                levelTransitionInProgress = false;
                levelTransitionTimer = 0f;
                currentExitDoor = null;
                if (chapterContext != null) {
                    chapterContext.clearActiveRelic();
                }
                nextLevel();
            }
            return;
        }
        for (Player p : players) {
            p.update(delta);
        }
        updateEndlessRevive(delta);

        boolean fogOn = fogSystem != null && fogSystem.isActive();
        if (difficultyConfig.difficulty == Difficulty.HARD) {
            if (fogOn) {
                if (cat == null)
                    cat = new CatFollower(player, this);
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

        // ===== 🔥 新增：更新陷阱 =====
        for (Trap trap : traps) {
            if (trap.isActive()) {
                trap.update(delta);
            }
        }

        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy e = enemyIterator.next();
            e.update(delta, this);
            e.setGameManager(this);
            if (e.isDead() || !e.isActive()) {
                if (e.isDead()) {
                    EnemyTier tier = EnemyTier.E01;
                    if (e instanceof EnemyE02_SmallCoffeeBean) tier = EnemyTier.E02;
                    else if (e instanceof EnemyE03_CaramelJuggernaut) tier = EnemyTier.E03;
                    else if (e instanceof EnemyE04_CrystallizedCaramelShell) tier = EnemyTier.E04;

                    GameEventSource.getInstance().onEnemyKilled(tier, e.isHitByDash());

                    // 🔥 [HP-UP] E04 击杀掉落判定
                    if (e instanceof EnemyE04_CrystallizedCaramelShell) {
                        handleEnemyDrop(e);
                    }
                }
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
        bobaBulletEffectManager.addBullets(bullets);
        bobaBulletEffectManager.update(delta);

        handlePlayerEnemyCollision();
        handleDashHitEnemies();
        checkAutoPickup();

        if (keyEffectManager != null) {
            keyEffectManager.update(delta);
        }
        if (itemEffectManager != null) {
            itemEffectManager.update(delta);
        }
        if (trapEffectManager != null) {
            trapEffectManager.update(delta);
        }
        if (combatEffectManager != null) {
            combatEffectManager.update(delta);
        }
        handlePlayerTrapInteraction();
        handleKeyLogic();

        // ===== 🔥 统一重置执行点 =====
        if (pendingReset) {
            pendingReset = false;

            if (restoreLock || restoringFromSave) return;
            resetGame();
            justReset = true;
        }

        debugTimer += delta;
        if (debugTimer >= 2.0f) {
            debugEnemiesAndBullets();
            debugTimer = 0f;
        }

        autoSaveTimer += delta;
        if (autoSaveTimer >= AUTO_SAVE_INTERVAL) {
            autoSaveTimer = 0f;
            if (!autoSaveEnabled) {
                Logger.error("⛔ AutoSave skipped (disabled)");
                return;
            }
            if (restoringFromSave) return;
            if (!levelTransitionInProgress && player != null && !player.isDead()) {

                StorageManager.SaveTarget old = currentSaveTarget;
                currentSaveTarget = StorageManager.SaveTarget.AUTO;

                saveGameProgress();

                currentSaveTarget = old; // 恢复
            }
        }

    }
    public float getReviveProgress() {
        if (!revivePending) return 0f;
        return Math.min(1f, reviveTimer / REVIVE_DELAY);
    }

    private Player lastReviveTarget = null;

    private void updateEndlessRevive(float delta) {

        if (!twoPlayerMode) return;

        Player p1 = getPlayerByIndex(Player.PlayerIndex.P1);
        Player p2 = getPlayerByIndex(Player.PlayerIndex.P2);
        if (p1 == null || p2 == null) return;

        boolean p1Dead = p1.isDead();
        boolean p2Dead = p2.isDead();

        // 双死：不处理（由 EndlessScreen 判定 GameOver）
        if (p1Dead && p2Dead) {
            revivePending = false;
            reviveTimer = 0f;
            lastReviveTarget = null;
            return;
        }

        // 一死一活
        if (p1Dead ^ p2Dead) {
            Player alive = p1Dead ? p2 : p1;
            Player dead  = p1Dead ? p1 : p2;

            if (dead != lastReviveTarget) {
                reviveTimer = 0f;
                lastReviveTarget = dead;
            }

            revivePending = true;
            reviveTimer += delta;

            if (reviveTimer >= REVIVE_DELAY) {
                revivePlayer(dead, alive);
                revivePending = false;
                reviveTimer = 0f;
                lastReviveTarget = null;
            }
        } else {
            revivePending = false;
            reviveTimer = 0f;
            lastReviveTarget = null;
        }
    }

    private void revivePlayer(Player dead, Player alive) {
        int[] spawn = findNearbySpawn(alive);
        if (spawn == null) {
            spawn = new int[]{alive.getX(), alive.getY()};
        }
        dead.reviveAt(spawn[0], spawn[1], 10);//复活的hp
        Logger.gameEvent("Revived " + dead.getPlayerIndex() + " near " + alive.getPlayerIndex());
    }

    public Player getNearestAlivePlayer(int x, int y) {
        Player nearest = null;
        float bestDist = Float.MAX_VALUE;

        for (Player p : players) {
            if (p == null || p.isDead()) continue;

            float dx = p.getX() - x;
            float dy = p.getY() - y;
            float dist = dx * dx + dy * dy;

            if (dist < bestDist) {
                bestDist = dist;
                nearest = p;
            }
        }
        return nearest;
    }

    private void handlePlayerEnemyCollision() {
        if (levelTransitionInProgress) return;

        for (Player p : players) {
            if (p == null || p.isDead()) continue;
            if (p.isDashInvincible()) continue;

            for (Enemy enemy : enemies) {
                if (enemy == null || enemy.isDead() || !enemy.isActive()) continue;

                boolean hit = false;

                if (enemy instanceof EnemyE02_SmallCoffeeBean e02) {
                    float px = p.getWorldX() + 0.5f;
                    float py = p.getWorldY() + 0.5f;

                    float dx = px - e02.getWorldX();
                    float dy = py - e02.getWorldY();
                    float radius = 0.6f;
                    hit = (dx * dx + dy * dy) <= radius * radius;
                } else {
                    hit = enemy.getX() == p.getX() && enemy.getY() == p.getY();
                }

                if (hit) {
                    int livesBefore = p.getLives();
                    p.takeDamage(enemy.getCollisionDamage());
                    int damage = livesBefore - p.getLives();

                    if (damage > 0) {
                        DamageSource source = DamageSource.UNKNOWN;
                        if (enemy instanceof EnemyE01_CorruptedPearl) source = DamageSource.ENEMY_E01;
                        else if (enemy instanceof EnemyE02_SmallCoffeeBean) source = DamageSource.ENEMY_E02;
                        else if (enemy instanceof EnemyE03_CaramelJuggernaut) source = DamageSource.ENEMY_E03;
                        else if (enemy instanceof EnemyE04_CrystallizedCaramelShell) source = DamageSource.ENEMY_E04;

                        GameEventSource.getInstance().onPlayerDamage(p.getLives(), source);

                        // 保留：红色大字扣分
                        int penalty = (int) (source.penaltyScore * difficultyConfig.penaltyMultiplier);
                        if (combatEffectManager != null && penalty > 0) {
                            float tx = (p.getX() + 0.5f) * GameConstants.CELL_SIZE;
                            float ty = (p.getY() + 0.5f) * GameConstants.CELL_SIZE;
                            combatEffectManager.spawnScoreText(tx, ty + 40, -penalty);
                        }
                    }
                }
            }
        }
    }

    private void handlePlayerTrapInteraction() {
        if (levelTransitionInProgress) return;

        for (Player p : players) {
            if (p == null || p.isDead()) continue;

            int px = p.getX();
            int py = p.getY();

            for (Trap trap : traps) {
                if (!trap.isActive()) continue;

                if (trap.getX() == px && trap.getY() == py) {
                    trap.onPlayerStep(p);
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
    private void handleDashHitEnemies() {
        if (levelTransitionInProgress) return;

        for (Player p : players) {
            if (p == null || !p.isDashing()) continue;

            float px = p.getWorldX() + 0.5f;
            float py = p.getWorldY() + 0.5f;

            for (Enemy enemy : enemies) {
                if (enemy == null || enemy.isDead() || !enemy.isActive()) continue;

                boolean hit = false;

                if (enemy instanceof EnemyE02_SmallCoffeeBean e02) {
                    float dx = px - e02.getWorldX();
                    float dy = py - e02.getWorldY();
                    hit = (dx * dx + dy * dy) <= 0.7f * 0.7f;
                } else if (enemy instanceof EnemyE04_CrystallizedCaramelShell shell) {
                    hit = shell.occupiesCell((int) px, (int) py);
                } else {
                    hit = enemy.getX() == (int) px && enemy.getY() == (int) py;
                }

                if (hit) {
                    enemy.markHitByDash();
                    enemy.takeDamage(2);
                }
            }
        }
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

    private void checkExitReached() {
        if (levelTransitionInProgress) return;

        if (!twoPlayerMode) {
            Player p = player;
            for (ExitDoor door : exitDoors) {
                if (!door.isLocked() &&
                        door.isActive() &&
                        door.getX() == p.getX() &&
                        door.getY() == p.getY()) {
                    door.onPlayerStep(p);
                    startLevelTransition(door);
                    return;
                }
            }
            return;
        }

        Player p1 = getPlayerByIndex(Player.PlayerIndex.P1);
        Player p2 = getPlayerByIndex(Player.PlayerIndex.P2);

        if (p1 == null || p2 == null) return;
        if (p1.isDead() || p2.isDead()) return;

        for (ExitDoor door : exitDoors) {
            if (!door.isActive() || door.isLocked()) continue;

            boolean p1On = p1.getX() == door.getX() && p1.getY() == door.getY();
            boolean p2On = p2.getX() == door.getX() && p2.getY() == door.getY();

            if (p1On && p2On) {
                door.onPlayerStep(p1);
                startLevelTransition(door);
                return;
            }
        }
    }

    private void startLevelTransition(ExitDoor door) {
        levelTransitionInProgress = true;
        currentExitDoor = door;
        levelTransitionTimer = 0f;
        GameEventSource.getInstance().onLevelFinished(currentLevel);
        Logger.gameEvent("Level transition started");
    }

    public void nextLevel() {
        levelCompletedPendingSettlement = true;
        Logger.gameEvent("Level " + currentLevel + " completed");
        currentLevel++;
        restoringFromSave = false;
        if (currentLevel > GameConstants.MAX_LEVELS) {
            Logger.gameEvent("Game completed!");
            return;
        }

        requestReset();
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


    public boolean isLevelCompletedPendingSettlement() {
        return levelCompletedPendingSettlement;
    }

    public void clearLevelCompletedFlag() {
        levelCompletedPendingSettlement = false;
    }



    public void requestReset() {
        pendingReset = true;
    }

    public void spawnProjectile(EnemyBullet bullet) {
        if (bullet == null) return;

        // 🔥 修复：检查类型，如果是 BobaBullet 则添加到相应的列表
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

    // ==========================================
    // 🔥 checkAutoPickup (修复版：加入 HeartContainer 拾取)
    // ==========================================
    private void checkAutoPickup() {
        if (levelTransitionInProgress) return;

        for (Player p : players) {
            if (p == null || p.isDead()) continue;

            int px = p.getX();
            int py = p.getY();

            // ===== Keys =====
            Iterator<Key> keyIterator = keys.iterator();
            while (keyIterator.hasNext()) {
                Key key = keyIterator.next();
                if (!key.isActive()) continue;

                if (key.getX() == px && key.getY() == py) {
                    float fx = key.getX() * GameConstants.CELL_SIZE;
                    float fy = key.getY() * GameConstants.CELL_SIZE;

                    if (keyEffectManager != null && key.getTexture() != null) {
                        keyEffectManager.spawnKeyEffect(fx, fy, key.getTexture());
                    }

                    key.onInteract(p);
                    keyIterator.remove();
                    onKeyCollected();

                    if (combatEffectManager != null) {
                        // 1. 蓝色小字 "KEY ACQUIRED" (修改了内容)
                        combatEffectManager.spawnStatusText(fx, fy + 50, "KEY ACQUIRED", Color.CYAN);
                        // 2. 黄色大字分数
                        combatEffectManager.spawnScoreText(fx, fy + 20, ScoreConstants.SCORE_KEY);
                    }
                    break;
                }
            }

            // ===== Hearts =====
            Iterator<Heart> heartIterator = hearts.iterator();
            while (heartIterator.hasNext()) {
                Heart h = heartIterator.next();
                if (!h.isActive()) continue;

                if (h.getX() == px && h.getY() == py) {
                    float fx = (h.getX() + 0.5f) * GameConstants.CELL_SIZE;
                    float fy = (h.getY() + 0.5f) * GameConstants.CELL_SIZE;

                    if (itemEffectManager != null) {
                        itemEffectManager.spawnHeart(fx, fy);
                    }

                    h.onInteract(p);
                    GameEventSource.getInstance().onItemCollected("HEART");
                    heartIterator.remove();

                    // 🚫 注释：不飘黄色分数，只让 Player.heal 显示绿色 HP+xx
                    // if (combatEffectManager != null) {
                    //     combatEffectManager.spawnScoreText(fx, fy + 20, ScoreConstants.SCORE_HEART);
                    // }
                }
            }

            // ===== 🔥 [HP-UP] 新增：Heart Containers 自动拾取 =====
            Iterator<HeartContainer> hcIterator = heartContainers.iterator();
            while (hcIterator.hasNext()) {
                HeartContainer hc = hcIterator.next();
                if (hc.isActive() && hc.getX() == px && hc.getY() == py) {
                    hc.onInteract(p);
                    hcIterator.remove();
                }
            }

            // ===== Treasures =====
            Iterator<Treasure> treasureIterator = treasures.iterator();
            while (treasureIterator.hasNext()) {
                Treasure t = treasureIterator.next();
                if (!t.isInteractable()) continue;

                if (t.getX() == px && t.getY() == py) {
                    float fx = (t.getX() + 0.5f) * GameConstants.CELL_SIZE;
                    float fy = (t.getY() + 0.5f) * GameConstants.CELL_SIZE;

                    if (itemEffectManager != null) {
                        itemEffectManager.spawnTreasure(fx, fy);
                    }

                    onTreasureOpened(p, t);
                    GameEventSource.getInstance().onItemCollected("TREASURE");
                    treasureIterator.remove();

                    // 🚫 注释：不飘黄色分数，只保留 Treasure 自己的 HP/Buff 提示
                    // if (combatEffectManager != null) {
                    //     combatEffectManager.spawnScoreText(fx, fy + 20, ScoreConstants.SCORE_TREASURE);
                    // }
                }
            }
        }
    }

    /* ================= Level Generation ================= */

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
    }
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

    private boolean isWalkableLine(int sx, int sy, int ex, int ey) {
        if (sy != ey) return false; // 只做水平路径
        for (int x = sx; x <= ex; x++) {
            if (maze[sy][x] != 1) return false;
        }
        return true;
    }
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

    private void generateKeys() {
        int keyCount = difficultyConfig.keyCount;

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
        for (Player p : players) {
            if (p != null && p.getX() == x && p.getY() == y) return true;
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
            int[] p = randomE04SpawnCell();
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
        int targetCount = 10; //宝箱数目
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
                for (int offset = 0; offset < Math.max(width, height); offset++) {
                    for (int cx = Math.max(1, width/2 - offset); cx <= Math.min(width-2, width/2 + offset); cx++) {
                        for (int cy = Math.max(1, height/2 - offset); cy <= Math.min(height-2, height/2 + offset); cy++) {
                            if (maze[cy][cx] != 0 && !isOccupied(cx, cy)) {
                                return new int[]{cx, cy};
                            }
                        }
                    }
                }
                return new int[]{maze[0].length / 2, maze.length / 2};
            }
        } while (!canPlayerMoveTo(x, y) || isOccupied(x, y));

        return new int[]{x, y};
    }
    /**
     * 🔥 Boss 模式：把所有玩家刷在一起 / 附近
     */
    public void respawnPlayersTogetherForBoss() {
        if (players.isEmpty()) return;

        // 1️⃣ 找一个主出生点
        int[] baseSpawn = randomEmptyCell();
        int bx = baseSpawn[0];
        int by = baseSpawn[1];

        // 2️⃣ 第一个玩家直接放
        Player leader = players.get(0);
        leader.teleportTo(bx, by);

        // 3️⃣ 其余玩家找附近
        int[][] offsets = {
                { 0,  0}, // 允许重叠
                { 1,  0}, {-1,  0},
                { 0,  1}, { 0, -1},
                { 1,  1}, {-1, -1},
                { 1, -1}, {-1,  1}
        };

        for (int i = 1; i < players.size(); i++) {
            Player p = players.get(i);
            boolean placed = false;

            for (int[] o : offsets) {
                int nx = bx + o[0];
                int ny = by + o[1];

                if (canPlayerMoveTo(nx, ny)) {
                    p.teleportTo(nx, ny);
                    placed = true;
                    break;
                }
            }

            // 4️⃣ 极端兜底：直接重叠
            if (!placed) {
                p.teleportTo(bx, by);
            }
        }

        Logger.gameEvent("🔥 Boss spawn: players grouped at (" + bx + "," + by + ")");
    }

    public int getMazeCell(int x, int y) {
        if (x < 0 || y < 0) {
            return 0;
        }

        if (y >= maze.length || x >= maze[0].length) {
            return 0;
        }

        return maze[y][x];
    }

    public Player getPlayer() { return player; }
    public List<Player> getPlayers() { return players; }
    public int[][] getMaze() { return maze; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<Trap> getTraps() { return traps; }
    public List<Heart> getHearts() { return hearts; }
    public List<Treasure> getTreasures() { return treasures; }
    public List<ExitDoor> getExitDoors() { return exitDoors; }
    public List<Key> getKeys() { return keys; }
    public int getCurrentLevel() { return currentLevel; }
    public boolean isTwoPlayerMode() { return twoPlayerMode; }
    public Compass getCompass() { return compass; }


    @Override
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
    public float getMoveDelayMultiplier() {
        return 1.0f;
    }

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
        for (Chapter1Relic relic : chapterRelics) {
            if (relic.isInteractable()
                    && relic.getX() == px
                    && relic.getY() == py) {
                relic.onInteract(p);
                return;
            }
        }
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



    public void setVariable(String key, float value) {
        if (gameVariables == null) gameVariables = new HashMap<>();
        gameVariables.put(key, value);
    }

    public float getVariable(String key) {
        if (gameVariables == null) return 1.0f;
        return gameVariables.getOrDefault(key, 1.0f);
    }

    public int getScore() {
        return scoreManager != null ? scoreManager.getCurrentScore() : 0;
    }

    public PlayerInputHandler getInputHandler() {
        return inputHandler;
    }

    public void dispose() {
        // 🔥🔥🔥【修复 2】: 退出/销毁时，强制保存当前进度！
        // 防止玩家在自动保存间隔期(30s)内退出导致进度丢失。
        if (player != null && !player.isDead()) {
            saveGameProgress();
        }

        GameEventSource eventSource = GameEventSource.getInstance();
        if (scoreManager != null) eventSource.removeListener(scoreManager);
        if (achievementManager != null) {
            eventSource.removeListener(achievementManager);
            achievementManager.saveIfNeeded();
        }

        if (itemEffectManager != null) itemEffectManager.dispose();
        if (trapEffectManager != null) trapEffectManager.dispose();
        if (combatEffectManager != null) combatEffectManager.dispose();
        if (bobaBulletEffectManager != null) bobaBulletEffectManager.dispose();
        if (playerSpawnPortal != null) playerSpawnPortal.dispose();
        if (keyEffectManager != null) keyEffectManager.dispose();

        for (ExitDoor door : exitDoors) door.dispose();
        for (Treasure t : treasures) t.dispose();

        // 等待所有异步保存写入磁盘
        StorageManager.getInstance().flushAllSaves();

        Logger.info("GameManager disposed");
    }
    public KeyEffectManager getKeyEffectManager() {
        return keyEffectManager;
    }
    public PortalEffectManager getPlayerSpawnPortal() {
        return playerSpawnPortal;
    }
    private void syncSinglePlayerRef() {
        if (!players.isEmpty()) {
            player = players.get(0);
        } else {
            player = null;
        }
    }
    public boolean isLevelTransitionInProgress() {
        return levelTransitionInProgress;
    }

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
        if (nx < 0 || ny < 0 ||
                ny >= maze.length ||
                nx >= maze[0].length) {
            return false;
        }

        if (maze[ny][nx] == 0) {
            return false;
        }

        for (ExitDoor door : exitDoors) {
            if (door.getX() == nx && door.getY() == ny) {
                return false;
            }
        }

        for (Enemy e : enemies) {
            if (e.isActive() &&
                    e.getX() == nx &&
                    e.getY() == ny) {
                return false;
            }
        }

        for (DynamicObstacle o : obstacles) {
            if (o instanceof MovingWall mw) {
                if (mw.occupiesCell(nx, ny)) {
                    return false;
                }
            } else {
                if (o.getX() == nx && o.getY() == ny) {
                    return false;
                }
            }
        }


        return true;
    }

    public List<DynamicObstacle> getObstacles() { return obstacles; }
    public CatFollower getCat() {
        return cat;
    }

    // 🔥 彻底保留了原版 saveGameProgress
    public void saveGameProgress() {
        if (restoringFromSave) {
            Logger.error("🚫 SAVE BLOCKED (restoring)");
            return;
        }


        Logger.error("=== SAVE DEBUG ===");

        Logger.error("local path = " + com.badlogic.gdx.Gdx.files.local("").file().getAbsolutePath());
        Logger.error("auto file = " + com.badlogic.gdx.Gdx.files.local("save_auto.json.gz").file().getAbsolutePath());
        Logger.error("exists? " + com.badlogic.gdx.Gdx.files.local("save_auto.json.gz").exists());
        Logger.error("🔥 SAVE CALLED target=" + currentSaveTarget);
        if (gameSaveData == null) {
            gameSaveData = new GameSaveData();
        }
        gameSaveData.maze = deepCopyMaze(maze);
        gameSaveData.currentLevel = currentLevel;
        gameSaveData.difficulty = difficultyConfig.difficulty.name();
        gameSaveData.twoPlayerMode = twoPlayerMode;

        gameSaveData.players.clear();

        for (Player p : players) {
            if (p == null) continue;

            PlayerSaveData ps = new PlayerSaveData();

            ps.x = p.getX();
            ps.y = p.getY();

            ps.lives = p.getLives();
            ps.maxLives = p.getMaxLives();
            ps.mana = (int) p.getMana();

            ps.hasKey = p.hasKey();
            ps.buffAttack = p.hasBuffAttack();
            ps.buffRegen = p.hasBuffRegen();
            ps.buffManaEfficiency = p.hasBuffManaEfficiency();
            Logger.error(
                    "SAVE CHECK buffAttack=" + p.hasBuffAttack()
            );
            // 技能等级
            if (p.getAbilityManager() != null) {
                for (Ability a : p.getAbilityManager().getAbilities().values()) {
                    ps.abilityStates.put(
                            a.getId(),
                            a.saveState()
                    );

                }
            }

            gameSaveData.players.put(p.getPlayerIndex().name(), ps);
        }

        if (scoreManager != null) {
            scoreManager.saveState(gameSaveData);
            int currentRaw = Math.max(0, gameSaveData.levelBaseScore - gameSaveData.levelPenalty);
            int currentFinal = (int) (currentRaw * difficultyConfig.scoreMultiplier);
            int currentTotal = scoreManager.getCurrentScore();
            gameSaveData.score = Math.max(0, currentTotal - currentFinal);
        }

        StorageManager storage = StorageManager.getInstance();
        switch (currentSaveTarget) {
            case SLOT_1 -> storage.saveGameToSlot(1, gameSaveData);
            case SLOT_2 -> storage.saveGameToSlot(2, gameSaveData);
            case SLOT_3 -> storage.saveGameToSlot(3, gameSaveData);
        }


        Logger.info("Game progress saved: Level=" + currentLevel + ", Score=" + gameSaveData.score);
    }

    // 🔥 彻底保留了原版 deepCopyMaze
    private int[][] deepCopyMaze(int[][] src) {
        if (src == null) return null;
        int[][] copy = new int[src.length][];
        for (int i = 0; i < src.length; i++) {
            copy[i] = Arrays.copyOf(src[i], src[i].length);
        }
        return copy;
    }


    public LevelResult getLevelResult() {
        if (scoreManager == null) {
            return new LevelResult(0, 0, 0, "D", 0, 1.0f);
        }

        int theoreticalMaxScore = calculateTheoreticalMaxScore();

        return scoreManager.calculateResult(theoreticalMaxScore);
    }

    private int calculateTheoreticalMaxScore() {
        int maxScore = 0;

        maxScore += difficultyConfig.enemyE01PearlCount * 100;
        maxScore += difficultyConfig.enemyE02CoffeeBeanCount * 200;
        maxScore += difficultyConfig.enemyE03CaramelCount * 300;
        maxScore += difficultyConfig.enemyE04ShellCount * 500;

        maxScore += 10 * 50;  // hearts
        maxScore += 3 * 100;  // treasures
        maxScore += difficultyConfig.keyCount * 200;  // keys

        return maxScore;
    }




    public GameSaveData getGameSaveData() {
        return gameSaveData;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    // 🔥 彻底保留了原版 restorePlayers
    private void restorePlayers(GameSaveData saveData) {
        if (saveData == null) return;

        this.currentLevel = saveData.currentLevel;

        if (scoreManager != null) {
            scoreManager.restoreState(saveData);
        }

        for (Player p : players) {
            PlayerSaveData ps = saveData.players.get(p.getPlayerIndex().name());
            if (ps == null) continue;


            p.teleportTo(ps.x, ps.y);
            p.setLives(ps.lives);
            p.setMaxLives(ps.maxLives);
            p.setMana(ps.mana);
            p.setHasKey(ps.hasKey);

            p.restoreBuffState(
                    ps.buffAttack,
                    ps.buffRegen,
                    ps.buffManaEfficiency
            );
            if (p.getAbilityManager() != null) {
                for (Ability a : p.getAbilityManager().getAbilities().values()) {
                    Map<String, Object> state = ps.abilityStates.get(a.getId());
                    if (state != null) {
                        a.loadState(state);
                    }
                }
            }


            p.setMovingAnim(false);

            Logger.error(
                    "RESTORE CHECK P=" + p.getPlayerIndex()
                            + " atk=" + p.hasBuffAttack()
                            + " regen=" + p.hasBuffRegen()
                            + " mana=" + p.hasBuffManaEfficiency()
            );
        }

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


    public void setMouseTargetTile(int x, int y) {
        this.mouseTileX = x;
        this.mouseTileY = y;
    }

    public int getMouseTileX() {
        return mouseTileX;
    }

    public int getMouseTileY() {
        return mouseTileY;
    }

    // 🔥 [HP-UP] Helper: 掉落判定
    private void handleEnemyDrop(Enemy enemy) {
        if (Math.random() <= 1.00) {
            int x = enemy.getX();
            int y = enemy.getY();

            HeartContainer container = new HeartContainer(x, y);

            heartContainers.add(container);

            Logger.gameEvent("✨ E04 掉落了焦糖核心！");
        }
    }

    // 🔥 [HP-UP] Getter
    public List<HeartContainer> getHeartContainers() {
        return heartContainers;
    }

    public boolean isReviving() {
        return revivePending;
    }
    public Player getRevivingTarget() {
        if (!revivePending) return null;

        Player p1 = getPlayerByIndex(Player.PlayerIndex.P1);
        Player p2 = getPlayerByIndex(Player.PlayerIndex.P2);

        if (p1 == null || p2 == null) return null;

        if (p1.isDead() && !p2.isDead()) return p1;
        if (p2.isDead() && !p1.isDead()) return p2;

        return null;
    }
    public void readChapter1Relic(Chapter1Relic relic) {
        relic.onRead();
        chapterContext.markRelicRead(relic.getData().id);
        chapterRelics.remove(relic);
        chapter1Relic = null;
    }

    public void discardChapter1Relic(Chapter1Relic relic) {
        relic.onDiscard();
        chapterContext.markRelicDiscarded(relic.getData().id);
        chapterRelics.remove(relic);
        chapter1Relic = null;
    }
    private Chapter1RelicListener chapter1RelicListener;
    public void setChapter1RelicListener(Chapter1RelicListener listener) {
        this.chapter1RelicListener = listener;
    }


    public void requestChapter1Relic(Chapter1Relic relic) {
        if (chapter1RelicListener != null) {
            enterChapterRelicView();  // ⭐ 进入查看态
            chapter1RelicListener.onChapter1RelicRequested(relic);
        } else {
            Logger.warning(
                    "Chapter1Relic requested but no Chapter1RelicListener registered"
            );
        }
    }

    // 🔥 修改后的 onTreasureOpened (不调用 applyTreasureBuff，防止双重奖励)
    public void onTreasureOpened(Player player, Treasure treasure) {

        // 非章节模式 → 普通宝箱 (由 Treasure 内部逻辑决定奖励，GM 不干涉)
        if (!chapterMode || chapterContext == null) {
            // applyTreasureBuff(player); <--- ❌ 删除了这行
            treasure.onInteract(player);
            return;
        }

        // 🔥 新逻辑：每次宝箱都“请求 relic”
        RelicData data = chapterContext.requestRelic();

        if (data != null) {
            Chapter1Relic relic = new Chapter1Relic(
                    treasure.getX(),
                    treasure.getY(),
                    data,
                    chapterContext
            );
            spawnChapter1Relic(relic);
        } else {
            // 没有可用 relic → 普通奖励 (由 Treasure 内部逻辑决定)
            // applyTreasureBuff(player); <--- ❌ 删除了这行
        }

        treasure.onInteract(player);
    }

    // ❌ 彻底删除了 applyTreasureBuff 方法

    private void spawnChapter1Relic(Chapter1Relic relic) {
        this.chapter1Relic = relic;
        chapterRelics.add(relic);

        Logger.gameEvent("📜 Chapter1Relic added to world");

    }


    public List<Chapter1Relic> getChapterRelics() {
        return chapterRelics;
    }
    public void enterChapterRelicView() {
        viewingChapterRelic = true;
    }

    public void exitChapterRelicView() {
        viewingChapterRelic = false;
    }


    public boolean isViewingChapterRelic() {
        return viewingChapterRelic;
    }


    public void setCurrentSaveTarget(StorageManager.SaveTarget target) {
        if (target != null) {
            this.currentSaveTarget = target;
        }
    }

    public StorageManager.SaveTarget getCurrentSaveTarget() {
        return currentSaveTarget;
    }

    public void applyRestoreIfNeeded() {
        if (pendingRestoreData == null) return;

        GameSaveData data = pendingRestoreData;

        buildWorldFromRestore(data);
        restorePlayers(data);

        pendingRestoreData = null;
        restoringFromSave = false;
        restoreLock = false;
        pendingReset = false;

        Logger.error("🔥 APPLY RESTORE DATA (FINAL)");
    }


    private void buildWorldFromRestore(GameSaveData data) {

        Logger.error("🧩 buildWorldFromRestore START");

        // ===== 0. 基础 =====
        restoringFromSave = true;

        // ===== 1. Maze =====
        this.maze = deepCopyMaze(data.maze);

        // ===== 2. 清空世界 =====
        enemies.clear();
        traps.clear();
        hearts.clear();
        heartContainers.clear();
        treasures.clear();
        keys.clear();
        exitDoors.clear();
        obstacles.clear();
        players.clear();
        bullets.clear();

        // ===== 3. Players =====
        for (Map.Entry<String, PlayerSaveData> entry : data.players.entrySet()) {

            Player.PlayerIndex index =
                    Player.PlayerIndex.valueOf(entry.getKey());

            PlayerSaveData ps = entry.getValue();

            Player p = new Player(ps.x, ps.y, this, index);
            players.add(p);
        }

        syncSinglePlayerRef();

        // ===== 4. Exit Doors =====
        generateExitDoors();

        // ===== 5. Level Content（重新生成即可）=====
        generateEnemies();
        generateTraps();
        generateHearts();
        generateTreasures();
        generateKeys();
        generateMovingWalls();

        // ===== 6. Fog / Cat =====
        if (difficultyConfig.difficulty == Difficulty.HARD) {
            fogSystem = new FogSystem();
            cat = new CatFollower(player, this);
        } else {
            fogSystem = null;
            cat = null;
        }

        // ===== 7. Effect Managers（必须全部 new）=====
        keyEffectManager     = new KeyEffectManager();
        itemEffectManager    = new ItemEffectManager();
        trapEffectManager    = new TrapEffectManager();
        combatEffectManager  = new CombatEffectManager();

        bobaBulletEffectManager.clearAllBullets(false);

        // ===== 8. Compass =====
        if (player != null) {
            compass = new Compass(player);
        }

        // ===== 9. 状态 =====
        levelTransitionInProgress = false;
        currentExitDoor = null;
        levelTransitionTimer = 0f;

        Logger.error("🧩 buildWorldFromRestore DONE");
    }

    public void markAsNewGame() {
        Logger.error("🆕 MARK AS NEW GAME");

        // 🔥 清空 restore 状态
        restoringFromSave = false;
        restoreLock = false;
        pendingRestoreData = null;

        // 🔥 新游戏 = 立刻初始化世界
        resetGame();
    }
    private boolean uiConsumesMouse = false;

    public void setUIConsumesMouse(boolean v) {
        uiConsumesMouse = v;
    }

    public boolean isUIConsumingMouse() {
        return uiConsumesMouse;
    }

    public void onEnemyKilled(Enemy enemy) {
        // 先什么都不做，留给外部监听
        if (enemyKillListener != null) {
            enemyKillListener.accept(enemy);
        }
    }
    /**
     * 🔥 Boss 专用：只重建迷宫与关卡内容
     * ❗ 不创建 Player
     * ❗ 不 reset 分数 / 技能 / Buff
     */
    public void rebuildMazeForBoss(DifficultyConfig dc) {

        Logger.error("🔥 rebuildMazeForBoss CALLED");

        this.difficultyConfig = dc;

        // ===== 1️⃣ 重新生成 Maze =====
        this.maze = generator.generateMaze(dc);

        // ===== 2️⃣ 清空【环境实体】=====
        enemies.clear();
        traps.clear();
        hearts.clear();
        heartContainers.clear();
        treasures.clear();
        keys.clear();
        obstacles.clear();
        exitDoors.clear();
        bullets.clear();

        bobaBulletEffectManager.clearAllBullets(false);

        // ===== 3️⃣ 重新生成关卡内容 =====
        generateExitDoors();
        generateEnemies();
        generateTraps();
        generateHearts();
        generateTreasures();
        generateKeys();
        generateMovingWalls();

        // ===== 4️⃣ 玩家重定位（Boss 战必须处理所有玩家）=====
        respawnPlayersTogetherForBoss();

        // ===== 5️⃣ Boss 战中：禁止这些状态 =====
        levelTransitionInProgress = false;
        pendingReset = false;
        justReset = false;

        Logger.error("🔥 rebuildMazeForBoss DONE");
    }

    public void rebuildMazeForBossWithPrebuilt(
            DifficultyConfig dc,
            int[][] prebuiltMaze
    ) {
        this.difficultyConfig = dc;

        this.maze = deepCopyMaze(prebuiltMaze);

        enemies.clear();
        traps.clear();
        hearts.clear();
        treasures.clear();
        keys.clear();
        obstacles.clear();
        exitDoors.clear();

        generateExitDoors();
        generateEnemies();
        generateTraps();
        generateHearts();
        generateTreasures();
        generateKeys();
        generateMovingWalls();
    }

    /**
     * 🔥 E04 专用出生点（2x2）
     * - 必须是 2x2 全可走地面
     * - 不能和玩家重叠
     * - 可以和其他敌人重叠
     */
    private int[] randomE04SpawnCell() {
        int width = maze[0].length;
        int height = maze.length;

        for (int attempt = 0; attempt < 500; attempt++) {
            int x = random(1, width - 3);
            int y = random(1, height - 3);

            // 1️⃣ 2x2 地形检查
            if (maze[y][x] != 1) continue;
            if (maze[y + 1][x] != 1) continue;
            if (maze[y][x + 1] != 1) continue;
            if (maze[y + 1][x + 1] != 1) continue;

            // 2️⃣ 不允许与玩家重叠（任何一格）
            boolean overlapsPlayer = false;
            for (Player p : players) {
                if (p == null) continue;

                int px = p.getX();
                int py = p.getY();

                if ((px == x || px == x + 1) &&
                        (py == y || py == y + 1)) {
                    overlapsPlayer = true;
                    break;
                }
            }

            if (overlapsPlayer) continue;

            return new int[]{x, y};
        }

        // 🧯 极端兜底：用玩家附近（理论上很少触发）
        Logger.warning("E04 spawn fallback used");
        return new int[]{player.getX(), player.getY()};
    }


}