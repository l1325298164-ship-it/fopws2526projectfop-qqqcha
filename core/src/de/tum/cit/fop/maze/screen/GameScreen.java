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
import de.tum.cit.fop.maze.abilities.Ability;
import de.tum.cit.fop.maze.abilities.MagicAbility;
import de.tum.cit.fop.maze.effects.fog.FogSystem;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.Obstacle.DynamicObstacle;
import de.tum.cit.fop.maze.entities.Obstacle.MovingWall;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.entities.trap.Trap;
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
import de.tum.cit.fop.maze.utils.Logger;

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

    //PAUSE
    private boolean paused = false;
    private Stage pauseStage;
    private boolean pauseUIInitialized = false;

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
    //CHAPTER!
//    private final ChapterContext chapterContext;


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
//        uiTop.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
//        uiBottom.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
//        uiLeft.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
//        uiRight.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        input = new PlayerInputHandler();

        batch = game.getSpriteBatch();
        gm = new GameManager(difficultyConfig);
        maze = new MazeRenderer(gm,difficultyConfig);
        cam = new CameraManager(difficultyConfig);
        float mazeW = difficultyConfig.mazeHeight * GameConstants.CELL_SIZE;
        float mazeH = difficultyConfig.mazeWidth * GameConstants.CELL_SIZE;

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

        // ===== 更新鼠标指向的格子（给技能用）=====
        Vector3 world = cam.getCamera().unproject(
                new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0)
        );

        int tileX = (int)(world.x / GameConstants.CELL_SIZE);
        int tileY = (int)(world.y / GameConstants.CELL_SIZE);

        gm.setMouseTargetTile(tileX, tileY);

        // ===== DEBUG TOGGLE (F2) =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            Logger.toggleDebug();
        }
        OrthographicCamera camera = cam.getCamera();

        float camLeft   = camera.position.x - camera.viewportWidth  / 2f;
        float camBottom = camera.position.y - camera.viewportHeight / 2f;
        float camWidth  = camera.viewportWidth;
        float camHeight = camera.viewportHeight;

        // ===== DEBUG CAMERA ZOOM =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.F6)) {

            if (!cam.isDebugZoom()) {
                cam.setDebugZoom(5f); // 超广角
                Logger.debug("DEBUG CAMERA: Wide view enabled");
            } else {
                cam.clearDebugZoom(); // 恢复正常
                Logger.debug("DEBUG CAMERA: Normal view restored");
            }
        }





        worldViewport.apply();
        batch.setProjectionMatrix(cam.getCamera().combined);

        /* ================= 输入 ================= */
        // 🔥 修复：只有在非关卡过渡期间才处理输入
        /* ================= 输入 ================= */

        // 1. 监听控制台开关键
        // 如果按键没反应，请看控制台有没有打印 "尝试切换控制台..."
        if (KeyBindingManager.getInstance().isJustPressed(KeyBindingManager.GameAction.CONSOLE)) {
            System.out.println("检测到控制台按键，正在切换状态...");
            console.toggle();
        }

        // 2. 只有在 [控制台关闭] 且 [非转场期间] 才允许玩家操作
        // 🔥 修复：这里原来漏了 !console.isVisible()
        if (!paused && !console.isVisible() && !gm.isLevelTransitionInProgress()) {

            input.update(delta, new PlayerInputHandler.InputHandlerCallback() {

                @Override
                public void onMoveInput(Player.PlayerIndex index, int dx, int dy) {
                    gm.onMoveInput(index, dx, dy);
                }

                @Override
                public float getMoveDelayMultiplier() {
                    return 1.0f;
                }

                @Override
                public boolean onAbilityInput(Player.PlayerIndex index, int slot) {
                    return gm.onAbilityInput(index, slot);
                }

                @Override
                public void onInteractInput(Player.PlayerIndex index) {
                    gm.onInteractInput(index);
                }

                @Override
                public void onMenuInput() {
                    togglePause();
                }

            }, Player.PlayerIndex.P1);

// 🔥 第二个玩家输入（关键）
            if (gm.isTwoPlayerMode()) {
                input.update(delta, new PlayerInputHandler.InputHandlerCallback() {

                    @Override
                    public void onMoveInput(Player.PlayerIndex index, int dx, int dy) {
                        gm.onMoveInput(index, dx, dy);
                    }

                    @Override
                    public float getMoveDelayMultiplier() {
                        return 1.0f;
                    }

                    @Override
                    public boolean onAbilityInput(Player.PlayerIndex index, int slot) {
                        return gm.onAbilityInput(index, slot);
                    }

                    @Override
                    public void onInteractInput(Player.PlayerIndex index) {
                        gm.onInteractInput(index);
                    }

                    @Override
                    public void onMenuInput() {}

                }, Player.PlayerIndex.P2);
            }
            // ===== Magic 鼠标技能（P2）=====
            if (gm.isTwoPlayerMode() && gm.getPlayers().size() > 1) {

                Player p2 = gm.getPlayers().get(1);
                Ability ability = p2.getAbilityManager().getAbility(0);

                if (ability instanceof MagicAbility m) {

                    // 只监听「再次按下」
                    if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {

                        // 统一走 Ability 的 onActivate 状态机
                        m.activate(p2, gm);
                    }
                }
            }



        }

        /* ================= 更新 ================= */
        if (!paused &&!console.isVisible()) {
            gm.update(delta);
            if (fogSystem != null) fogSystem.update(delta);

        }

        /* ================= 清屏 ================= */
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);

        if (!console.isVisible()) {

            // 🔥 [Console] 获取时间流速变量 (默认 1.0)
            // 如果你在控制台输入 set time_scale 0.5，游戏就会变成慢动作
            float timeScale = gm.getVariable("time_scale");

            // 计算“真实”经过的游戏时间
            float gameDelta = delta * timeScale;

            // 注意：这里需要把 gameDelta 传进去，这样相机的跟随速度也会随时间变慢
            cam.update(gameDelta, gm);
        }

        worldViewport.apply();
        batch.setProjectionMatrix(cam.getCamera().combined);

        /* =========================================================
           ① 地板 + 门背后呼吸光（Portal Back）
           ========================================================= */
        batch.begin();
        maze.renderFloor(batch);


        // 🔥 关键修复：使用防御性副本避免 ConcurrentModificationException
        List<ExitDoor> exitDoorsCopy = new ArrayList<>(gm.getExitDoors());
        exitDoorsCopy.forEach(d -> d.renderPortalBack(batch));
        batch.end();

        /* =========================================================
           ② 世界实体排序渲染
           ========================================================= */
        List<Item> items = new ArrayList<>();

        // 墙壁
        for (var wg : maze.getWallGroups()) {
            boolean front = maze.isWallInFrontOfAnyEntity(wg.startX, wg.startY);
            items.add(new Item(wg, front ? Type.WALL_FRONT : Type.WALL_BEHIND));
        }

        // 🔥 玩家始终渲染（不会被隐藏）
        for (Player p : gm.getPlayers()) {
            items.add(new Item(p, 100));
        }
        if (gm.getCat() != null) {
            items.add(new Item(gm.getCat(), 95)); // 比玩家略低
        }


        // 🔥 修复：为所有实体集合创建防御性副本
        List<Enemy> enemiesCopy = new ArrayList<>(gm.getEnemies());
        enemiesCopy.forEach(e -> items.add(new Item(e, 50)));

        List<Trap> trapsCopy = new ArrayList<>(gm.getTraps());
        Logger.debug("准备渲染陷阱数量: " + trapsCopy.size());
        trapsCopy.forEach(t -> {
            if (t.isActive()) {
                // 检查陷阱是否实现了GameObject接口
                if (t instanceof GameObject) {
                    items.add(new Item((GameObject)t, 45)); // 优先级45
                    Logger.debug("添加陷阱到渲染列表: " + t.getClass().getSimpleName() +
                            " at (" + t.getX() + "," + t.getY() + ")");
                } else {
                    Logger.warning("陷阱 " + t.getClass().getSimpleName() + " 没有实现GameObject接口");
                }
            }
        });

        // 再次使用 exitDoorsCopy（而不是原始集合）
        exitDoorsCopy.forEach(d -> items.add(new Item(d, 45)));

        List<Heart> heartsCopy = new ArrayList<>(gm.getHearts());
        heartsCopy.forEach(h -> {
            if (h.isActive()) items.add(new Item(h, 30));
        });

        List<Treasure> treasuresCopy = new ArrayList<>(gm.getTreasures());
        treasuresCopy.forEach(t -> items.add(new Item(t, 20)));

        List<HeartContainer> containersCopy = new ArrayList<>(gm.getHeartContainers());
        containersCopy.forEach(hc -> {
            // 只有激活状态才渲染 (捡起后 active 会变成 false)
            if (hc.isActive()) {
                items.add(new Item(hc, 30));
            }
        });

        // 🔥 新增：动态障碍物（移动墙）
        List<DynamicObstacle> obstaclesCopy = new ArrayList<>(gm.getObstacles());
        obstaclesCopy.forEach(o -> items.add(new Item(o, 40)));

        List<Key> keysCopy = new ArrayList<>(gm.getKeys());
        keysCopy.forEach(k -> {
            if (k.isActive()) {
                items.add(new Item(k, 35));
            }
        });
        // 排序
        items.sort(
                Comparator
                        .comparingDouble((Item i) -> -i.y)
                        .thenComparingInt(i -> i.type.ordinal())
                        .thenComparingInt(i -> i.priority)
        );

        // 渲染
        batch.begin();
        for (Item it : items) {
            if (it.wall != null) {
                maze.renderWallGroup(batch, it.wall);
            } else {
                it.entity.drawSprite(batch);
            }
        }
        batch.end();

        /* =========================================================
           ③ 门前龙卷风粒子（Portal Front）
           ========================================================= */
        batch.begin();
        // 🔥 使用防御性副本
        exitDoorsCopy.forEach(d -> d.renderPortalFront(batch));
        gm.getKeyEffectManager().render(batch);
        gm.getBobaBulletEffectManager().render(batch);
        batch.end();
/* =========================================================
   玩家脚下传送阵（Portal Effect）
   ========================================================= */
        batch.begin();
        if (gm.getPlayerSpawnPortal() != null) {
            float px = (gm.getPlayer().getX() + 0.5f) * GameConstants.CELL_SIZE;
            float py = (gm.getPlayer().getY() + 0.5f) * GameConstants.CELL_SIZE;

            gm.getPlayerSpawnPortal().renderBack(batch, px, py);
            gm.getPlayerSpawnPortal().renderFront(batch);
        }
        batch.end();
// ===== Ability Debug / Targeting (AOE etc.) =====
        shapeRenderer.setProjectionMatrix(cam.getCamera().combined);

        for (Player p : gm.getPlayers()) {
            if (p.getAbilityManager() != null) {
                p.getAbilityManager().drawAbilities(batch, shapeRenderer, p);
            }
        }






// ===== 雾（一定在这里）=====
        batch.begin();
        float fogX, fogY;

        CatFollower cat = gm.getCat();
        if (cat != null) {
            fogX = cat.getWorldX();
            fogY = cat.getWorldY();
        } else {
            fogX = gm.getPlayer().getWorldX();  // 让雾跟玩家走
            fogY = gm.getPlayer().getWorldY();
        }

        if (fogSystem != null) {
            fogSystem.render(
                    batch,
                    camLeft, camBottom, camWidth, camHeight,
                    gm.getCat() != null ? gm.getCat().getWorldX() : gm.getPlayer().getWorldX(),
                    gm.getCat() != null ? gm.getCat().getWorldY() : gm.getPlayer().getWorldY()
            );
        }
        batch.end();





        /* =========================================================
   DEBUG：迷宫范围 / Camera 视野 / MovingWall 位置
   ========================================================= */
        if (Logger.isDebugEnabled()) {

            // ⚠️ ShapeRenderer 必须使用和 world 一样的投影矩阵
            shapeRenderer.setProjectionMatrix(cam.getCamera().combined);

            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

            float cs = GameConstants.CELL_SIZE;

            /* ===== 1️⃣ 迷宫整体边界（红色） ===== */
            int mazeWidth  = difficultyConfig.mazeWidth;
            int mazeHeight = difficultyConfig.mazeHeight;

            shapeRenderer.setColor(1, 0, 0, 1); // 红色
            shapeRenderer.rect(
                    0,
                    0,
                    mazeWidth * cs,
                    mazeHeight * cs
            );

            /* ===== 2️⃣ Camera 实际可视范围（黄色） ===== */
            shapeRenderer.setColor(1, 1, 0, 1); // 黄色
            shapeRenderer.rect(
                    camLeft,
                    camBottom,
                    camWidth,
                    camHeight
            );

            /* ===== 3️⃣ 所有 MovingWall 的 world 位置（蓝色十字） ===== */
            shapeRenderer.setColor(0, 0, 1, 1); // 蓝色

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

        if (paused) {
            if (!pauseUIInitialized) {
                initPauseUI();
            }

            Gdx.input.setInputProcessor(pauseStage);

            pauseStage.act(delta);
            pauseStage.draw();
            return; // ⛔ 非常重要：不要再继续渲染后面的逻辑
        }


    }

    private void togglePause() {
        paused = !paused;

        if (paused) {
            // ⏸ 进入暂停：输入交给 Pause UI
            if (pauseStage == null) {
                initPauseUI();
            }
            Gdx.input.setInputProcessor(pauseStage);
        } else {
            // ▶ 继续游戏：把输入还给游戏
            Gdx.input.setInputProcessor(null);
            // 如果你后面有 Stage 输入（比如 HUD），这里再换成对应的
        }

        Gdx.app.log("GameScreen", paused ? "Paused" : "Resumed");
    }
    private void initPauseUI() {
        pauseStage = new Stage(new ScreenViewport());
        Table root = new Table();
        root.setFillParent(true);
        pauseStage.addActor(root);

        // 1. 分数：放在屏幕正上方
        Label scoreLabel = new Label("SCORE: " + gm.getScore(), game.getSkin(), "title");
        scoreLabel.setFontScale(1.0f);
        root.add(scoreLabel).colspan(4).padTop(60).expandY().top().row();

        // 2. 按钮区域：横向排列
        Table buttonTable = new Table();
        ButtonFactory bf = new ButtonFactory(game.getSkin());

        // 统一设置按钮尺寸。
        // 因为文字较多，我们将宽度从 300 增加到 350
        float btnW = 350;
        float btnH = 90;
        float padding = 15;

        // CONTINUE
        buttonTable.add(bf.create("CONTINUE", this::togglePause))
                .width(btnW).height(btnH).pad(padding);

        // RESET (新加入)
        buttonTable.add(bf.create("RESET MAZE", () -> {
                    game.resetMaze(difficultyConfig.difficulty); // 调用你 MazeRunnerGame 里的开始新游戏逻辑
                }))
                .width(btnW).height(btnH).pad(padding);

        // SETTINGS
        buttonTable.add(bf.create("SETTINGS", () -> {
            game.setScreen(
                    new SettingsScreen(
                            game,
                            SettingsScreen.SettingsSource.PAUSE_MENU,
                            game.getScreen() // 当前 GameScreen
                    )
            );
        })).width(btnW).height(btnH).pad(padding);

        // BACK TO MENU (增加宽度以容纳文字)
        buttonTable.add(bf.create("MENU", () -> {
                    game.goToMenu();
                }))
                .width(btnW).height(btnH).pad(padding);

        // 将整排按钮居中显示
        root.add(buttonTable).expandY().center();

        pauseUIInitialized = true;
    }


    //decoration Wall
    private void renderMazeBorderDecorations(SpriteBatch batch) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        int thickness = 1000;

        batch.draw(uiTop,    0, h - thickness+860, w, thickness-120);
        batch.draw(uiBottom, 0, 0-800,             w, thickness-120);
        batch.draw(uiLeft,   -600, 0,             thickness-220, h);
        batch.draw(uiRight,  w - thickness+810, 0, thickness-220, h);
    }


    private void renderUI() {
        // ===== 保存 batch 状态 =====
        Matrix4 oldProjection = batch.getProjectionMatrix().cpy();
        Color oldColor = batch.getColor().cpy();
        // ===== 1. UI SpriteBatch（HUD / 装饰）=====
        uiStage.getViewport().apply();
        batch.setProjectionMatrix(uiStage.getCamera().combined);

        batch.begin();

        // 边框装饰（如果这是 UI 装饰，放这里）
        renderMazeBorderDecorations(batch);
        // HUD
        hud.renderInGameUI(batch);
        batch.end();

        // ===== 2. Scene2D UI =====
        uiStage.act(Gdx.graphics.getDeltaTime());
        uiStage.draw();

        // ===== 3. Debug / Console（如果需要）=====
        if (console != null) {
            console.render();
        }

        // ===== 4. 恢复世界相机（非常重要）=====
        batch.setProjectionMatrix(cam.getCamera().combined);

        // ===== 🔥 恢复 batch 状态（关键）=====
        batch.setColor(oldColor);
        batch.setProjectionMatrix(oldProjection);
    }


    @Override
    public void dispose() {
        maze.dispose();
        if (console != null) console.dispose();
    }

    @Override
    public void resize(int w, int h) {
        worldViewport.update(w, h, true);

        if (uiStage != null) {
            uiStage.getViewport().update(w, h, true);
        }

        if (pauseStage != null) {
            pauseStage.getViewport().update(w, h, true);
        }

        if (console != null) {
            console.resize(w, h);
        }
    }


    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}