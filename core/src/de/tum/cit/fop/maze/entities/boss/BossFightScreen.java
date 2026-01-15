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
import com.badlogic.gdx.utils.viewport.ExtendViewport;
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
import de.tum.cit.fop.maze.utils.BossMazeCamera;
import de.tum.cit.fop.maze.utils.CameraManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BossFightScreen implements Screen {
    // ===== Tea Cup =====
    private Texture teacupTex;
    // ===== Tea Cup (Boss Fullscreen Layer) =====
    private float teacupWorldX = 640f;   // ← 左右（魔法数字）
    private float teacupWorldY = 230f;   // ← 上下（魔法数字）
    private float teacupSize   = 920f;   // ← 茶杯大小

    // 圆形裁剪参数（世界坐标）
    private float cupRadius;
    private float cupCenterX;
    private float cupCenterY;
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

    private ShapeRenderer shapeRenderer;

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
    private BossMazeCamera bossMazeCamera;

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

    // 屏幕尺寸
    private int screenWidth;
    private int screenHeight;

    // ✅ 迷宫相机的固定视野范围（格子数）
    private static final float MAZE_VIEW_CELLS_WIDTH = 20f;  // 横向看8格
    private static final float MAZE_VIEW_CELLS_HEIGHT = 17f; // 纵向看6格

    public BossFightScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        bg = new Texture(Gdx.files.internal("debug/boss_bg.jpg"));
        bossTex = new Texture(Gdx.files.internal("debug/boss.png"));
        teacupTex = new Texture(Gdx.files.internal("debug/teacup_top.png"));

        currentBossConfig = BossMazeConfigLoader.loadOne("boss/boss_phases.json");
        phaseSelector = new BossMazePhaseSelector(currentBossConfig.phases);

        // ===== boss camera =====
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
    }

    @Override
    public void render(float delta) {


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

        // 使用整个屏幕绘制Boss
        batch.draw(bg, 0, 0, bossViewport.getWorldWidth(), bossViewport.getWorldHeight());

        // 调整Boss位置
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


        // ===== 茶杯（全屏层，但用魔法数字定位）=====
        batch.draw(
                teacupTex,
                teacupWorldX - teacupSize / 2f,
                teacupWorldY - teacupSize / 2f,
                teacupSize,
                teacupSize
        );
        batch.end();

        // =====================================
// ✅ 第二层：全屏迷宫（覆盖在 Boss 上面）
// =====================================
        if (gameManager != null && gameManager.getPlayer() != null) {

            // ❗ 只 apply，不 update
            mazeViewport.apply();

            // ===== 更新迷宫相机（现在终于生效了）=====
            if (bossDeathState == BossDeathState.NONE) {
                bossMazeCamera.update(delta, gameManager.getPlayer());
            }

            OrthographicCamera cam = mazeCameraManager.getCamera();

            // ===== 圆形裁剪参数（迷宫世界坐标）=====
            cupCenterX = cam.position.x;
            cupCenterY = cam.position.y;
            cupRadius  = cam.viewportHeight * cam.zoom * 0.30f;

            // ===== 写入 Stencil（圆形）=====
            Gdx.gl.glEnable(GL20.GL_STENCIL_TEST);
            Gdx.gl.glClearStencil(0);
            Gdx.gl.glClear(GL20.GL_STENCIL_BUFFER_BIT);

            Gdx.gl.glStencilFunc(GL20.GL_ALWAYS, 1, 0xFF);
            Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_REPLACE);
            Gdx.gl.glColorMask(false, false, false, false);

            shapeRenderer.setProjectionMatrix(cam.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.circle(cupCenterX, cupCenterY, cupRadius, 64);
            shapeRenderer.end();

            // ===== 只在圆内画迷宫 =====
            Gdx.gl.glColorMask(true, true, true, true);
            Gdx.gl.glStencilFunc(GL20.GL_EQUAL, 1, 0xFF);
            Gdx.gl.glStencilOp(GL20.GL_KEEP, GL20.GL_KEEP, GL20.GL_KEEP);

            batch.setProjectionMatrix(cam.combined);
            batch.begin();

            mazeRenderer.renderFloor(batch);
            for (MazeRenderer.WallGroup g : mazeRenderer.getWallGroups()) {
                mazeRenderer.renderWallGroup(batch, g);
            }

            Player p = gameManager.getPlayer();
            if (p != null) {
                p.drawSprite(batch);
            }

            for (Enemy e : gameManager.getEnemies()) {
                if (e.isActive()) {
                    e.drawSprite(batch);
                }
            }

            batch.end();
            Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);
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
        gameManager.resetGame();
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
        // 4️⃣ 相机 & Renderer - 关键修正：使用固定视野范围
        // ===============================
        mazeCameraManager = new CameraManager(dc);
        OrthographicCamera cam = mazeCameraManager.getCamera();

        // ✅ 关键修正：设置固定的视野范围（不是缩放整个迷宫）
        // 计算固定视野的世界尺寸
        float viewWorldWidth = MAZE_VIEW_CELLS_WIDTH * GameConstants.CELL_SIZE;
        float viewWorldHeight = MAZE_VIEW_CELLS_HEIGHT * GameConstants.CELL_SIZE;

        // 设置相机的固定视野
        cam.viewportWidth = viewWorldWidth;
        cam.viewportHeight = viewWorldHeight;
        cam.zoom = 1.0f; // 不使用缩放，用固定视野

        // 先更新相机
        cam.update();

        // 居中到玩家
        mazeCameraManager.centerOnPlayerImmediately(newPlayer);

        // 创建Boss战相机控制器
        bossMazeCamera = new BossMazeCamera(cam, dc) {
            @Override
            public void update(float delta, Player player) {
                super.update(delta, player);

                // ✅ 保持相机在迷宫边界内
                float halfViewW = cam.viewportWidth * cam.zoom / 2;
                float halfViewH = cam.viewportHeight * cam.zoom / 2;
                float mazeWidth = dc.mazeWidth * GameConstants.CELL_SIZE;
                float mazeHeight = dc.mazeHeight * GameConstants.CELL_SIZE;

                cam.position.x = Math.max(halfViewW, Math.min(cam.position.x, mazeWidth - halfViewW));
                cam.position.y = Math.max(halfViewH, Math.min(cam.position.y, mazeHeight - halfViewH));
                cam.update();
            }
        };

        mazeRenderer = new BossMazeRenderer(gameManager, dc);
        player = newPlayer;

        // ✅ 关键：使用 ExtendViewport 而不是 FitViewport
        // ExtendViewport会扩展世界而不是缩放

        // 使用 ExtendViewport，设置最小世界尺寸
        mazeViewport = new ExtendViewport(
                viewWorldWidth,  // 最小宽度
                viewWorldHeight, // 最小高度
                cam
        );
    }

    @Override
    public void resize(int width, int height) {
        screenWidth = width;
        screenHeight = height;

        // Boss视口：全屏
        bossViewport.update(width, height, true);

        // ✅ 迷宫视口：使用新的屏幕尺寸
        if (mazeViewport != null) {
            mazeViewport.update(width, height);
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
        batch.dispose();
        shapeRenderer.dispose();
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
        if (mazeViewport != null && mazeCameraManager != null) {
            mazeViewport.update(screenWidth, screenHeight);
            // 设置固定的视野范围
            mazeCameraManager.centerOnPlayerImmediately(player);
        }
    }
}