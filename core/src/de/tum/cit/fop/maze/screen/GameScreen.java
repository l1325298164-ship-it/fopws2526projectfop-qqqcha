package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.effects.fog.FogSystem;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.Obstacle.DynamicObstacle;
import de.tum.cit.fop.maze.entities.Obstacle.MovingWall;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.entities.trap.Trap;
import de.tum.cit.fop.maze.game.*;
import de.tum.cit.fop.maze.game.score.LevelResult;
import de.tum.cit.fop.maze.input.KeyBindingManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.tools.DeveloperConsole;
import de.tum.cit.fop.maze.ui.HUD;
import de.tum.cit.fop.maze.utils.CameraManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.StorageManager;

import java.util.*;

public class GameScreen implements Screen {

    private Viewport worldViewport;
    private Stage uiStage;
    private FogSystem fogSystem;

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

    private boolean paused = false;
    private Stage pauseStage;
    private boolean pauseUIInitialized = false;

    private boolean gameOverShown = false;
    private Stage gameOverStage;

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
        this.game = game;
        this.difficultyConfig = difficultyConfig;
        if (difficultyConfig.difficulty == Difficulty.HARD) {
            fogSystem = new FogSystem();
        } else {
            fogSystem = null;
        }
    }

    @Override
    public void show() {
        uiTop    = new Texture("Wallpaper/HUD_up.png");
        uiBottom = new Texture("Wallpaper/HUD_down.png");
        uiLeft   = new Texture("Wallpaper/HUD_left.png");
        uiRight  = new Texture("Wallpaper/HUD_right.png");

        input = new PlayerInputHandler();
        batch = game.getSpriteBatch();

        gm = game.getGameManager();
        if (gm == null) {
            Logger.warning("GameManager is null, creating new one");
            gm = new GameManager(difficultyConfig, game.isTwoPlayerMode());
            game.setGameManager(gm);
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

        cam.centerOnPlayerImmediately(gm.getPlayer());
        console = new DeveloperConsole(gm, game.getSkin());
    }

    @Override
    public void render(float delta) {
        // ... Input Update Logic ...
        Vector3 world = cam.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        gm.setMouseTargetTile((int)(world.x / GameConstants.CELL_SIZE), (int)(world.y / GameConstants.CELL_SIZE));
        
        OrthographicCamera camera = cam.getCamera();
        float camLeft   = camera.position.x - camera.viewportWidth  / 2f;
        float camBottom = camera.position.y - camera.viewportHeight / 2f;
        float camWidth  = camera.viewportWidth;
        float camHeight = camera.viewportHeight;
        worldViewport.apply();
        batch.setProjectionMatrix(cam.getCamera().combined);

        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) Logger.toggleDebug();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
            if (!cam.isDebugZoom()) cam.setDebugZoom(5f); else cam.clearDebugZoom();
        }
        if (KeyBindingManager.getInstance().isJustPressed(KeyBindingManager.GameAction.CONSOLE)) {
            console.toggle();
        }

        if (!paused && !console.isVisible() && !gm.isLevelTransitionInProgress()) {
            input.update(delta, new PlayerInputHandler.InputHandlerCallback() {
                @Override public void onMoveInput(Player.PlayerIndex i, int dx, int dy) { gm.onMoveInput(i, dx, dy); }
                @Override public float getMoveDelayMultiplier() { return 1f; }
                @Override public boolean onAbilityInput(Player.PlayerIndex i, int s) { return gm.onAbilityInput(i, s); }
                @Override public void onInteractInput(Player.PlayerIndex i) { gm.onInteractInput(i); }
                @Override public void onMenuInput() { togglePause(); }
            }, Player.PlayerIndex.P1);

            if (gm.isTwoPlayerMode()) {
                input.update(delta, new PlayerInputHandler.InputHandlerCallback() {
                    @Override public void onMoveInput(Player.PlayerIndex i, int dx, int dy) { gm.onMoveInput(i, dx, dy); }
                    @Override public float getMoveDelayMultiplier() { return 1f; }
                    @Override public boolean onAbilityInput(Player.PlayerIndex i, int s) { return gm.onAbilityInput(i, s); }
                    @Override public void onInteractInput(Player.PlayerIndex i) { gm.onInteractInput(i); }
                    @Override public void onMenuInput() {}
                }, Player.PlayerIndex.P2);
            }
        }
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);

        if (!paused && !console.isVisible()) {
            gm.update(delta);
            if (fogSystem != null) fogSystem.update(delta);
            if (gm.isLevelCompletedPendingSettlement()) {
                goToSettlementScreen();
                return;
            }
            if (gm.isPlayerDead() && !gameOverShown) {
                showGameOverScreen();
            }
            if (!console.isVisible()) {
                float timeScale = gm.getVariable("time_scale");
                cam.update(delta * timeScale, gm);
            }
        }

        worldViewport.apply();
        batch.setProjectionMatrix(cam.getCamera().combined);

        // ===== 1. Render World (Floor & Entities) =====
        batch.begin();
        maze.renderFloor(batch);
        List<ExitDoor> exitDoorsCopy = new ArrayList<>(gm.getExitDoors());
        exitDoorsCopy.forEach(d -> d.renderPortalBack(batch));
        batch.end();

        // Prepare Entities
        List<Item> items = new ArrayList<>();
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
            if (it.wall != null) maze.renderWallGroup(batch, it.wall);
            else it.entity.drawSprite(batch);
        }
        exitDoorsCopy.forEach(d -> d.renderPortalFront(batch));
        
        // Draw Particles (Low Priority)
        gm.getKeyEffectManager().render(batch);
        gm.getBobaBulletEffectManager().render(batch);
        if (gm.getItemEffectManager() != null) gm.getItemEffectManager().renderSprites(batch);
        if (gm.getTrapEffectManager() != null) gm.getTrapEffectManager().renderSprites(batch);
        if (gm.getPlayerSpawnPortal() != null) {
            float px = (gm.getPlayer().getX() + 0.5f) * GameConstants.CELL_SIZE;
            float py = (gm.getPlayer().getY() + 0.5f) * GameConstants.CELL_SIZE;
            gm.getPlayerSpawnPortal().renderBack(batch, px, py);
            gm.getPlayerSpawnPortal().renderFront(batch);
        }
        batch.end();

        // ===== 2. Render Fog (Overlay) =====
        // 🔥 [修复] 把迷雾渲染放在实体之后，但必须在漂浮文字(FloatingText)之前！
        if (fogSystem != null) {
            batch.begin();
            fogSystem.render(
                    batch,
                    camLeft, camBottom, camWidth, camHeight,
                    gm.getCat() != null ? gm.getCat().getWorldX() : gm.getPlayer().getWorldX(),
                    gm.getCat() != null ? gm.getCat().getWorldY() : gm.getPlayer().getWorldY()
            );
            batch.end();
        }

        // ===== 3. Render High Priority Effects (Floating Text / Combat) =====
        // 🔥 [修复] 确保 CombatEffect 渲染在迷雾之上，否则数字看不见
        batch.begin();
        if (gm.getCombatEffectManager() != null) gm.getCombatEffectManager().renderSprites(batch);
        batch.end();

        // ===== 4. Shapes =====
        shapeRenderer.setProjectionMatrix(cam.getCamera().combined);
        if (gm.getItemEffectManager() != null) gm.getItemEffectManager().renderShapes(shapeRenderer);
        if (gm.getTrapEffectManager() != null) gm.getTrapEffectManager().renderShapes(shapeRenderer);
        if (gm.getCombatEffectManager() != null) {
            Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            gm.getCombatEffectManager().renderShapes(shapeRenderer);
            shapeRenderer.end();
        }

        for (Player p : gm.getPlayers()) {
            if (p.getAbilityManager() != null) {
                p.getAbilityManager().drawAbilities(batch, shapeRenderer, p);
            }
        }

        // Debug
        if (Logger.isDebugEnabled()) {
            shapeRenderer.setProjectionMatrix(cam.getCamera().combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            float cs = GameConstants.CELL_SIZE;
            shapeRenderer.setColor(1, 0, 0, 1);
            shapeRenderer.rect(0, 0, difficultyConfig.mazeWidth * cs, difficultyConfig.mazeHeight * cs);
            shapeRenderer.setColor(1, 1, 0, 1);
            shapeRenderer.rect(camLeft, camBottom, camWidth, camHeight);
            shapeRenderer.end();
        }

        // ===== 5. UI =====
        renderUI();

        if (paused) {
            if (!pauseUIInitialized) initPauseUI();
            Gdx.input.setInputProcessor(pauseStage);
            pauseStage.act(delta);
            pauseStage.draw();
        }
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

        Label score = new Label("SCORE: " + gm.getScore(), game.getSkin(), "title");
        root.add(score).padBottom(40).row();

        Table buttonTable = new Table();
        ButtonFactory bf = new ButtonFactory(game.getSkin());
        float btnW = 350; float btnH = 90; float padding = 15;

        buttonTable.add(bf.create("CONTINUE", this::togglePause)).width(btnW).height(btnH).pad(padding);
        buttonTable.add(bf.create("RESET MAZE", () -> game.resetMaze(difficultyConfig.difficulty))).width(btnW).height(btnH).pad(padding);
        buttonTable.add(bf.create("SETTINGS", () -> game.setScreen(new SettingsScreen(game, SettingsScreen.SettingsSource.PAUSE_MENU, game.getScreen())))).width(btnW).height(btnH).pad(padding);
        buttonTable.add(bf.create("MENU", game::goToMenu)).width(btnW).height(btnH).pad(padding);

        root.add(buttonTable).expandY().center();
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
            if (gm.getGameSaveData() != null) StorageManager.getInstance().saveGameSync(gm.getGameSaveData());
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
    @Override public void dispose() {
        maze.dispose();
        if (console != null) console.dispose();
        if (gameOverStage != null) gameOverStage.dispose();
    }
}