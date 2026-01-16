package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.effects.fog.FogSystem;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.Obstacle.DynamicObstacle;
import de.tum.cit.fop.maze.entities.Obstacle.MovingWall;
import de.tum.cit.fop.maze.entities.boss.BossFoundDialog;
import de.tum.cit.fop.maze.entities.boss.BossLoadingScreen;
import de.tum.cit.fop.maze.entities.chapter.Chapter1Relic;
import de.tum.cit.fop.maze.entities.chapter.ChapterContext;
import de.tum.cit.fop.maze.entities.chapter.ChapterDialogCallback;
import de.tum.cit.fop.maze.entities.chapter.ChapterTextDialog;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.entities.trap.Trap;
import de.tum.cit.fop.maze.game.*;
import de.tum.cit.fop.maze.game.save.GameSaveData;
import de.tum.cit.fop.maze.game.score.LevelResult;
import de.tum.cit.fop.maze.input.KeyBindingManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.tools.DeveloperConsole;
import de.tum.cit.fop.maze.ui.HUD;
import de.tum.cit.fop.maze.utils.CameraManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.game.save.StorageManager;

import java.util.*;

public class GameScreen implements Screen, Chapter1RelicListener {

    private Viewport worldViewport;
    private Stage uiStage;
    private FogSystem fogSystem;
    private Label pauseScoreLabel;
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
    private ShapeRenderer shapeRenderer = new ShapeRenderer();

    // ===== Pause =====
    private boolean paused = false;
    private Stage pauseStage;
    private boolean pauseUIInitialized = false;

    // ===== Game Over =====
    private boolean gameOverShown = false;
    private Stage gameOverStage;
    // ⭐ Chapter / 剧情强制暂停
    private boolean chapterPaused = false;

    @Override
    public void onChapter1RelicRequested(Chapter1Relic relic) {

        gm.enterChapterRelicView();
        chapterPaused = true;
        Gdx.input.setInputProcessor(uiStage);
        new ChapterTextDialog(
                uiStage,
                game.getSkin(),
                relic.getData(),   // ⭐ 来自 RelicData
                new ChapterDialogCallback() {

                    @Override
                    public void onRead() {

                        gm.readChapter1Relic(relic);

                        // ⭐ 立刻检查是否全部已读
                        if (chapterContext != null
                                && chapterContext.areAllRelicsRead()) {

                            Logger.gameEvent("👁 All relics read — Boss found immediately");

                            BossFoundDialog bossDialog =
                                    new BossFoundDialog(game.getSkin());

                            bossDialog.setOnFight(() -> {
                                AudioManager.getInstance().stopMusic();
                                game.setScreen(new BossLoadingScreen(game));
                            });

                            bossDialog.setOnEscape(() -> {
                                Logger.gameEvent("🏃 Player escaped Boss");
                                gm.exitChapterRelicView();
                                chapterPaused = false;
                            });

                            bossDialog.show(uiStage);
                            Gdx.input.setInputProcessor(uiStage);

                            return; // ⛔ 不往下走
                        }

                        // 普通情况
                        gm.exitChapterRelicView();
                        chapterPaused = false;
                        Gdx.input.setInputProcessor(null);
                    }


                    @Override
                    public void onDiscard() {

                        gm.discardChapter1Relic(relic);
                        gm.exitChapterRelicView();
                        chapterPaused = false;
                        Gdx.input.setInputProcessor(null);
                    }
                }
        );
    }


    private final ChapterContext chapterContext;
    private BitmapFont worldHintFont;

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
    public GameScreen(MazeRunnerGame game, DifficultyConfig difficultyConfig) {
        this(game, difficultyConfig, null);
    }
    public GameScreen(MazeRunnerGame game, DifficultyConfig difficultyConfig,ChapterContext chapterContext) {
        this.game = game;
        this.difficultyConfig = difficultyConfig;
        this.chapterContext = chapterContext;
        // HARD 才有雾
        // ⭐ 所有规则只写在这里
        if (difficultyConfig.difficulty == Difficulty.HARD
                || (chapterContext != null && chapterContext.enableFogOverride())) {
            fogSystem = new FogSystem();
        } else {
            fogSystem = null;
        }
    }

    @Override
    public void show() {
        worldHintFont = new BitmapFont(); // LibGDX 默认字体
        worldHintFont.setColor(Color.GOLD);
        worldHintFont.getData().setScale(0.9f);


        uiTop    = new Texture("Wallpaper/HUD_up.png");
        uiBottom = new Texture("Wallpaper/HUD_down.png");
        uiLeft   = new Texture("Wallpaper/HUD_left.png");
        uiRight  = new Texture("Wallpaper/HUD_right.png");

        input = new PlayerInputHandler();
        batch = game.getSpriteBatch();

        gm = game.getGameManager();

        gm.setChapter1RelicListener(this);
        // ⭐⭐⭐ 关键修复：剧情模式下，确保世界被初始化
        if (gm.getPlayers().isEmpty()) {
            Logger.error("🧩 GameScreen.show(): players empty, calling resetGame()");
            gm.resetGame();
        }

        maze = new MazeRenderer(gm, difficultyConfig);
        cam  = new CameraManager(difficultyConfig);

        worldViewport = new FitViewport(
                GameConstants.CAMERA_VIEW_WIDTH,
                GameConstants.CAMERA_VIEW_HEIGHT,
                cam.getCamera()
        );

        uiStage = new Stage(new ScreenViewport(), batch);
        hud = new HUD(gm);
        gm.applyRestoreIfNeeded();
        cam.centerOnPlayerImmediately(gm.getPlayer());
        console = new DeveloperConsole(gm, game.getSkin());

    }

    @Override
    public void render(float delta) {



        // ===============================
// 🔥 Chapter Boss Encounter Check（最优先）
// ===============================
        if (chapterContext != null
                && chapterContext.consumeBossPending()) {

            BossFoundDialog bossDialog =
                    new BossFoundDialog(game.getSkin());

            bossDialog.setOnFight(() -> {
                AudioManager.getInstance().stopMusic();
                game.setScreen(new BossLoadingScreen(game));
            });

            bossDialog.setOnEscape(() -> {
                Logger.gameEvent("🏃 Player escaped Boss");
                gm.clearLevelCompletedFlag();
                goToSettlementScreen();
            });

            bossDialog.show(uiStage);
            Gdx.input.setInputProcessor(uiStage);
            return; // ⛔ 整帧终止
        }

        // ✅ 必须在处理输入之前先算好 UI 是否吃鼠标
        gm.setUIConsumesMouse(hud.isMouseOverInteractiveUI());









// ===== Global Menu / Pause Input =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {

            // ⚠️ Chapter Relic 期间，ESC 交给 Dialog
            if (gm.isViewingChapterRelic()) {
                return;
            }

            if (!gameOverShown) {
                togglePause();
                return;
            }
        }

        // ===== Mouse → Tile (Ability Targeting) =====
        Vector3 world = cam.getCamera().unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );
        gm.setMouseTargetTile(
                (int)(world.x / GameConstants.CELL_SIZE),
                (int)(world.y / GameConstants.CELL_SIZE)
        );
        OrthographicCamera camera = cam.getCamera();

        float camLeft   = camera.position.x - camera.viewportWidth  / 2f;
        float camBottom = camera.position.y - camera.viewportHeight / 2f;
        float camWidth  = camera.viewportWidth;
        float camHeight = camera.viewportHeight;
        worldViewport.apply();
        batch.setProjectionMatrix(cam.getCamera().combined);

        // ===== Debug Toggles =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) Logger.toggleDebug();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
            if (!cam.isDebugZoom()) cam.setDebugZoom(5f);
            else cam.clearDebugZoom();
        }

        // ===== Console Toggle =====
        if (KeyBindingManager.getInstance()
                .isJustPressed(KeyBindingManager.GameAction.CONSOLE)) {
            console.toggle();
        }

        // ===== Input (如果 Game Over 显示了，禁止玩家操作) =====
        // 🔥 [修复] 添加 && !gameOverShown
//        if (!paused && !console.isVisible() && !gm.isLevelTransitionInProgress() && !gameOverShown) {
//
//            input.update(delta, new PlayerInputHandler.InputHandlerCallback() {
//                @Override public void onMoveInput(Player.PlayerIndex i, int dx, int dy) { gm.onMoveInput(i, dx, dy); }
//                @Override public float getMoveDelayMultiplier() { return 1f; }
//                @Override public boolean onAbilityInput(Player.PlayerIndex i, int s) { return gm.onAbilityInput(i, s); }
//                @Override public void onInteractInput(Player.PlayerIndex i) { gm.onInteractInput(i); }
//                @Override public void onMenuInput() { togglePause();  }
//                @Override
//                public boolean isUIConsumingMouse() {
//                    return gm.isUIConsumingMouse();
//                }
//            }, Player.PlayerIndex.P1);
//
//            if (gm.isTwoPlayerMode()) {
//                input.update(delta, new PlayerInputHandler.InputHandlerCallback() {
//                    @Override public void onMoveInput(Player.PlayerIndex i, int dx, int dy) { gm.onMoveInput(i, dx, dy); }
//                    @Override public float getMoveDelayMultiplier() { return 1f; }
//                    @Override public boolean onAbilityInput(Player.PlayerIndex i, int s) { return gm.onAbilityInput(i, s); }
//                    @Override public void onInteractInput(Player.PlayerIndex i) { gm.onInteractInput(i); }
//                    @Override public void onMenuInput() {}
//                    // ⭐ 同样必须有
//                    @Override
//                    public boolean isUIConsumingMouse() {
//                        return gm.isUIConsumingMouse();
//                    }
//                }, Player.PlayerIndex.P2);
//            }
//        }
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);

        // ===== Update (如果 Game Over 显示了，暂停游戏逻辑) =====
        // 🔥 [修复] 添加 && !gameOverShown
        if (!isGamePaused()) {
            gm.update(delta);
            if (fogSystem != null) fogSystem.update(delta);
// ===============================
// 🔥 Chapter Boss Encounter Check（必须在 Settlement 前）
// ===============================
            if (chapterContext != null
                    && chapterContext.consumeBossPending()) {

                BossFoundDialog bossDialog =
                        new BossFoundDialog(game.getSkin());

                bossDialog.setOnFight(() -> {
                    AudioManager.getInstance().stopMusic();
                    game.setScreen(new BossLoadingScreen(game));
                });

                bossDialog.setOnEscape(() -> {
                    Logger.gameEvent("🏃 Player escaped Boss");
                    // 👇 如果逃跑，才允许进结算
                    gm.clearLevelCompletedFlag();
                    goToSettlementScreen();
                });

                bossDialog.show(uiStage);
                Gdx.input.setInputProcessor(uiStage);
                return; // ⛔ 阻断本帧后续流程
            }

            if (gm.isLevelCompletedPendingSettlement()) {
                goToSettlementScreen();
                return;
            }

            if (gm.isPlayerDead() && !gameOverShown) {
                showGameOverScreen();
            }

            if (!console.isVisible()) {
                float timeScale = gm.getVariable("time_scale");
                float gameDelta = delta * timeScale;
                cam.update(gameDelta, gm);
            }
        }


        // ===== World Render =====
        worldViewport.apply();
        batch.setProjectionMatrix(cam.getCamera().combined);

        /* =========================================================
           ① 地板 + 门背后呼吸光
           ========================================================= */
        batch.begin();
        maze.renderFloor(batch);
        List<ExitDoor> exitDoorsCopy = new ArrayList<>(gm.getExitDoors());
        exitDoorsCopy.forEach(d -> d.renderPortalBack(batch));
        batch.end();

        /* =========================================================
           ② 世界实体排序渲染
           ========================================================= */
        List<Item> items = new ArrayList<>();

        for (var wg : maze.getWallGroups()) {
            boolean front = maze.isWallInFrontOfAnyEntity(wg.startX, wg.startY);
            items.add(new Item(wg, front ? Type.WALL_FRONT : Type.WALL_BEHIND));
        }

        for (Player p : gm.getPlayers()) {
            items.add(new Item(p, 100));
        }
        if (gm.getCat() != null) {
            items.add(new Item(gm.getCat(), 95));
        }

        List<Enemy> enemiesCopy = new ArrayList<>(gm.getEnemies());
        enemiesCopy.forEach(e -> items.add(new Item(e, 50)));

        List<Trap> trapsCopy = new ArrayList<>(gm.getTraps());
        trapsCopy.forEach(t -> {
            if (t.isActive() && t instanceof GameObject) {
                items.add(new Item((GameObject)t, 45));
            }
        });

        exitDoorsCopy.forEach(d -> items.add(new Item(d, 45)));

        List<Heart> heartsCopy = new ArrayList<>(gm.getHearts());
        heartsCopy.forEach(h -> { if (h.isActive()) items.add(new Item(h, 30)); });

        List<Treasure> treasuresCopy = new ArrayList<>(gm.getTreasures());
        treasuresCopy.forEach(t -> items.add(new Item(t, 20)));
        List<Chapter1Relic> relicsCopy =
                new ArrayList<>(gm.getChapterRelics());

        relicsCopy.forEach(r -> {
            items.add(new Item(r, 25)); // 层级：比宝箱高一点
        });
        List<HeartContainer> containersCopy = new ArrayList<>(gm.getHeartContainers());
        containersCopy.forEach(hc -> { if (hc.isActive()) items.add(new Item(hc, 30)); });

        List<DynamicObstacle> obstaclesCopy = new ArrayList<>(gm.getObstacles());
        obstaclesCopy.forEach(o -> items.add(new Item(o, 40)));

        List<Key> keysCopy = new ArrayList<>(gm.getKeys());
        keysCopy.forEach(k -> { if (k.isActive()) items.add(new Item(k, 35)); });

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
        batch.begin();

        Player player = gm.getPlayer();

        for (Chapter1Relic relic : gm.getChapterRelics()) {
            if (!relic.isInteractable()) continue;

            // 只在玩家靠近时显示（1.5 格）
            int dx = relic.getX() - player.getX();
            int dy = relic.getY() - player.getY();
            if (dx * dx + dy * dy > 2) continue;

            float wx = (relic.getX() + 0.5f) * GameConstants.CELL_SIZE;
            float wy = (relic.getY() + 0.5f) * GameConstants.CELL_SIZE;

            // 轻微上下浮动
            float bob = (float)Math.sin(Gdx.graphics.getFrameId() * 0.1f) * 4f;

            worldHintFont.draw(
                    batch,
                    "Press E",
                    wx - 20,
                    wy + GameConstants.CELL_SIZE + 10 + bob
            );
        }

        batch.end();


        /* =========================================================
           ③ 门前龙卷风粒子 + 特效
           ========================================================= */
        batch.begin();
        exitDoorsCopy.forEach(d -> d.renderPortalFront(batch));
        if (gm.getKeyEffectManager() != null) {
            gm.getKeyEffectManager().render(batch);
        }
        gm.getBobaBulletEffectManager().render(batch);
        if (gm.getItemEffectManager() != null) gm.getItemEffectManager().renderSprites(batch);
        if (gm.getTrapEffectManager() != null) gm.getTrapEffectManager().renderSprites(batch);
        if (gm.getCombatEffectManager() != null) gm.getCombatEffectManager().renderSprites(batch);
        batch.end();

        // ===== Shape/粒子层 =====
        shapeRenderer.setProjectionMatrix(cam.getCamera().combined);
        if (gm.getItemEffectManager() != null) gm.getItemEffectManager().renderShapes(shapeRenderer);
        if (gm.getTrapEffectManager() != null) gm.getTrapEffectManager().renderShapes(shapeRenderer);
        if (gm.getCombatEffectManager() != null) {
            Gdx.gl.glEnable(Gdx.gl.GL_BLEND);
            Gdx.gl.glBlendFunc(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            gm.getCombatEffectManager().renderShapes(shapeRenderer);
            shapeRenderer.end();
        }

        // 玩家脚下传送阵
        batch.begin();
        if (gm.getPlayerSpawnPortal() != null) {
            float px = (gm.getPlayer().getX() + 0.5f) * GameConstants.CELL_SIZE;
            float py = (gm.getPlayer().getY() + 0.5f) * GameConstants.CELL_SIZE;
            gm.getPlayerSpawnPortal().renderBack(batch, px, py);
            gm.getPlayerSpawnPortal().renderFront(batch);
        }
        batch.end();

        // Ability Debug / Targeting
        shapeRenderer.setProjectionMatrix(cam.getCamera().combined);
        for (Player p : gm.getPlayers()) {
            if (p.getAbilityManager() != null) {
                p.getAbilityManager().drawAbilities(batch, shapeRenderer, p);
            }
        }

        // 雾
        batch.begin();
        if (fogSystem != null) {
            fogSystem.render(
                    batch,
                    camLeft, camBottom, camWidth, camHeight,
                    gm.getCat() != null ? gm.getCat().getWorldX() : gm.getPlayer().getWorldX(),
                    gm.getCat() != null ? gm.getCat().getWorldY() : gm.getPlayer().getWorldY()
            );
        }
        batch.end();

        // Debug Lines
        if (Logger.isDebugEnabled()) {
            shapeRenderer.setProjectionMatrix(cam.getCamera().combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            float cs = GameConstants.CELL_SIZE;
            int mazeWidth  = difficultyConfig.mazeWidth;
            int mazeHeight = difficultyConfig.mazeHeight;
            shapeRenderer.setColor(1, 0, 0, 1);
            shapeRenderer.rect(0, 0, mazeWidth * cs, mazeHeight * cs);
            shapeRenderer.setColor(1, 1, 0, 1);
            shapeRenderer.rect(camLeft, camBottom, camWidth, camHeight);
            shapeRenderer.setColor(0, 0, 1, 1);
            for (DynamicObstacle o : gm.getObstacles()) {
                if (o instanceof MovingWall mw) {
                    float wx = mw.getWorldX() * cs + cs / 2f;
                    float wy = mw.getWorldY() * cs + cs / 2f;
                    shapeRenderer.line(wx - 10, wy, wx + 10, wy);
                    shapeRenderer.line(wx, wy - 10, wx, wy + 10);
                }
            }
            shapeRenderer.end();
        }

        /* =========================================================
           ④ UI（正交相机）
           ========================================================= */
        renderUI();

        // ===== Pause Logic =====
        if (paused) {
            if (!pauseUIInitialized) initPauseUI();
            pauseScoreLabel.setText("SCORE: " + gm.getScore());
            Gdx.input.setInputProcessor(pauseStage);
            pauseStage.act(delta);
            pauseStage.draw();
            return;
        }

        // 🔥 [修复] Game Over Logic (补上绘制)
        if (gameOverShown) {
            gameOverStage.act(delta);
            gameOverStage.draw();
        }
    }

    private boolean isGamePaused() {
        return paused              // ESC 暂停
                || chapterPaused       // 📜 Chapter 阅读
                || console.isVisible()
                || gameOverShown;
    }


    private void renderUI() {
        Matrix4 oldProjection = batch.getProjectionMatrix().cpy();
        Color oldColor = batch.getColor().cpy();

        uiStage.getViewport().apply();
        batch.setProjectionMatrix(uiStage.getCamera().combined);

        batch.begin();
        renderMazeBorderDecorations(batch);
        hud.renderInGameUI(batch);
        batch.end();

        uiStage.act(Gdx.graphics.getDeltaTime());
        uiStage.draw();

        if (console != null) console.render();

        batch.setProjectionMatrix(cam.getCamera().combined);
        batch.setColor(oldColor);
        batch.setProjectionMatrix(oldProjection);
    }

    private void togglePause() {
        if (gameOverShown) return;
        paused = !paused;
        if (paused) {
            if (pauseStage == null) initPauseUI();
            Gdx.input.setInputProcessor(pauseStage);
        } else {
            Gdx.input.setInputProcessor(null);
        }
    }

    private void initPauseUI() {
        pauseStage = new Stage(new ScreenViewport());
        Table root = new Table();
        root.setFillParent(true);
        pauseStage.addActor(root);

        pauseScoreLabel = new Label("", game.getSkin(), "title");
        pauseScoreLabel.setColor(Color.GOLD);
        root.add(pauseScoreLabel).padBottom(40).row();

        Table buttonTable = new Table();
        ButtonFactory bf = new ButtonFactory(game.getSkin());
        float btnW = 350; float btnH = 90; float padding = 15;

        buttonTable.add(bf.create("CONTINUE", this::togglePause)).width(btnW).height(btnH).pad(padding);
        buttonTable.add(bf.create("RESET MAZE", () -> game.resetMaze(difficultyConfig.difficulty))).width(btnW).height(btnH).pad(padding);
        buttonTable.add(bf.create("SETTINGS", () -> game.setScreen(new SettingsScreen(game, SettingsScreen.SettingsSource.PAUSE_MENU, game.getScreen())))).width(btnW).height(btnH).pad(padding);
        buttonTable.add(bf.create("MENU", game::goToMenu)).width(btnW).height(btnH).pad(padding);
        buttonTable.add(
                bf.create("SAVE GAME", this::openManualSaveDialog)
        ).width(btnW).height(btnH).pad(padding);
        
        root.add(buttonTable).expandY().center();
        pauseUIInitialized = true;
    }

    private void openManualSaveDialog() {
        Stage dialogStage = pauseStage; // 用 pause 的 stage
        Skin skin = game.getSkin();

        Dialog dialog = new Dialog(" SAVE GAME ", skin) {
            @Override
            protected void result(Object object) {
                if (object instanceof Integer slot) {
                    // 1️⃣ 切 SaveTarget
                    gm.setCurrentSaveTarget(
                            StorageManager.SaveTarget.fromSlot(slot)
                    );

                    // 2️⃣ 立刻存一次
                    gm.saveGameProgress();

                    Logger.info("Manual save to slot " + slot);
                }
            }
        };

        dialog.text("\n  Choose a save slot:\n");

        dialog.button(" SLOT 1 ", 1);
        dialog.button(" SLOT 2 ", 2);
        dialog.button(" SLOT 3 ", 3);
        dialog.button(" CANCEL ", null);

        dialog.show(dialogStage);
    }


    private void renderMazeBorderDecorations(SpriteBatch batch) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        batch.draw(uiTop, 0, h - 140, w, 140);
        batch.draw(uiBottom, 0, 0, w, 140);
        batch.draw(uiLeft, 0, 0, 140, h);
        batch.draw(uiRight, w - 140, 0, 140, h);
    }

    private void goToSettlementScreen() {
        if (gm != null) {
            gm.saveGameProgress();
            if (gm.getGameSaveData() != null) {
                StorageManager.getInstance().saveGameSync(gm.getGameSaveData());
            }
        }
        LevelResult result = gm.getLevelResult();
        if (result == null) result = new LevelResult(0,0,0,"D",0,1f);
        GameSaveData save = gm.getGameSaveData();
        if (save == null) save = new GameSaveData();
        gm.clearLevelCompletedFlag();
        game.setScreen(new SettlementScreen(game, result, save));
    }

    private void showGameOverScreen() {
        gameOverShown = true;
        gameOverStage = new Stage(new ScreenViewport());
        Table root = new Table();
        root.setFillParent(true);
        gameOverStage.addActor(root);

        root.add(new Label("GAME OVER", game.getSkin(), "title")).padBottom(30).row();
        root.add(new Label("Final Score: " + gm.getScore(), game.getSkin())).padBottom(40).row();

        ButtonFactory bf = new ButtonFactory(game.getSkin());
        root.add(bf.create("RETRY", () -> game.resetMaze(difficultyConfig.difficulty))).pad(10).row();
        root.add(bf.create("MENU", game::goToMenu)).pad(10);

        Gdx.input.setInputProcessor(gameOverStage);
    }

    @Override public void resize(int w, int h) {
        worldViewport.update(w, h, true);
        if (uiStage != null) uiStage.getViewport().update(w, h, true);
        if (pauseStage != null) pauseStage.getViewport().update(w, h, true);
        if (gameOverStage != null) gameOverStage.getViewport().update(w, h, true);
        if (console != null) console.resize(w, h);
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        maze.dispose();
        if (console != null) console.dispose();
        if (gameOverStage != null) gameOverStage.dispose();
        if (worldHintFont != null) worldHintFont.dispose();
    }
}