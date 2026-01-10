package de.tum.cit.fop.maze.game;

import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.effects.Enemy.boba.BobaBulletManager;
import de.tum.cit.fop.maze.effects.environment.items.ItemEffectManager;
import de.tum.cit.fop.maze.effects.environment.items.traps.TrapEffectManager;
import de.tum.cit.fop.maze.effects.environment.portal.PortalEffectManager;
import de.tum.cit.fop.maze.effects.fog.FogSystem;
import de.tum.cit.fop.maze.effects.Player.combat.CombatEffectManager;
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
import java.util.Map;

import static com.badlogic.gdx.math.MathUtils.random;
import static de.tum.cit.fop.maze.maze.MazeGenerator.BORDER_THICKNESS;

public class GameManager implements PlayerInputHandler.InputHandlerCallback {
    private final DifficultyConfig difficultyConfig;
    private float debugTimer = 0f;
    
    // ✨ [新增] 自动保存计时器
    private float autoSaveTimer = 0f;
    private static final float AUTO_SAVE_INTERVAL = 30.0f; // 每30秒自动保存一次

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

    private ItemEffectManager itemEffectManager;
    private TrapEffectManager trapEffectManager;
    private CombatEffectManager combatEffectManager;
    private BobaBulletManager bobaBulletEffectManager = new BobaBulletManager();

    private ScoreManager scoreManager;
    // ✨ [集成] 成就管理器
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
    
    // ✨ [新增] 关卡完成标志，用于 GameScreen 跳转到结算界面
    private boolean levelCompletedPendingSettlement = false;

    private int currentLevel = 1;
    private PortalEffectManager playerSpawnPortal;

    public GameManager(DifficultyConfig difficultyConfig) {
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
        gameVariables.put("speed_mult", 1.0f);
        gameVariables.put("dmg_taken", 1.0f);
        gameVariables.put("cam_zoom", 1.0f);
        gameVariables.put("time_scale", 1.0f);

        maze = generator.generateMaze(difficultyConfig);

        enemies.clear();
        traps.clear();
        hearts.clear();
        treasures.clear();
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

        if (twoPlayerMode) {
            int[] spawn2 = findNearbySpawn(p1);
            Player p2 = new Player(spawn2[0], spawn2[1], this, Player.PlayerIndex.P2);
            players.add(p2);
        }

        syncSinglePlayerRef();

        cat = null;
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

        itemEffectManager = new ItemEffectManager();
        trapEffectManager = new TrapEffectManager();
        combatEffectManager = new CombatEffectManager();

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
        if (data == null) {
            Logger.warning("GameManager.restoreState: data is null, skipping restore");
            return;
        }

        Logger.info("GameManager.restoreState: Starting restore, Level=" + data.currentLevel + ", Score=" + data.score);

        this.gameSaveData = data;
        this.currentLevel = data.currentLevel;

        // ✨ [修复] 先保存分数状态，因为 resetGame() 会重置它
        int currentScoreBeforeReset = 0;
        if (scoreManager != null) {
            currentScoreBeforeReset = scoreManager.getCurrentScore();
            Logger.info("GameManager.restoreState: Current score before resetGame: " + currentScoreBeforeReset);
        }

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

        // ✨ [修复] 在 resetGame() 之后恢复分数状态
        // resetGame() 会调用 scoreManager.reset()，重置 levelBaseScore = 0, levelPenalty = 0
        // 所以需要在 resetGame() 之后恢复分数状态，使用存档数据中的分数
        if (scoreManager != null) {
            scoreManager.restoreState(data);
            Logger.info("GameManager.restoreState: Restored score state after resetGame: accumulated=" + scoreManager.accumulatedScore + ", levelBase=" + scoreManager.levelBaseScore);
        }

        Logger.info("Game State Restored: Level " + currentLevel + ", Score " + data.score);
    }

    private int[] findNearbySpawn(Player p1) {
        int px = p1.getX();
        int py = p1.getY();
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

    public void debugEnemiesAndBullets() { }

    public void update(float delta) {
        inputHandler.update(delta, this, Player.PlayerIndex.P1);
        if (twoPlayerMode) inputHandler.update(delta, this, Player.PlayerIndex.P2);

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

        for (Player p : players) if (!p.isDead()) p.update(delta);

        boolean fogOn = fogSystem != null && fogSystem.isActive();
        if (difficultyConfig.difficulty == Difficulty.HARD) {
            if (fogOn) {
                if (cat == null) cat = new CatFollower(player, this);
                cat.update(delta);
            } else { cat = null; }
        } else { cat = null; }

        if (fogSystem != null) fogSystem.update(delta);

        for (Trap trap : traps) if (trap.isActive()) trap.update(delta);

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
                }
                enemyIterator.remove();
            }
        }

        for (ExitDoor door : exitDoors) door.update(delta, this);
        checkExitReached();
        updateCompass();
        updateBullets(delta);
        for (DynamicObstacle o : obstacles) o.update(delta, this);

        bobaBulletEffectManager.addBullets(bullets);
        bobaBulletEffectManager.update(delta);

        handlePlayerEnemyCollision();
        handleDashHitEnemies();
        checkAutoPickup();

        if (itemEffectManager != null) itemEffectManager.update(delta);
        if (trapEffectManager != null) trapEffectManager.update(delta);
        if (combatEffectManager != null) combatEffectManager.update(delta);

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

    private void handlePlayerTrapInteraction() {
        if (levelTransitionInProgress || player == null || player.isDead()) return;
        int px = player.getX();
        int py = player.getY();

        Iterator<Trap> it = traps.iterator();
        while (it.hasNext()) {
            Trap trap = it.next();
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
        // 使用事件源通知监听器
        GameEventSource.getInstance().onLevelFinished(currentLevel);
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

    public void requestReset() { pendingReset = true; }

    public void onKeyCollected() {
        player.setHasKey(true);
        unlockAllExitDoors();
        // 使用事件源通知监听器
        GameEventSource.getInstance().onItemCollected("KEY");
        Logger.gameEvent("All exits unlocked");
    }

    private void unlockAllExitDoors() {
        for (ExitDoor door : exitDoors) if (door.isLocked()) door.unlock();
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
        for (ExitDoor door : exitDoors) if (door.getX() == x && door.getY() == y) return true;
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
    }

    private boolean isWalkableLine(int sx, int sy, int x2, int y2) {
        if (sy != y2) return false;
        for (int x = sx; x <= x2; x++) if (maze[sy][x] != 1) return false;
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
        if (!possibleDirections.isEmpty()) return possibleDirections.get(random.nextInt(possibleDirections.size()));
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
        return new int[]{BORDER_THICKNESS, BORDER_THICKNESS};
    }

    private void generateEnemies() {
        // ✨ [修复] 性能优化：避免重复调用randomEmptyCell()
        for (int i = 0; i < difficultyConfig.enemyE01PearlCount; i++) {
            int[] pos = randomEmptyCell();
            enemies.add(new EnemyE01_CorruptedPearl(pos[0], pos[1]));
        }
        for (int i = 0; i < difficultyConfig.enemyE02CoffeeBeanCount; i++) {
            int[] pos = randomEmptyCell();
            enemies.add(new EnemyE02_SmallCoffeeBean(pos[0], pos[1]));
        }
        for (int i = 0; i < difficultyConfig.enemyE03CaramelCount; i++) {
            int[] pos = randomEmptyCell();
            enemies.add(new EnemyE03_CaramelJuggernaut(pos[0], pos[1]));
        }
        for (int i = 0; i < difficultyConfig.enemyE04ShellCount; i++) {
            int[] pos = randomEmptyCell();
            enemies.add(new EnemyE04_CrystallizedCaramelShell(pos[0], pos[1]));
        }
    }

    private void generateTraps() {
        // ✨ [修复] 性能优化：避免重复调用randomEmptyCell()
        for (int i = 0; i < difficultyConfig.trapT01GeyserCount; i++) {
            int[] pos = randomEmptyCell();
            traps.add(new TrapT01_Geyser(pos[0], pos[1], 3f));
        }
        for (int i = 0; i < difficultyConfig.trapT02PearlMineCount; i++) {
            int[] pos = randomEmptyCell();
            traps.add(new TrapT02_PearlMine(pos[0], pos[1], this));
        }
        for (int i = 0; i < difficultyConfig.trapT03TeaShardCount; i++) {
            int[] pos = randomEmptyCell();
            traps.add(new TrapT03_TeaShards(pos[0], pos[1]));
        }
        for (int i = 0; i < difficultyConfig.trapT04MudTileCount; i++) {
            int[] pos = randomEmptyCell();
            traps.add(new TrapT04_Mud(pos[0], pos[1]));
        }
    }

    private void generateHearts() {
        // ✨ [修复] 性能优化：避免重复调用randomEmptyCell()
        for (int i = 0; i < 10; i++) {
            int[] pos = randomEmptyCell();
            hearts.add(new Heart(pos[0], pos[1]));
        }
    }

    private void generateTreasures() {
        int spawned = 0;
        int attempts = 0;
        while (spawned < 3 && attempts < 200) {
            attempts++;
            int[] p = randomEmptyCell();
            if (isOccupied(p[0], p[1])) continue;
            treasures.add(new Treasure(p[0], p[1]));
            spawned++;
        }
    }

    private int[] randomEmptyCell() {
        // ✨ [修复] 添加空值检查，防止数组越界
        if (maze == null || maze.length == 0 || maze[0] == null || maze[0].length == 0) {
            Logger.error("randomEmptyCell: Maze is not initialized");
            return new int[]{1, 1}; // 返回默认安全位置
        }
        
        int x, y;
        int width = maze[0].length;
        int height = maze.length;
        int attempts = 0;
        do {
            x = random(1, width - 2);
            y = random(1, height - 2);
            attempts++;
            if (attempts > 500) {
                // ✨ [修复] 检查player是否为null，提供安全默认值
                if (player != null) {
                    return new int[]{player.getX(), player.getY()};
                } else {
                    Logger.warning("randomEmptyCell: Max attempts reached, player is null, using default");
                    return new int[]{1, 1}; // 安全默认位置
                }
            }
        } while (maze[y][x] == 0 || isOccupied(x, y));
        return new int[]{x, y};
    }

    public boolean canPlayerMoveTo(int x, int y) {
        // ✨ [修复] 添加空值检查，防止数组越界
        if (maze == null || maze.length == 0 || maze[0] == null || maze[0].length == 0) return false;
        if (x < 0 || y < 0 || y >= maze.length || x >= maze[0].length) return false;
        for (Enemy enemy : enemies) {
            if (enemy instanceof EnemyE04_CrystallizedCaramelShell) {
                EnemyE04_CrystallizedCaramelShell shell = (EnemyE04_CrystallizedCaramelShell) enemy;
                if (shell.isActive() && shell.occupiesCell(x, y)) return false;
            }
        }
        for (ExitDoor door : exitDoors) if (door.getX() == x && door.getY() == y) return !door.isLocked();
        for (DynamicObstacle o : obstacles) if (o.getX() == x && o.getY() == y) return false;
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
        if (canPlayerMoveTo(nx, ny)) p.move(dx, dy);
    }

    private Player getPlayerByIndex(Player.PlayerIndex index) {
        for (Player p : players) if (p.getPlayerIndex() == index) return p;
        return null;
    }

    @Override public float getMoveDelayMultiplier() { return 1.0f; }
    @Override public boolean onAbilityInput(Player.PlayerIndex index, int slot) {
        if (levelTransitionInProgress) return false;
        Player p = getPlayerByIndex(index);
        if (p == null || p.isDead()) return false;
        p.useAbility(slot);
        return true;
    }

    @Override public void onInteractInput(Player.PlayerIndex index) {
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

    @Override public void onMenuInput() {}

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
                }
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
        if (bullet instanceof BobaBullet) bullets.add((BobaBullet) bullet);
    }
    public void spawnProjectile(BobaBullet bullet) { if (bullet != null) bullets.add(bullet); }

    public BobaBulletManager getBobaBulletEffectManager() { return bobaBulletEffectManager; }

    private void handlePlayerEnemyCollision() {
        if (levelTransitionInProgress) return;
        Player player = this.player;
        if (player == null || player.isDead()) return;
        for (Enemy enemy : enemies) {
            if (!enemy.isActive() || enemy.isDead()) continue;
            if (enemy.getX() == player.getX() && enemy.getY() == player.getY()) {
                if (player.isDashInvincible()) continue;
                
                int damage = enemy.getAttackDamage();
                int livesBefore = player.getLives();
                player.takeDamage(damage);

                DamageSource source = DamageSource.UNKNOWN;
                if (enemy instanceof EnemyE01_CorruptedPearl) source = DamageSource.ENEMY_E01;
                else if (enemy instanceof EnemyE02_SmallCoffeeBean) source = DamageSource.ENEMY_E02;
                else if (enemy instanceof EnemyE03_CaramelJuggernaut) source = DamageSource.ENEMY_E03;
                else if (enemy instanceof EnemyE04_CrystallizedCaramelShell) source = DamageSource.ENEMY_E04;

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
                }
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

    public PortalEffectManager getPlayerSpawnPortal() { return playerSpawnPortal; }
    public ItemEffectManager getItemEffectManager() { return itemEffectManager; }
    public TrapEffectManager getTrapEffectManager() { return trapEffectManager; }
    public CombatEffectManager getCombatEffectManager() { return combatEffectManager; }


    public void setVariable(String key, float value) {
        if (gameVariables == null) gameVariables = new HashMap<>();
        gameVariables.put(key, value);
        Logger.debug("Console Variable Set: " + key + " = " + value);
    }
    public float getVariable(String key) {
        if (gameVariables == null) return 1.0f;
        return gameVariables.getOrDefault(key, 1.0f);
    }

    public int getScore() {
        return scoreManager != null ? scoreManager.getCurrentScore() : 0;
    }
    public ScoreManager getScoreManager() { return scoreManager; }
    public PlayerInputHandler getInputHandler() { return  inputHandler; }
    public boolean isPlayerDead() { return player != null && player.isDead(); }
    public boolean isObstacleValidMove(int nx, int ny) {
        // ✨ [修复] 添加空值检查，防止数组越界
        if (maze == null || maze.length == 0 || maze[0] == null || maze[0].length == 0) return false;
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