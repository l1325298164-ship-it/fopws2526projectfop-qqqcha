package de.tum.cit.fop.maze.entities.boss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.abilities.Ability;
import de.tum.cit.fop.maze.abilities.AbilityManager;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.boss.config.*;
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
    private enum BossRageState {
        NORMAL,             // < 90s
        RAGE_WARNING,       // >= 90s 进入狂暴判定
        MAZE_TRAP_END,      // 未达 50% → 永久困住
        RAGE_PUNISH,        // 达 50% → 全屏AOE惩罚
        FINAL_LOCKED,       // <5% 锁血无敌
        AUTO_DEATH          // 120s 自动死亡
    }

    // ===== Victory Flow =====
    private enum VictoryState {
        NONE,
        BOSS_ONLY,      // K 触发后：只渲染 Boss，Boss 时间轴继续
        STORY_DIALOG,   // 剧情确认框
        CREDITS         // 滚动谢幕
    }

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
    private float aoeIntervalTimer = 0f;
    private final Map<AoeTimeline.AoePattern, Float> aoeTimers = new HashMap<>();


    private final GlyphLayout glyphLayout = new GlyphLayout();

    private Sound currentDialogueSound;

    private BossTimeline bossTimeline;
    private BossTimelineRunner timelineRunner;


    private VictoryState victoryState = VictoryState.NONE;
    private BossRageState rageState = BossRageState.NORMAL;
    private static final float RAGE_TIME = 90f;
    private static final float AUTO_DEATH_TIME = 120f;

    private boolean rageChecked = false;
    private float rageAoeTimer = 0f;
    private float rageAoeTickTimer = 0f;
    private static final float RAGE_AOE_DURATION = 2f;
    private boolean showMazeTrapEnding = false;

    // Boss 时间轴：永远跑（不要被迷宫冻结影响）
    private float bossTimelineTime = 0f;

    // ===== Story / Credits UI =====
    private boolean showStory = false;
    private float creditsY = 0f;
    private static final float CREDITS_SCROLL_SPEED = 60f; // 越大滚得越快

    // 你自己的剧情文案（先写死，后面可换 json）
    private final String[] storyLines = new String[] {
            "Story: ...",
            "The tea has cooled.",
            "But the maze remembers."
    };

    private final String[] creditsLines = new String[] {
            "THE END",
            "",
            "Thanks for playing",
            "",
            "QQCHA Team",
            "Producer: You",
            "Programmer: You",
            "Art: You",
            "",
            "See you next time."
    };

    // ===== Maze Rebuild Warning =====
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
    private Texture bg;
    private Texture bossTex;

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

    public BossFightScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
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

        bg = new Texture(Gdx.files.internal("debug/boss_bg.jpg"));
        bossTex = new Texture(Gdx.files.internal("debug/boss.png"));
        teacupTex = new Texture(Gdx.files.internal("debug/teacup_top.png"));
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
        // ===== HUD 初始化 =====
        applyPhase(phaseSelector.getCurrent());

        aoeTimers.clear();
        aoeCycleTime = 0f;


        gameManager.setEnemyKillListener(enemy -> {
            // 🔥 魔法数字阶段
            dealDamageToBoss(50f);
        });
        hud = new de.tum.cit.fop.maze.ui.HUD(gameManager);
        hud.enableBossHUD(bossMaxHp);
        hud.updateBossHp(bossHp);
    }

    @Override
    public void render(float delta) {

        if (rageState == BossRageState.MAZE_TRAP_END) {
            renderMazeTrapEnding();
            return;
        }


        if (rageState == BossRageState.RAGE_PUNISH) {
            rageOverlayPulse += delta * 4f; // 呼吸速度
        } else {
            rageOverlayPulse = 0f;
        }

        boolean renderMazeLayer = (victoryState == VictoryState.NONE);
        if (Gdx.input.isKeyJustPressed(Input.Keys.H)) {
            bossHp -= 50f;
            bossHp = Math.max(0f, bossHp);
            hud.updateBossHp(bossHp);
        }

        Gdx.app.log(
                "MAZE_VIEWPORT",
                "screen = " + mazeViewport.getScreenWidth() + " x " + mazeViewport.getScreenHeight()
        );
        boolean isMergingOrAfter =
                bossDeathState == BossDeathState.TRIGGERED
                        || bossDeathState == BossDeathState.MERGING_SCREEN
                        || bossDeathState == BossDeathState.PLAYING_DEATH
                        || bossDeathState == BossDeathState.FINISHED;

        if (victoryState == VictoryState.NONE && Gdx.input.isKeyJustPressed(Input.Keys.K)) {
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
        float bossWorldY = -80f; // 离底部一些距离

        batch.draw(
                bossTex,
                bossWorldX,
                bossWorldY,
                BOSS_WIDTH,
                BOSS_HEIGHT
        );

// ===== Victory Overlay =====
        if (victoryState != VictoryState.NONE) {
            renderVictoryOverlays(batch);
        }





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
        if (victoryState == VictoryState.NONE) {
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


        if (victoryState == VictoryState.NONE
                && gameManager != null
                && gameManager.getPlayer() != null) {

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



    private void renderMazeTrapEnding() {
        //TODO
    }

    private void enterVictoryMode() {
        victoryState = VictoryState.BOSS_ONLY;
        bossTimelineTime = 0f;

        // ✅ 下半屏全部立即消失
        activeAOEs.clear();
        showMazeWarning = false;
        fadeAlpha = 0f;
        transitionState = PhaseTransitionState.NONE;

        // 这些资源你也可以不置空，只是不再渲染
        // teacupTex = null;
        // hud = null;

        // ✅ 关键：不要冻结 bossTimelineTime（它继续跑）
        // ✅ 关键：从现在开始不再 update gameManager（迷宫停止）
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


    private void update(float delta) {
        if (rageState == BossRageState.RAGE_PUNISH) {
            rageAoeTimer += delta;
            rageAoeTickTimer += delta;

            if (rageAoeTickTimer >= 0.5f) { // 1s 2 次
                rageAoeTickTimer = 0f;
                Player p = gameManager.getPlayer();
                if (p != null) {
                    p.takeDamage(5);
                }
            }

            if (rageAoeTimer >= RAGE_AOE_DURATION) {
                rageState = BossRageState.NORMAL;
            }
        }


        bossTimelineTime += delta;
        timelineRunner.update(bossTimelineTime, this);

        if (bossTimelineFinished()) {
            game.setScreen(new BossStoryScreen(game));
            return;
        }

        if (cupShakeActive) {
            cupShakeTimer += delta;
            if (cupShakeTimer >= cupShakeDuration) {
                cupShakeActive = false;
            }
        }






// 胜利后：迷宫不再推进（但 Boss 时间轴继续）
        if (victoryState != VictoryState.NONE) {
            // 只处理 Boss-only 状态的“结束检测”
            updateVictoryFlow(delta);
            return;
        }
// ===============================
// AOE TIMELINE (JSON driven)
// ===============================
        if (victoryState == VictoryState.NONE
                && !isMazeFrozen()
                && currentBossConfig.aoeTimeline != null) {

            AoeTimeline aoeTimeline = currentBossConfig.aoeTimeline;

            // 推进 cycle 时间
            aoeCycleTime += delta;
            float t = aoeCycleTime % aoeTimeline.cycle;

            for (AoeTimeline.AoePattern p : aoeTimeline.patterns) {

                if (t >= p.start && t <= p.end) {

                    float timer = aoeTimers.getOrDefault(p, 0f);
                    timer += delta;

                    if (timer >= p.interval) {
                        timer = 0f;

                        for (int i = 0; i < p.count; i++) {
                            spawnTimelineAOE(
                                    gameManager.getPlayer(),
                                    p.radius,
                                    p.damage
                            );
                        }
                    }

                    aoeTimers.put(p, timer);

                } else {
                    // 离开区间 → 清 timer，防止瞬爆
                    aoeTimers.remove(p);
                }
            }
        }



        //warning time
        if (showMazeWarning) {
            mazeWarningTimer -= delta;

            if (mazeWarningTimer <= 0f) {
                mazeWarningTimer = 0f;
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
                if (bossDeathState == BossDeathState.NONE &&
                        !phaseSwitchQueued &&
                        phaseSelector.shouldPrepareNextPhase(
                                showMazeWarning ? 0f : delta
                        )) {

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
                if (bossDeathState != BossDeathState.NONE) return;

                // ⭐ 真正推进 phase（只发生一次）
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

        updateBossDeath(delta);
        for (int i = activeAOEs.size() - 1; i >= 0; i--) {
            BossAOE aoe = activeAOEs.get(i);

            aoe.life -= delta;

            // ⭐ 预警结束 → 生效
            if (!aoe.active && aoe.life <= aoe.maxLife - aoe.warningTime) {
                aoe.active = true;
                // 这里是“描边变红”的时刻
            }

            if (aoe.life <= 0f) {
                activeAOEs.remove(i);
            }
            if (aoe.active && !aoe.damageDone) {
                Player p = gameManager.getPlayer();
                if (p != null && isPlayerInsideAOE(p, aoe)) {
                    p.takeDamage(aoe.damage); // 或你自己的伤害接口
                    aoe.damageDone = true; // ⭐ 防止一帧多次
                }
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
                // TODO: 切到剧情 Screen
                // game.setScreen(new BossEndingStoryScreen(game));
            }
        }
    }

    private boolean isMazeFrozen() {
        return bossDeathState != BossDeathState.NONE
                || transitionState != PhaseTransitionState.NONE;
    }

    private boolean isViolentShake() {
        if (bossTimelineTime >= RAGE_TIME) return true;

        float t = phaseTime % 30f;
        return t < 5f;
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

        aoe.warningTime = 0.8f;
        aoe.active = false;
        aoe.damageDone = false;

        // ⭐ 你以后如果要不同 damage，这里可以扩展 BossAOE
        activeAOEs.add(aoe);
    }

    private void spawnTrackingAOE(Player player) {
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

        aoe.radius = GameConstants.CELL_SIZE * 1.8f;

        aoe.maxLife = 1.5f;     // 总时长
        aoe.life = aoe.maxLife;

        aoe.warningTime = 1.2f; // ⭐ 关键
        aoe.active = false;
        aoe.damageDone = false;

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
    private void updateVictoryFlow(float delta) {
        // 这里必须接你自己的 Boss 时间轴结束判断
        // ✅ 你只要把 bossTimelineFinished() 换成你自己的条件就行

        if (victoryState == VictoryState.BOSS_ONLY) {
            if (bossTimelineFinished()) {
                victoryState = VictoryState.STORY_DIALOG;
                showStory = true;
            }
        }

        if (victoryState == VictoryState.STORY_DIALOG) {
            // 点击 / Enter 确认
            if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                showStory = false;
                victoryState = VictoryState.CREDITS;

                // 字幕从屏幕底下开始
                creditsY = -50f;
            }
        }

        if (victoryState == VictoryState.CREDITS) {
            creditsY += delta * CREDITS_SCROLL_SPEED;

            // 全滚完：回 Menu
            float endY = Gdx.graphics.getHeight() + creditsLines.length * 30f;
            if (creditsY > endY) {
                // TODO: 切 Menu + 切 BGM
                game.setScreen(new MenuScreen(game));
            }
        }
    }

    // ⚠️ 你要改的就这里：接你的 Boss 时间轴“结束”判断
    private boolean bossTimelineFinished() {
        return bossTimelineTime >= bossTimeline.length;
    }
    private void renderVictoryOverlays(SpriteBatch batch) {
        if (victoryState == VictoryState.STORY_DIALOG) {
            drawStoryDialog(batch);
        } else if (victoryState == VictoryState.CREDITS) {
            drawCredits(batch);
        }
    }
    private void drawStoryDialog(SpriteBatch batch) {
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        float boxW = 720f;
        float boxH = 240f;
        float boxX = w / 2f - boxW / 2f;
        float boxY = h * 0.55f;

        // 背景框（ShapeRenderer 是独立的，OK）
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.setProjectionMatrix(uiCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.08f, 0.08f, 0.10f, 0.88f);
        shapeRenderer.rect(boxX, boxY, boxW, boxH);
        shapeRenderer.end();

        // ⭐ 注意：这里【不】begin / end
        uiFont.getData().setScale(0.45f);
        uiFont.setColor(1f, 1f, 1f, 1f);

        float y = boxY + boxH - 40f;
        for (String line : storyLines) {
            uiFont.draw(batch, line, boxX + 30f, y);
            y -= 28f;
        }

        uiFont.getData().setScale(0.35f);
        uiFont.setColor(0.9f, 0.9f, 0.6f, 1f);
        uiFont.draw(batch, "[Click / ENTER to continue]", boxX + 30f, boxY + 35f);
    }
    private void drawCredits(SpriteBatch batch) {
        float w = Gdx.graphics.getWidth();

        uiFont.getData().setScale(0.5f);
        uiFont.setColor(1f, 1f, 1f, 1f);

        float startX = w * 0.25f;
        float y = creditsY;

        for (String line : creditsLines) {
            uiFont.draw(batch, line, startX, y);
            y += 30f;
        }
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
            // 没打够 → 失败结局
            rageState = BossRageState.MAZE_TRAP_END;
            showMazeTrapEnding = true;
        } else {
            // 打够 → AOE 惩罚
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
        // 如果你以后要参数化，可以把 duration / damage 存字段
    }

    /** 锁定最终血量（5%） */
    public void lockFinalHp(float threshold) {
        if (rageState != BossRageState.FINAL_LOCKED) {
            bossHp = bossMaxHp * threshold;
            rageState = BossRageState.FINAL_LOCKED;
            hud.updateBossHp(bossHp);
        }
    }

    /** 时间轴结束（120s） */
    public void markTimelineFinished() {
        // 你已经在 bossTimelineFinished() 里用时间判断
        // 所以这里可以什么都不做
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