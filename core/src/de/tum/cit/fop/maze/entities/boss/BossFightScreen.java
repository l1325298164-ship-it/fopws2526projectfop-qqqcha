package de.tum.cit.fop.maze.entities.boss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.abilities.Ability;
import de.tum.cit.fop.maze.abilities.AbilityManager;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.boss.config.*;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.maze.BossMazeRenderer;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.screen.MenuScreen;
import de.tum.cit.fop.maze.utils.BlockingInputProcessor;
import de.tum.cit.fop.maze.utils.BossCamera;
import de.tum.cit.fop.maze.utils.BossMazeCamera;
import de.tum.cit.fop.maze.utils.CameraManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BossFightScreen implements Screen {

    // ===== Intro Delay =====
    private float introDelayTimer = 0f;
    private static final float INTRO_DELAY =10f;
    private static final float INTRO_FADE_TIME = 1.0f;

    private enum BossRageState {
        NORMAL,             // < 90s
        RAGE_WARNING,       // >= 90s 进入狂暴判定
        RAGE_PUNISH,        // 达 50% → 全屏AOE惩罚
        FINAL_LOCKED,       // <5% 锁血无敌
        AUTO_DEATH          // 120s 自动死亡
    }

    private boolean inVictoryHold = false;


    // ===== Cup Shake Runtime =====
    private boolean cupShakeActive = false;
    private float cupShakeTimer = 0f;
    private float cupShakeDuration = 0f;

    private float cupShakeXAmp = 0f;
    private float cupShakeYAmp = 0f;
    private float cupShakeXFreq = 1f;
    private float cupShakeYFreq = 1f;


    // ===== AOE Timeline Runtime =====
    private float aoeCycleTime = 0f;
    private final Map<AoeTimeline.AoePattern, Float> aoeTimers = new HashMap<>();


    private final GlyphLayout glyphLayout = new GlyphLayout();

    private Sound currentDialogueSound;

    private BossTimeline bossTimeline;
    private BossTimelineRunner timelineRunner;


    private BossRageState rageState = BossRageState.NORMAL;

    private float rageAoeTimer = 0f;
    private float rageAoeTickTimer = 0f;
    private static final float RAGE_AOE_DURATION = 2f;

    // Boss 时间轴：永远跑（不要被迷宫冻结影响）
    private float bossTimelineTime = 0f;


    //*-+=== Maze Rebuild Warning =====
    private boolean showMazeWarning = false;
    private float mazeWarningTimer = 0f;
    private static final float MAZE_WARNING_TIME = 10f;
    private BitmapFont uiFont;
    private boolean phaseSwitchQueued = false;



    private OrthographicCamera uiCamera;
    private Texture aoeFillTex;
    private Texture aoeRingTex;
    // ===== Boss HP =====
    private float bossMaxHp = 1000f;
    private float bossHp = bossMaxHp;

    // ===== HUD =====
    private de.tum.cit.fop.maze.ui.HUD hud;

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
    private static final float BOSS_WIDTH  = 1320f;
    private static final float BOSS_HEIGHT = 1120f;

    // 合屏动画用
    private float mazeSlideOffsetY = 0f;
    private float mergeProgress = 0f; // 0 → 1
    private static final float MERGE_TIME = 3.6f;
    private float mergeTimer = 0f;
    // ===== Cup Shake Time =====
    private float phaseTime = 0f;
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
    private static class BossAOE {
        float x;
        float y;

        float radius;

        float life;        // 剩余总时间
        float maxLife;

        float warningTime; // 预警时间（0.5s）
        boolean active;    // 是否已生效（危险）
        boolean damageDone; // 防止一帧扣多次血

        int damage;
    }
    private float rageOverlayPulse = 0f;

    private final List<BossAOE> activeAOEs = new ArrayList<>();
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

    // 屏幕尺寸
    private int screenWidth;
    private int screenHeight;
    // ===== Phase Shake（迷宫切换前用）=====
    private boolean phaseShakeActive = false;
    private float phaseShakeTimer = 0f;
    private float phaseShakeDuration = 0f;

    private float phaseShakeXAmp;
    private float phaseShakeYAmp;
    private float phaseShakeXFreq;
    private float phaseShakeYFreq;

    // ✅ 迷宫相机的固定视野范围（格子数）
    private static final float MAZE_VIEW_CELLS_WIDTH = 20f;  // 横向看8格
    private static final float MAZE_VIEW_CELLS_HEIGHT = 17f; // 纵向看6格
    // ===== Boss Animation =====
    private TextureAtlas bossAtlas;
    private Animation<TextureRegion> bossAnim;
    private float bossAnimTime = 0f;

    public BossFightScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new BlockingInputProcessor());
        uiCamera = new OrthographicCamera();
        uiCamera.setToOrtho(
                false,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );
        uiCamera.update();
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();


        uiFont = game.getSkin().get("default-font", BitmapFont.class);

        var assets = game.getAssets();

// ===== 背景 & 茶杯 =====
        teacupTex = assets.get("debug/teacup_top.png", Texture.class);

// ===== Boss Atlas Animation =====
        bossAtlas = assets.get("bossFight/BOSS_PV.atlas", TextureAtlas.class);

// 因为只有一个动画，直接用全部 regions
        bossAnim = new Animation<>(
                1f / 24f,                  // ⭐ 帧率，自己调（24fps 推荐）
                bossAtlas.getRegions(),
                Animation.PlayMode.LOOP
        );

        aoeFillTex = new Texture(Gdx.files.internal("effects/aoe_fill.png"));
        aoeRingTex = new Texture(Gdx.files.internal("effects/aoe_ring.png"));
        bossTimeline = BossTimelineLoader.load("boss/boss_timeline.json");
        timelineRunner = new BossTimelineRunner(bossTimeline);

        currentBossConfig = BossMazeConfigLoader.loadOne("boss/boss_phases.json");
        phaseSelector = new BossMazePhaseSelector(currentBossConfig.phases);
        if (currentBossConfig.aoeTimeline != null) {
            Gdx.app.log(
                    "BOSS_AOE",
                    "patterns = " + currentBossConfig.aoeTimeline.patterns.size
            );
        }
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
        pendingInitialPhase = phaseSelector.getCurrent();
        gameManager = null;
        aoeTimers.clear();
        aoeCycleTime = 0f;



    }
    private BossMazeConfig.Phase pendingInitialPhase;
    private boolean mazeStarted = false;

    @Override
    public void render(float delta) {
        bossAnimTime += delta;



        if (rageState == BossRageState.RAGE_PUNISH) {
            rageOverlayPulse += delta * 4f; // 呼吸速度
        } else {
            rageOverlayPulse = 0f;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            bossHp -= 50f;
            bossHp = Math.max(0f, bossHp);
            hud.updateBossHp(bossHp);
        }

        if (mazeStarted && mazeViewport != null) {
            Gdx.app.log(
                    "MAZE_VIEWPORT",
                    "screen = " + mazeViewport.getScreenWidth()
                            + " x " + mazeViewport.getScreenHeight()
            );
        }
        boolean isMergingOrAfter =
                bossDeathState == BossDeathState.TRIGGERED
                        || bossDeathState == BossDeathState.MERGING_SCREEN
                        || bossDeathState == BossDeathState.PLAYING_DEATH
                        || bossDeathState == BossDeathState.FINISHED;

        if ( Gdx.input.isKeyJustPressed(Input.Keys.K)) {
            enterVictoryMode();
        }


        // ===== 测试期：ESC 直接回主菜单 =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            bossDeathState = BossDeathState.NONE;
            mergeProgress = 0f;
            game.setScreen(new MenuScreen(game));
            return;
        }

        update(delta);

        if (mazeStarted && !isMazeFrozen()) {
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


        // 调整Boss位置
        float worldWidth = bossViewport.getWorldWidth();
        float worldHeight = bossViewport.getWorldHeight();
        float bossWorldX = worldWidth / 2 - BOSS_WIDTH / 2;
        float bossWorldY = -100f; // 离底部一些距离

        TextureRegion bossFrame = bossAnim.getKeyFrame(bossAnimTime);

        batch.draw(
                bossFrame,
                bossWorldX,
                bossWorldY,
                BOSS_WIDTH,
                BOSS_HEIGHT
        );






        float shakeX = 0f;
        float shakeY = 0f;

        // ===== Boss Timeline / CUP_SHAKE =====
        if (cupShakeActive) {
            float t = cupShakeTimer;
            shakeX += MathUtils.sin(t * cupShakeXFreq) * cupShakeXAmp;
            shakeY += MathUtils.cos(t * cupShakeYFreq) * cupShakeYAmp;
        }

// ===== Phase Transition Shake（迷宫切换前）=====
        if (phaseShakeActive) {
            float t = phaseShakeTimer;
            shakeX += MathUtils.sin(t * phaseShakeXFreq) * phaseShakeXAmp;
            shakeY += MathUtils.cos(t * phaseShakeYFreq) * phaseShakeYAmp;
        }
        // ===== 茶杯（胜利后不再渲染）=====
        if (shouldRenderGameplay()) {
            batch.draw(
                    teacupTex,
                    teacupWorldX - teacupSize / 2f + shakeX,
                    teacupWorldY - teacupSize / 2f + shakeY,
                    teacupSize,
                    teacupSize
            );
        }
        batch.end();

        // =====================================
// ✅ 第二层：全屏迷宫（覆盖在 Boss 上面）
// =====================================


        if (mazeStarted
                && mazeViewport != null
                && mazeCameraManager != null
                && gameManager != null
                && gameManager.getPlayer() != null
                && shouldRenderGameplay()) {

            // ❗ 只 apply，不 update
            mazeViewport.apply();

            // ===== 更新迷宫相机（现在终于生效了）=====
            if (!isMazeFrozen()) {
                bossMazeCamera.update(delta, gameManager.getPlayer());
            }

            OrthographicCamera cam = mazeCameraManager.getCamera();
            cam.update();
            // ===== 圆形裁剪参数（迷宫世界坐标）=====
            cupCenterX = cam.position.x + shakeX;
            cupCenterY = cam.position.y + shakeY;
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

// ===== 椭圆参数 =====
            float ellipseRadius = cupRadius;
            float ellipseScaleX = 1.15f; // ← 左右更宽（1.3 ~ 1.6 都行）
            float ellipseScaleY = 0.85f; // ← 上下更矮（0.75 ~ 0.95 都行）

// 保存原矩阵
            shapeRenderer.identity();

// 平移到中心
            shapeRenderer.translate(cupCenterX, cupCenterY, 0);

// 缩放成椭圆
            shapeRenderer.scale(ellipseScaleX, ellipseScaleY, 1f);

// 画“单位圆”（经过 scale 后就是椭圆）
            shapeRenderer.circle(0, 0, ellipseRadius, 64);

// 恢复
            shapeRenderer.identity();

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


            // =====================================
// ⭐ Items / Pickups Rendering
// =====================================

// 🔑 Keys
            for (Key k : gameManager.getKeys()) {
                if (k != null && k.isActive()) {
                    k.drawSprite(batch);
                }
            }

// ❤️ Hearts
            for (Heart h : gameManager.getHearts()) {
                if (h != null && h.isActive()) {
                    h.drawSprite(batch);
                }
            }

// 💰 Treasures
            for (Treasure t : gameManager.getTreasures()) {
                if (t != null && t.isActive()) {
                    t.drawSprite(batch);
                }
            }

// 📦 Heart Containers（E04 掉落，可选）
            for (HeartContainer hc : gameManager.getHeartContainers()) {
                if (hc != null && hc.isActive()) {
                    hc.drawSprite(batch);
                }
            }







            if (!activeAOEs.isEmpty()) {
                Gdx.gl.glEnable(GL20.GL_BLEND);
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

                batch.setProjectionMatrix(mazeCameraManager.getCamera().combined);


                for (BossAOE aoe : activeAOEs) {
                    float size = aoe.radius * 2f;
                    float drawX = aoe.x - aoe.radius;
                    float drawY = aoe.y - aoe.radius;




                    // ===== 填充 =====
                    batch.setColor(1f, 1f, 1f, 0.35f);
                    batch.draw(aoeFillTex, drawX, drawY, size, size);

                    // ===== 外圈 =====
                    if (aoe.active) {
                        // ⭐ 生效：红色
                        batch.setColor(1f, 0.1f, 0.1f, 0.9f);
                    } else {
                        // 预警：白 / 橙
                        batch.setColor(1f, 0.8f, 0.3f, 0.9f);
                    }

                    batch.draw(aoeRingTex, drawX, drawY, size, size);
                }

                batch.setColor(1f, 1f, 1f, 1f);

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
            if (gameManager.getBobaBulletEffectManager() != null) {
                gameManager.getBobaBulletEffectManager().render(batch);
            }
            if (gameManager.getCombatEffectManager() != null) {
                gameManager.getCombatEffectManager().renderSprites(batch);
            }
            batch.end();

            if (gameManager.getCombatEffectManager() != null) {
                Gdx.gl.glEnable(GL20.GL_BLEND);
                Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

                shapeRenderer.setProjectionMatrix(cam.combined);
                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                gameManager.getCombatEffectManager().renderShapes(shapeRenderer);
                shapeRenderer.end();
            }
            Gdx.gl.glDisable(GL20.GL_STENCIL_TEST);










            if (showMazeWarning) {
                renderMazeRebuildWarning();
            }




// =====================================
// ✅ Boss HUD（血条）
// =====================================
            batch.setProjectionMatrix(uiCamera.combined);
            batch.begin();
            hud.renderInGameUI(batch);
            batch.end();


            if (rageState == BossRageState.RAGE_PUNISH) {
                drawRageOverlay();
            }


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

    private void drawRageOverlay() {
        // 呼吸式 alpha（0.25 ~ 0.45）
        float pulse =
                0.35f
                        + 0.10f * MathUtils.sin(rageOverlayPulse);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // 深红偏黑（不像普通受伤红）
        shapeRenderer.setColor(
                0.35f,   // R
                0.05f,   // G
                0.05f,   // B
                pulse    // A
        );

        shapeRenderer.rect(
                0,
                0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );

        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }



    private float failTimer = 0f;


    private void enterVictoryMode() {
        inVictoryHold = true;

        // ⭐ 明确开始计时
        victoryEndTimer = 0f;

        activeAOEs.clear();
        showMazeWarning = false;
        transitionState = PhaseTransitionState.NONE;
    }


    private void renderMazeRebuildWarning() {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        // 方框尺寸 & 位置（正中偏上）
        float boxW = 420f;
        float boxH = 140f;
        float boxX = w / 2f ;
        float boxY = h * 0.82f;

        // 文字闪烁
        float blink =
                0.75f + 0.25f * MathUtils.sin(mazeWarningTimer * 6f);


        // ===== 画文字 =====
        batch.setProjectionMatrix(uiCamera.combined);
        batch.begin();

        batch.setColor(0.15f, 0.12f, 0.05f, blink);

        String title = "ATTENTION";
        int seconds = MathUtils.ceil(mazeWarningTimer);
        uiFont.getData().setScale(0.5f);
        uiFont.setColor(0.92f, 0.90f, 0.78f, blink);

// 计算文字尺寸
        glyphLayout.setText(uiFont, title);

// ⭐ 居中 X
        float textX = boxX - glyphLayout.width / 2f;

// ⭐ 垂直位置（你原来的逻辑）
        float textY = boxY + boxH - 30f;

        uiFont.draw(batch, glyphLayout, textX, textY);

        uiFont.draw(
                batch,
                String.valueOf(seconds),
                boxX,
                boxY + 40
        );

        batch.setColor(1, 1, 1, 1);
        batch.end();
    }

    // ===== BGM delay =====
    private float pvTimer = 0f;
    private static final float BGM_DELAY = 0.5f;
    private boolean bossBgmStarted = false;

    private void update(float delta) {


        // ===============================
        // 1️⃣ BGM & Boss Timeline —— 永远跑
        // ===============================
        pvTimer += delta;
        if (!bossBgmStarted && pvTimer >= BGM_DELAY) {
            AudioManager.getInstance().playMusic(AudioType.BOSS_BGM);
            bossBgmStarted = true;
        }

        bossTimelineTime += delta;
        timelineRunner.update(bossTimelineTime, this);

        // ===============================
        // 2️⃣ 迷宫延迟生成（20s）
        // ===============================
        if (!mazeStarted) {
            introDelayTimer += delta;

            if (introDelayTimer >= INTRO_DELAY) {
                mazeStarted = true;
                applyPhase(pendingInitialPhase);
            }
            return; // ❗只挡迷宫，不挡 BGM
        }
        if (inVictoryHold) {
            victoryEndTimer += delta;

            if (victoryEndTimer >= VICTORY_PV_TIME) {
                AudioManager.getInstance().stopMusic();
                game.setScreen(new BossStoryScreen(game));
            }
            return;
        }

        // ===============================
        // 3️⃣ 下面才是迷宫 update
        // ===============================
        Player player = gameManager.getPlayer();
        if (checkPlayerDeath(player)) return;

        if (!isMazeFrozen()) {
            gameManager.update(delta);
        }

        updateCupShake(delta);
        updateRagePunish(delta, player);
        updateAoeTimeline(delta, player);
        updatePhaseTransition(delta);
        updateBossDeath(delta);
        updateActiveAOEs(delta, player);
    }


    private boolean checkPlayerDeath(Player player) {
        if (player != null && player.getLives() <= 0) {
            game.setScreen(
                    new BossFailScreen(game, BossFailType.PLAYER_DEAD)
            );
            return true;
        }
        return false;
    }

    private void updateCupShake(float delta) {
        if (!cupShakeActive) return;

        cupShakeTimer += delta;
        if (cupShakeTimer >= cupShakeDuration) {
            cupShakeActive = false;
        }
    }
    private void updateRagePunish(float delta, Player player) {
        if (rageState != BossRageState.RAGE_PUNISH) return;

        rageAoeTimer += delta;
        rageAoeTickTimer += delta;

        if (rageAoeTickTimer >= 0.5f) {
            rageAoeTickTimer = 0f;
            if (player != null) {
                player.takeDamage(5);
            }
        }

        if (rageAoeTimer >= RAGE_AOE_DURATION) {
            rageState = BossRageState.NORMAL;
        }
    }
    private void updateAoeTimeline(float delta, Player player) {
        if (player == null
                || isMazeFrozen()
                || currentBossConfig.aoeTimeline == null) {
            return;
        }

        AoeTimeline aoeTimeline = currentBossConfig.aoeTimeline;
        aoeCycleTime += delta;

        float t = aoeCycleTime % aoeTimeline.cycle;

        for (AoeTimeline.AoePattern pattern : aoeTimeline.patterns) {

            if (t < pattern.start || t > pattern.end) {
                aoeTimers.remove(pattern);
                continue;
            }

            float timer = aoeTimers.getOrDefault(pattern, 0f) + delta;

            if (timer >= pattern.interval) {
                timer = 0f;

                for (int i = 0; i < pattern.count; i++) {
                    spawnTimelineAOE(
                            player,
                            pattern.radius,
                            pattern.damage
                    );
                }
            }

            aoeTimers.put(pattern, timer);
        }
    }
    private void updatePhaseTransition(float delta) {

        if (showMazeWarning) {
            mazeWarningTimer -= delta;
            if (mazeWarningTimer <= 0f) {
                showMazeWarning = false;
                transitionState = PhaseTransitionState.FREEZE;
                transitionTimer = 0f;
            }
        }

        phaseTime += delta;

        if (phaseShakeActive) {
            phaseShakeTimer += delta;
            if (phaseShakeTimer >= phaseShakeDuration) {
                phaseShakeActive = false;
            }
        }

        switch (transitionState) {

            case NONE -> {
                if (!phaseSwitchQueued
                        && bossDeathState == BossDeathState.NONE
                        && phaseSelector.shouldPrepareNextPhase(delta)) {

                    phaseSwitchQueued = true;
                    triggerPhaseShake();
                    showMazeWarning = true;
                    mazeWarningTimer = MAZE_WARNING_TIME;
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
                BossMazeConfig.Phase next = phaseSelector.advanceAndGet();
                applyPhase(next);

                phaseSwitchQueued = false;
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
    }
    private void updateActiveAOEs(float delta, Player player) {
        for (int i = activeAOEs.size() - 1; i >= 0; i--) {
            BossAOE aoe = activeAOEs.get(i);
            aoe.life -= delta;

            if (!aoe.active && aoe.life <= aoe.maxLife - aoe.warningTime) {
                aoe.active = true;
            }

            if (aoe.life <= 0f) {
                activeAOEs.remove(i);
                continue;
            }

            if (aoe.active && !aoe.damageDone && player != null
                    && isPlayerInsideAOE(player, aoe)) {

                player.takeDamage(aoe.damage);
                aoe.damageDone = true;
            }
        }
    }

    private void triggerPhaseShake() {
        phaseShakeActive = true;
        phaseShakeTimer = 0f;
        phaseShakeDuration = 0.6f; // ⭐ 短促但有力

        phaseShakeXAmp = 9f;
        phaseShakeYAmp = 7f;
        phaseShakeXFreq = 2.8f;
        phaseShakeYFreq = 2.4f;
    }


    private void applyPhase(BossMazeConfig.Phase phase) {
        phaseTime = 0f;
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
        Gdx.input.setInputProcessor(null);
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
        mazeViewport.update(screenWidth, screenHeight, false);

// ⭐ phase 切换后，强制对齐相机
        mazeCameraManager.centerOnPlayerImmediately(newPlayer);

// ⭐ 确保 camera 的 combined 是最新的
        mazeCameraManager.getCamera().update();

        aoeCycleTime = 0f;
        aoeTimers.clear();
        gameManager.setEnemyKillListener(enemy -> {
            // 🔥 魔法数字阶段
            dealDamageToBoss(50f);
        });
        hud = new de.tum.cit.fop.maze.ui.HUD(gameManager);
        hud.enableBossHUD(bossMaxHp);
        hud.updateBossHp(bossHp);
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
        if (uiCamera != null) {
            uiCamera.setToOrtho(false, width, height);
            uiCamera.update();
        }
    }
    public void dealDamageToBoss(float damage) {

        if (bossHp <= bossMaxHp * 0.05f && rageState != BossRageState.FINAL_LOCKED) {
            bossHp = bossMaxHp * 0.05f;
            rageState = BossRageState.FINAL_LOCKED;
            hud.setBossFinalLocked(true);
        }

        if (rageState == BossRageState.FINAL_LOCKED) {
            return; // 不再扣血
        }
        bossHp -= damage;
        bossHp = Math.max(0f, bossHp);

        hud.updateBossHp(bossHp);

    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        if (gameManager != null) {
            gameManager.dispose();
        }
        if (currentDialogueSound != null) {
            currentDialogueSound.dispose();
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


            }
        }
    }

    private boolean isMazeFrozen() {
        return bossDeathState != BossDeathState.NONE
                || transitionState != PhaseTransitionState.NONE;
    }




    private void spawnTimelineAOE(Player player, float radius, int damage) {
        if (player == null) return;

        float px =
                player.getX() * GameConstants.CELL_SIZE
                        + GameConstants.CELL_SIZE / 2f;

        float py =
                player.getY() * GameConstants.CELL_SIZE
                        + GameConstants.CELL_SIZE / 2f;

        BossAOE aoe = new BossAOE();
        aoe.x = px;
        aoe.y = py;
        aoe.damage = damage;
        aoe.radius = radius;

        aoe.maxLife = 1.5f;
        aoe.life = aoe.maxLife;

        aoe.warningTime = 1.2f; //aoe 预警
        aoe.active = false;
        aoe.damageDone = false;

        // ⭐ 你以后如果要不同 damage，这里可以扩展 BossAOE
        activeAOEs.add(aoe);
    }


    private boolean isPlayerInsideAOE(Player player, BossAOE aoe) {
        float px =
                player.getX() * GameConstants.CELL_SIZE
                        + GameConstants.CELL_SIZE / 2f;
        float py =
                player.getY() * GameConstants.CELL_SIZE
                        + GameConstants.CELL_SIZE / 2f;

        float dx = px - aoe.x;
        float dy = py - aoe.y;

        return dx * dx + dy * dy <= aoe.radius * aoe.radius;
    }



    public void playBossDialogue(String speaker, String text, String voicePath) {


        if (currentDialogueSound != null) {
            currentDialogueSound.stop();
            currentDialogueSound.dispose();
            currentDialogueSound = null;
        }

        if (voicePath != null && !voicePath.isEmpty()) {
            currentDialogueSound = Gdx.audio.newSound(Gdx.files.internal(voicePath));
            currentDialogueSound.play(1.0f);
        }
    }
// ===============================
// Timeline Interface (FOR RUNNER)
// ===============================

    /** 90s 狂暴检查触发点（目前你逻辑已在 update 里） */
    public void enterRageCheck() {
        // 现在不需要做任何事
        // 真正逻辑仍由 update() 中的 rageChecked 控制
    }



    /** 血量阈值检查（50% 判定） */
    public void handleHpThreshold(float threshold, String failEnding) {
        if (bossHp > bossMaxHp * threshold) {
            game.setScreen(
                    new BossFailScreen(game, BossFailType.DAMAGE_NOT_ENOUGH)
            );
        } else {
            rageState = BossRageState.RAGE_PUNISH;
            rageAoeTimer = 0f;
            rageAoeTickTimer = 0f;
        }
    }

    /** 全屏 AOE（时间轴版） */
    public void startGlobalAoe(float duration, float tickInterval, int damage) {
        rageState = BossRageState.RAGE_PUNISH;
        rageAoeTimer = 0f;
        rageAoeTickTimer = 0f;
    }

    /** 锁定最终血量（5%） */
    public void lockFinalHp(float threshold) {
        if (rageState != BossRageState.FINAL_LOCKED) {
            bossHp = bossMaxHp * threshold;
            rageState = BossRageState.FINAL_LOCKED;
            hud.updateBossHp(bossHp);
        }
    }

    private boolean victoryTriggered = false;

    /** 时间轴结束（≈115s）：若玩家仍存活，进入胜利结算 */
    public void markTimelineFinished() {
        if (victoryTriggered) return;
        victoryTriggered = true;


        // 1️⃣ 进入纯欣赏状态
        enterVictoryMode();

        // 2️⃣ 延迟切 Screen（例如 3 秒）
        victoryEndTimer = 0f;
    }
    private float victoryEndTimer = 0f;
    private static final float VICTORY_PV_TIME = 12f;

    private boolean shouldRenderGameplay() {
        return mazeStarted && !inVictoryHold;
    }

    public void startCupShake(
            float duration,
            float xAmp,
            float yAmp,
            float xFreq,
            float yFreq
    ) {
        cupShakeActive = true;
        cupShakeTimer = 0f;
        cupShakeDuration = duration;

        cupShakeXAmp = xAmp;
        cupShakeYAmp = yAmp;
        cupShakeXFreq = xFreq;
        cupShakeYFreq = yFreq;
    }


}