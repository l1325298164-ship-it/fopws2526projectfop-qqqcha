package de.tum.cit.fop.maze.entities.boss;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.abilities.Ability;
import de.tum.cit.fop.maze.abilities.AbilityManager;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.boss.config.BossDifficultyFactory;
import de.tum.cit.fop.maze.entities.boss.config.BossMazeConfig;
import de.tum.cit.fop.maze.entities.boss.config.BossMazeConfigLoader;
import de.tum.cit.fop.maze.entities.boss.config.BossMazePhaseSelector;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.maze.BossMazeRenderer;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.screen.MenuScreen;
import de.tum.cit.fop.maze.utils.BossCamera;
import de.tum.cit.fop.maze.utils.CameraManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
public class BossFightScreen implements Screen {

    private static final float BOSS_WIDTH  = 420f;
    private static final float BOSS_HEIGHT = 420f;

    // 合屏动画用
    private float mazeSlideOffsetY = 0f;
    private float mergeProgress = 0f; // 0 → 1
    private static final float MERGE_TIME = 3.6f;
    private float mergeTimer = 0f;

    private enum BossDeathState {
        NONE,
        TRIGGERED,      // 已触发（冻结游戏）
        MERGING_SCREEN, // 分屏 → 全屏
        PLAYING_DEATH,  // Boss 死亡演出 / BGM
        FINISHED        // 切 Screen
    }

    private BossDeathState bossDeathState = BossDeathState.NONE;
    private float bossDeathTimer = 0f;

    private ShapeRenderer shapeRenderer = new ShapeRenderer();

    private enum PhaseTransitionState {
        NONE,        // 正常游戏
        FREEZE,      // 冻结 0.5s
        FADING_OUT,  // 渐暗
        SWITCHING,   // 重建迷宫
        FADING_IN    // 渐亮
    }

    private PhaseTransitionState transitionState = PhaseTransitionState.NONE;
    private float transitionTimer = 0f;

    // 渐变用
    private float fadeAlpha = 0f;
    private static final float FREEZE_TIME = 0.5f;
    private static final float FADE_TIME = 0.4f;

    private BossMazePhaseSelector phaseSelector;
    private BossMazeConfig currentBossConfig;

    // ===== Cameras =====
    private BossCamera bossCamera;
    private CameraManager mazeCameraManager;

    // ===== Viewports =====
    private Viewport bossViewport;
    private Viewport mazeViewport;

    // ===== 下半屏迷宫 =====
    private GameManager gameManager;
    private DifficultyConfig difficultyConfig;
    private MazeRenderer mazeRenderer;

    private Player player;
    private final MazeRunnerGame game;
    private SpriteBatch batch;

    // ===== 占位资源 =====
    private Texture bg;
    private Texture bossTex;

    private float bossX;
    private float bossY;

    // 屏幕尺寸
    private int screenWidth;
    private int screenHeight;

    public BossFightScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        bg = new Texture(Gdx.files.internal("debug/boss_bg.jpg"));
        bossTex = new Texture(Gdx.files.internal("debug/boss.png"));

        currentBossConfig = BossMazeConfigLoader.loadOne("boss/boss_phases.json");
        phaseSelector = new BossMazePhaseSelector(currentBossConfig.phases);

        // ===== boss camera =====
        bossX = 640f - BOSS_WIDTH / 2f;
        bossY = 40f;

        // ✅ 修改：Boss相机使用整个屏幕高度
        bossCamera = new BossCamera(1280, 720);
        bossCamera.getCamera().position.set(640f, 360f, 0f);
        bossCamera.getCamera().update();

        // ✅ 初始化屏幕尺寸
        screenWidth = Gdx.graphics.getWidth();
        screenHeight = Gdx.graphics.getHeight();

        // ✅ 1) 创建 bossViewport - 占整个屏幕
        bossViewport = new FitViewport(1280, 720, bossCamera.getCamera());

        // ✅ 2) 初始化phase
        bossDeathState = BossDeathState.NONE;
        bossDeathTimer = 0f;
        mergeTimer = 0f;
        mergeProgress = 0f;
        mazeSlideOffsetY = 0f;

        transitionState = PhaseTransitionState.NONE;
        transitionTimer = 0f;
        fadeAlpha = 0f;

        applyPhase(phaseSelector.getCurrent());

        // ✅ 3) 创建 mazeViewport - 占整个屏幕（但显示在下半部分）
        mazeViewport = new FitViewport(
                difficultyConfig.mazeWidth * GameConstants.CELL_SIZE,
                difficultyConfig.mazeHeight * GameConstants.CELL_SIZE,
                mazeCameraManager.getCamera()
        );

        // ✅ 4) 重置视口
        resetViewportsToDefault();
    }

    @Override
    public void render(float delta) {
        if (bossDeathState == BossDeathState.NONE && mazeViewport != null) {
            mazeViewport.setScreenPosition(0, 0);
        }

        boolean isMergingOrAfter =
                bossDeathState == BossDeathState.TRIGGERED
                        || bossDeathState == BossDeathState.MERGING_SCREEN
                        || bossDeathState == BossDeathState.PLAYING_DEATH
                        || bossDeathState == BossDeathState.FINISHED;

        if (bossDeathState == BossDeathState.NONE &&
                Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            triggerBossDeath(); // 测试用
        }

        // ===== 测试期：ESC 直接回主菜单 =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            bossDeathState = BossDeathState.NONE;
            mergeProgress = 0f;
            game.setScreen(new MenuScreen(game));
            return;
        }

        update(delta);

        if (!isMazeFrozen()) {
            gameManager.update(delta);
        }

        // ===== 清屏 =====
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // =====================================
        // ✅ 第一层：Boss 演出层（全屏）
        // =====================================
        bossViewport.apply();
        batch.setProjectionMatrix(bossCamera.getCamera().combined);
        batch.begin();

        // ✅ 修改：使用整个屏幕绘制Boss
        // 假设bg是1920x1080，我们需要拉伸到屏幕大小
        batch.draw(bg, 0, 0, bossViewport.getWorldWidth(), bossViewport.getWorldHeight());

        // ✅ 修改：调整Boss位置到屏幕中央
        float worldWidth = bossViewport.getWorldWidth();
        float worldHeight = bossViewport.getWorldHeight();
        float bossWorldX = worldWidth / 2 - BOSS_WIDTH / 2;
        float bossWorldY = 80f; // 离底部一些距离

        batch.draw(
                bossTex,
                bossWorldX,
                bossWorldY,
                BOSS_WIDTH,
                BOSS_HEIGHT
        );
        batch.end();

        // ✅ 调试：绘制中心点
        shapeRenderer.setProjectionMatrix(bossCamera.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1, 0, 0, 1);
        shapeRenderer.circle(worldWidth / 2, worldHeight / 2, 5);
        shapeRenderer.end();

        // =====================================
        // ✅ 第二层：迷宫层（覆盖在下半部分）
        // =====================================
        if (bossDeathState == BossDeathState.NONE
                || bossDeathState == BossDeathState.TRIGGERED
                || bossDeathState == BossDeathState.MERGING_SCREEN) {

            // ✅ 关键：设置迷宫视口的位置和大小
            int mazeScreenHeight = screenHeight / 2;
            int mazeScreenY = 0;

            if (bossDeathState == BossDeathState.MERGING_SCREEN) {
                // 合屏动画中：逐渐向上移动
                int slideOffset = (int)(mergeProgress * (screenHeight / 2));
                mazeScreenY = -slideOffset;
            }

            // 设置视口为屏幕下半部分
            Gdx.gl.glViewport(0, mazeScreenY, screenWidth, mazeScreenHeight);
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            Gdx.gl.glScissor(0, mazeScreenY, screenWidth, mazeScreenHeight);

            // 清除迷宫区域的深度缓存
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

            // 渲染迷宫
            mazeCameraManager.update(delta, gameManager);

            batch.setProjectionMatrix(mazeCameraManager.getCamera().combined);
            batch.begin();

            mazeRenderer.renderFloor(batch);
            for (MazeRenderer.WallGroup g : mazeRenderer.getWallGroups()) {
                mazeRenderer.renderWallGroup(batch, g);
            }

            gameManager.getPlayer().drawSprite(batch);
            for (Enemy e : gameManager.getEnemies()) {
                if (e.isActive()) e.drawSprite(batch);
            }

            batch.end();

            // 关闭裁剪
            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

            // 恢复全屏视口
            Gdx.gl.glViewport(0, 0, screenWidth, screenHeight);
        }

        // =====================================
        // ✅ 渐变效果
        // =====================================
        if (fadeAlpha > 0f && !isMergingOrAfter) {
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shapeRenderer.setProjectionMatrix(
                    mazeCameraManager.getCamera().combined
            );

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0f, 0f, 0f, fadeAlpha);

            shapeRenderer.rect(
                    0,
                    0,
                    difficultyConfig.mazeWidth * GameConstants.CELL_SIZE,
                    difficultyConfig.mazeHeight * GameConstants.CELL_SIZE
            );

            shapeRenderer.end();
            Gdx.gl.glDisable(GL20.GL_BLEND);
        }
    }

    private void update(float delta) {
        switch (transitionState) {
            case NONE -> {
                if (bossDeathState == BossDeathState.NONE &&
                        phaseSelector.update(delta)) {
                    transitionState = PhaseTransitionState.FREEZE;
                    transitionTimer = 0f;
                }
            }

            case FREEZE -> {
                transitionTimer += delta;
                if (transitionTimer >= FREEZE_TIME) {
                    transitionState = PhaseTransitionState.FADING_OUT;
                    transitionTimer = 0f;
                }
            }

            case FADING_OUT -> {
                transitionTimer += delta;
                fadeAlpha = Math.min(1f, transitionTimer / FADE_TIME);

                if (fadeAlpha >= 1f) {
                    transitionState = PhaseTransitionState.SWITCHING;
                }
            }

            case SWITCHING -> {
                if (bossDeathState != BossDeathState.NONE) return;
                applyPhase(phaseSelector.getCurrent());
                transitionState = PhaseTransitionState.FADING_IN;
                transitionTimer = 0f;
            }

            case FADING_IN -> {
                transitionTimer += delta;
                fadeAlpha = 1f - Math.min(1f, transitionTimer / FADE_TIME);

                if (fadeAlpha <= 0f) {
                    fadeAlpha = 0f;
                    transitionState = PhaseTransitionState.NONE;
                }
            }
        }

        updateBossDeath(delta);
    }

    private void applyPhase(BossMazeConfig.Phase phase) {
        // ===============================
        // 1️⃣ 快照旧 Player（如果存在）
        // ===============================
        PlayerSnapshot snapshot = null;

        if (gameManager != null && gameManager.getPlayer() != null) {
            Player p = gameManager.getPlayer();
            snapshot = new PlayerSnapshot();

            snapshot.lives = p.getLives();
            snapshot.mana  = p.getMana();

            // ===== 技能快照 =====
            AbilityManager am = p.getAbilityManager();
            AbilityManagerSnapshot amSnap = new AbilityManagerSnapshot();

            int index = 0;
            Map<String, Ability> abilities = am.getAbilities();
            Map<Ability, Integer> abilityIndexMap = new HashMap<>();

            for (Map.Entry<String, Ability> entry : abilities.entrySet()) {
                Ability a = entry.getValue();

                AbilitySnapshot as = new AbilitySnapshot();
                as.abilityId = entry.getKey();
                as.level = a.getLevel();

                amSnap.abilities.add(as);
                abilityIndexMap.put(a, index++);
            }

            // 记录 slot 装备
            Ability[] slots = am.getAbilitySlots();
            for (int i = 0; i < slots.length; i++) {
                Ability slotAbility = slots[i];
                if (slotAbility != null) {
                    amSnap.equippedSlots[i] = abilityIndexMap.get(slotAbility);
                } else {
                    amSnap.equippedSlots[i] = -1;
                }
            }

            snapshot.abilitySnapshot = amSnap;
        }

        // ===============================
        // 2️⃣ 创建新的 GameManager
        // ===============================
        DifficultyConfig dc =
                BossDifficultyFactory.create(
                        currentBossConfig.base,
                        phase
                );

        this.difficultyConfig = dc;

        if (gameManager != null) {
            gameManager.dispose();
        }

        gameManager = new GameManager(dc, false);
        Player newPlayer = gameManager.getPlayer();

        // ===============================
        // 3️⃣ 恢复 Player 状态
        // ===============================
        if (snapshot != null) {
            newPlayer.setLives(snapshot.lives);
            newPlayer.setMana(snapshot.mana);

            AbilityManager newAM = newPlayer.getAbilityManager();
            AbilityManagerSnapshot amSnap = snapshot.abilitySnapshot;

            // 恢复技能等级
            for (AbilitySnapshot as : amSnap.abilities) {
                Ability a = newAM.getAbilities().get(as.abilityId);
                if (a != null) {
                    a.setLevel(as.level);
                }
            }

            // 恢复 slot 装备
            Ability[] slots = newAM.getAbilitySlots();
            for (int i = 0; i < slots.length; i++) {
                int idx = amSnap.equippedSlots[i];
                if (idx >= 0) {
                    AbilitySnapshot as = amSnap.abilities.get(idx);
                    slots[i] = newAM.getAbilities().get(as.abilityId);
                } else {
                    slots[i] = null;
                }
            }
        }

        // ===============================
        // 4️⃣ 相机 & Renderer
        // ===============================
        mazeCameraManager = new CameraManager(dc);
        mazeCameraManager.centerOnPlayerImmediately(newPlayer);

        mazeRenderer = new BossMazeRenderer(gameManager, dc);

        player = newPlayer;
    }

    @Override
    public void resize(int width, int height) {
        screenWidth = width;
        screenHeight = height;
        resetViewportsToDefault();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void dispose() {
        batch.dispose();
        bg.dispose();
        bossTex.dispose();
        if (gameManager != null) {
            gameManager.dispose();
        }
    }

    private static class PlayerSnapshot {
        int lives;
        float mana;
        AbilityManagerSnapshot abilitySnapshot;
    }

    private static class AbilitySnapshot {
        String abilityId;
        int level;
    }

    private static class AbilityManagerSnapshot {
        List<AbilitySnapshot> abilities = new ArrayList<>();
        int[] equippedSlots = new int[4]; // slot -> index
    }

    private void triggerBossDeath() {
        bossDeathState = BossDeathState.TRIGGERED;
        bossDeathTimer = 0f;

        // 1️⃣ 冻结下半屏逻辑
        transitionState = PhaseTransitionState.NONE; // 防止 phase 切换
    }

    private float deathHoldTimer = 0f;

    private void updateBossDeath(float delta) {
        if (bossDeathState == BossDeathState.NONE) return;

        bossDeathTimer += delta;

        switch (bossDeathState) {
            case TRIGGERED -> {
                if (bossDeathTimer > 0.5f) {
                    bossDeathState = BossDeathState.MERGING_SCREEN;
                    bossDeathTimer = 0f;
                    mergeTimer = 0f;
                }
            }

            case MERGING_SCREEN -> {
                mergeTimer += delta;
                mergeProgress = Math.min(1f, mergeTimer / MERGE_TIME);

                if (mergeProgress >= 1f) {
                    bossDeathState = BossDeathState.PLAYING_DEATH;
                    deathHoldTimer = 0f;
                }
            }

            case PLAYING_DEATH -> {
                deathHoldTimer += delta;
                if (deathHoldTimer > 3.0f) {
                    bossDeathState = BossDeathState.FINISHED;
                }
            }

            case FINISHED -> {
                // TODO: 切到剧情 Screen
                // game.setScreen(new BossEndingStoryScreen(game));
            }
        }
    }

    private boolean isMazeFrozen() {
        return bossDeathState != BossDeathState.NONE
                || transitionState != PhaseTransitionState.NONE;
    }

    private void resetViewportsToDefault() {
        // ✅ Boss视口：全屏
        if (bossViewport != null) {
            bossViewport.update(screenWidth, screenHeight, true);
            bossCamera.getCamera().position.set(
                    bossViewport.getWorldWidth() / 2,
                    bossViewport.getWorldHeight() / 2,
                    0
            );
            bossCamera.getCamera().update();
        }

        // ✅ 迷宫视口：重新创建以适应新尺寸
        if (mazeViewport != null && difficultyConfig != null) {
            mazeViewport.update(
                    screenWidth,
                    screenHeight / 2,
                    true
            );
            mazeCameraManager.centerOnPlayerImmediately(player);
        }
    }
}
