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
import de.tum.cit.fop.maze.game.GameConstants;
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

public class EndlessScreen implements Screen {

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
    private float endlessSpawnInterval = 2.5f;       // 🔥 从4秒改为2.5秒（加快40%）
    private boolean endlessGameOver = false;         // 游戏是否结束标志
    private Stage endlessGameOverStage;              // 游戏结束界面舞台
    private boolean endlessGameOverUIInitialized = false; // 游戏结束UI是否初始化

    // 🔥 新增：波次管理系统
    private int totalEnemiesKilledThisWave = 0;      // 本波已击杀敌人
    private int targetEnemiesPerWave = 0;            // 每波目标敌人数量（基于配置）

    // ===== 物品生成系统 =====
    private float itemSpawnTimer = 0f;               // 物品生成计时器
    private final float ITEM_SPAWN_INTERVAL = 30f;   // 🔥 从60秒改为30秒（加快50%）
    private int treasureSpawnCount = 0;              // 已生成宝箱计数
    private int heartSpawnCount = 0;                 // 已生成血量包计数

    // 🔥 新增：全局物品刷新系统
    private float globalItemRespawnTimer = 0f;       // 全局物品刷新计时器
    private final float GLOBAL_ITEM_RESPAWN_INTERVAL = 45f; // 全局刷新间隔45秒
    private final int MAX_TOTAL_ITEMS_ON_MAP = 15;   // 地图上最多同时存在的物品数量

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

    public EndlessScreen(MazeRunnerGame game, DifficultyConfig difficultyConfig) {
        this.game = game;
        this.difficultyConfig = difficultyConfig;
    }

    @Override
    public void show() {
        // 加载UI背景纹理
        uiTop = new Texture("Wallpaper/background.png");
        uiBottom = new Texture("Wallpaper/frontground.png");
        uiLeft = new Texture("Wallpaper/leftground.png");
        uiRight = new Texture("Wallpaper/rightground.png");

        input = new PlayerInputHandler();

        // 初始化核心组件
        batch = game.getSpriteBatch();
        gm = new GameManager(difficultyConfig);
        maze = new MazeRenderer(gm, difficultyConfig);
        cam = new CameraManager(difficultyConfig);
        hud = new HUD(gm);

        // 🔥 添加调试信息
        System.out.println("=== EndlessScreen 初始化 ===");
        Player player = gm.getPlayer();
        if (player != null) {
            System.out.println("玩家初始生命值: " + player.getLives() + "/" + player.getMaxLives());
            System.out.println("玩家是否死亡: " + player.isDead());
        } else {
            System.out.println("警告：玩家对象为空！");
        }

        // 使用反射安全调用 setActiveGameScreen，避免编译错误
        trySetActiveGameScreen();

        cam.centerOnPlayerImmediately(gm.getPlayer());
        console = new DeveloperConsole(gm, game.getSkin());

        // 无尽模式专属初始化
        if (isEndlessMode()) {
            initializeEndlessMode();
        }
    }

    // ===== 安全调用 setActiveGameScreen 的方法 =====
    private void trySetActiveGameScreen() {
        try {
            // 尝试调用 setActiveGameScreen 方法
            Method method = game.getClass().getMethod("setActiveGameScreen", Screen.class);
            method.invoke(game, this);
        } catch (NoSuchMethodException e) {
            // 方法不存在，可能是参数类型不匹配，尝试其他重载
            try {
                // 尝试 GameScreen 参数类型
                Class<?> gameScreenClass = Class.forName("de.tum.cit.fop.maze.screen.GameScreen");
                Method method = game.getClass().getMethod("setActiveGameScreen", gameScreenClass);
                method.invoke(game, this);
            } catch (Exception ex) {
                // 所有尝试都失败，记录日志并继续
                System.out.println("无尽模式：setActiveGameScreen 方法不可用，但这不影响游戏运行");
            }
        } catch (Exception e) {
            // 其他异常，记录并继续
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
        itemSpawnTimer = 0f;
        globalItemRespawnTimer = 0f;
        endlessGameOver = false;
        treasureSpawnCount = 0;
        heartSpawnCount = 0;
        totalEnemiesKilledThisWave = 0;

        // 🔥 根据 DifficultyConfig 计算每波目标
        calculateWaveTargets();

        // 移除不需要的元素（无尽模式没有出口和钥匙）
        gm.getExitDoors().clear();
        gm.getKeys().clear();

        // 🔥 新增：为无尽模式设置玩家初始生命值
        Player player = gm.getPlayer();
        if (player != null) {
            // 重置玩家生命值
            player.reset(); // 这会重置生命值为 200
            System.out.println("无尽模式玩家生命值重置为: " + player.getLives() + "/" + player.getMaxLives());
        }

        // 初始生成敌人（根据 DifficultyConfig 配置）
        spawnInitialEndlessEnemies();

        // 初始生成一些物品
        spawnInitialItems();

        System.out.println("=== 无尽模式初始化（快速节奏版） ===");
        System.out.println("初始血量: " + gm.getPlayer().getLives() + " (100%)");
        System.out.println("初始敌人数量: " + gm.getEnemies().size());
        System.out.println("配置敌人: E01=" + difficultyConfig.enemyE01PearlCount +
                ", E02=" + difficultyConfig.enemyE02CoffeeBeanCount +
                ", E03=" + difficultyConfig.enemyE03CaramelCount);
        System.out.println("快速刷新系统已激活！");
    }

    // 🔥 新增：根据 DifficultyConfig 计算波次目标
    private void calculateWaveTargets() {
        // 从配置中获取基础敌人数量
        int totalBaseEnemies = difficultyConfig.enemyE01PearlCount +
                difficultyConfig.enemyE02CoffeeBeanCount +
                difficultyConfig.enemyE03CaramelCount;

        // 根据波次计算目标敌人数量（随着波次增加而增加）
        targetEnemiesPerWave = (int)(totalBaseEnemies * (1.0f + (endlessWave - 1) * 0.2f));

        System.out.println("=== 波次 " + endlessWave + " 目标计算 ===");
        System.out.println("配置总敌人: " + totalBaseEnemies);
        System.out.println("目标敌人/波: " + targetEnemiesPerWave);
        System.out.println("敌人比例 - E01: " + difficultyConfig.enemyE01PearlCount +
                " (" + String.format("%.1f", (difficultyConfig.enemyE01PearlCount * 100f / totalBaseEnemies)) + "%)");
        System.out.println("敌人比例 - E02: " + difficultyConfig.enemyE02CoffeeBeanCount +
                " (" + String.format("%.1f", (difficultyConfig.enemyE02CoffeeBeanCount * 100f / totalBaseEnemies)) + "%)");
        System.out.println("敌人比例 - E03: " + difficultyConfig.enemyE03CaramelCount +
                " (" + String.format("%.1f", (difficultyConfig.enemyE03CaramelCount * 100f / totalBaseEnemies)) + "%)");
    }

    // ===== 主渲染循环 =====
    @Override
    public void render(float delta) {
        /* ================= 调试快捷键 ================= */
        // 按F1查看玩家状态
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F1)) {
            Player player = gm.getPlayer();
            if (player != null) {
                System.out.println("=== 玩家状态 ===");
                System.out.println("生命值: " + player.getLives() + "/" + player.getMaxLives());
                System.out.println("是否死亡: " + player.isDead());
                System.out.println("无敌状态: " + (player.isInvincible() || player.isDashInvincible()));
                System.out.println("位置: (" + player.getX() + ", " + player.getY() + ")");
                System.out.println("生命值百分比: " + calculatePlayerHealthPercentage() + "%");
            }
        }

        // 按F2直接杀死玩家（测试用）
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F2)) {
            Player player = gm.getPlayer();
            if (player != null) {
                player.takeDamage(1000); // 大量伤害
                System.out.println("玩家受到1000点伤害！");
            }
        }

        // 按F3治疗玩家
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.F3)) {
            Player player = gm.getPlayer();
            if (player != null) {
                player.heal(100);
                System.out.println("玩家恢复100点生命！");
            }
        }

        /* ================= 调试：按T键手动生成敌人 ================= */
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.T)) {
            System.out.println("=== 手动测试生成 ===");
            float healthPercent = calculatePlayerHealthPercentage();
            spawnHealthBasedEnemies(healthPercent);
        }

        /* ================= 调试：按H键手动生成物品 ================= */
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.H)) {
            System.out.println("=== 手动测试生成物品 ===");
            float healthPercent = calculatePlayerHealthPercentage();
            spawnPeriodicItems(healthPercent);
        }

        /* ================= 调试：按G键手动全局刷新物品 ================= */
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.G)) {
            System.out.println("=== 手动全局刷新物品 ===");
            for (int i = 0; i < 3; i++) {
                spawnSingleItemWithPriority();
            }
        }

        /* ================= 调试：按D键查看状态 ================= */
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.D)) {
            System.out.println("=== 状态检查 ===");
            System.out.println("敌人计时器: " + endlessSpawnTimer);
            System.out.println("物品计时器: " + itemSpawnTimer);
            System.out.println("全局物品计时器: " + globalItemRespawnTimer);
            System.out.println("生存时间: " + endlessSurvivalTime);
            System.out.println("当前波次: " + endlessWave);
            System.out.println("当前敌人数量: " + (gm != null ? gm.getEnemies().size() : "N/A"));
            System.out.println("地图物品总数: " + (gm != null ? (gm.getHearts().size() + gm.getTreasures().size()) : "N/A"));
            System.out.println("当前血量百分比: " + calculatePlayerHealthPercentage() + "%");
        }

        // 按R键重置计时器
        if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.R)) {
            System.out.println("=== 重置生成计时器 ===");
            endlessSpawnTimer = 0f;
            itemSpawnTimer = 0f;
            globalItemRespawnTimer = 0f;
        }

        /* ================= 输入处理 ================= */
        handleInput(delta);

        /* ================= 游戏更新 ================= */
        if (!paused && !console.isVisible()) {
            gm.update(delta);  // 更新游戏管理器

            // 如果是无尽模式，更新无尽模式逻辑
            if (isEndlessMode() && !endlessGameOver) {
                updateEndlessMode(delta);
            }
        }

        /* ================= 清屏 ================= */
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);
        batch.setProjectionMatrix(cam.getCamera().combined);

        /* ================= 渲染阶段1：地板和门背景效果 ================= */
        batch.begin();
        maze.renderFloor(batch);

        // 更新时间缩放（控制台功能）
        if (!console.isVisible()) {
            float timeScale = gm.getVariable("time_scale");
            float gameDelta = delta * timeScale;
            gm.update(gameDelta);
            cam.update(gameDelta, gm.getPlayer(), gm);
        }

        // 渲染门背景效果
        List<ExitDoor> exitDoorsCopy = new ArrayList<>(gm.getExitDoors());
        exitDoorsCopy.forEach(d -> d.renderPortalBack(batch));
        batch.end();

        /* ================= 渲染阶段2：玩家传送阵效果 ================= */
        batch.begin();
        if (gm.getPlayerSpawnPortal() != null) {
            float px = (gm.getPlayer().getX() + 0.5f) * GameConstants.CELL_SIZE;
            float py = (gm.getPlayer().getY() + 0.5f) * GameConstants.CELL_SIZE;
            gm.getPlayerSpawnPortal().renderBack(batch, px, py);
            gm.getPlayerSpawnPortal().renderFront(batch);
        }
        batch.end();

        /* ================= 渲染阶段3：实体排序渲染 ================= */
        List<Item> items = prepareRenderItems(exitDoorsCopy);
        items.sort(Comparator.comparingDouble((Item i) -> -i.y)
                .thenComparingInt(i -> i.type.ordinal())
                .thenComparingInt(i -> i.priority));

        batch.begin();
        for (Item it : items) {
            if (it.wall != null) {
                maze.renderWallGroup(batch, it.wall);
            } else {
                it.entity.drawSprite(batch);
            }
        }
        batch.end();

        /* ================= 渲染阶段4：特效和UI ================= */
        batch.begin();
        exitDoorsCopy.forEach(d -> d.renderPortalFront(batch));
        gm.getKeyEffectManager().render(batch);
        gm.getBobaBulletEffectManager().render(batch);
        batch.end();

        /* ================= 渲染UI ================= */
        renderUI();

        // 渲染暂停界面
        if (paused) {
            renderPauseScreen(delta);
            return;
        }

        // 渲染游戏结束界面
        if (endlessGameOver && endlessGameOverStage != null) {
            renderGameOverScreen(delta);
            return;
        }
    }

    // ===== 无尽模式核心方法 =====
    private void updateEndlessMode(float delta) {
        // 更新生存时间
        endlessSurvivalTime += delta;

        // 🔥 更新击杀计数
        updateKillCount();

        // 🔥 首先检查玩家是否死亡
        Player player = gm.getPlayer();
        if (player != null && player.isDead()) {
            endlessGameOver = true;
            showEndlessGameOverScreen();
            return; // 玩家死亡，不再更新游戏逻辑
        }

        // 计算当前生命值百分比
        float healthPercent = calculatePlayerHealthPercentage();

        // 🔥 智能调整生成速度
        int currentEnemies = gm.getEnemies().size();
        int remainingTarget = targetEnemiesPerWave - totalEnemiesKilledThisWave;

        // 如果敌人太少且没有达到目标，加快生成
        if (currentEnemies < 3 && remainingTarget > 0) {
            endlessSpawnTimer += delta * 2.0f; // 加速生成
        } else {
            endlessSpawnTimer += delta;
        }

        // 检查是否需要生成敌人
        float enemyInterval = getDynamicSpawnInterval(healthPercent);
        if (endlessSpawnTimer >= enemyInterval) {
            System.out.println("=== 计时器触发生成敌人 ===");
            System.out.println("当前敌人: " + currentEnemies +
                    ", 本波目标: " + targetEnemiesPerWave +
                    ", 本波已击杀: " + totalEnemiesKilledThisWave);
            spawnHealthBasedEnemies(healthPercent);
            endlessSpawnTimer = 0f; // 重置计时器
        }

        // 🔥 全局物品刷新系统
        updateGlobalItemRespawn(delta);

        // 原有的物品生成计时器（保留，作为额外补充）
        itemSpawnTimer += delta;
        float itemInterval = getItemSpawnInterval(healthPercent);
        if (itemSpawnTimer >= itemInterval) {
            System.out.println("=== 计时器触发生成物品 ===");
            System.out.println("物品计时器: " + itemSpawnTimer + ", 间隔: " + itemInterval);
            spawnPeriodicItems(healthPercent);
            itemSpawnTimer = 0f; // 重置计时器
        }

        // 每5秒输出一次调试信息
        if ((int)endlessSurvivalTime % 5 == 0 && (int)endlessSurvivalTime > 0) {
            System.out.println("时间: " + (int)endlessSurvivalTime + "s | " +
                    "敌人计时器: " + String.format("%.1f", endlessSpawnTimer) + "/" +
                    String.format("%.1f", enemyInterval) + " | " +
                    "物品计时器: " + String.format("%.1f", itemSpawnTimer) + "/" +
                    String.format("%.1f", itemInterval) + " | " +
                    "全局物品计时器: " + String.format("%.1f", globalItemRespawnTimer) + "/" +
                    String.format("%.1f", GLOBAL_ITEM_RESPAWN_INTERVAL) + " | " +
                    "血量: " + (int)healthPercent + "% | " +
                    "敌人: " + gm.getEnemies().size() + " | " +
                    "物品总数: " + (gm.getHearts().size() + gm.getTreasures().size()) + "/" + MAX_TOTAL_ITEMS_ON_MAP + " | " +
                    "波次: " + endlessWave + " | " +
                    "本波击杀: " + totalEnemiesKilledThisWave + "/" + targetEnemiesPerWave);
        }

        // 波次推进（每30秒一波，原来45秒）
        int newWave = 1 + (int)(endlessSurvivalTime / 30f);
        if (newWave > endlessWave) {
            endlessWave = newWave;
            onEndlessWaveAdvanced();
        }
    }

    // 🔥 新增：更新击杀计数
    private void updateKillCount() {
        List<Enemy> enemiesCopy = new ArrayList<>(gm.getEnemies());
        for (Enemy enemy : enemiesCopy) {
            if (enemy.isDead() && !isEnemyCounted(enemy)) {
                endlessKills++;
                totalEnemiesKilledThisWave++;
                endlessScore += calculateEnemyKillScore(enemy);

                markEnemyAsCounted(enemy);

                System.out.println("击杀敌人: " + enemy.getClass().getSimpleName() +
                        ", 总击杀: " + endlessKills +
                        ", 本波击杀: " + totalEnemiesKilledThisWave +
                        ", 得分: " + endlessScore);
            }
        }
    }

    // 🔥 辅助方法：检查敌人是否已被计数
    private boolean isEnemyCounted(Enemy enemy) {
        // 这里可以使用一个Set来记录已计数的敌人ID
        // 简单实现：检查敌人是否还在列表中（死亡敌人会被移除）
        return false; // 简化实现
    }

    private void markEnemyAsCounted(Enemy enemy) {
        // 在实际项目中，可以标记敌人为已计数
        System.out.println("标记敌人为已计数: " + enemy.getClass().getSimpleName());
    }

    private int calculateEnemyKillScore(Enemy enemy) {
        if (enemy instanceof EnemyE01_CorruptedPearl) {
            return 100;
        } else if (enemy instanceof EnemyE02_SmallCoffeeBean) {
            return 150;
        } else if (enemy instanceof EnemyE03_CaramelJuggernaut) {
            return 250;
        }
        return 100; // 默认
    }

    // 🔥 全局物品刷新系统
    private void updateGlobalItemRespawn(float delta) {
        // 全局物品刷新系统
        globalItemRespawnTimer += delta;

        if (globalItemRespawnTimer >= GLOBAL_ITEM_RESPAWN_INTERVAL) {
            System.out.println("=== 全局物品刷新时间到！ ===");

            // 计算需要刷新的物品数量（1-3个）
            int itemsToSpawn = 1 + randomGenerator.nextInt(3); // 生成1-3个物品

            // 根据当前地图上的物品数量调整
            int currentItems = gm.getHearts().size() + gm.getTreasures().size();
            if (currentItems < MAX_TOTAL_ITEMS_ON_MAP / 3) {
                itemsToSpawn += 2; // 物品太少，多生成一些
                System.out.println("地图物品太少，额外增加2个");
            } else if (currentItems > MAX_TOTAL_ITEMS_ON_MAP) {
                itemsToSpawn = 0; // 物品太多，暂时不生成
                System.out.println("地图物品已满(" + currentItems + ")，暂停生成");
            }

            System.out.println("准备生成 " + itemsToSpawn + " 个新物品");

            // 生成物品
            for (int i = 0; i < itemsToSpawn; i++) {
                spawnSingleItemWithPriority();
            }

            globalItemRespawnTimer = 0f; // 重置计时器
        }
    }

    // 🔥 智能生成单个物品
    private void spawnSingleItemWithPriority() {
        float healthPercent = calculatePlayerHealthPercentage();
        float rand = randomGenerator.nextFloat();

        // 🔥 智能判断生成哪种物品
        boolean spawnTreasure;

        if (healthPercent < 30) {
            // 血量<30%：优先生成血量包（80%概率）
            spawnTreasure = rand > 0.8f;
            System.out.println("血量<30%，优先生成血量包");
        }
        else if (healthPercent < 50) {
            // 血量30-50%：平衡生成（60%血量包，40%宝箱）
            spawnTreasure = rand > 0.6f;
            System.out.println("血量30-50%，平衡生成");
        }
        else if (healthPercent < 70) {
            // 血量50-70%：稍微偏向宝箱（40%血量包，60%宝箱）
            spawnTreasure = rand > 0.4f;
            System.out.println("血量50-70%，偏向宝箱");
        }
        else {
            // 血量>70%：主要生成宝箱（20%血量包，80%宝箱）
            spawnTreasure = rand > 0.2f;
            System.out.println("血量>70%，主要生成宝箱");
        }

        int[] pos = findSmartItemSpawnLocation();  // 🔥 使用智能位置查找
        if (pos == null) {
            System.out.println("警告：找不到空位生成物品");
            return;
        }

        try {
            if (spawnTreasure) {
                Treasure treasure = new Treasure(pos[0], pos[1]);
                gm.getTreasures().add(treasure);
                treasureSpawnCount++;
                System.out.println("全局刷新生成宝箱于位置 (" + pos[0] + ", " + pos[1] + ")");
            } else {
                Heart heart = new Heart(pos[0], pos[1]);
                gm.getHearts().add(heart);
                heartSpawnCount++;
                System.out.println("全局刷新生成血量包于位置 (" + pos[0] + ", " + pos[1] + ")");
            }
        } catch (Exception e) {
            System.out.println("全局刷新生成物品失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // 🔥 智能物品位置查找（避免物品堆积）
    private int[] findSmartItemSpawnLocation() {
        Player player = gm.getPlayer();
        if (player == null) return findAnyEmptyCell();

        int playerX = player.getX();
        int playerY = player.getY();

        // 优先在玩家中等距离的位置生成（3-6格）
        for (int attempt = 0; attempt < 50; attempt++) {
            int distance = 3 + randomGenerator.nextInt(4); // 3-6格距离
            int angle = randomGenerator.nextInt(360);

            // 计算候选位置
            int x = playerX + (int)(Math.cos(Math.toRadians(angle)) * distance);
            int y = playerY + (int)(Math.sin(Math.toRadians(angle)) * distance);

            // 确保在迷宫范围内
            x = Math.max(BORDER_THICKNESS,
                    Math.min(difficultyConfig.mazeWidth - BORDER_THICKNESS - 1, x));
            y = Math.max(BORDER_THICKNESS,
                    Math.min(difficultyConfig.mazeHeight - BORDER_THICKNESS - 1, y));

            if (isCellWalkable(x, y) && !isCellOccupied(x, y)) {
                // 🔥 检查附近是否已有其他物品（避免堆积）
                boolean hasNearbyItem = false;
                for (Heart heart : gm.getHearts()) {
                    if (Math.abs(heart.getX() - x) <= 2 && Math.abs(heart.getY() - y) <= 2) {
                        hasNearbyItem = true;
                        break;
                    }
                }
                for (Treasure treasure : gm.getTreasures()) {
                    if (Math.abs(treasure.getX() - x) <= 2 && Math.abs(treasure.getY() - y) <= 2) {
                        hasNearbyItem = true;
                        break;
                    }
                }

                if (!hasNearbyItem) {
                    System.out.println("智能找到位置: (" + x + ", " + y + ")，附近无其他物品");
                    return new int[]{x, y};
                } else {
                    System.out.println("位置 (" + x + ", " + y + ") 附近已有物品，跳过");
                }
            }
        }

        // 如果智能查找失败，回退到普通查找
        System.out.println("智能查找失败，使用普通查找");
        return findEmptyCellForItemSpawn();
    }

    private float calculatePlayerHealthPercentage() {
        Player player = gm.getPlayer();
        if (player == null) {
            System.out.println("警告：玩家对象为空！");
            return 100f;
        }

        // 🔥 使用 player.isInvincible() getter 方法
        int maxLives = player.getMaxLives();
        int currentLives = player.getLives();

        if (maxLives <= 0) {
            System.out.println("警告：玩家最大生命值为0或负数！");
            return 100f;
        }

        float percent = (currentLives / (float)maxLives) * 100f;
        return Math.max(0f, Math.min(100f, percent)); // 确保在0-100之间
    }

    private float getDynamicSpawnInterval(float healthPercent) {
        float interval = endlessSpawnInterval;  // 现在是2.5秒

        // 生命值越低，生成间隔越短（更激进）
        if (healthPercent < 20) {               // 🔥 从30%改为20%
            interval *= 0.3f;    // 生命<20%，生成速度加快70%（原来50%）
        } else if (healthPercent < 40) {        // 🔥 从60%改为40%
            interval *= 0.5f;    // 生命<40%，生成速度加快50%（原来25%）
        } else if (healthPercent < 60) {
            interval *= 0.7f;    // 生命<60%，生成速度加快30%（新增）
        }

        // 波次越高，生成越快（更激进）
        interval -= (endlessWave * 0.2f);  // 🔥 从0.1改为0.2

        // 保证最小生成间隔为0.8秒（原来1秒）
        return Math.max(0.8f, interval);
    }

    private void spawnHealthBasedEnemies(float healthPercent) {
        // 🔥 根据配置比例和目标数量决定生成多少敌人
        int currentEnemies = gm.getEnemies().size();
        int remainingTarget = targetEnemiesPerWave - totalEnemiesKilledThisWave;

        // 基础生成数量考虑血量因素
        int baseCount = calculateSpawnCount(healthPercent);

        // 确保不超过目标数量
        int spawnCount = Math.min(baseCount, Math.max(1, remainingTarget - currentEnemies));

        // 如果已经达到目标，减少生成
        if (remainingTarget <= 0) {
            spawnCount = Math.min(1, currentEnemies < 3 ? 1 : 0); // 保持最小敌人数量
        }

        System.out.println("=== 生成敌人（配置比例版） ===");
        System.out.println("当前血量: " + (int)healthPercent + "%");
        System.out.println("当前敌人: " + currentEnemies + ", 已击杀: " + totalEnemiesKilledThisWave);
        System.out.println("剩余目标: " + remainingTarget + ", 生成数量: " + spawnCount);
        System.out.println("迷宫大小: " + difficultyConfig.mazeWidth + "x" + difficultyConfig.mazeHeight);
        System.out.println("边界厚度: " + BORDER_THICKNESS);

        int actuallySpawned = 0;
        for (int i = 0; i < spawnCount; i++) {
            int[] pos = findEmptyCellForEndlessSpawn();
            if (pos == null) {
                System.out.println("警告：找不到空位生成敌人，尝试次数: " + i);
                continue;
            }

            System.out.println("找到空位 (" + pos[0] + ", " + pos[1] + "), walkable: " +
                    isCellWalkable(pos[0], pos[1]) + ", occupied: " +
                    isCellOccupied(pos[0], pos[1]));

            // 🔥 使用配置比例生成敌人
            Enemy enemy = createEnemyBasedOnConfig(healthPercent, pos[0], pos[1]);
            gm.getEnemies().add(enemy);
            actuallySpawned++;
            System.out.println("生成 " + enemy.getClass().getSimpleName() +
                    " 于位置 (" + pos[0] + ", " + pos[1] + ")");
        }

        System.out.println("生成完成，实际生成: " + actuallySpawned + " 个，总敌人: " + gm.getEnemies().size());
    }

    // 🔥 新增：根据配置比例创建敌人
    private Enemy createEnemyBasedOnConfig(float healthPercent, int x, int y) {
        // 计算配置中的总敌人数量和比例
        int totalConfig = difficultyConfig.enemyE01PearlCount +
                difficultyConfig.enemyE02CoffeeBeanCount +
                difficultyConfig.enemyE03CaramelCount;

        if (totalConfig == 0) {
            // 如果没有配置，回退到原来的血量逻辑
            return createEnemyBasedOnHealth(healthPercent, x, y);
        }

        float e01Ratio = difficultyConfig.enemyE01PearlCount / (float)totalConfig;
        float e02Ratio = difficultyConfig.enemyE02CoffeeBeanCount / (float)totalConfig;
        float e03Ratio = difficultyConfig.enemyE03CaramelCount / (float)totalConfig;

        // 生成随机数决定敌人类型
        float rand = randomGenerator.nextFloat();

        // 🔥 根据血量调整比例（血量低时增加高级敌人比例）
        float healthFactor = 1.0f - (healthPercent / 100f); // 血量越低，因子越高
        float adjustedE03Ratio = e03Ratio * (1.0f + healthFactor * 0.5f); // 血量低时增加50%
        float adjustedE02Ratio = e02Ratio * (1.0f + healthFactor * 0.3f); // 血量低时增加30%
        float adjustedE01Ratio = 1.0f - adjustedE02Ratio - adjustedE03Ratio; // 剩余的给E01

        // 确保比例有效
        adjustedE01Ratio = Math.max(0.1f, adjustedE01Ratio);
        adjustedE02Ratio = Math.max(0.1f, adjustedE02Ratio);
        adjustedE03Ratio = Math.max(0.1f, adjustedE03Ratio);

        // 归一化
        float sum = adjustedE01Ratio + adjustedE02Ratio + adjustedE03Ratio;
        adjustedE01Ratio /= sum;
        adjustedE02Ratio /= sum;
        adjustedE03Ratio /= sum;

        System.out.println(String.format("敌人比例调整 - E01:%.1f%% E02:%.1f%% E03:%.1f%%",
                adjustedE01Ratio*100, adjustedE02Ratio*100, adjustedE03Ratio*100));

        try {
            if (rand < adjustedE01Ratio) {
                return new EnemyE01_CorruptedPearl(x, y);
            } else if (rand < adjustedE01Ratio + adjustedE02Ratio) {
                return new EnemyE02_SmallCoffeeBean(x, y);
            } else {
                return new EnemyE03_CaramelJuggernaut(x, y);
            }
        } catch (Exception e) {
            System.out.println("创建敌人失败: " + e.getMessage());
            e.printStackTrace();
            return new EnemyE01_CorruptedPearl(x, y); // 默认返回普通敌人
        }
    }

    // ===== 根据血量精确调整敌人数量 =====
    private int calculateSpawnCount(float healthPercent) {
        int baseCount = 2;  // 基础生成数量

        // 🔥 根据血量百分比精确调整敌人数量
        if (healthPercent >= 80) {
            // 血量80%以上：轻松模式，敌人很少
            baseCount = 1 + randomGenerator.nextInt(2);  // 1-2个
        }
        else if (healthPercent >= 60) {
            // 血量60-80%：正常模式
            baseCount = 2 + randomGenerator.nextInt(2);  // 2-3个
        }
        else if (healthPercent >= 40) {
            // 血量40-60%：中等压力
            baseCount = 3 + randomGenerator.nextInt(2);  // 3-4个
        }
        else if (healthPercent >= 20) {
            // 血量20-40%：高压模式
            baseCount = 4 + randomGenerator.nextInt(3);  // 4-6个
        }
        else {
            // 血量<20%：绝望模式，大量敌人
            baseCount = 6 + randomGenerator.nextInt(4);  // 6-9个
        }

        // 波次越高，额外敌人越多
        int waveBonus = endlessWave / 3;  // 每3波加1个敌人
        int total = baseCount + waveBonus;

        // 🔥 添加随机爆发：偶尔会有大批敌人
        if (randomGenerator.nextFloat() < 0.1f) { // 10%几率
            total += 2 + randomGenerator.nextInt(3); // 额外2-4个
            System.out.println("随机爆发！额外增加敌人");
        }

        return Math.max(1, total); // 确保至少生成1个敌人
    }

    // ===== 根据血量精确调整敌人强度 =====
    private Enemy createEnemyBasedOnHealth(float healthPercent, int x, int y) {
        float randValue = randomGenerator.nextFloat() * 100f;

        try {
            if (healthPercent >= 80) {
                // 血量>80%：90%普通敌人，10%中等敌人
                if (randValue < 90) {
                    return new EnemyE01_CorruptedPearl(x, y);  // 普通
                } else {
                    return new EnemyE02_SmallCoffeeBean(x, y); // 中等
                }
            }
            else if (healthPercent >= 60) {
                // 血量60-80%：70%普通，25%中等，5%困难
                if (randValue < 70) {
                    return new EnemyE01_CorruptedPearl(x, y);
                } else if (randValue < 95) {
                    return new EnemyE02_SmallCoffeeBean(x, y);
                } else {
                    return new EnemyE03_CaramelJuggernaut(x, y); // 困难
                }
            }
            else if (healthPercent >= 40) {
                // 血量40-60%：50%普通，40%中等，10%困难
                if (randValue < 50) {
                    return new EnemyE01_CorruptedPearl(x, y);
                } else if (randValue < 90) {
                    return new EnemyE02_SmallCoffeeBean(x, y);
                } else {
                    return new EnemyE03_CaramelJuggernaut(x, y);
                }
            }
            else if (healthPercent >= 20) {
                // 血量20-40%：30%普通，50%中等，20%困难
                if (randValue < 30) {
                    return new EnemyE01_CorruptedPearl(x, y);
                } else if (randValue < 80) {
                    return new EnemyE02_SmallCoffeeBean(x, y);
                } else {
                    return new EnemyE03_CaramelJuggernaut(x, y);
                }
            }
            else {
                // 血量<20%：10%普通，40%中等，50%困难（绝望模式）
                if (randValue < 10) {
                    return new EnemyE01_CorruptedPearl(x, y);
                } else if (randValue < 50) {
                    return new EnemyE02_SmallCoffeeBean(x, y);
                } else {
                    return new EnemyE03_CaramelJuggernaut(x, y);
                }
            }
        } catch (Exception e) {
            System.out.println("创建敌人失败: " + e.getMessage());
            e.printStackTrace();
            return new EnemyE01_CorruptedPearl(x, y); // 默认返回普通敌人
        }
    }

    // ===== 修复：检查格子是否被占用（只检查活跃的实体） =====
    private boolean isCellOccupied(int x, int y) {
        // 检查玩家
        Player player = gm.getPlayer();
        if (player != null && !player.isDead() && player.getX() == x && player.getY() == y) {
            return true;
        }

        // 只检查活跃的敌人（非死亡状态）
        for (Enemy enemy : gm.getEnemies()) {
            if (enemy != null && !enemy.isDead() && enemy.getX() == x && enemy.getY() == y) {
                return true;
            }
        }

        // 检查所有生命包
        for (Heart heart : gm.getHearts()) {
            if (heart != null && heart.isActive() && heart.getX() == x && heart.getY() == y) {
                return true;
            }
        }

        // 检查所有宝箱
        for (Treasure treasure : gm.getTreasures()) {
            if (treasure != null && treasure.isActive() && treasure.getX() == x && treasure.getY() == y) {
                return true;
            }
        }

        // 检查所有钥匙
        for (Key key : gm.getKeys()) {
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

        for (int attempt = 0; attempt < 200; attempt++) {
            // 使用 randomGenerator
            int x = BORDER_THICKNESS + randomGenerator.nextInt(
                    difficultyConfig.mazeWidth - BORDER_THICKNESS * 2
            );
            int y = BORDER_THICKNESS + randomGenerator.nextInt(
                    difficultyConfig.mazeHeight - BORDER_THICKNESS * 2
            );

            // 使用修复后的 isCellOccupied 方法
            if (isCellWalkable(x, y) && !isCellOccupied(x, y)) {
                // 尽量远离玩家（至少3格距离）
                if (Math.abs(x - playerX) > 3 || Math.abs(y - playerY) > 3) {
                    return new int[]{x, y};
                }
            }
        }

        // 如果找不到合适位置，放宽条件再试50次
        for (int attempt = 0; attempt < 50; attempt++) {
            int x = BORDER_THICKNESS + randomGenerator.nextInt(difficultyConfig.mazeWidth - BORDER_THICKNESS * 2);
            int y = BORDER_THICKNESS + randomGenerator.nextInt(difficultyConfig.mazeHeight - BORDER_THICKNESS * 2);

            if (isCellWalkable(x, y) && !isCellOccupied(x, y)) {
                System.out.println("找到备选空位: (" + x + ", " + y + ")");
                return new int[]{x, y};
            }
        }

        System.out.println("警告：找不到有效空位生成敌人！");
        return null; // 返回null让调用者处理
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

    // ===== 修复：findAnyEmptyCell 可能返回 null =====
    private int[] findAnyEmptyCell() {
        int width = difficultyConfig.mazeWidth;
        int height = difficultyConfig.mazeHeight;
        int maxAttempts = width * height * 2; // 尝试更多次

        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            int x = BORDER_THICKNESS + randomGenerator.nextInt(width - BORDER_THICKNESS * 2);
            int y = BORDER_THICKNESS + randomGenerator.nextInt(height - BORDER_THICKNESS * 2);

            if (isCellWalkable(x, y) && !isCellOccupied(x, y)) {
                return new int[]{x, y};
            }
        }

        // 如果实在找不到，尝试遍历所有单元格
        for (int y = BORDER_THICKNESS; y < height - BORDER_THICKNESS; y++) {
            for (int x = BORDER_THICKNESS; x < width - BORDER_THICKNESS; x++) {
                if (isCellWalkable(x, y) && !isCellOccupied(x, y)) {
                    return new int[]{x, y};
                }
            }
        }

        System.out.println("严重警告：迷宫已满，无法找到空位！");
        return null; // 返回null让调用者处理
    }

    // ===== 修复：spawnEndlessHealthPack 处理 null 情况 =====
    private void spawnEndlessHealthPack() {
        int[] pos = findEmptyCellForItemSpawn();
        if (pos == null) {
            System.out.println("警告：找不到空位生成生命包");
            return;
        }

        try {
            Heart heart = new Heart(pos[0], pos[1]);
            gm.getHearts().add(heart);
            heartSpawnCount++;
            System.out.println("生成生命包于位置 (" + pos[0] + ", " + pos[1] + ")");
        } catch (Exception e) {
            System.out.println("生成生命包失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===== 根据 DifficultyConfig 生成初始敌人 =====
    private void spawnInitialEndlessEnemies() {
        System.out.println("=== 生成初始敌人（根据配置） ===");

        // 从 difficultyConfig 读取配置
        int e01Count = difficultyConfig.enemyE01PearlCount;
        int e02Count = difficultyConfig.enemyE02CoffeeBeanCount;
        int e03Count = difficultyConfig.enemyE03CaramelCount;

        System.out.println("配置敌人数量 - E01: " + e01Count + ", E02: " + e02Count + ", E03: " + e03Count);

        int actuallySpawned = 0;

        // 生成 E01 敌人
        for (int i = 0; i < e01Count; i++) {
            int[] pos = findEmptyCellForEndlessSpawn();
            if (pos != null) {
                try {
                    gm.getEnemies().add(new EnemyE01_CorruptedPearl(pos[0], pos[1]));
                    actuallySpawned++;
                    System.out.println("初始敌人 E01 " + (i+1) + " 于位置 (" + pos[0] + ", " + pos[1] + ")");
                } catch (Exception e) {
                    System.out.println("初始敌人生成失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // 生成 E02 敌人
        for (int i = 0; i < e02Count; i++) {
            int[] pos = findEmptyCellForEndlessSpawn();
            if (pos != null) {
                try {
                    gm.getEnemies().add(new EnemyE02_SmallCoffeeBean(pos[0], pos[1]));
                    actuallySpawned++;
                    System.out.println("初始敌人 E02 " + (i+1) + " 于位置 (" + pos[0] + ", " + pos[1] + ")");
                } catch (Exception e) {
                    System.out.println("初始敌人生成失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        // 生成 E03 敌人
        for (int i = 0; i < e03Count; i++) {
            int[] pos = findEmptyCellForEndlessSpawn();
            if (pos != null) {
                try {
                    gm.getEnemies().add(new EnemyE03_CaramelJuggernaut(pos[0], pos[1]));
                    actuallySpawned++;
                    System.out.println("初始敌人 E03 " + (i+1) + " 于位置 (" + pos[0] + ", " + pos[1] + ")");
                } catch (Exception e) {
                    System.out.println("初始敌人生成失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        System.out.println("初始敌人生成完成，实际生成: " + actuallySpawned + " 个，总敌人: " + gm.getEnemies().size());
        System.out.println("预期生成: " + (e01Count + e02Count + e03Count) + " 个");
    }

    private void onEndlessWaveAdvanced() {
        System.out.println("=== 无尽模式第 " + endlessWave + " 波 ===");

        // 🔥 重新计算新波次的目标
        calculateWaveTargets();
        totalEnemiesKilledThisWave = 0; // 重置本波击杀计数

        // 波次奖励敌人（基于配置比例）
        int waveBonusEnemies = endlessWave; // 每波增加1个敌人

        int actuallySpawned = 0;
        for (int i = 0; i < waveBonusEnemies; i++) {
            int[] pos = findEmptyCellForEndlessSpawn();
            if (pos != null) {
                try {
                    // 🔥 波次奖励也使用配置比例
                    Enemy enemy = createEnemyBasedOnConfig(50.0f, pos[0], pos[1]); // 使用中等血量参数
                    gm.getEnemies().add(enemy);
                    actuallySpawned++;
                    System.out.println("波次奖励敌人 " + (i+1) + ": " + enemy.getClass().getSimpleName());
                } catch (Exception e) {
                    System.out.println("波次敌人生成失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        System.out.println("波次敌人生成完成，实际生成: " + actuallySpawned + " 个");

        // 🔥 每2波生成生命包（条件：实际生成了敌人）
        if (endlessWave % 2 == 0 && actuallySpawned > 0) {
            spawnEndlessHealthPack();
        }
    }

    // ===== 物品生成系统 =====

    private float getItemSpawnInterval(float healthPercent) {
        float baseInterval = ITEM_SPAWN_INTERVAL;  // 现在是30秒

        // 生命值越低，物品生成越快（更激进）
        if (healthPercent < 20) {                 // 🔥 从30%改为20%
            baseInterval *= 0.4f;    // 生命<20%，物品生成更快（60%→40%）
        } else if (healthPercent < 40) {          // 🔥 从60%改为40%
            baseInterval *= 0.6f;    // 生命<40%，物品生成稍快（80%→60%）
        } else if (healthPercent < 60) {
            baseInterval *= 0.8f;    // 新增中间档
        }

        // 波次越高，物品生成越频繁（更激进）
        baseInterval -= (endlessWave * 1.0f);  // 🔥 从0.5改为1.0

        // 保证最小生成间隔为15秒（原来30秒）
        return Math.max(15f, baseInterval);
    }

    private void spawnPeriodicItems(float healthPercent) {
        System.out.println("=== 生成补给物品（快速版） ===");

        // 根据生命值决定生成数量
        int itemCount = calculateItemSpawnCount(healthPercent);
        System.out.println("当前血量: " + (int)healthPercent + "%，生成数量: " + itemCount);

        int actuallySpawned = 0;
        for (int i = 0; i < itemCount; i++) {
            int[] pos = findSmartItemSpawnLocation();  // 🔥 使用智能位置查找
            if (pos == null) {
                System.out.println("警告：找不到空位生成物品");
                continue;
            }

            // 决定生成宝箱还是血量包
            boolean spawnTreasure = shouldSpawnTreasure(healthPercent);

            try {
                if (spawnTreasure) {
                    Treasure treasure = new Treasure(pos[0], pos[1]);
                    gm.getTreasures().add(treasure);
                    treasureSpawnCount++;
                    actuallySpawned++;
                    System.out.println("生成宝箱于位置 (" + pos[0] + ", " + pos[1] + ")");
                } else {
                    Heart heart = new Heart(pos[0], pos[1]);
                    gm.getHearts().add(heart);
                    heartSpawnCount++;
                    actuallySpawned++;
                    System.out.println("生成血量包于位置 (" + pos[0] + ", " + pos[1] + ")");
                }
            } catch (Exception e) {
                System.out.println("生成物品失败: " + e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println("物品生成完成，实际生成: " + actuallySpawned + " 个");
    }

    private int calculateItemSpawnCount(float healthPercent) {
        int baseCount = 2;  // 🔥 默认生成2个（原来1个）

        // 生命值越低，生成越多补给
        if (healthPercent < 20) {
            baseCount = 4;  // 生命<20%，生成4个（原来3个）
        } else if (healthPercent < 40) {
            baseCount = 3;  // 生命<40%，生成3个（原来2个）
        } else if (healthPercent < 60) {
            baseCount = 2;  // 生命<60%，生成2个（原来1个）
        }

        // 波次越高，偶尔多生成一些
        if (endlessWave % 3 == 0) {  // 每3波额外多一个
            baseCount++;
        }

        return Math.max(2, baseCount); // 🔥 确保至少生成2个
    }

    private boolean shouldSpawnTreasure(float healthPercent) {
        float rand = randomGenerator.nextFloat() * 100f;

        // 根据生命值调整概率
        if (healthPercent < 30) {
            // 生命<30%：70%概率血量包，30%概率宝箱（更需要回血）
            return rand > 70;
        } else if (healthPercent < 60) {
            // 生命<60%：50%概率血量包，50%概率宝箱
            return rand > 50;
        } else {
            // 生命>60%：30%概率血量包，70%概率宝箱（更需要增强）
            return rand > 30;
        }
    }

    private int[] findEmptyCellForItemSpawn() {
        Player player = gm.getPlayer();
        if (player == null) return findAnyEmptyCell();

        int playerX = player.getX();
        int playerY = player.getY();

        for (int attempt = 0; attempt < 100; attempt++) {
            int x = BORDER_THICKNESS + randomGenerator.nextInt(
                    difficultyConfig.mazeWidth - BORDER_THICKNESS * 2
            );
            int y = BORDER_THICKNESS + randomGenerator.nextInt(
                    difficultyConfig.mazeHeight - BORDER_THICKNESS * 2
            );

            if (isCellWalkable(x, y) && !isCellOccupied(x, y)) {
                // 物品可以离玩家近一些（2-8格距离）
                int distance = Math.abs(x - playerX) + Math.abs(y - playerY);
                if (distance >= 2 && distance <= 8) {
                    return new int[]{x, y};
                }
            }
        }

        return findAnyEmptyCell();
    }

    private void spawnInitialItems() {
        System.out.println("=== 生成初始物品 ===");
        // 初始生成3个血量包和2个宝箱
        int heartSpawned = 0;
        int treasureSpawned = 0;

        for (int i = 0; i < 3; i++) {  // 🔥 从2个改为3个
            int[] pos = findSmartItemSpawnLocation();
            if (pos != null) {
                try {
                    Heart heart = new Heart(pos[0], pos[1]);
                    gm.getHearts().add(heart);
                    heartSpawnCount++;
                    heartSpawned++;
                    System.out.println("初始血量包 " + (i+1) + " 于位置 (" + pos[0] + ", " + pos[1] + ")");
                } catch (Exception e) {
                    System.out.println("初始血量包生成失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        for (int i = 0; i < 2; i++) {  // 🔥 从1个改为2个
            int[] pos = findSmartItemSpawnLocation();
            if (pos != null) {
                try {
                    Treasure treasure = new Treasure(pos[0], pos[1]);
                    gm.getTreasures().add(treasure);
                    treasureSpawnCount++;
                    treasureSpawned++;
                    System.out.println("初始宝箱 " + (i+1) + " 于位置 (" + pos[0] + ", " + pos[1] + ")");
                } catch (Exception e) {
                    System.out.println("初始宝箱生成失败: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        System.out.println("初始物品生成完成：血量包 " + heartSpawned + " 个，宝箱 " + treasureSpawned + " 个");
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

        root.add(new Label("无尽模式 - 游戏结束", game.getSkin(), "title"))
                .padBottom(40).row();

        root.add(new Label(
                String.format("生存时间: %02d分%02d秒",
                        (int)endlessSurvivalTime / 60,
                        (int)endlessSurvivalTime % 60),
                game.getSkin()
        )).padBottom(10).row();

        root.add(new Label(
                String.format("最终波次: %d", endlessWave),
                game.getSkin()
        )).padBottom(10).row();

        root.add(new Label(
                String.format("击杀敌人: %d", endlessKills),
                game.getSkin()
        )).padBottom(10).row();

        // 新增：显示物品生成统计
        root.add(new Label(
                String.format("生成宝箱: %d | 生成血量包: %d",
                        treasureSpawnCount, heartSpawnCount),
                game.getSkin()
        )).padBottom(10).row();

        root.add(new Label(
                String.format("最终得分: %d", finalScore),
                game.getSkin()
        )).padBottom(40).row();

        ButtonFactory bf = new ButtonFactory(game.getSkin());

        root.add(bf.create("再玩一次", () -> {
            game.startNewGame(Difficulty.ENDLESS);
            game.goToGame();
        })).width(400).height(80).padBottom(20).row();

        root.add(bf.create("主菜单", () -> {
            game.goToMenu();
        })).width(400).height(80).row();

        Gdx.input.setInputProcessor(endlessGameOverStage);
        endlessGameOverUIInitialized = true;
    }

    // ===== 辅助方法 =====
    private boolean isEndlessMode() {
        return difficultyConfig.exitCount == 0; // 无尽模式没有出口
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
                public void onMoveInput(int dx, int dy) {
                    gm.onMoveInput(dx, dy);
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
            });
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

        // 如果是无尽模式，显示额外信息
        if (isEndlessMode() && !endlessGameOver) {
            renderEndlessHUD(batch);
        }

        hud.renderInGameUI(batch);
        batch.end();
        hud.renderManaBar();

        if (console != null) {
            console.render();
        }

        batch.setProjectionMatrix(cam.getCamera().combined);
    }

    private void renderEndlessHUD(SpriteBatch batch) {
        float healthPercent = calculatePlayerHealthPercentage();

        // 🔥 显示更详细的信息
        String endlessInfo = String.format(
                "波次: %d | 时间: %02d:%02d | 击杀: %d\n" +
                        "生命: %.0f%% | 本波: %d/%d | 地图物品: %d/%d",
                endlessWave,
                (int)endlessSurvivalTime / 60,
                (int)endlessSurvivalTime % 60,
                endlessKills,
                healthPercent,
                totalEnemiesKilledThisWave,
                targetEnemiesPerWave,
                gm.getHearts().size() + gm.getTreasures().size(),
                MAX_TOTAL_ITEMS_ON_MAP
        );

        // 使用游戏皮肤中的字体
        Label.LabelStyle style = game.getSkin().get(Label.LabelStyle.class);
        if (style != null && style.font != null) {
            // 分行显示
            String[] lines = endlessInfo.split("\n");
            float y = Gdx.graphics.getHeight() - 40;
            for (String line : lines) {
                style.font.draw(batch, line, 20, y);
                y -= 25; // 行间距
            }
        } else {
            // 备用：使用默认字体
            game.getSpriteBatch().begin();
            //game.getSpriteBatch().drawString(endlessInfo, 20, Gdx.graphics.getHeight() - 40);
            game.getSpriteBatch().end();
        }
    }

    private void renderMazeBorderDecorations(SpriteBatch batch) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        int thickness = 1000;

        batch.draw(uiTop, 0, h - thickness, w, thickness);
        batch.draw(uiBottom, 0, 0, w, thickness);
        batch.draw(uiLeft, -50, 0, thickness + 400, h);
        batch.draw(uiRight, w - thickness - 200, 0, thickness + 300, h);
    }

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

        Gdx.app.log("EndlessScreen", paused ? "暂停" : "继续");
    }

    private void initPauseUI() {
        pauseStage = new Stage(new ScreenViewport());
        Table root = new Table();
        root.setFillParent(true);
        pauseStage.addActor(root);

        root.add(new Label("暂停", game.getSkin(), "title"))
                .padBottom(40).row();

        ButtonFactory bf = new ButtonFactory(game.getSkin());

        root.add(bf.create("继续", this::togglePause))
                .width(400).height(80).padBottom(20).row();

        root.add(bf.create("设置", () -> {
                    // TODO: 打开设置界面
                }))
                .width(400).height(80).padBottom(20).row();

        root.add(bf.create("返回主菜单", () -> {
                    game.goToMenu();
                }))
                .width(400).height(80).padBottom(40).row();

        // 如果是无尽模式，显示无尽模式得分
        if (isEndlessMode()) {
            root.add(new Label(
                    "波次: " + endlessWave + " | 本波击杀: " + totalEnemiesKilledThisWave +
                            " | 得分: " + calculateEndlessScore(),
                    game.getSkin()
            ));
        } else {
            root.add(new Label(
                    "得分: " + gm.getScore(),
                    game.getSkin()
            ));
        }

        pauseUIInitialized = true;
        if (game.hasRunningGame()) {
            root.add(bf.create("重置迷宫", game::resumeGame));
        }
    }

    // ===== LibGDX Screen接口方法 =====
    @Override
    public void resize(int width, int height) {
        if (console != null) console.resize(width, height);
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
    }
}