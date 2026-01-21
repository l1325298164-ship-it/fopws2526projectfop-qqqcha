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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.effects.Player.PlayerTrailManager;
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
import de.tum.cit.fop.maze.game.story.StoryProgress;
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
import java.util.List;

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

    private PlayerTrailManager playerTrailManager;
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
                relic.getData(),
                new ChapterDialogCallback() {

                    @Override
                    public void onRead() {
                        gm.readChapter1Relic(relic);
                        if (chapterContext != null && chapterContext.areAllRelicsRead()) {
                            Logger.gameEvent("👁 All relics read — Boss found immediately");
                            BossFoundDialog bossDialog = new BossFoundDialog(game.getSkin());

                            bossDialog.setOnFight(() -> {
                                StoryProgress sp = StoryProgress.load();
                                sp.markBossUnlocked(1);
                                sp.save();
                                Logger.gameEvent("⚔ Enter Boss Fight");

                                gm.exitChapterRelicView();
                                chapterPaused = false;
                                Gdx.input.setInputProcessor(null);

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
                            return;
                        }
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

    public GameScreen(MazeRunnerGame game, DifficultyConfig difficultyConfig, ChapterContext chapterContext) {
        this.game = game;
        this.difficultyConfig = difficultyConfig;
        this.chapterContext = chapterContext;
        if (difficultyConfig.difficulty == Difficulty.HARD
                || (chapterContext != null && chapterContext.enableFogOverride())) {
            fogSystem = new FogSystem();
        } else {
            fogSystem = null;
        }
    }

    @Override
    public void show() {
        worldHintFont = new BitmapFont();
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

        if (gm.getPlayers().isEmpty()) {
            Logger.error("🧩 GameScreen.show(): players empty, calling resetGame()");
            gm.resetGame();
        }

        maze = new MazeRenderer(gm, difficultyConfig);
        cam  = new CameraManager(difficultyConfig);

        if (gm != null) {
            gm.setCameraManager(cam);
        }

        worldViewport = new FitViewport(
                GameConstants.CAMERA_VIEW_WIDTH,
                GameConstants.CAMERA_VIEW_HEIGHT,
                cam.getCamera()
        );

        uiStage = new Stage(new ScreenViewport(), batch);
        hud = new HUD(gm);
        playerTrailManager = new PlayerTrailManager();
        gm.applyRestoreIfNeeded();
        cam.centerOnPlayerImmediately(gm.getPlayer());
        console = new DeveloperConsole(gm, game.getSkin());
        playMazeBGM();
    }

    private void playMazeBGM() {
        AudioManager audio = AudioManager.getInstance();
        audio.stopMusic();
        if (difficultyConfig.difficulty == Difficulty.ENDLESS) {
            audio.play(AudioType.MUSIC_MAZE_ENDLESS);
            return;
        }
        switch (difficultyConfig.difficulty) {
            case EASY -> audio.play(AudioType.MUSIC_MAZE_EASY);
            case NORMAL -> audio.play(AudioType.MUSIC_MAZE_NORMAL);
            case HARD -> audio.play(AudioType.MUSIC_MAZE_HARD);
            case ENDLESS ->  audio.play(AudioType.MUSIC_MAZE_ENDLESS);
            default -> audio.play(AudioType.MUSIC_MAZE_NORMAL);
        }
    }

    @Override
    public void render(float delta) {
        gm.setUIConsumesMouse(hud.isMouseOverInteractiveUI());

        // ===== Global Menu / Pause Input =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (gm.isViewingChapterRelic()) {
                return;
            }
            if (!gameOverShown) {
                togglePause();
                return;
            }
        }

        // ===== Mouse → Tile =====
        Vector3 world = cam.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
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
        if (KeyBindingManager.getInstance().isJustPressed(KeyBindingManager.GameAction.CONSOLE)) {
            console.toggle();
        }

        // ===== Input =====
        if (!paused && !console.isVisible() && !gm.isLevelTransitionInProgress() && !gameOverShown) {
            input.update(delta, new PlayerInputHandler.InputHandlerCallback() {
                @Override public void onMoveInput(Player.PlayerIndex i, int dx, int dy) { gm.onMoveInput(i, dx, dy); }
                @Override public float getMoveDelayMultiplier() { return 1f; }
                @Override public boolean onAbilityInput(Player.PlayerIndex i, int s) { return gm.onAbilityInput(i, s); }
                @Override public void onInteractInput(Player.PlayerIndex i) { gm.onInteractInput(i); }
                @Override public void onMenuInput() { togglePause();  }
                @Override public boolean isUIConsumingMouse() { return gm.isUIConsumingMouse(); }
            }, Player.PlayerIndex.P1);

            if (gm.isTwoPlayerMode()) {
                input.update(delta, new PlayerInputHandler.InputHandlerCallback() {
                    @Override public void onMoveInput(Player.PlayerIndex i, int dx, int dy) { gm.onMoveInput(i, dx, dy); }
                    @Override public float getMoveDelayMultiplier() { return 1f; }
                    @Override public boolean onAbilityInput(Player.PlayerIndex i, int s) { return gm.onAbilityInput(i, s); }
                    @Override public void onInteractInput(Player.PlayerIndex i) { gm.onInteractInput(i); }
                    @Override public void onMenuInput() {}
                    @Override public boolean isUIConsumingMouse() { return gm.isUIConsumingMouse(); }
                }, Player.PlayerIndex.P2);
            }
        }
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);

        // ===== Update =====
        if (!isGamePaused()) {
            gm.update(delta);
            if (fogSystem != null) fogSystem.update(delta);

            if (playerTrailManager != null) {
                for (Player p : gm.getPlayers()) {
                    playerTrailManager.update(
                            delta,
                            p.getWorldX(),
                            p.getWorldY(),
                            p.isDashing(),
                            p.getCurrentFrame(),
                            Color.CYAN // 残影颜色
                    );
                }
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
        // 强制重置颜色为白色！防止上一帧的残影/特效颜色污染地板
        batch.setColor(Color.WHITE);

        maze.renderFloor(batch);
        List<ExitDoor> exitDoorsCopy = new ArrayList<>(gm.getExitDoors());
        exitDoorsCopy.forEach(d -> d.renderPortalBack(batch));
        batch.end();

        /* =========================================================
           ② 世界实体排序渲染
           ========================================================= */
        List<Item> items = new ArrayList<>();
        // ... (Item sorting logic unchanged)
        for (var wg : maze.getWallGroups()) {
            boolean front = maze.isWallInFrontOfAnyEntity(wg.startX, wg.startY);
            items.add(new Item(wg, front ? Type.WALL_FRONT : Type.WALL_BEHIND));
        }
        for (Player p : gm.getPlayers()) items.add(new Item(p, 100));
        if (gm.getCat() != null) items.add(new Item(gm.getCat(), 95));
        List<Enemy> enemiesCopy = new ArrayList<>(gm.getEnemies());
        enemiesCopy.forEach(e -> items.add(new Item(e, 50)));
        List<Trap> trapsCopy = new ArrayList<>(gm.getTraps());
        trapsCopy.forEach(t -> { if (t.isActive() && t instanceof GameObject) items.add(new Item((GameObject)t, 45)); });
        exitDoorsCopy.forEach(d -> items.add(new Item(d, 45)));
        List<Heart> heartsCopy = new ArrayList<>(gm.getHearts());
        heartsCopy.forEach(h -> { if (h.isActive()) items.add(new Item(h, 30)); });
        List<Treasure> treasuresCopy = new ArrayList<>(gm.getTreasures());
        treasuresCopy.forEach(t -> items.add(new Item(t, 20)));
        List<Chapter1Relic> relicsCopy = new ArrayList<>(gm.getChapterRelics());
        relicsCopy.forEach(r -> items.add(new Item(r, 25)));
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
            int dx = relic.getX() - player.getX();
            int dy = relic.getY() - player.getY();
            if (dx * dx + dy * dy > 2) continue;
            float wx = (relic.getX() + 0.5f) * GameConstants.CELL_SIZE;
            float wy = (relic.getY() + 0.5f) * GameConstants.CELL_SIZE;
            float bob = (float)Math.sin(Gdx.graphics.getFrameId() * 0.1f) * 4f;
            worldHintFont.draw(batch, "Press E", wx - 20, wy + GameConstants.CELL_SIZE + 10 + bob);
        }
        batch.end();

        /* =========================================================
           ③ 门前龙卷风粒子 + 特效
           ========================================================= */
        batch.begin();
        exitDoorsCopy.forEach(d -> d.renderPortalFront(batch));
        if (gm.getKeyEffectManager() != null) gm.getKeyEffectManager().render(batch);

        if (playerTrailManager != null) {
            playerTrailManager.render(batch);
            // 残影绘制完后，立即重置颜色为白色，防止影响后续特效
            batch.setColor(Color.WHITE);
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

            // 确保使用 Filled 模式，避免 DashEffect 的潜在 Line 模式残留
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            gm.getCombatEffectManager().renderShapes(shapeRenderer);
            shapeRenderer.end();
        }

        // 玩家脚下传送阵
        batch.begin();
        // 再次保险重置颜色
        batch.setColor(Color.WHITE);
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
            // ... (Debug lines logic unchanged)
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

        renderUI();

        if (paused) {
            if (!pauseUIInitialized) initPauseUI();

            // 只有当不是在显示 "GAME SAVED!" 反馈时，才显示 "GAME PAUSED"
            // (通过颜色判断，如果是金色则说明是默认状态，如果是绿色则是在显示Saved)
            if (pauseScoreLabel.getColor().equals(Color.GOLD)) {
                pauseScoreLabel.setText("GAME PAUSED");
            }

            Gdx.input.setInputProcessor(pauseStage);
            pauseStage.act(delta);
            pauseStage.draw();
            return;
        }

        if (gameOverShown) {
            gameOverStage.act(delta);
            gameOverStage.draw();
        }
    }

    private boolean isGamePaused() {
        return paused || chapterPaused || console.isVisible() || gameOverShown;
    }

    private void renderUI() {
        Matrix4 oldProjection = batch.getProjectionMatrix().cpy();
        Color oldColor = batch.getColor().cpy();

        uiStage.getViewport().apply();
        batch.setProjectionMatrix(uiStage.getCamera().combined);

        batch.begin();
        renderMazeBorderDecorations(batch);
        boolean allowInteraction = !paused && !gameOverShown;
        hud.renderInGameUI(batch, allowInteraction);
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

    // =================================================================
    // 🔥 修复方法: initPauseUI (实现 Strategy A 一键保存)
    // =================================================================
    private void initPauseUI() {
        pauseStage = new Stage(new ScreenViewport());
        Table root = new Table();
        root.setFillParent(true);
        pauseStage.addActor(root);

        // 标题 (兼作状态提示)
        pauseScoreLabel = new Label("GAME PAUSED", game.getSkin(), "title");
        pauseScoreLabel.setColor(Color.GOLD);
        root.add(pauseScoreLabel).padBottom(40).row();

        Table btns = new Table();
        ButtonFactory bf = new ButtonFactory(game.getSkin());
        float w = 350, h = 90, pad = 15;

        // 1. CONTINUE
        btns.add(bf.create("CONTINUE", this::togglePause)).size(w,h).pad(pad);

        // 2. RESET MAZE
        btns.add(bf.create("RESET MAZE", () -> game.resetMaze(difficultyConfig.difficulty))).size(w,h).pad(pad);

        // 3. SETTINGS
        btns.add(bf.create("SETTINGS", () -> game.setScreen(
                new SettingsScreen(game, SettingsScreen.SettingsSource.PAUSE_MENU, game.getScreen())
        ))).size(w,h).pad(pad);

        // 4. MENU (返回主菜单)
        btns.add(bf.create("MENU", game::goToMenu)).size(w,h).pad(pad);

        // ============================================================
        // 🔥 5. SAVE GAME (一键保存)
        // ============================================================
        btns.add(bf.create("SAVE GAME", () -> {
            // 调用 GameManager 的定向保存逻辑
            gm.saveGameProgress();

            // UI 反馈：修改标题颜色提示保存成功
            String oldText = pauseScoreLabel.getText().toString();
            pauseScoreLabel.setText("GAME SAVED!");
            pauseScoreLabel.setColor(Color.GREEN);

            // 1秒后恢复原状
            com.badlogic.gdx.utils.Timer.schedule(new com.badlogic.gdx.utils.Timer.Task() {
                @Override public void run() {
                    pauseScoreLabel.setText("GAME PAUSED");
                    pauseScoreLabel.setColor(Color.GOLD);
                }
            }, 1f);
        })).size(w,h).pad(pad);

        root.add(btns);
        pauseUIInitialized = true;
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
        if (playerTrailManager != null) playerTrailManager.dispose();
    }
}