package de.tum.cit.fop.maze.game;

import com.badlogic.gdx.utils.Array;
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
import de.tum.cit.fop.maze.entities.enemy.*;
import de.tum.cit.fop.maze.entities.enemy.EnemyBoba.BobaBullet;
import de.tum.cit.fop.maze.entities.trap.*;
import de.tum.cit.fop.maze.game.achievement.AchievementManager;
import de.tum.cit.fop.maze.game.achievement.CareerData;
import de.tum.cit.fop.maze.game.event.GameEventSource;
import de.tum.cit.fop.maze.game.score.DamageSource;
import de.tum.cit.fop.maze.game.score.LevelResult;
import de.tum.cit.fop.maze.game.score.ScoreConstants;
import de.tum.cit.fop.maze.game.score.ScoreManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeGenerator;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.StorageManager;

import java.util.*;
import static com.badlogic.gdx.math.MathUtils.random;
import static de.tum.cit.fop.maze.maze.MazeGenerator.BORDER_THICKNESS;

public class GameManager implements PlayerInputHandler.InputHandlerCallback {

    private final DifficultyConfig difficultyConfig;
    private float debugTimer = 0f;

    // ===== Endless Co-op Revive =====
    private static final float REVIVE_DELAY = 10f;

    // ✨ 自动保存
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

    public GameManager(DifficultyConfig difficultyConfig, boolean twoPlayerMode) {
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
        heartContainers.clear();
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

    public void debugEnemiesAndBullets() {
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
            if (o.getX() == x && o.getY() == y) {
                return false;  // 玩家不能走进移动的墙
            }
        }

        // 3️⃣ 普通墙体
        return maze[y][x] == 1;
    }
    public void update(float delta) {

        inputHandler.update(delta, this, Player.PlayerIndex.P1);
        if (twoPlayerMode) {
            inputHandler.update(delta, this, Player.PlayerIndex.P2);
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
                nextLevel();
            }
            return;
        }

        for (Player p : players) {
            if (!p.isDead()) {
                p.update(delta);
            }
        }

        updateEndlessRevive(delta);

        boolean fogOn = fogSystem != null && fogSystem.isActive();
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

        Iterator<Enemy> enemyIterator = enemies.iterator();
        while (enemyIterator.hasNext()) {
            Enemy e = enemyIterator.next();
            e.update(delta, this);

            if (e.isDead() || !e.isActive()) {
                if (e.isDead()) {
                    EnemyTier tier = EnemyTier.E01;
                    if (e instanceof EnemyE02_SmallCoffeeBean) tier = EnemyTier.E02;
                    else if (e instanceof EnemyE03_CaramelJuggernaut) tier = EnemyTier.E03;
                    else if (e instanceof EnemyE04_CrystallizedCaramelShell) tier = EnemyTier.E04;

                    GameEventSource.getInstance().onEnemyKilled(tier, e.isHitByDash());
                }
                enemyIterator.remove();
            }
        }
        //原来的写法/ ===== 修复: 使用 Iterator 遍历敌人，避免并发修改异常 =====
        //        Iterator<Enemy> enemyIterator = enemies.iterator();
        //        while (enemyIterator.hasNext()) {
        //            Enemy e = enemyIterator.next();
        //            e.update(delta, this);
        //
        //            if (e.isDead() || !e.isActive()) {
        //                if (e.isDead() && e instanceof EnemyE04_CrystallizedCaramelShell) {
        //                    handleEnemyDrop(e);
        //                }
        //                enemyIterator.remove();
        //            }
        //        }

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
            resetGame();
            justReset = true;
        }

        debugTimer += delta;
        if (debugTimer >= 2.0f) {
            debugEnemiesAndBullets();
            debugTimer = 0f;
        }

        if (System.currentTimeMillis() % 2000 < 16) { // 大约每2秒一次
            Logger.debug("Enemies: " + enemies.size() +
                    " | Bullets: " + bullets.size +
                    " | Player: (" + player.getX() + ", " + player.getY() + ")");

            for (Enemy enemy : enemies) {
                float dist = (float) Math.sqrt(
                        Math.pow(enemy.getX() - player.getX(), 2) +
                                Math.pow(enemy.getY() - player.getY(), 2)
                );
                if (dist < 8) {
                    Logger.debug("  " + enemy.getClass().getSimpleName() +
                            " at (" + enemy.getX() + ", " + enemy.getY() +
                            ") - Dist: " + String.format("%.1f", dist));
                }
            }
        }

        debugTimer += delta;
        if (debugTimer >= 2f) {
            debugTimer = 0f;
        }

        autoSaveTimer += delta;
        if (autoSaveTimer >= AUTO_SAVE_INTERVAL) {
            autoSaveTimer = 0f;
            if (!levelTransitionInProgress && player != null && !player.isDead()) {
                saveGameProgress();//DONE
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

                        // HUD 提示：受击扣分（保持简单，不做复杂漂字系统）
                        int penalty = (int) (source.penaltyScore * difficultyConfig.penaltyMultiplier);
                        if (penalty > 0) {
                            p.showNotification("HIT!  SCORE -" + penalty);
                        } else {
                            p.showNotification("HIT!");
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

    public void proceedToNextLevel() {
        currentLevel++;
        if (currentLevel > GameConstants.MAX_LEVELS) {
            Logger.gameEvent("Game completed!");
            return;
        }
        levelCompletedPendingSettlement = false;
        requestReset();
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
            // 如果是其他类型的 EnemyBullet，可能需要单独处理
            // 例如：添加到另一个子弹列表，或直接忽略
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
                    // HUD 提示（避免只有宝箱才有提示）
                    p.showNotification("KEY ACQUIRED!  SCORE +" + ScoreConstants.SCORE_KEY);
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
                    // HUD 提示（避免只有宝箱才有提示）
                    p.showNotification("HEAL +10  SCORE +" + ScoreConstants.SCORE_HEART);
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

                    t.onInteract(p);
                    GameEventSource.getInstance().onItemCollected("TREASURE");
                    treasureIterator.remove();
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

        // 添加调试日志
        Logger.debug("MovingWall created: (" + sx + "," + sy + ") -> (" + ex + "," + ey + ")");
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

    public int getMazeCell(int x, int y) {
        if (x < 0 || y < 0) {
            return 0;
        }

        if (y >= maze.length || x >= maze[0].length) {
            return 0;
        }

        return maze[y][x];
    }



    /* ================= Getters ================= */

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


    /* ================= Input ================= */

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

    /* ================= Console ================= */

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

    /* ================= Dispose ================= */

    public void dispose() {
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
        if (keyEffectManager != null) {
            keyEffectManager.dispose();
        }
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

    /* ================= Save/Load Support ================= */

    /**
     * 保存当前游戏进度到存档
     */
    public void saveGameProgress() {
        if (gameSaveData == null) {
            gameSaveData = new GameSaveData();
        }

        // 同步当前关卡
        gameSaveData.currentLevel = currentLevel;

        // 同步难度
        gameSaveData.difficulty = difficultyConfig.difficulty.name();
        // 同步单/双人模式
        gameSaveData.twoPlayerMode = twoPlayerMode;

        // 同步玩家状态
        if (player != null) {
            gameSaveData.lives = player.getLives();
            gameSaveData.maxLives = player.getMaxLives();
            gameSaveData.mana = (int) player.getMana();
            gameSaveData.hasKey = player.hasKey();
            gameSaveData.buffAttack = player.hasBuffAttack();
            gameSaveData.buffRegen = player.hasBuffRegen();
            gameSaveData.buffManaEfficiency = player.hasBuffManaEfficiency();
        }

        // 同步分数管理器状态
        if (scoreManager != null) {
            scoreManager.saveState(gameSaveData);
            // GameSaveData.score 应保存“已结算累计分”(accumulatedScore)，避免在 SettlementScreen 再次加分时重复累加。
            // 这里通过：当前总分 - 当前关卡(按倍率后的实时分) 来反推 accumulatedScore。
            int currentRaw = Math.max(0, gameSaveData.levelBaseScore - gameSaveData.levelPenalty);
            int currentFinal = (int) (currentRaw * difficultyConfig.scoreMultiplier);
            int currentTotal = scoreManager.getCurrentScore();
            gameSaveData.score = Math.max(0, currentTotal - currentFinal);
        }

        // 保存到文件
        StorageManager.getInstance().saveGame(gameSaveData);
        Logger.info("Game progress saved: Level=" + currentLevel + ", Score=" + gameSaveData.score);
    }

    /**
     * 获取关卡结算结果
     */
    public LevelResult getLevelResult() {
        if (scoreManager == null) {
            return new LevelResult(0, 0, 0, "D", 0, 1.0f);
        }

        // 计算理论最大分数（基于敌人数量和道具数量）
        int theoreticalMaxScore = calculateTheoreticalMaxScore();

        return scoreManager.calculateResult(theoreticalMaxScore);
    }

    /**
     * 计算理论最大分数
     */
    private int calculateTheoreticalMaxScore() {
        int maxScore = 0;

        // 敌人分数
        maxScore += difficultyConfig.enemyE01PearlCount * 100;
        maxScore += difficultyConfig.enemyE02CoffeeBeanCount * 200;
        maxScore += difficultyConfig.enemyE03CaramelCount * 300;
        maxScore += difficultyConfig.enemyE04ShellCount * 500;

        // 道具分数
        maxScore += 10 * 50;  // hearts
        maxScore += 3 * 100;  // treasures
        maxScore += difficultyConfig.keyCount * 200;  // keys

        return maxScore;
    }

    /**
     * 获取游戏存档数据
     */
    public GameSaveData getGameSaveData() {
        return gameSaveData;
    }

    /**
     * 获取分数管理器
     */
    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    /**
     * 获取成就管理器
     */
    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    /**
     * 从存档恢复游戏状态
     */
    public void restoreFromSaveData(GameSaveData saveData) {
        if (saveData == null) return;

        this.gameSaveData = saveData;
        this.currentLevel = saveData.currentLevel;

        // 恢复分数管理器状态
        if (scoreManager != null) {
            scoreManager.restoreState(saveData);
        }

        // 恢复玩家状态（需要在玩家创建后调用）
        if (player != null) {
            // 恢复生命值：先计算需要回复的量
            int targetLives = saveData.lives > 0 ? saveData.lives : difficultyConfig.initialLives;
            int currentLives = player.getLives();
            if (targetLives > currentLives) {
                player.heal(targetLives - currentLives);
            }
            player.setHasKey(saveData.hasKey);
            if (saveData.buffAttack) player.activateAttackBuff();
            if (saveData.buffRegen) player.activateRegenBuff();
            if (saveData.buffManaEfficiency) player.activateManaBuff();
        }

        Logger.info("Game state restored: Level=" + currentLevel + ", Score=" + saveData.score);
    }

    /**
     * 获取道具特效管理器
     */
    public ItemEffectManager getItemEffectManager() {
        return itemEffectManager;
    }

    /**
     * 获取陷阱特效管理器
     */
    public TrapEffectManager getTrapEffectManager() {
        return trapEffectManager;
    }

    /**
     * 获取战斗特效管理器
     */
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
    // 🔥 [HP-UP] 掉落判定逻辑
    private void handleEnemyDrop(Enemy enemy) {
        // 33% 概率
        if (Math.random() < 0.33) {
            int x = enemy.getX();
            int y = enemy.getY();

            // 创建道具
            HeartContainer container = new HeartContainer(x, y);

            // 加入管理列表
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


}

