package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20; // 🔥 [Fix] Import GL20
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.abilities.Ability;
import de.tum.cit.fop.maze.abilities.MagicAbility;
import de.tum.cit.fop.maze.effects.fog.FogSystem;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.Obstacle.DynamicObstacle;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.entities.trap.Trap;
import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.game.GameSaveData;
import de.tum.cit.fop.maze.game.score.LevelResult;
import de.tum.cit.fop.maze.input.KeyBindingManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.tools.DeveloperConsole;
import de.tum.cit.fop.maze.ui.HUD;
import de.tum.cit.fop.maze.utils.CameraManager;
import de.tum.cit.fop.maze.utils.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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

    // PAUSE
    private boolean paused = false;
    private Stage pauseStage;
    private boolean pauseUIInitialized = false;

    // ✨ [新增] GAME OVER
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
        uiTop = new Texture("Wallpaper/HUD_up.png");
        uiBottom = new Texture("Wallpaper/HUD_down.png");
        uiLeft = new Texture("Wallpaper/HUD_left.png");
        uiRight = new Texture("Wallpaper/HUD_right.png");
        input = new PlayerInputHandler();

        batch = game.getSpriteBatch();
        
        // ✨ [修复] 优先使用 MazeRunnerGame 中的 GameManager，避免重复创建
        gm = game.getGameManager();
        if (gm == null) {
            gm = new GameManager(difficultyConfig);
        }
        
        maze = new MazeRenderer(gm, difficultyConfig);
        cam = new CameraManager(difficultyConfig);

        worldViewport = new FitViewport(
                GameConstants.CAMERA_VIEW_WIDTH,
                GameConstants.CAMERA_VIEW_HEIGHT,
                cam.getCamera()
        );
        uiStage = new Stage(new ScreenViewport(), batch);
        hud = new HUD(gm);
        game.setActiveGameScreen(this);
        cam.centerOnPlayerImmediately(gm.getPlayer());
        console = new DeveloperConsole(gm, game.getSkin());
    }

    @Override
    public void render(float delta) {
        Vector3 world = cam.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        int tileX = (int)(world.x / GameConstants.CELL_SIZE);
        int tileY = (int)(world.y / GameConstants.CELL_SIZE);
        gm.setMouseTargetTile(tileX, tileY);

        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) Logger.toggleDebug();
        if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {
            if (!cam.isDebugZoom()) {
                cam.setDebugZoom(5f);
                Logger.debug("DEBUG CAMERA: Wide view enabled");
            } else {
                cam.clearDebugZoom();
                Logger.debug("DEBUG CAMERA: Normal view restored");
            }
        }

        OrthographicCamera camera = cam.getCamera();
        float camLeft = camera.position.x - camera.viewportWidth / 2f;
        float camBottom = camera.position.y - camera.viewportHeight / 2f;
        float camWidth = camera.viewportWidth;
        float camHeight = camera.viewportHeight;

        worldViewport.apply();
        batch.setProjectionMatrix(cam.getCamera().combined);

        // 输入更新
        if (KeyBindingManager.getInstance().isJustPressed(KeyBindingManager.GameAction.CONSOLE)) {
            console.toggle();
        }
        if (!paused && !console.isVisible() && !gm.isLevelTransitionInProgress()) {
            // 输入处理逻辑保持不变...
            handleInput(delta);
        }

        if (!paused && !console.isVisible()) {
            gm.update(delta);
            if (fogSystem != null) fogSystem.update(delta);
            
            // ✨ [新增] 检查关卡是否完成，跳转到结算界面
            if (gm.isLevelCompletedPendingSettlement()) {
                goToSettlementScreen();
                return; // 跳过剩余渲染
            }
            
            // ✨ [新增] 检查玩家是否死亡，显示游戏结束界面
            if (gm.isPlayerDead() && !gameOverShown) {
                showGameOverScreen();
            }
        }

        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);
        batch.setProjectionMatrix(cam.getCamera().combined);

        // ① 地板 + 门背后呼吸光
        batch.begin();
        maze.renderFloor(batch);
        if (!console.isVisible()) {
            float timeScale = gm.getVariable("time_scale");
            cam.update(delta * timeScale, gm.getPlayer(), gm);
        }
        List<ExitDoor> exitDoorsCopy = new ArrayList<>(gm.getExitDoors());
        exitDoorsCopy.forEach(d -> d.renderPortalBack(batch));
        batch.end();

        // ② 世界实体排序渲染
        renderEntities(batch, exitDoorsCopy);

        // ③ 门前粒子 + 物品/陷阱特效 + 战斗特效(Sprite层)
        batch.begin();
        exitDoorsCopy.forEach(d -> d.renderPortalFront(batch));
        gm.getBobaBulletEffectManager().render(batch);
        if (gm.getItemEffectManager() != null) gm.getItemEffectManager().renderSprites(batch);
        if (gm.getTrapEffectManager() != null) gm.getTrapEffectManager().renderSprites(batch);

        // 🔥 [Fix] 渲染战斗特效 (SpriteBatch层 - 文字、贴图)
        if (gm.getCombatEffectManager() != null) {
            gm.getCombatEffectManager().renderSprites(batch);
        }
        batch.end();

        // ④ 特效粒子 (ShapeRenderer 层)
        if (gm.getItemEffectManager() != null) gm.getItemEffectManager().renderShapes(shapeRenderer);
        if (gm.getTrapEffectManager() != null) gm.getTrapEffectManager().renderShapes(shapeRenderer);

        // 🔥 [Fix] 渲染战斗特效 (ShapeRenderer层 - 粒子、形状)
        if (gm.getCombatEffectManager() != null) {
            // 启用混合模式以获得更好的光效
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

            shapeRenderer.setProjectionMatrix(cam.getCamera().combined);
            gm.getCombatEffectManager().renderShapes(shapeRenderer);

            Gdx.gl.glDisable(GL20.GL_BLEND);
        }

        // ⑤ 玩家出生点
        batch.begin();
        if (gm.getPlayerSpawnPortal() != null) {
            float px = (gm.getPlayer().getX() + 0.5f) * GameConstants.CELL_SIZE;
            float py = (gm.getPlayer().getY() + 0.5f) * GameConstants.CELL_SIZE;
            gm.getPlayerSpawnPortal().renderBack(batch, px, py);
            gm.getPlayerSpawnPortal().renderFront(batch);
        }
        batch.end();

        // ⑥ 技能 Debug
        shapeRenderer.setProjectionMatrix(cam.getCamera().combined);
        for (Player p : gm.getPlayers()) {
            if (p.getAbilityManager() != null) p.getAbilityManager().drawAbilities(batch, shapeRenderer, p);
        }

        // ⑦ 雾
        batch.begin();
        float fogX, fogY;
        CatFollower cat = gm.getCat();
        if (cat != null) { fogX = cat.getWorldX(); fogY = cat.getWorldY(); }
        else { fogX = gm.getPlayer().getWorldX(); fogY = gm.getPlayer().getWorldY(); }
        if (fogSystem != null) {
            fogSystem.render(batch, camLeft, camBottom, camWidth, camHeight, fogX, fogY);
        }
        batch.end();

        // UI
        renderUI();
        if (paused) {
            if (!pauseUIInitialized) initPauseUI();
            Gdx.input.setInputProcessor(pauseStage);
            pauseStage.act(delta);
            pauseStage.draw();
        }
        
        // ✨ [新增] 游戏结束界面渲染
        if (gameOverShown && gameOverStage != null) {
            gameOverStage.act(delta);
            gameOverStage.draw();
        }
    }

    // 提取出的辅助方法，保持 render 清晰
    private void handleInput(float delta) {
        input.update(delta, new PlayerInputHandler.InputHandlerCallback() {
            @Override public void onMoveInput(Player.PlayerIndex index, int dx, int dy) { gm.onMoveInput(index, dx, dy); }
            @Override public float getMoveDelayMultiplier() { return 1.0f; }
            @Override public boolean onAbilityInput(Player.PlayerIndex index, int slot) { return gm.onAbilityInput(index, slot); }
            @Override public void onInteractInput(Player.PlayerIndex index) { gm.onInteractInput(index); }
            @Override public void onMenuInput() { togglePause(); }
        }, Player.PlayerIndex.P1);
        if (gm.isTwoPlayerMode()) {
            input.update(delta, new PlayerInputHandler.InputHandlerCallback() {
                @Override public void onMoveInput(Player.PlayerIndex index, int dx, int dy) { gm.onMoveInput(index, dx, dy); }
                @Override public float getMoveDelayMultiplier() { return 1.0f; }
                @Override public boolean onAbilityInput(Player.PlayerIndex index, int slot) { return gm.onAbilityInput(index, slot); }
                @Override public void onInteractInput(Player.PlayerIndex index) { gm.onInteractInput(index); }
                @Override public void onMenuInput() {}
            }, Player.PlayerIndex.P2);
        }
        // ✨ [修复] 优化双人模式检查，提高代码健壮性
        if (gm.isTwoPlayerMode()) {
            List<Player> players = gm.getPlayers();
            if (players != null && players.size() > 1) {
                Player p2 = players.get(1);
                if (p2 != null) {
                    Ability ability = p2.getAbilityManager().getAbility(0);
                    if (ability instanceof MagicAbility m) {
                        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) m.activate(p2, gm);
                    }
                }
            }
        }
    }

    private void renderEntities(SpriteBatch batch, List<ExitDoor> exitDoorsCopy) {
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
        trapsCopy.forEach(t -> {
            if (t.isActive() && t instanceof GameObject) items.add(new Item((GameObject)t, 45));
        });
        exitDoorsCopy.forEach(d -> items.add(new Item(d, 45)));
        List<Heart> heartsCopy = new ArrayList<>(gm.getHearts());
        heartsCopy.forEach(h -> { if (h.isActive()) items.add(new Item(h, 30)); });
        List<Treasure> treasuresCopy = new ArrayList<>(gm.getTreasures());
        treasuresCopy.forEach(t -> items.add(new Item(t, 20)));
        List<DynamicObstacle> obstaclesCopy = new ArrayList<>(gm.getObstacles());
        obstaclesCopy.forEach(o -> items.add(new Item(o, 40)));
        List<Key> keysCopy = new ArrayList<>(gm.getKeys());
        keysCopy.forEach(k -> { if (k.isActive()) items.add(new Item(k, 35)); });

        items.sort(Comparator.comparingDouble((Item i) -> -i.y).thenComparingInt(i -> i.type.ordinal()).thenComparingInt(i -> i.priority));

        batch.begin();
        for (Item it : items) {
            if (it.wall != null) maze.renderWallGroup(batch, it.wall);
            else it.entity.drawSprite(batch);
        }
        batch.end();
    }

    private void renderUI() {
        uiStage.getViewport().apply();
        batch.setProjectionMatrix(uiStage.getCamera().combined);
        batch.begin();
        renderMazeBorderDecorations(batch);
        hud.renderInGameUI(batch);
        hud.renderManaBar(batch);
        batch.end();
        uiStage.act(Gdx.graphics.getDeltaTime());
        uiStage.draw();
        if (console != null) console.render();
        batch.setProjectionMatrix(cam.getCamera().combined);
    }

    /**
     * ✨ [新增] 跳转到结算界面
     */
    private void goToSettlementScreen() {
        // 1. 计算关卡结果（自动计算理论最高分）
        LevelResult result = gm.getLevelResult();
        
        // 2. 获取存档数据
        GameSaveData saveData = gm.getGameSaveData();
        
        // 3. 清除关卡完成标志
        gm.clearLevelCompletedFlag();
        
        // 4. 跳转到结算界面
        game.setScreen(new SettlementScreen(game, result, saveData));
        
        Logger.info("Navigating to SettlementScreen with score: " + result.finalScore);
    }

    /**
     * ✨ [新增] 显示游戏结束界面
     */
    private void showGameOverScreen() {
        gameOverShown = true;
        gameOverStage = new Stage(new ScreenViewport());
        
        Table root = new Table();
        root.setFillParent(true);
        gameOverStage.addActor(root);
        
        // 标题
        Label gameOverLabel = new Label("GAME OVER", game.getSkin(), "title");
        gameOverLabel.setColor(Color.RED);
        gameOverLabel.setFontScale(2.0f);
        root.add(gameOverLabel).padBottom(30).row();
        
        // 最终分数
        Label scoreLabel = new Label("Final Score: " + gm.getScore(), game.getSkin());
        scoreLabel.setColor(Color.GOLD);
        scoreLabel.setFontScale(1.5f);
        root.add(scoreLabel).padBottom(50).row();
        
        // 按钮
        Table buttonTable = new Table();
        ButtonFactory bf = new ButtonFactory(game.getSkin());
        float btnW = 300;
        float btnH = 70;
        float padding = 15;
        
        // 重试按钮
        buttonTable.add(bf.create("RETRY", () -> {
            game.resetMaze(difficultyConfig.difficulty);
        })).width(btnW).height(btnH).pad(padding);
        
        // 返回菜单按钮
        buttonTable.add(bf.create("MENU", () -> {
            game.goToMenu();
        })).width(btnW).height(btnH).pad(padding);
        
        root.add(buttonTable);
        
        Gdx.input.setInputProcessor(gameOverStage);
    }

    private void togglePause() { 
        if (gameOverShown) return; // 游戏结束时禁止暂停
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
        
        Label scoreLabel = new Label("SCORE: " + gm.getScore(), game.getSkin(), "title"); 
        root.add(scoreLabel).colspan(4).padTop(60).expandY().top().row(); 
        
        Table buttonTable = new Table(); 
        ButtonFactory bf = new ButtonFactory(game.getSkin()); 
        float btnW = 350; 
        float btnH = 90; 
        float padding = 15; 
        
        buttonTable.add(bf.create("CONTINUE", this::togglePause)).width(btnW).height(btnH).pad(padding); 
        buttonTable.add(bf.create("RESET MAZE", () -> { game.resetMaze(difficultyConfig.difficulty); })).width(btnW).height(btnH).pad(padding); 
        buttonTable.add(bf.create("MENU", () -> { game.goToMenu(); })).width(btnW).height(btnH).pad(padding); 
        
        root.add(buttonTable).expandY().center(); 
        pauseUIInitialized = true; 
    }
    
    private void renderMazeBorderDecorations(SpriteBatch batch) { 
        int w = Gdx.graphics.getWidth(); 
        int h = Gdx.graphics.getHeight(); 
        int thickness = 1000; 
        batch.draw(uiTop, 0, h - thickness+860, w, thickness-120); 
        batch.draw(uiBottom, 0, 0-800, w, thickness-120); 
        batch.draw(uiLeft, -600, 0, thickness-220, h); 
        batch.draw(uiRight, w - thickness+810, 0, thickness-220, h); 
    }
    
    @Override 
    public void resize(int w, int h) { 
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
    }
}