package de.tum.cit.fop.maze.entities.boss;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
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
    private FitViewport bossViewport;
    private FitViewport mazeViewport;

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

    // ===== 简单状态 =====
    private float bossX = 800;
    private float bossY = 300;

    public BossFightScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        // 占位贴图，之后随时换
        bg = new Texture(Gdx.files.internal("debug/boss_bg.jpg"));
        bossTex = new Texture(Gdx.files.internal("debug/boss.jpg"));

        currentBossConfig = BossMazeConfigLoader.loadOne("boss/boss_phases.json");
// Phase selector 只拿 phases
        phaseSelector = new BossMazePhaseSelector(currentBossConfig.phases);



// 3️⃣ Viewport（不要手动改 Y）
        mazeViewport = new FitViewport(
                GameConstants.CAMERA_VIEW_WIDTH,
                GameConstants.CAMERA_VIEW_HEIGHT / 2f
        );
// 初始化第一个 Phase
        applyPhase(phaseSelector.getCurrent());


// 5️⃣ Boss Camera / Viewport
        bossCamera = new BossCamera(1280, 360);
        bossViewport = new FitViewport(1280, 360, bossCamera.getCamera());


    }

    @Override
    public void render(float delta) {
        if (bossDeathState == BossDeathState.NONE &&
                Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            triggerBossDeath(); // 测试用
        }

        // ===== 测试期：ESC 直接回主菜单 =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
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
        // 上半屏：Boss 演出层
        // =====================================

        bossViewport.apply();

        batch.setProjectionMatrix(bossCamera.getCamera().combined);
        batch.begin();
        batch.draw(bg, 0, 0, 1280, 360);
        batch.draw(bossTex, bossX, bossY);
        batch.end();

        // ================= 下半屏：Maze =================

        mazeViewport.apply();

// 更新世界 & 相机
        mazeCameraManager.update(delta, gameManager);

        batch.setProjectionMatrix(
                mazeCameraManager.getCamera().combined
        );
        batch.begin();

        mazeRenderer.renderFloor(batch);

        for (MazeRenderer.WallGroup g : mazeRenderer.getWallGroups()) {
            mazeRenderer.renderWallGroup(batch, g);
        }

// 玩家 / 敌人 / 陷阱
        gameManager.getPlayer().drawSprite(batch);


        for (Enemy e : gameManager.getEnemies()) {
            if (e.isActive()) {
                e.drawSprite(batch);
            }
        }
        batch.end();





        if (fadeAlpha > 0f) {
            Gdx.gl.glEnable(GL20.GL_BLEND);

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
                // 正常推进 phase 计时
                if (phaseSelector.update(delta)) {
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
        mazeViewport.setCamera(mazeCameraManager.getCamera());

        player = newPlayer;
    }













    @Override
    public void resize(int width, int height) {
        // 上半屏
        bossViewport.update(width, height / 2, true);

        // 下半屏
        mazeViewport.update(width, height / 2, true);

        // 🔥 关键：把 mazeViewport 挪到屏幕底部
        mazeViewport.setScreenPosition(0, 0);
        bossViewport.setScreenPosition(0, height / 2);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

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
    private void updateBossDeath(float delta) {

        if (bossDeathState == BossDeathState.NONE) return;

        bossDeathTimer += delta;

        switch (bossDeathState) {

            case TRIGGERED -> {
                // 冻结迷宫更新
                // gameManager.update 已经被你在 render 中 gate 掉了 👍

                if (bossDeathTimer > 0.5f) {
                    bossDeathState = BossDeathState.MERGING_SCREEN;
                    bossDeathTimer = 0f;
                }
            }

            case MERGING_SCREEN -> {
                // 之后我们会在 render 里真正合屏
                if (bossDeathTimer > 0.6f) {
                    bossDeathState = BossDeathState.PLAYING_DEATH;
                    bossDeathTimer = 0f;

                    // TODO: 播放 Boss 死亡 BGM
                    // game.getAudioManager().playBossDeath();
                }
            }

            case PLAYING_DEATH -> {
                // 给完整演出时间（测试阶段写死）
                if (bossDeathTimer > 3.0f) {
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
        return transitionState != PhaseTransitionState.NONE
                || bossDeathState != BossDeathState.NONE;
    }

}
