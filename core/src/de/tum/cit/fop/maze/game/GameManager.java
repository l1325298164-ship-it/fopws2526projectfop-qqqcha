package de.tum.cit.fop.maze.game;

import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.effects.Enemy.boba.BobaBulletManager;
import de.tum.cit.fop.maze.effects.environment.items.ItemEffectManager;
import de.tum.cit.fop.maze.effects.environment.items.traps.TrapEffectManager;
import de.tum.cit.fop.maze.effects.environment.portal.PortalEffectManager;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.effects.boba.BobaBulletManager;
import de.tum.cit.fop.maze.effects.fog.FogSystem;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffectManager;
import de.tum.cit.fop.maze.effects.key.KeyEffectManager;
import de.tum.cit.fop.maze.effects.portal.PortalEffectManager;
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
import de.tum.cit.fop.maze.game.score.ScoreManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeGenerator;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.StorageManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import static com.badlogic.gdx.math.MathUtils.random;
import static de.tum.cit.fop.maze.maze.MazeGenerator.BORDER_THICKNESS;

public class GameManager implements PlayerInputHandler.InputHandlerCallback {
    private final DifficultyConfig difficultyConfig;
    private float debugTimer = 0f;

    // ===== Endless Co-op Revive =====
    private static final float REVIVE_DELAY = 10f;
    // ===== 双人复活系统 =====

    // ✨ [新增] 自动保存计时器
    private float autoSaveTimer = 0f;
    private static final float AUTO_SAVE_INTERVAL = 30.0f; // 每30秒自动保存一次

    private boolean revivePending = false;
    private float reviveTimer = 0f;
    public DifficultyConfig getDifficultyConfig() { return difficultyConfig; }

    public DifficultyConfig getDifficultyConfig() {
        return difficultyConfig;
    }
    private int[][] maze;
    private List<Player> players = new ArrayList<>();
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
    // ===== 鼠标目标格子（给技能用）=====
    private int mouseTileX = -1;
    private int mouseTileY = -1;
    // GameManager.java
    private FogSystem fogSystem;
    private Compass compass;
    private MazeGenerator generator = new MazeGenerator();
    private KeyEffectManager keyEffectManager;
    private PlayerInputHandler inputHandler;

    private ItemEffectManager itemEffectManager;
    private TrapEffectManager trapEffectManager;
    private CombatEffectManager combatEffectManager;
    private BobaBulletManager bobaBulletEffectManager = new BobaBulletManager();

    private ScoreManager scoreManager;
    // ✨ [集成] 成就管理器
    private AchievementManager achievementManager;
    private GameSaveData gameSaveData;

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
    private static final float LEVEL_TRANSITION_DELAY = 0.5f;

    // ✨ [新增] 关卡完成标志，用于 GameScreen 跳转到结算界面
    private boolean levelCompletedPendingSettlement = false;
    private static final float LEVEL_TRANSITION_DELAY = 0.5f; // 动画完成后延迟0.5秒

    private int currentLevel = 1;

    //effect to player
    private PortalEffectManager playerSpawnPortal;
//    private final MazeRunnerGame game;

    public GameManager(DifficultyConfig difficultyConfig) {
    /* ================= 生命周期 ================= */
    public GameManager(DifficultyConfig difficultyConfig, boolean twoPlayerMode) {
        this.inputHandler = new PlayerInputHandler();
        if (difficultyConfig == null) {
            throw new IllegalArgumentException("difficultyConfig must not be null");
        }
        this.difficultyConfig = difficultyConfig;

        // ✨ [重要] 先清理旧的监听器，防止监听器泄漏
        GameEventSource eventSource = GameEventSource.getInstance();
        eventSource.clearListeners();

        // ✨ [集成] 初始化成就系统与事件监听
        this.gameSaveData = new GameSaveData(); // 基础会话数据
        this.scoreManager = new ScoreManager(difficultyConfig);

        StorageManager storageManager = StorageManager.getInstance();
        CareerData careerData = storageManager.loadCareer();
        this.achievementManager = new AchievementManager(
                careerData,
                this.gameSaveData,
                storageManager,
                difficultyConfig.difficulty
        );

        // 注册到全局事件源（实现自动分发）
        eventSource.addListener(this.scoreManager);
        eventSource.addListener(this.achievementManager);

        this.twoPlayerMode = twoPlayerMode;
        resetGame();
    }

    /**
     * ✨ [新增] 清理资源，移除监听器
     * 在 GameManager 不再使用时调用
     */
    public void dispose() {
        // 1. 清理事件监听器
        GameEventSource eventSource = GameEventSource.getInstance();
        if (scoreManager != null) {
            eventSource.removeListener(scoreManager);
        }
        if (achievementManager != null) {
            eventSource.removeListener(achievementManager);
            // 保存未保存的成就数据
            achievementManager.saveIfNeeded();
        }

        // 2. 清理特效管理器
        if (itemEffectManager != null) itemEffectManager.dispose();
        if (trapEffectManager != null) trapEffectManager.dispose();
        if (combatEffectManager != null) combatEffectManager.dispose();
        if (players != null) for (Player p : players) p.dispose();
        for (ExitDoor door : exitDoors) door.dispose();
        for (Treasure t : treasures) t.dispose();
        if (bobaBulletEffectManager != null) bobaBulletEffectManager.dispose();
        if (playerSpawnPortal != null) playerSpawnPortal.dispose();

        // 3. 确保所有异步保存完成
        StorageManager.getInstance().flushAllSaves();
        Logger.info("GameManager disposed, listeners cleaned up");
    }

    /**
     * ✨ [新增] 保存游戏进度（整合所有保存逻辑）
     * 用于自动保存和手动保存
     */
    public void saveGameProgress() {
        if (gameSaveData == null || player == null) return;

        // 同步最新状态到存档数据
        gameSaveData.currentLevel = currentLevel;
        gameSaveData.lives = player.getLives();
        gameSaveData.maxLives = player.getMaxLives();
        gameSaveData.mana = (int) player.getMana();
        gameSaveData.hasKey = player.hasKey();
        gameSaveData.buffAttack = player.hasBuffAttack();
        gameSaveData.buffRegen = player.hasBuffRegen();
        gameSaveData.buffManaEfficiency = player.hasBuffManaEfficiency();

        // 同步分数管理器状态
        if (scoreManager != null) {
            scoreManager.saveState(gameSaveData);
            // ✨ [修复] 使用 saveData.score（已在 SettlementScreen 中累加），而不是 getCurrentScore()
            // 因为 getCurrentScore() 返回的是 accumulatedScore + currentLevelFinal，
            // 但 accumulatedScore 在关卡结束时还未更新，所以使用已累加的 saveData.score
            // gameSaveData.score 已经在 SettlementScreen 中正确累加，这里不需要覆盖
        }

        // 保存难度配置
        if (difficultyConfig != null && difficultyConfig.difficulty != null) {
            gameSaveData.difficulty = difficultyConfig.difficulty.name();
        }

        // 异步保存（不阻塞主线程）
        StorageManager.getInstance().saveGame(gameSaveData);
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
        heartContainers.clear();
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
        Player p1 = new Player(spawn1[0],spawn1[1],this,Player.PlayerIndex.P1);
        players.add(p1);
        Player p1 = new Player(
                spawn1[0],spawn1[1],this,Player.PlayerIndex.P1
        );players.add(p1);


        if (twoPlayerMode) {
            int[] spawn2 = findNearbySpawn(p1);
            Player p2 = new Player(spawn2[0], spawn2[1], this, Player.PlayerIndex.P2);

            Player p2 = new Player(
                    spawn2[0],
                    spawn2[1],
                    this,
                    Player.PlayerIndex.P2
            );
            players.add(p2);

            Logger.gameEvent(
                    "P2 spawned near P1 at (" + spawn2[0] + ", " + spawn2[1] + ")"
            );

            // ===== Reset revive system =====
            revivePending = false;
            reviveTimer = 0f;
        }

// 🔥 关键：同步旧 player 引用
        syncSinglePlayerRef();

        cat = null;

        cat = null;  // 默认没有小猫
        if (difficultyConfig.difficulty == Difficulty.HARD) {
            fogSystem = new FogSystem();
        } else {
            fogSystem = null;
        }

        // ✨ [修复] 添加空值检查，防止 player 为 null
        if (player == null) {
            Logger.error("Player is null after resetGame, cannot create spawn portal");
            return;
        }

        // 🔥 玩家出生传送阵（一次性）
        float px = player.getX() * GameConstants.CELL_SIZE;
        float py = player.getY() * GameConstants.CELL_SIZE;


        playerSpawnPortal = new PortalEffectManager(PortalEffectManager.PortalOwner.PLAYER);
        playerSpawnPortal.startPlayerSpawnEffect(px, py);
        obstacles = new ArrayList<>();

        generateLevel();

        // ✨ [修复] 添加空值检查
        if (player != null) {
            compass = new Compass(player);
        } else {
            Logger.error("Player is null, cannot create Compass");
        }

        // ✨ [修复] 重置本关分数统计（新关卡开始时）
        if (scoreManager != null) {
            scoreManager.reset();
        }
        bullets.clear();
        bobaBulletEffectManager.clearAllBullets(false);
        keyEffectManager = new KeyEffectManager();

        itemEffectManager = new ItemEffectManager();
        trapEffectManager = new TrapEffectManager();
        combatEffectManager = new CombatEffectManager();

        // 🔥 重置动画状态
        levelTransitionInProgress = false;
        currentExitDoor = null;
        levelTransitionTimer = 0f;

        Logger.gameEvent("Game reset complete");
    }

    // ✨ [集成] 供 HUD 使用的 Getter
    public AchievementManager getAchievementManager() {
        return achievementManager;
    }

    public void restoreState(GameSaveData data) {
        if (data == null) return;

        this.gameSaveData = data;
        this.currentLevel = data.currentLevel;

        // ✨ [修复] 恢复状态后，需要重新生成对应关卡的迷宫和内容
        // 因为迷宫是随机生成的，需要确保读档后生成的是对应关卡的新迷宫
        resetGame();

        // 重新恢复玩家状态（因为resetGame()会重置玩家）
        if (player != null) {
            player.setLives(data.lives);
            player.setMaxLives(data.maxLives);
            player.setMana(data.mana);
            player.setHasKey(data.hasKey);

            if (data.buffAttack) player.applyAttackBuff(9999f);
            if (data.buffRegen) player.applyRegenBuff(9999f);
            if (data.buffManaEfficiency) player.applyManaEfficiencyBuff(9999f);
        }

        if (scoreManager != null) {
            scoreManager.restoreState(data);
        }

        Logger.info("Game State Restored: Level " + currentLevel + ", Score " + data.score);
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
        for (Player p : players) {
            if (!p.isDead()) {
                p.update(delta);
            }
        }

        updateEndlessRevive(delta);

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
                if (e.isDead()) {
                    EnemyTier tier = EnemyTier.E01;
                    if (e instanceof EnemyE01_CorruptedPearl) tier = EnemyTier.E01;
                    else if (e instanceof EnemyE02_SmallCoffeeBean) tier = EnemyTier.E02;
                    else if (e instanceof EnemyE03_CaramelJuggernaut) tier = EnemyTier.E03;
                    else if (e instanceof EnemyE04_CrystallizedCaramelShell) tier = EnemyTier.E04;

                    // 使用事件源通知监听器
                    GameEventSource.getInstance().onEnemyKilled(tier, e.isHitByDash());
                if (e.isDead() && e instanceof EnemyE04_CrystallizedCaramelShell) {
                    handleEnemyDrop(e);
                }
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

        if (itemEffectManager != null) itemEffectManager.update(delta);
        if (trapEffectManager != null) trapEffectManager.update(delta);
        if (combatEffectManager != null) combatEffectManager.update(delta);

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

        debugTimer += delta;
        if (debugTimer >= 2.0f) {
            debugEnemiesAndBullets();
            debugTimer = 0f;
        }

        // ✨ [新增] 自动保存逻辑
        autoSaveTimer += delta;
        if (autoSaveTimer >= AUTO_SAVE_INTERVAL) {
            autoSaveTimer = 0f;
            // 只在游戏进行中时自动保存（不在关卡过渡、暂停等状态）
            if (!levelTransitionInProgress && player != null && !player.isDead()) {
                saveGameProgress();
                Logger.debug("Auto-save triggered (every " + AUTO_SAVE_INTERVAL + "s)");
            }
        }
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

        // 双死 → 不处理（EndlessScreen 会 GameOver）
        if (p1Dead && p2Dead) {
            revivePending = false;
            reviveTimer = 0f;
            return;
        }

        // 一死一活
        if (p1Dead ^ p2Dead) {
            Player alive = p1Dead ? p2 : p1;
            Player dead  = p1Dead ? p1 : p2;

            // 🔥 如果复活对象发生变化，重置计时
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
        }else {
            // 🔥 都活着 或 都死 → 清状态
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
        dead.reviveAt(
                spawn[0],
                spawn[1],
                10
        );

        Logger.gameEvent(
                "Revived " + dead.getPlayerIndex() + " near " + alive.getPlayerIndex()
        );
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


    private void handlePlayerTrapInteraction() {
        if (levelTransitionInProgress || player == null || player.isDead()) return;
        int px = player.getX();
        int py = player.getY();
        if (levelTransitionInProgress) return;

        for (Player p : players) {
            if (p == null || p.isDead()) continue;

            int px = p.getX();
            int py = p.getY();

            for (Trap trap : traps) {
                if (!trap.isActive()) continue;

            if (trap.getX() == px && trap.getY() == py) {
                int livesBefore = player.getLives();
                trap.onPlayerStep(player);
                int damage = livesBefore - player.getLives(); // 计算实际伤害

                DamageSource source = DamageSource.UNKNOWN;
                if (trap instanceof TrapT01_Geyser) source = DamageSource.TRAP_GEYSER;
                else if (trap instanceof TrapT02_PearlMine) source = DamageSource.TRAP_MINE;
                else if (trap instanceof TrapT03_TeaShards) source = DamageSource.TRAP_SPIKE;
                else if (trap instanceof TrapT04_Mud) source = DamageSource.TRAP_MUD;

                if (source != DamageSource.UNKNOWN && damage > 0) {
                    // 使用事件源通知监听器
                    GameEventSource.getInstance().onPlayerDamage(player.getLives(), source);

                    // ✨ 显示伤害通知消息和扣分提示
                    if (scoreManager != null && difficultyConfig != null) {
                        int penalty = (int) (source.penaltyScore * difficultyConfig.penaltyMultiplier);
                        player.showNotification("Trap Damage! -" + penalty + " pts (" + damage + " HP)");
                    } else {
                        player.showNotification("Trap Damage! -" + damage + " HP");
                    }
                }

                float effectX = (trap.getX() + 0.5f) * GameConstants.CELL_SIZE;
                float effectY = (trap.getY() + 0.5f) * GameConstants.CELL_SIZE;

                if (trapEffectManager != null) {
                    if (trap instanceof TrapT01_Geyser) trapEffectManager.spawnGeyser(effectX, effectY);
                    else if (trap instanceof TrapT02_PearlMine) trapEffectManager.spawnPearlMine(effectX, effectY);
                    else if (trap instanceof TrapT03_TeaShards) trapEffectManager.spawnTeaShards(effectX, effectY);
                    else if (trap instanceof TrapT04_Mud) trapEffectManager.spawnMudTrap(effectX, effectY);
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

    // 🔥 新增：检查玩家是否到达出口
    private void checkExitReached() {

        if (levelTransitionInProgress) return;

        // ===== 单人模式：保持原逻辑 =====
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

        // ===== 双人模式：两人必须同时在门上 =====
        Player p1 = getPlayerByIndex(Player.PlayerIndex.P1);
        Player p2 = getPlayerByIndex(Player.PlayerIndex.P2);

        if (p1 == null || p2 == null) return;
        if (p1.isDead() || p2.isDead()) return;

        for (ExitDoor door : exitDoors) {
            if (!door.isLocked() || !door.isActive()) continue;

            boolean p1OnDoor =
                    p1.getX() == door.getX() &&
                            p1.getY() == door.getY();

            boolean p2OnDoor =
                    p2.getX() == door.getX() &&
                            p2.getY() == door.getY();

            if (p1OnDoor && p2OnDoor) {
                // ⭐ 用 P1 触发即可（动画/逻辑只需要一次）
                door.onPlayerStep(p1);
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
        // 使用事件源通知监听器
        GameEventSource.getInstance().onLevelFinished(currentLevel);

        // 可选：禁用玩家输入
        Logger.gameEvent("Level transition started at door " + door.getPositionString());
    }

    public void nextLevel() {
        // ✨ [修改] 不再直接 reset，而是标记等待结算界面处理
        levelCompletedPendingSettlement = true;
        Logger.gameEvent("Level " + currentLevel + " completed, pending settlement screen");
    }

    /**
     * ✨ [新增] 检查是否有待结算的关卡
     */
    public boolean isLevelCompletedPendingSettlement() {
        return levelCompletedPendingSettlement;
    }

    /**
     * ✨ [新增] 计算理论最高基础分
     * 根据关卡中实际存在的敌人、物品等计算理论最高分
     */
    public int calculateTheoreticalMaxBaseScore() {
        int maxScore = 0;

        // 1. 计算所有敌人的击杀分数
        for (Enemy enemy : enemies) {
            if (enemy == null || enemy.isDead()) continue;

            if (enemy instanceof EnemyE01_CorruptedPearl) {
                maxScore += de.tum.cit.fop.maze.game.score.ScoreConstants.SCORE_E01_PEARL;
            } else if (enemy instanceof EnemyE02_SmallCoffeeBean) {
                maxScore += de.tum.cit.fop.maze.game.score.ScoreConstants.SCORE_E02_COFFEE;
            } else if (enemy instanceof EnemyE03_CaramelJuggernaut) {
                maxScore += de.tum.cit.fop.maze.game.score.ScoreConstants.SCORE_E03_CARAMEL;
            } else if (enemy instanceof EnemyE04_CrystallizedCaramelShell) {
                maxScore += de.tum.cit.fop.maze.game.score.ScoreConstants.SCORE_E04_SHELL;
            }
            // Boss敌人：目前游戏中可能没有Boss敌人，如果将来添加，需要根据实际的Boss类名来检查
            // 例如：else if (enemy instanceof EnemyBoss) { maxScore += SCORE_BOSS; }
        }

        // 2. 计算所有物品的收集分数
        // 心/波霸
        maxScore += hearts.size() * de.tum.cit.fop.maze.game.score.ScoreConstants.SCORE_HEART;

        // 宝藏
        maxScore += treasures.size() * de.tum.cit.fop.maze.game.score.ScoreConstants.SCORE_TREASURE;

        // 钥匙
        maxScore += keys.size() * de.tum.cit.fop.maze.game.score.ScoreConstants.SCORE_KEY;

        // 3. 迷雾清除分数（如果迷雾系统存在且激活）
        if (fogSystem != null && fogSystem.isActive()) {
            maxScore += de.tum.cit.fop.maze.game.score.ScoreConstants.SCORE_FOG_CLEARED;
        }

        // 4. 如果计算出的分数为0，使用基于难度配置的估算值作为后备
        if (maxScore == 0) {
            // 基于DifficultyConfig中的敌人数量估算
            maxScore = difficultyConfig.enemyE01PearlCount * de.tum.cit.fop.maze.game.score.ScoreConstants.SCORE_E01_PEARL
                    + difficultyConfig.enemyE02CoffeeBeanCount * de.tum.cit.fop.maze.game.score.ScoreConstants.SCORE_E02_COFFEE
                    + difficultyConfig.enemyE03CaramelCount * de.tum.cit.fop.maze.game.score.ScoreConstants.SCORE_E03_CARAMEL
                    + difficultyConfig.enemyE04ShellCount * de.tum.cit.fop.maze.game.score.ScoreConstants.SCORE_E04_SHELL
                    + difficultyConfig.keyCount * de.tum.cit.fop.maze.game.score.ScoreConstants.SCORE_KEY;
        }

        return maxScore;
    }

    /**
     * ✨ [新增] 获取关卡结算结果
     * 自动计算理论最高基础分
     */
    public LevelResult getLevelResult() {
        if (scoreManager == null) return null;
        int theoreticalMaxBaseScore = calculateTheoreticalMaxBaseScore();
        return scoreManager.calculateResult(theoreticalMaxBaseScore);
    }

    /**
     * ✨ [保留] 获取关卡结算结果（兼容旧代码）
     * @param theoreticalMaxBaseScore 理论最高基础分（如果传入0或负数，将自动计算）
     */
    public LevelResult getLevelResult(int theoreticalMaxBaseScore) {
        if (scoreManager == null) return null;
        if (theoreticalMaxBaseScore <= 0) {
            theoreticalMaxBaseScore = calculateTheoreticalMaxBaseScore();
        }
        return scoreManager.calculateResult(theoreticalMaxBaseScore);
    }

    /**
     * ✨ [新增] 获取当前游戏存档数据（用于传递给结算界面）
     */
    public GameSaveData getGameSaveData() {
        // 在返回前，同步最新的玩家状态到存档数据
        if (gameSaveData != null && player != null) {
            gameSaveData.currentLevel = currentLevel;
            gameSaveData.lives = player.getLives();
            gameSaveData.maxLives = player.getMaxLives();
            gameSaveData.mana = (int) player.getMana();
            gameSaveData.hasKey = player.hasKey();
            gameSaveData.buffAttack = player.hasBuffAttack();
            gameSaveData.buffRegen = player.hasBuffRegen();
            gameSaveData.buffManaEfficiency = player.hasBuffManaEfficiency();
            // ✨ [新增] 保存难度配置
            if (difficultyConfig != null && difficultyConfig.difficulty != null) {
                gameSaveData.difficulty = difficultyConfig.difficulty.name();
            }
        }
        return gameSaveData;
    }

    /**
     * ✨ [新增] 清除关卡完成标志（结算界面处理后调用）
     */
    public void clearLevelCompletedFlag() {
        levelCompletedPendingSettlement = false;
    }

    /**
     * ✨ [新增] 进入下一关（从结算界面调用）
     */
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

    public void onKeyCollected() {
        player.setHasKey(true);
        unlockAllExitDoors();
        // 使用事件源通知监听器
        GameEventSource.getInstance().onItemCollected("KEY");
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
        // ✨ [修复] 性能优化：避免重复调用randomEmptyCell()
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
        // ✨ [修复] 性能优化：避免重复调用randomEmptyCell()
        for (int i = 0; i < 10; i++) {
            int[] pos = randomEmptyCell();
            hearts.add(new Heart(pos[0], pos[1]));
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
    public void onMoveInput(Player.PlayerIndex index, int dx, int dy) {
        Player p = getPlayerByIndex(index);
        if (p == null) return;

        // 无论能不能走，先更新朝向
        p.updateDirection(dx, dy);

        int nx = p.getX() + dx;
        int ny = p.getY() + dy;

        if (canPlayerMoveTo(nx, ny)) {
            p.move(dx, dy);
        } else {
            Logger.debug("Player " + index + " blocked at (" + nx + "," + ny + ")");
        }
    }


    private Player getPlayerByIndex(Player.PlayerIndex index) {
        for (Player p : players) {
            if (p.getPlayerIndex() == index) {
                return p;
            }
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
    public void onMenuInput() {

    }

    private void checkAutoPickup() {
        if (levelTransitionInProgress) return;

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
        for (Player p : players) {
            if (p == null || p.isDead()) continue;

            int px = p.getX();
            int py = p.getY();

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

                    key.onInteract(p);
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
                if (itemEffectManager != null) itemEffectManager.spawnHeart(effectX, effectY);
                h.onInteract(player);

                // 使用事件源通知监听器
                GameEventSource.getInstance().onItemCollected("HEART");

                // ✨ 显示通知消息和分数提示
                if (scoreManager != null) {
                    int scoreBefore = scoreManager.getCurrentScore();
                    // 等待一帧让事件处理完成
                    player.showNotification("Heart Collected! +" + de.tum.cit.fop.maze.game.score.ScoreConstants.SCORE_HEART + " pts");
                } else {
                    player.showNotification("Heart Collected! +50 pts");
                }

                heartIterator.remove();
            }
        }
            // ===== 爱心 =====
            Iterator<Heart> heartIterator = hearts.iterator();
            while (heartIterator.hasNext()) {
                Heart h = heartIterator.next();
                if (h.isActive() && h.getX() == px && h.getY() == py) {
                    h.onInteract(p);
                    heartIterator.remove();
                }
            }

        Iterator<Treasure> treasureIterator = treasures.iterator();
        while (treasureIterator.hasNext()) {
            Treasure t = treasureIterator.next();
            if (t.isInteractable() && t.getX() == px && t.getY() == py) {
                float effectX = (t.getX() + 0.5f) * GameConstants.CELL_SIZE;
                float effectY = (t.getY() + 0.5f) * GameConstants.CELL_SIZE;
                if (itemEffectManager != null) itemEffectManager.spawnTreasure(effectX, effectY);
                t.onInteract(player);

                // 使用事件源通知监听器
                GameEventSource.getInstance().onItemCollected("TREASURE");

                // ✨ 追加分数提示到宝箱通知消息
                if (scoreManager != null) {
                    player.appendNotification("Treasure Collected! +" + de.tum.cit.fop.maze.game.score.ScoreConstants.SCORE_TREASURE + " pts");
                } else {
                    player.appendNotification("Treasure Collected! +800 pts");
            // ===== 宝箱 =====
            for (Treasure t : treasures) {
                if (t.isInteractable() && t.getX() == px && t.getY() == py) {
                    t.onInteract(p);
                }
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

    // GameManager.java
    private BobaBulletManager bobaBulletEffectManager = new BobaBulletManager();
    public BobaBulletManager getBobaBulletEffectManager() {
        return bobaBulletEffectManager;
    }

    private void handlePlayerEnemyCollision() {
        if (levelTransitionInProgress) return;

        for (Player p : players) {
            if (p == null || p.isDead()) continue;
            if (p.isDashInvincible()) continue;

            for (Enemy enemy : enemies) {
                if (enemy == null || !enemy.isActive() || enemy.isDead()) continue;

                boolean hit = false;

                // 🔥 连续移动敌人（E02）
                if (enemy instanceof EnemyE02_SmallCoffeeBean e02) {

                DamageSource source = DamageSource.UNKNOWN;
                if (enemy instanceof EnemyE01_CorruptedPearl) source = DamageSource.ENEMY_E01;
                else if (enemy instanceof EnemyE02_SmallCoffeeBean) source = DamageSource.ENEMY_E02;
                else if (enemy instanceof EnemyE03_CaramelJuggernaut) source = DamageSource.ENEMY_E03;
                else if (enemy instanceof EnemyE04_CrystallizedCaramelShell) source = DamageSource.ENEMY_E04;
                    // 玩家中心 vs 敌人 world 坐标
                    float px = p.getX() + 0.5f;
                    float py = p.getY() + 0.5f;

                if (source != DamageSource.UNKNOWN) {
                    // 使用事件源通知监听器
                    GameEventSource.getInstance().onPlayerDamage(player.getLives(), source);

                    // ✨ 显示伤害通知消息和扣分提示
                    if (scoreManager != null && difficultyConfig != null) {
                        int penalty = (int) (source.penaltyScore * difficultyConfig.penaltyMultiplier);
                        player.showNotification("Enemy Damage! -" + penalty + " pts (" + damage + " HP)");
                    } else {
                        player.showNotification("Enemy Damage! -" + damage + " HP");
                    }
                    float dx = px - e02.getWorldX();
                    float dy = py - e02.getWorldY();

                    float radius = 0.6f; // ⭐ 可调，0.5~0.7 都行
                    hit = (dx * dx + dy * dy) <= radius * radius;

                } else {
                    // 🔹 原有格子敌人逻辑（E01 / E03 / E04）
                    hit = (enemy.getX() == p.getX() &&
                            enemy.getY() == p.getY());
                }

                if (hit) {
                    p.takeDamage(enemy.getCollisionDamage());
                }
            }
        }
    }

    private void handleDashHitEnemies() {
        if (levelTransitionInProgress) return;

        for (Player p : players) {
            if (p == null || !p.isDashing()) continue;

            // 玩家中心（连续坐标）
            float px = p.getWorldX() + 0.5f;
            float py = p.getWorldY() + 0.5f;

            for (Enemy enemy : enemies) {
                if (enemy == null || !enemy.isActive() || enemy.isDead()) continue;

                boolean hit = false;

                // ===============================
                // 🔥 E02：连续移动敌人（圆形判定）
                // ===============================
                if (enemy instanceof EnemyE02_SmallCoffeeBean e02) {
                    float dx = px - e02.getWorldX();
                    float dy = py - e02.getWorldY();
                    float radius = 0.7f; // Dash 命中宽容度
                    hit = (dx * dx + dy * dy) <= radius * radius;
                }

                // ===============================
                // 🔥 E04：2x2 占格敌人
                // ===============================
                else if (enemy instanceof EnemyE04_CrystallizedCaramelShell shell) {
                    int cx = (int) px;
                    int cy = (int) py;
                    hit = shell.occupiesCell(cx, cy);
                }

                // ===============================
                // 🔹 其他普通 1x1 敌人
                // ===============================
                else {
                    hit = (enemy.getX() == (int) px &&
                            enemy.getY() == (int) py);
                }

                if (hit) {
                    // ⭐ 顺序非常重要
                    enemy.markHitByDash();
                    enemy.takeDamage(2);
                }
            }
        }
    }


    public PortalEffectManager getPlayerSpawnPortal() { return playerSpawnPortal; }
    public ItemEffectManager getItemEffectManager() { return itemEffectManager; }
    public TrapEffectManager getTrapEffectManager() { return trapEffectManager; }
    public CombatEffectManager getCombatEffectManager() { return combatEffectManager; }


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

    public int getScore() {
        return scoreManager != null ? scoreManager.getCurrentScore() : 0;

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
    public ScoreManager getScoreManager() { return scoreManager; }
    public PlayerInputHandler getInputHandler() { return  inputHandler; }
    public boolean isPlayerDead() { return player != null && player.isDead(); }

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
    private void syncSinglePlayerRef() {
        if (!players.isEmpty()) {
            player = players.get(0); // P1 永远是主玩家
        } else {
            player = null;
        }
    }
    public boolean isTwoPlayerMode() {
        return twoPlayerMode;
    }

    public List<Player> getPlayers() {
        return players;
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