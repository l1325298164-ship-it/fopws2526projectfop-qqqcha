package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.enemy.*;
import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.ui.HUD;
import de.tum.cit.fop.maze.utils.CameraManager;
import de.tum.cit.fop.maze.tools.DeveloperConsole;
import de.tum.cit.fop.maze.input.KeyBindingManager;

import java.lang.reflect.Method;
import java.util.*;

import static de.tum.cit.fop.maze.maze.MazeGenerator.BORDER_THICKNESS;
import static com.badlogic.gdx.math.MathUtils.random;

public class EndlessScreen implements Screen {
    private boolean isInitialized = false;
    private final MazeRunnerGame game;
    private final DifficultyConfig difficultyConfig;
    private GameManager gm;
    private MazeRenderer maze;
    private CameraManager cam;
    private SpriteBatch batch;
    private HUD hud;
    private PlayerInputHandler input;
    private DeveloperConsole console;
    private Texture uiTop, uiBottom, uiLeft, uiRight;

    // ===== 暂停相关 =====
    private boolean paused = false;
    private Stage pauseStage;
    private boolean pauseUIInitialized = false;

    // ===== 无尽模式专属字段 =====
    private float endlessSurvivalTime = 0f;          // 生存时间（秒）
    private int endlessWave = 1;                     // 当前波次
    private int endlessKills = 0;                    // 击杀敌人总数
    private int endlessScore = 0;                    // 无尽模式得分
    private float endlessSpawnTimer = 0f;            // 敌人生成计时器
    private float endlessSpawnInterval = 4f;         // 初始生成间隔：4秒
    private boolean endlessGameOver = false;         // 游戏是否结束标志
    private Stage endlessGameOverStage;              // 游戏结束界面舞台
    private boolean endlessGameOverUIInitialized = false; // 游戏结束UI是否初始化

    // ===== 物品生成相关 =====
    private float heartSpawnTimer = 0f;              // 血包生成计时器
    private float powerupSpawnTimer = 0f;            // 强化物品生成计时器
    private float minHeartSpawnInterval = 15f;       // 最小血包生成间隔
    private float minPowerupSpawnInterval = 30f;     // 最小强化物品生成间隔
    private int heartsSpawnedThisWave = 0;           // 当前波次已生成血包数量
    private int powerupsSpawnedThisWave = 0;         // 当前波次已生成强化物品数量
    private Map<String, Long> heartCreationTimes = new HashMap<>(); // 血包创建时间记录

    // ===== 随机数生成器 =====
    private final Random randomGenerator = new Random();

    // ===== 渲染排序相关 =====
    enum Type { WALL_BEHIND, ENTITY, WALL_FRONT }

    static class Item {
        float y;
        int priority;
        Type type;
        MazeRenderer.WallGroup wall;
        GameObject entity;

        Item(MazeRenderer.WallGroup w, Type t) {
            wall = w;
            y = w.startY;
            type = t;
        }

        Item(GameObject e, int p) {
            entity = e;
            y = e.getY();
            priority = p;
            type = Type.ENTITY;
        }
    }

    // ===== 血包生成策略枚举 =====
    enum HeartSpawnStrategy {
        NEAR_PLAYER,      // 靠近玩家（血量很低时）
        SAFE_ZONE,        // 安全区域（有敌人时）
        FAR_FROM_ENEMIES, // 远离敌人
        STRATEGIC_POINT   // 战略位置（岔路口等）
    }

    // ===== 强化物品类型枚举 =====
    enum PowerupType {
        ATTACK_BOOST,        // 攻击力提升
        SPEED_BOOST,         // 速度提升
        DEFENSE_BOOST,       // 防御提升
        COOLDOWN_REDUCTION   // 技能冷却减少
    }

    public EndlessScreen(MazeRunnerGame game, DifficultyConfig difficultyConfig) {
        this.game = game;
        this.difficultyConfig = difficultyConfig;
        System.out.println("🎯 EndlessScreen 构造函数调用");
        System.out.println("   Game 实例: " + game);
        System.out.println("   配置钥匙数量: " + difficultyConfig.keyCount);
    }

    @Override
    public void show() {
        System.out.println("=== EndlessScreen.show() 开始 ===");

        // 如果已经初始化，跳过
        if (isInitialized) {
            System.out.println("✅ EndlessScreen 已初始化，跳过重复初始化");
            return;
        }

        System.out.println("🚀 第一次初始化 EndlessScreen");

        // 只加载一次 UI 纹理
        try {
            uiTop = new Texture("Wallpaper/HUD_up.png");
            uiBottom = new Texture("Wallpaper/HUD_down.png");
            uiLeft = new Texture("Wallpaper/HUD_left.png");
            uiRight = new Texture("Wallpaper/HUD_right.png");
            System.out.println("✅ UI 纹理加载完成");
        } catch (Exception e) {
            System.out.println("❌ UI 纹理加载失败: " + e.getMessage());
        }

        input = new PlayerInputHandler();
        batch = game.getSpriteBatch();

        // 🔥 关键修改：使用 MazeRunnerGame 中已创建的 GameManager
        if (game.getGameManager() != null) {
            gm = game.getGameManager();
            System.out.println("✅ 使用 MazeRunnerGame 的 GameManager");
        } else {
            // 如果 gameManager 不存在，才创建一个
            gm = new GameManager(difficultyConfig);
            System.out.println("⚠️ 创建新的 GameManager");
        }

        // 初始化其他组件
        cam = new CameraManager(difficultyConfig);

        // 🔥🔥🔥 关键修复：立即调用 resize 设置相机视口
        int screenWidth = Gdx.graphics.getWidth();
        int screenHeight = Gdx.graphics.getHeight();
        System.out.println("屏幕尺寸: " + screenWidth + "x" + screenHeight);
        cam.resize(screenWidth, screenHeight);

        maze = new MazeRenderer(gm, difficultyConfig);
        hud = new HUD(gm);

        if (gm.getPlayer() != null) {
            cam.centerOnPlayerImmediately(gm.getPlayer());
        }

        // 尝试设置活动游戏屏幕
        trySetActiveGameScreen();

        console = new DeveloperConsole(gm, game.getSkin());

        // 无尽模式专属初始化
        if (isEndlessMode()) {
            System.out.println("🎯 初始化无尽模式...");
            initializeEndlessMode();
        }

        isInitialized = true;
        System.out.println("✅ EndlessScreen 初始化完成");

        // 🔥 关键修复：确保相机正确初始化并居中于玩家
        if (gm != null && gm.getPlayer() != null) {
            Player player = gm.getPlayer();
            System.out.println("🎯 玩家位置: (" + player.getX() + ", " + player.getY() + ")");

            // 立即将相机居中于玩家
            cam.centerOnPlayerImmediately(player);
            System.out.println("📷 相机已居中于玩家");
        }

        // 调试相机状态
        System.out.println("相机位置: " + cam.getCamera().position);
        System.out.println("相机缩放: " + cam.getCamera().zoom);
        System.out.println("相机视口: " + cam.getCamera().viewportWidth + "x" + cam.getCamera().viewportHeight);
    }

    // ===== 安全调用 setActiveGameScreen 的方法 =====
    private void trySetActiveGameScreen() {
        try {
            Method method = game.getClass().getMethod("setActiveGameScreen", Screen.class);
            method.invoke(game, this);
        } catch (NoSuchMethodException e) {
            try {
                Class<?> gameScreenClass = Class.forName("de.tum.cit.fop.maze.screen.GameScreen");
                Method method = game.getClass().getMethod("setActiveGameScreen", gameScreenClass);
                method.invoke(game, this);
            } catch (Exception ex) {
                System.out.println("无尽模式：setActiveGameScreen 方法不可用，但这不影响游戏运行");
            }
        } catch (Exception e) {
            System.out.println("无尽模式：调用 setActiveGameScreen 时出错: " + e.getMessage());
        }
    }

    // ===== 无尽模式初始化 =====
    private void initializeEndlessMode() {
        endlessSurvivalTime = 0f;
        endlessWave = 1;
        endlessKills = 0;
        endlessScore = 0;
        endlessSpawnTimer = 0f;
        heartSpawnTimer = 0f;
        powerupSpawnTimer = 0f;
        endlessGameOver = false;
        heartsSpawnedThisWave = 0;
        powerupsSpawnedThisWave = 0;
        heartCreationTimes.clear();

        // 移除不需要的元素（无尽模式没有出口和钥匙）
        gm.getExitDoors().clear();
        gm.getKeys().clear();

        // 初始生成一些敌人
        spawnInitialEndlessEnemies();
        System.out.println("无尽模式已初始化！");
        System.out.println("   - 生命值: " + gm.getPlayer().getLives());
        System.out.println("   - 玩家位置: (" + gm.getPlayer().getX() + ", " + gm.getPlayer().getY() + ")");
    }

    // ===== 主渲染循环 =====
    @Override
    public void render(float delta) {
        // 1. 逻辑更新 (保持放在最前面)
        handleInput(delta);
        float timeScale = console.isVisible() ? 0f : gm.getVariable("time_scale");
        float gameDelta = delta * timeScale;

        if (!paused && !console.isVisible()) {
            gm.update(gameDelta);
            if (isEndlessMode() && !endlessGameOver) {
                updateEndlessMode(gameDelta);
            }

        }
        // 🔥 关键修复：相机更新必须放在这里，确保玩家位置已更新
        if (gm != null && gm.getPlayer() != null && !paused && !console.isVisible()) {
            cam.update(gameDelta, gm.getPlayer(), gm);
        }

        // 🔥 减少调试输出频率（每2秒一次）
        if ((int)(System.currentTimeMillis() / 2000) != (int)((System.currentTimeMillis() - delta * 1000) / 2000)) {
            System.out.println("📷 相机状态: Pos(" + cam.getCamera().position.x + ", " + cam.getCamera().position.y +
                    ") Viewport(" + cam.getCamera().viewportWidth + "x" + cam.getCamera().viewportHeight + ")");
            System.out.println("👤 玩家位置: (" + gm.getPlayer().getX() + ", " + gm.getPlayer().getY() + ")");
        }
        // 2. 清屏
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);

        // 3. 【核心修复】设置世界坐标矩阵
        // 先获取相机矩阵
        Matrix4 cameraMatrix = cam.getCamera().combined;

        // 调试输出相机矩阵信息
        System.out.println("Camera combined matrix: " + cameraMatrix);

        // 设置到 batch
        batch.setProjectionMatrix(cameraMatrix);

        /* ================= 渲染世界物体 (都在一个 begin/end 块中更高效) ================= */
        batch.begin();

        // A. 地板
        maze.renderFloor(batch);

        // B. 传送阵背景
        List<ExitDoor> exitDoorsCopy = new ArrayList<>(gm.getExitDoors());
        exitDoorsCopy.forEach(d -> d.renderPortalBack(batch));

        // C. 排序后的实体 (墙、玩家、敌人等)
        List<Item> items = prepareRenderItems(exitDoorsCopy);
        items.sort(Comparator.comparingDouble((Item i) -> -i.y)
                .thenComparingInt(i -> i.type.ordinal())
                .thenComparingInt(i -> i.priority));

        for (Item it : items) {
            if (it.wall != null) {
                maze.renderWallGroup(batch, it.wall);
            } else {
                it.entity.drawSprite(batch);
            }
        }

        // D. 特效
        gm.getKeyEffectManager().render(batch);
        gm.getBobaBulletEffectManager().render(batch);
        batch.end();

        /* ================= 渲染 UI (切换到屏幕坐标) ================= */
        renderUI();

        // 4. 暂停和结束界面 (它们使用 Stage，会自动管理自己的投影矩阵)
        if (paused) renderPauseScreen(delta);
        if (endlessGameOver && endlessGameOverStage != null) renderGameOverScreen(delta);
    }

    // ===== 无尽模式核心更新方法 =====
    private void updateEndlessMode(float delta) {
        if (gm == null || gm.getPlayer() == null) {
            System.out.println("❌ updateEndlessMode: gm 或 player 为空");
            return;
        }

        // 更新计时器
        endlessSurvivalTime += delta;
        endlessSpawnTimer += delta;
        heartSpawnTimer += delta;
        powerupSpawnTimer += delta;

        // 🔥 新增：实时监控敌人类型和数量
        int pearlCount = 0;
        int coffeeCount = 0;
        int caramelCount = 0;

        for (Enemy enemy : gm.getEnemies()) {
            if (enemy instanceof EnemyE01_CorruptedPearl) {
                pearlCount++;
            } else if (enemy instanceof EnemyE02_SmallCoffeeBean) {
                coffeeCount++;
            } else if (enemy instanceof EnemyE03_CaramelJuggernaut) {
                caramelCount++;
            }
        }

        // 每3秒输出一次敌人状态
        if ((int)(endlessSurvivalTime / 3) != (int)((endlessSurvivalTime - delta) / 3)) {
            System.out.println("🎯 敌人类型统计:");
            System.out.println("   珍珠敌人: " + pearlCount + " 个");
            System.out.println("   咖啡敌人: " + coffeeCount + " 个");
            System.out.println("   焦糖敌人: " + caramelCount + " 个");
            System.out.println("   敌人总数: " + gm.getEnemies().size() + " 个");
        }

        // 检查玩家死亡
        if (gm.getPlayer().isDead()) {
            System.out.println("💀 玩家死亡，游戏结束");
            endlessGameOver = true;
            showEndlessGameOverScreen();
            return;
        }

        // 计算玩家当前生命百分比
        float healthPercent = calculatePlayerHealthPercentage();

        // 每5秒输出一次状态（仅日志）
        if ((int)(endlessSurvivalTime / 5) != (int)((endlessSurvivalTime - delta) / 5)) {
            System.out.println("=== 无尽模式状态 ===");
            System.out.println("时间: " + String.format("%.1f", endlessSurvivalTime) + "秒");
            System.out.println("波次: " + endlessWave);
            System.out.println("生命值: " + String.format("%.1f", healthPercent) + "%");
            System.out.println("敌人数量: " + gm.getEnemies().size());
        }

        // 1. 敌人生成逻辑（基于血量）
        float enemySpawnInterval = getDynamicEnemySpawnInterval(healthPercent);
        if (endlessSpawnTimer >= enemySpawnInterval) {
            System.out.println("🎯 生成新敌人");
            endlessSpawnTimer = 0f;
            spawnHealthBasedEnemies(healthPercent);
        }

        // 2. 血包生成逻辑
        updateHeartSpawnLogic(delta, healthPercent);

        // 3. 强化物品生成逻辑
        updatePowerupSpawnLogic(delta, healthPercent);

        // 4. 自动清理过期物品
        cleanupExpiredItems();

        // 5. 波次推进（每60秒一波）
        int newWave = 1 + (int)(endlessSurvivalTime / 60f);
        if (newWave > endlessWave) {
            endlessWave = newWave;
            System.out.println("🎉 进入第 " + endlessWave + " 波！");
            onEndlessWaveAdvanced();
            resetWaveSpawnCounters();
        }
    }

    // ===== 计算玩家生命值百分比 =====
    private float calculatePlayerHealthPercentage() {
        Player player = gm.getPlayer();
        if (player == null) return 100f;

        int maxLives = difficultyConfig.initialLives;
        int currentLives = player.getLives();
        if (maxLives <= 0) return 100f;

        return (currentLives / (float)maxLives) * 100f;
    }

    // ===== 动态敌人生成间隔 =====
    private float getDynamicEnemySpawnInterval(float healthPercent) {
        float interval = endlessSpawnInterval;

        // 生命值越低，生成间隔越短
        if (healthPercent < 30) {
            interval *= 0.5f;    // 生命<30%，生成速度加倍
        } else if (healthPercent < 60) {
            interval *= 0.75f;   // 生命<60%，生成速度加快25%
        }

        // 波次越高，生成越快
        float waveReduction = (endlessWave * 0.1f);
        interval -= waveReduction;

        // 保证最小生成间隔为1秒
        return Math.max(1f, interval);
    }

    // ===== 血包生成逻辑 =====
    private void updateHeartSpawnLogic(float delta, float healthPercent) {
        // 计算血包生成间隔（血量越低，生成越快）
        float heartInterval = calculateHeartSpawnInterval(healthPercent);

        if (heartSpawnTimer >= heartInterval) {
            // 检查是否已经生成了足够的血包
            int maxHeartsPerWave = getMaxHeartsPerWave();
            if (heartsSpawnedThisWave < maxHeartsPerWave) {
                // 检查当前场上血包数量
                int currentHeartCount = gm.getHearts().size();
                int maxHeartsOnField = 3; // 场上最多同时存在3个血包

                if (currentHeartCount < maxHeartsOnField) {
                    spawnSmartHeart(healthPercent);
                    heartsSpawnedThisWave++;
                    heartSpawnTimer = 0f;
                }
            }
        }
    }

    private float calculateHeartSpawnInterval(float healthPercent) {
        // 基础间隔：生命值越低，生成越快
        float baseInterval = 20f; // 20秒

        // 生命值影响
        if (healthPercent < 20) {
            baseInterval = 8f;    // 生命<20%，8秒生成一次
        } else if (healthPercent < 40) {
            baseInterval = 12f;   // 生命<40%，12秒一次
        } else if (healthPercent < 60) {
            baseInterval = 16f;   // 生命<60%，16秒一次
        }

        // 波次影响（波次越高，生成越频繁）
        float waveMultiplier = Math.max(0.5f, 1.0f - (endlessWave * 0.05f));

        // 场上血包数量影响（血包越多，生成越慢）
        int heartCount = gm.getHearts().size();
        float countMultiplier = 1.0f + (heartCount * 0.3f);

        return Math.max(5f, baseInterval * waveMultiplier * countMultiplier);
    }

    private int getMaxHeartsPerWave() {
        // 根据波次决定每波最大血包数量
        return Math.min(5, 2 + (endlessWave / 3));
    }

    // ===== 智能血包生成 =====
    private void spawnSmartHeart(float healthPercent) {
        Player player = gm.getPlayer();
        if (player == null) return;

        int playerX = player.getX();
        int playerY = player.getY();

        // 根据血量决定生成策略
        HeartSpawnStrategy strategy = determineHeartSpawnStrategy(healthPercent);
        int[] spawnPos = findOptimalHeartPosition(playerX, playerY, strategy);

        if (spawnPos != null) {
            try {
                Heart heart = new Heart(spawnPos[0], spawnPos[1]);

                // 记录创建时间
                String heartKey = spawnPos[0] + "," + spawnPos[1];
                heartCreationTimes.put(heartKey, System.currentTimeMillis());

                // 根据血量决定血包类型（普通/加强）
                if (healthPercent < 30 && randomGenerator.nextFloat() < 0.3f) {
                    // 30%几率生成加强血包（回2血）
                    heart = createEnhancedHeart(spawnPos[0], spawnPos[1]);
                }

                gm.getHearts().add(heart);

                System.out.println("❤️ 生成血包于位置 (" + spawnPos[0] + ", " + spawnPos[1] + ")");
                System.out.println("   策略: " + strategy + " | 生命值: " + healthPercent + "%");

            } catch (Exception e) {
                System.out.println("生成血包失败: " + e.getMessage());
            }
        }
    }

    private HeartSpawnStrategy determineHeartSpawnStrategy(float healthPercent) {
        if (healthPercent < 20) {
            // 生命危急，生成在玩家附近
            return HeartSpawnStrategy.NEAR_PLAYER;
        } else if (healthPercent < 40) {
            // 生命较低，生成在安全区域
            return HeartSpawnStrategy.SAFE_ZONE;
        } else if (gm.getEnemies().size() > 5) {
            // 敌人很多，生成在远离敌人的地方
            return HeartSpawnStrategy.FAR_FROM_ENEMIES;
        } else {
            // 正常情况，生成在战略位置
            return HeartSpawnStrategy.STRATEGIC_POINT;
        }
    }

    private int[] findOptimalHeartPosition(int playerX, int playerY, HeartSpawnStrategy strategy) {
        int bestX = -1, bestY = -1;
        float bestScore = -Float.MAX_VALUE;

        // 搜索最佳生成位置
        for (int attempt = 0; attempt < 50; attempt++) {
            int[] pos = findAnyEmptyCell();
            if (pos == null) continue;

            float score = calculatePositionScore(pos[0], pos[1], playerX, playerY, strategy);

            if (score > bestScore) {
                bestScore = score;
                bestX = pos[0];
                bestY = pos[1];
            }
        }

        if (bestX != -1 && bestY != -1) {
            return new int[]{bestX, bestY};
        }

        // 如果没有找到理想位置，返回随机位置
        return findAnyEmptyCell();
    }

    private float calculatePositionScore(int x, int y, int playerX, int playerY, HeartSpawnStrategy strategy) {
        float score = 0f;

        // 1. 基本分：必须是可通行格子
        if (!isCellWalkable(x, y) || isCellOccupied(x, y)) {
            return -9999f;
        }

        // 2. 根据策略计算得分
        switch (strategy) {
            case NEAR_PLAYER:
                // 靠近玩家（距离3-8格最好）
                float distToPlayer = Math.abs(x - playerX) + Math.abs(y - playerY);
                if (distToPlayer >= 3 && distToPlayer <= 8) {
                    score += 100 - distToPlayer;
                }
                break;

            case SAFE_ZONE:
                // 远离敌人
                float minEnemyDist = getMinDistanceToEnemies(x, y);
                score += minEnemyDist * 10;
                break;

            case FAR_FROM_ENEMIES:
                // 非常远离敌人
                float enemyDist = getMinDistanceToEnemies(x, y);
                score += enemyDist * 20;
                if (enemyDist > 10) score += 50;
                break;

            case STRATEGIC_POINT:
                // 在路口或开阔区域
                int openDirections = countOpenDirections(x, y);
                score += openDirections * 30;
                break;
        }

        // 3. 额外加分：不在角落
        if (!isInCorner(x, y)) {
            score += 20;
        }

        // 4. 额外加分：远离其他血包
        float minHeartDist = getMinDistanceToHearts(x, y);
        if (minHeartDist > 5) {
            score += 30;
        }

        return score;
    }

    // 获取到最近敌人的距离
    private float getMinDistanceToEnemies(int x, int y) {
        float minDist = Float.MAX_VALUE;
        for (Enemy enemy : gm.getEnemies()) {
            float dist = Math.abs(enemy.getX() - x) + Math.abs(enemy.getY() - y);
            minDist = Math.min(minDist, dist);
        }
        return minDist == Float.MAX_VALUE ? 10f : minDist;
    }

    // 获取到最近血包的距离
    private float getMinDistanceToHearts(int x, int y) {
        float minDist = Float.MAX_VALUE;
        for (Heart heart : gm.getHearts()) {
            float dist = Math.abs(heart.getX() - x) + Math.abs(heart.getY() - y);
            minDist = Math.min(minDist, dist);
        }
        return minDist == Float.MAX_VALUE ? 10f : minDist;
    }

    // 计算开放方向数量
    private int countOpenDirections(int x, int y) {
        int count = 0;
        if (isCellWalkable(x + 1, y)) count++;
        if (isCellWalkable(x - 1, y)) count++;
        if (isCellWalkable(x, y + 1)) count++;
        if (isCellWalkable(x, y - 1)) count++;
        return count;
    }

    // 检查是否在角落
    private boolean isInCorner(int x, int y) {
        int blockedCount = 0;
        if (!isCellWalkable(x + 1, y) || isCellOccupied(x + 1, y)) blockedCount++;
        if (!isCellWalkable(x - 1, y) || isCellOccupied(x - 1, y)) blockedCount++;
        if (!isCellWalkable(x, y + 1) || isCellOccupied(x, y + 1)) blockedCount++;
        if (!isCellWalkable(x, y - 1) || isCellOccupied(x, y - 1)) blockedCount++;
        return blockedCount >= 3;
    }

    // 创建加强血包
    private Heart createEnhancedHeart(int x, int y) {
        Heart heart = new Heart(x, y);
        // 假设Heart类有setHealAmount方法
        try {
            // 尝试设置治疗量为2（默认可能是1）
            Method setHealMethod = Heart.class.getMethod("setHealAmount", int.class);
            setHealMethod.invoke(heart, 2);
            System.out.println("✨ 生成加强血包（回2血）");
        } catch (Exception e) {
            // 如果方法不存在，保持默认
            System.out.println("⚠️ 无法设置加强血包，使用默认");
        }
        return heart;
    }

    // ===== 强化物品生成逻辑 =====
    private void updatePowerupSpawnLogic(float delta, float healthPercent) {
        // 只有在较高波次才生成强化物品
        if (endlessWave < 3) return;

        float powerupInterval = calculatePowerupSpawnInterval(healthPercent);

        if (powerupSpawnTimer >= powerupInterval) {
            int maxPowerupsPerWave = getMaxPowerupsPerWave();
            if (powerupsSpawnedThisWave < maxPowerupsPerWave) {
                spawnRandomPowerup(healthPercent);
                powerupsSpawnedThisWave++;
                powerupSpawnTimer = 0f;
            }
        }
    }

    private float calculatePowerupSpawnInterval(float healthPercent) {
        float baseInterval = 45f;

        // 生命值影响
        if (healthPercent < 30) {
            baseInterval = 25f; // 低生命值时生成更快
        } else if (healthPercent > 70) {
            baseInterval = 60f; // 高生命值时生成更慢
        }

        // 波次影响
        float waveMultiplier = Math.max(0.3f, 1.0f - (endlessWave * 0.03f));

        return Math.max(15f, baseInterval * waveMultiplier);
    }

    private int getMaxPowerupsPerWave() {
        return Math.min(3, 1 + (endlessWave / 5));
    }

    private void spawnRandomPowerup(float healthPercent) {
        int[] pos = findEmptyCellForEndlessSpawn();
        if (pos == null) return;

        // 根据生命值选择强化类型
        PowerupType type = selectPowerupType(healthPercent);

        try {
            switch (type) {
                case ATTACK_BOOST:
                    spawnAttackBoost(pos[0], pos[1]);
                    break;
                case SPEED_BOOST:
                    spawnSpeedBoost(pos[0], pos[1]);
                    break;
                case DEFENSE_BOOST:
                    spawnDefenseBoost(pos[0], pos[1]);
                    break;
                case COOLDOWN_REDUCTION:
                    spawnCooldownReduction(pos[0], pos[1]);
                    break;
            }

            System.out.println("✨ 生成强化物品: " + type + " 于位置 (" + pos[0] + ", " + pos[1] + ")");

        } catch (Exception e) {
            System.out.println("生成强化物品失败: " + e.getMessage());
        }
    }

    private PowerupType selectPowerupType(float healthPercent) {
        float rand = randomGenerator.nextFloat();

        if (healthPercent < 30) {
            // 低生命值优先防御和冷却
            if (rand < 0.4) return PowerupType.DEFENSE_BOOST;
            if (rand < 0.7) return PowerupType.COOLDOWN_REDUCTION;
            if (rand < 0.9) return PowerupType.ATTACK_BOOST;
            return PowerupType.SPEED_BOOST;
        } else if (healthPercent > 70) {
            // 高生命值优先攻击和速度
            if (rand < 0.4) return PowerupType.ATTACK_BOOST;
            if (rand < 0.7) return PowerupType.SPEED_BOOST;
            if (rand < 0.9) return PowerupType.COOLDOWN_REDUCTION;
            return PowerupType.DEFENSE_BOOST;
        } else {
            // 中等生命值平均分配
            if (rand < 0.25) return PowerupType.ATTACK_BOOST;
            if (rand < 0.5) return PowerupType.SPEED_BOOST;
            if (rand < 0.75) return PowerupType.DEFENSE_BOOST;
            return PowerupType.COOLDOWN_REDUCTION;
        }
    }

    // 各种强化物品的生成方法
    private void spawnAttackBoost(int x, int y) {
        System.out.println("生成攻击力提升物品");
    }

    private void spawnSpeedBoost(int x, int y) {
        System.out.println("生成速度提升物品");
    }

    private void spawnDefenseBoost(int x, int y) {
        System.out.println("生成防御提升物品");
    }

    private void spawnCooldownReduction(int x, int y) {
        System.out.println("生成冷却减少物品");
    }

    // ===== 清理过期物品 =====
    private void cleanupExpiredItems() {
        // 清理过时的血包（生成超过60秒）
        long currentTime = System.currentTimeMillis();
        Iterator<Heart> heartIter = gm.getHearts().iterator();
        while (heartIter.hasNext()) {
            Heart heart = heartIter.next();
            String heartKey = heart.getX() + "," + heart.getY();
            Long creationTime = heartCreationTimes.get(heartKey);

            if (creationTime != null) {
                long age = currentTime - creationTime;
                if (age > 60000) { // 60秒后清理
                    heartIter.remove();
                    heartCreationTimes.remove(heartKey);
                    System.out.println("🧹 清理过期血包（生成超过60秒）");
                }
            }
        }
    }

    // ===== 波次推进相关 =====
    private void onEndlessWaveAdvanced() {
        System.out.println("=== 无尽模式第 " + endlessWave + " 波 ===");

        // 波次奖励：每波开始时给予奖励
        grantWaveRewards();

        // 特殊波次效果
        if (endlessWave % 5 == 0) {
            // 每5波生成一个超强血包
            spawnSuperHeart();
        }

        if (endlessWave % 10 == 0) {
            // 每10波生成一个稀有强化
            spawnRarePowerup();
        }
    }

    private void grantWaveRewards() {
        Player player = gm.getPlayer();
        if (player == null) return;

        // 根据当前血量决定奖励
        float healthPercent = calculatePlayerHealthPercentage();

        if (healthPercent < 40) {
            // 低生命值奖励：直接回复1点生命
            player.heal(1);
            System.out.println("🎁 波次奖励：回复1点生命");
        } else if (healthPercent < 70) {
            // 中等生命值奖励：临时护盾或加速
            System.out.println("🎁 波次奖励：获得临时增益");
        } else {
            // 高生命值奖励：增加最大生命或攻击力
            System.out.println("🎁 波次奖励：属性提升");
        }
    }

    private void spawnSuperHeart() {
        int[] pos = findAnyEmptyCell();
        if (pos != null) {
            System.out.println("🌟 生成超强血包（回3血）");
        }
    }

    private void spawnRarePowerup() {
        int[] pos = findAnyEmptyCell();
        if (pos != null) {
            System.out.println("💎 生成稀有强化物品");
        }
    }

    private void resetWaveSpawnCounters() {
        heartsSpawnedThisWave = 0;
        powerupsSpawnedThisWave = 0;
    }

    // ===== 敌人生成相关方法 =====
    private void spawnHealthBasedEnemies(float healthPercent) {
        int spawnCount = calculateSpawnCount(healthPercent);

        for (int i = 0; i < spawnCount; i++) {
            int[] pos = findEmptyCellForEndlessSpawn();
            if (pos == null) continue;

            Enemy enemy = createEnemyBasedOnHealth(healthPercent, pos[0], pos[1]);
            if (enemy != null) {
                gm.getEnemies().add(enemy);
            }
        }

        System.out.println("生成 " + spawnCount + " 个敌人 | 生命值: " + (int)healthPercent + "% | 波次: " + endlessWave);
    }

    private int calculateSpawnCount(float healthPercent) {
        int baseCount = 1;

        if (healthPercent > 70) {
            baseCount = 1;    // 生命>70%，生成1个
        } else if (healthPercent > 40) {
            baseCount = 2;    // 生命>40%，生成2个
        } else if (healthPercent > 20) {
            baseCount = 3;    // 生命>20%，生成3个
        } else {
            baseCount = 4;    // 生命<20%，生成4个（疯狂模式）
        }

        // 波次越高，额外敌人越多
        return baseCount + (endlessWave / 3);
    }

    private Enemy createEnemyBasedOnHealth(float healthPercent, int x, int y) {
        float randValue = randomGenerator.nextFloat() * 100f;

        try {
            if (healthPercent > 70) {
                // 生命>70%：80%珍珠敌人，20%咖啡敌人
                if (randValue < 80) {
                    return new EnemyE01_CorruptedPearl(x, y);
                } else {
                    return new EnemyE02_SmallCoffeeBean(x, y);
                }
            }
            else if (healthPercent > 40) {
                // 生命>40%：60%珍珠，30%咖啡，10%焦糖
                if (randValue < 60) {
                    return new EnemyE01_CorruptedPearl(x, y);
                } else if (randValue < 90) {
                    return new EnemyE02_SmallCoffeeBean(x, y);
                } else {
                    return new EnemyE03_CaramelJuggernaut(x, y);
                }
            }
            else if (healthPercent > 20) {
                // 生命>20%：40%珍珠，40%咖啡，20%焦糖
                if (randValue < 40) {
                    return new EnemyE01_CorruptedPearl(x, y);
                } else if (randValue < 80) {
                    return new EnemyE02_SmallCoffeeBean(x, y);
                } else {
                    return new EnemyE03_CaramelJuggernaut(x, y);
                }
            }
            else {
                // 生命<20%：20%珍珠，40%咖啡，40%焦糖（绝望模式）
                if (randValue < 20) {
                    return new EnemyE01_CorruptedPearl(x, y);
                } else if (randValue < 60) {
                    return new EnemyE02_SmallCoffeeBean(x, y);
                } else {
                    return new EnemyE03_CaramelJuggernaut(x, y);
                }
            }
        } catch (Exception e) {
            System.out.println("创建敌人失败: " + e.getMessage());
            return new EnemyE01_CorruptedPearl(x, y);
        }
    }

    // ===== 辅助方法：检查格子是否被占用 =====
    private boolean isCellOccupied(int x, int y) {
        // 检查玩家
        Player player = gm.getPlayer();
        if (player != null && player.getX() == x && player.getY() == y) {
            return true;
        }

        // 检查敌人
        List<Enemy> enemiesCopy = new ArrayList<>(gm.getEnemies());
        for (Enemy enemy : enemiesCopy) {
            if (enemy != null && enemy.isActive() && enemy.getX() == x && enemy.getY() == y) {
                return true;
            }
        }

        // 检查生命包
        List<Heart> heartsCopy = new ArrayList<>(gm.getHearts());
        for (Heart heart : heartsCopy) {
            if (heart != null && heart.isActive() && heart.getX() == x && heart.getY() == y) {
                return true;
            }
        }

        // 检查宝箱
        List<Treasure> treasuresCopy = new ArrayList<>(gm.getTreasures());
        for (Treasure treasure : treasuresCopy) {
            if (treasure != null && treasure.isActive() && treasure.getX() == x && treasure.getY() == y) {
                return true;
            }
        }

        // 检查钥匙
        List<Key> keysCopy = new ArrayList<>(gm.getKeys());
        for (Key key : keysCopy) {
            if (key != null && key.isActive() && key.getX() == x && key.getY() == y) {
                return true;
            }
        }

        return false;
    }

    private int[] findEmptyCellForEndlessSpawn() {
        Player player = gm.getPlayer();
        if (player == null) return findAnyEmptyCell();

        int playerX = player.getX();
        int playerY = player.getY();

        for (int attempt = 0; attempt < 50; attempt++) {
            int x = BORDER_THICKNESS + random.nextInt(
                    difficultyConfig.mazeWidth - BORDER_THICKNESS * 2
            );
            int y = BORDER_THICKNESS + random.nextInt(
                    difficultyConfig.mazeHeight - BORDER_THICKNESS * 2
            );

            if (isCellWalkable(x, y) && !isCellOccupied(x, y)) {
                // 尽量远离玩家（至少5格距离）
                if (Math.abs(x - playerX) > 5 || Math.abs(y - playerY) > 5) {
                    return new int[]{x, y};
                }
            }
        }

        return findAnyEmptyCell();
    }

    private boolean isCellWalkable(int x, int y) {
        // 检查边界
        if (x < 0 || y < 0 || x >= difficultyConfig.mazeWidth || y >= difficultyConfig.mazeHeight) {
            return false;
        }

        // 检查迷宫单元格（1表示可通行，0表示墙）
        int[][] mazeArray = gm.getMaze();
        if (mazeArray == null || y >= mazeArray.length || x >= mazeArray[0].length) {
            return false;
        }

        return mazeArray[y][x] == 1;
    }

    private int[] findAnyEmptyCell() {
        int width = difficultyConfig.mazeWidth;
        int height = difficultyConfig.mazeHeight;

        for (int attempt = 0; attempt < 100; attempt++) {
            int x = BORDER_THICKNESS + random.nextInt(width - BORDER_THICKNESS * 2);
            int y = BORDER_THICKNESS + random.nextInt(height - BORDER_THICKNESS * 2);

            if (isCellWalkable(x, y) && !isCellOccupied(x, y)) {
                return new int[]{x, y};
            }
        }

        // 返回一个安全的默认位置
        return new int[]{BORDER_THICKNESS + 1, BORDER_THICKNESS + 1};
    }

    private void spawnEndlessHealthPack() {
        int[] pos = findAnyEmptyCell();
        if (pos != null) {
            try {
                Heart heart = new Heart(pos[0], pos[1]);
                gm.getHearts().add(heart);
                System.out.println("生成生命包于位置 (" + pos[0] + ", " + pos[1] + ")");
            } catch (Exception e) {
                System.out.println("生成生命包失败: " + e.getMessage());
            }
        }
    }

    private void spawnInitialEndlessEnemies() {
        for (int i = 0; i < 3; i++) {
            int[] pos = findEmptyCellForEndlessSpawn();
            if (pos != null) {
                try {
                    gm.getEnemies().add(new EnemyE01_CorruptedPearl(pos[0], pos[1]));
                } catch (Exception e) {
                    System.out.println("初始敌人生成失败: " + e.getMessage());
                }
            }
        }
    }

    // 外部调用的击杀计数方法
    public void onEnemyKilledInEndless() {
        endlessKills++;
    }

    private int calculateEndlessScore() {
        int timeScore = (int)(endlessSurvivalTime * 10);
        int killScore = endlessKills * 100;
        int waveBonus = endlessWave * 500;
        float healthPercent = calculatePlayerHealthPercentage();
        int healthBonus = (int)(healthPercent * 10);

        return timeScore + killScore + waveBonus + healthBonus;
    }

    // ===== 游戏结束界面 =====
    private void showEndlessGameOverScreen() {
        endlessGameOverStage = new Stage(new ScreenViewport());
        Table root = new Table();
        root.setFillParent(true);
        endlessGameOverStage.addActor(root);

        int finalScore = calculateEndlessScore();

        // 🔥 修改：使用与GameScreen一致的标题样式
        root.add(new Label("Game Over", game.getSkin(), "title"))
                .padBottom(40).row();

        root.add(new Label(
                String.format("SURVIVAL TIME: %02d min  %02d sec",
                        (int)endlessSurvivalTime / 60,
                        (int)endlessSurvivalTime % 60),
                game.getSkin()
        )).padBottom(10).row();

        root.add(new Label(
                String.format("LEVEL %d", endlessWave),
                game.getSkin()
        )).padBottom(10).row();

        root.add(new Label(
                String.format("ENEMY KILLED: %d", endlessKills),
                game.getSkin()
        )).padBottom(10).row();

        root.add(new Label(
                String.format("SCORE: %d", finalScore),
                game.getSkin()
        )).padBottom(40).row();

        ButtonFactory bf = new ButtonFactory(game.getSkin());

        // 🔥 修改：使用与GameScreen一致的按钮尺寸和样式
        root.add(bf.create("try again", () -> {
            game.startNewGame(Difficulty.ENDLESS);
            game.goToGame();
        })).width(400).height(80).padBottom(20).row();

        root.add(bf.create("MENU", () -> {
            game.goToMenu();
        })).width(400).height(80).row();

        Gdx.input.setInputProcessor(endlessGameOverStage);
        endlessGameOverUIInitialized = true;
    }

    // ===== 辅助方法 =====
    private boolean isEndlessMode() {
        return difficultyConfig.keyCount == 0;
    }

    private void handleInput(float delta) {
        // 控制台开关
        if (KeyBindingManager.getInstance().isJustPressed(KeyBindingManager.GameAction.CONSOLE)) {
            console.toggle();
        }

        // 游戏输入（非暂停、非控制台、非转场、非游戏结束）
        if (!paused && !console.isVisible() && !gm.isLevelTransitionInProgress() && !endlessGameOver) {
            input.update(delta, new PlayerInputHandler.InputHandlerCallback() {
                @Override
                public void onMoveInput(Player.PlayerIndex index, int dx, int dy) {
                    gm.onMoveInput(index, dx, dy);
                }

                @Override
                public float getMoveDelayMultiplier() {
                    if (gm.getPlayer() != null) {
                        return gm.getPlayer().getMoveDelayMultiplier();
                    }
                    return 1.0f;
                }

                @Override
                public boolean onAbilityInput(int slot) {
                    return gm.onAbilityInput(slot);
                }

                @Override
                public void onInteractInput() {
                    gm.onInteractInput();
                }

                @Override
                public void onMenuInput() {
                    togglePause();
                }
            }, Player.PlayerIndex.P1);
        }

    }

    private List<Item> prepareRenderItems(List<ExitDoor> exitDoorsCopy) {
        List<Item> items = new ArrayList<>();

        // 墙壁
        for (var wg : maze.getWallGroups()) {
            boolean front = maze.isWallInFrontOfAnyEntity(wg.startX, wg.startY);
            items.add(new Item(wg, front ? Type.WALL_FRONT : Type.WALL_BEHIND));
        }

        // 玩家（最高优先级）
        items.add(new Item(gm.getPlayer(), 100));

        // 敌人
        List<Enemy> enemiesCopy = new ArrayList<>(gm.getEnemies());
        enemiesCopy.forEach(e -> items.add(new Item(e, 50)));

        // 门
        exitDoorsCopy.forEach(d -> items.add(new Item(d, 45)));

        // 生命包
        List<Heart> heartsCopy = new ArrayList<>(gm.getHearts());
        heartsCopy.forEach(h -> {
            if (h.isActive()) items.add(new Item(h, 30));
        });

        // 宝箱
        List<Treasure> treasuresCopy = new ArrayList<>(gm.getTreasures());
        treasuresCopy.forEach(t -> items.add(new Item(t, 20)));

        // 钥匙
        List<Key> keysCopy = new ArrayList<>(gm.getKeys());
        keysCopy.forEach(k -> {
            if (k.isActive()) {
                items.add(new Item(k, 35));
            }
        });

        return items;
    }

    // 🔥 修改：移除了无尽模式特殊UI的渲染
    private void renderUI() {
        batch.setProjectionMatrix(
                new Matrix4().setToOrtho2D(
                        0, 0,
                        Gdx.graphics.getWidth(),
                        Gdx.graphics.getHeight()
                )
        );

        batch.begin();
        renderMazeBorderDecorations(batch);

        hud.renderInGameUI(batch);

        hud.renderManaBar(batch);
        batch.end();
        if (console != null) {
            console.render();
        }
        batch.setProjectionMatrix(cam.getCamera().combined);
    }
    // 🔥 修改：使用与GameScreen一致的装饰渲染
    private void renderMazeBorderDecorations(SpriteBatch batch) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        int thickness = 1000;

        batch.draw(uiTop,    0, h - thickness+860, w, thickness-120);
        batch.draw(uiBottom, 0, 0-800,             w, thickness-120);
        batch.draw(uiLeft,   -600, 0,             thickness-220, h);
        batch.draw(uiRight,  w - thickness+810, 0, thickness-220, h);
    }

    // 🔥 修改：使暂停界面渲染与GameScreen一致
    private void renderPauseScreen(float delta) {
        if (!pauseUIInitialized) {
            initPauseUI();
        }

        Gdx.input.setInputProcessor(pauseStage);
        pauseStage.act(delta);
        pauseStage.draw();
    }

    private void renderGameOverScreen(float delta) {
        if (!endlessGameOverUIInitialized) {
            showEndlessGameOverScreen();
        }

        Gdx.input.setInputProcessor(endlessGameOverStage);
        endlessGameOverStage.act(delta);
        endlessGameOverStage.draw();
    }

    // ===== 暂停功能 =====
    private void togglePause() {
        paused = !paused;

        if (paused) {
            if (pauseStage == null) {
                initPauseUI();
            }
            Gdx.input.setInputProcessor(pauseStage);
        } else {
            Gdx.input.setInputProcessor(null);
        }

        Gdx.app.log("EndlessScreen", paused ? "pause" : "continue");
    }

    // 🔥 修改：使暂停界面与GameScreen一致
    private void initPauseUI() {
        pauseStage = new Stage(new ScreenViewport());
        Table root = new Table();
        root.setFillParent(true);
        pauseStage.addActor(root);

        // 🔥 修改：使用与GameScreen一致的标题
        root.add(new Label("PAUSED", game.getSkin(), "title"))
                .padBottom(40).row();

        ButtonFactory bf = new ButtonFactory(game.getSkin());

        // 🔥 修改：使用与GameScreen一致的按钮尺寸和文本
        root.add(bf.create("continue", this::togglePause))
                .width(400).height(80).padBottom(20).row();

        root.add(bf.create("setting", () -> {
                    // TODO: 打开设置界面
                }))
                .width(400).height(80).padBottom(20).row();

        root.add(bf.create("menu", () -> {
                    game.goToMenu();
                }))
                .width(400).height(80).padBottom(40).row();

        // 如果是无尽模式，显示无尽模式得分
        if (isEndlessMode()) {
            root.add(new Label(
                    "level" + endlessWave + " | score: " + calculateEndlessScore(),
                    game.getSkin()
            ));
        } else {
            root.add(new Label(
                    "score: " + gm.getScore(),
                    game.getSkin()
            ));
        }

        pauseUIInitialized = true;
        if (game.hasRunningGame()) {
            root.add(bf.create("reset", game::resumeGame));
        }
    }

    // ===== LibGDX Screen接口方法 =====
    @Override
    public void resize(int width, int height) {
        System.out.println("📐 EndlessScreen.resize(): " + width + "x" + height);

        // 🔥 确保相机也响应窗口大小变化
        if (cam != null) {
            cam.resize(width, height);
            System.out.println("📷 相机视口更新为: " + cam.getCamera().viewportWidth + "x" + cam.getCamera().viewportHeight);
        }

        if (console != null) console.resize(width, height);

        // 更新暂停界面和游戏结束界面
        if (pauseStage != null) {
            pauseStage.getViewport().update(width, height, true);
        }
        if (endlessGameOverStage != null) {
            endlessGameOverStage.getViewport().update(width, height, true);
        }
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        if (maze != null) maze.dispose();
        if (console != null) console.dispose();
        if (uiTop != null) uiTop.dispose();
        if (uiBottom != null) uiBottom.dispose();
        if (uiLeft != null) uiLeft.dispose();
        if (uiRight != null) uiRight.dispose();
        if (pauseStage != null) pauseStage.dispose();
        if (endlessGameOverStage != null) endlessGameOverStage.dispose();
        heartCreationTimes.clear();
    }
}