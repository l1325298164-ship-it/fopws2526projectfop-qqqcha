package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
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

import java.util.*;

import static de.tum.cit.fop.maze.maze.MazeGenerator.BORDER_THICKNESS;

public class GameScreen implements Screen {

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
    }

    @Override
    public void show() {
        uiTop = new Texture("Wallpaper/background.png");
        uiBottom = new Texture("Wallpaper/frontground.png");
        uiLeft = new Texture("Wallpaper/leftground.png");
        uiRight = new Texture("Wallpaper/rightground.png");
//        uiTop.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
//        uiBottom.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
//        uiLeft.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
//        uiRight.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        input = new PlayerInputHandler();

        batch = game.getSpriteBatch();
        gm = new GameManager(difficultyConfig);
        maze = new MazeRenderer(gm,difficultyConfig);
        cam = new CameraManager(difficultyConfig);
        hud = new HUD(gm);
        game.setActiveGameScreen(this);
        cam.centerOnPlayerImmediately(gm.getPlayer());
        console = new DeveloperConsole(gm, game.getSkin());
    }

    @Override
    public void render(float delta) {
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
                public void onMoveInput(int dx, int dy) {
                    gm.onMoveInput(dx, dy);
                }

                @Override
                public float getMoveDelayMultiplier() {
                    return gm.getPlayer().getMoveDelayMultiplier();
                }

                @Override
                public boolean onAbilityInput(int slot) {
                    return gm.onAbilityInput(slot);
                }

                @Override
                public void onInteractInput() {
                    gm.onInteractInput();
                }

                @Override
                public void onMenuInput() {
                    togglePause();
                }
            });
        }

        /* ================= 更新 ================= */
        if (!paused &&!console.isVisible()) {
            gm.update(delta);
            cam.update(delta, gm.getPlayer());
        }

        /* ================= 清屏 ================= */
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);
        batch.setProjectionMatrix(cam.getCamera().combined);

        /* =========================================================
           ① 地板 + 门背后呼吸光（Portal Back）
           ========================================================= */
        batch.begin();
        maze.renderFloor(batch);

        /* ================= 更新 ================= */
        if (!console.isVisible()) {

            // 🔥 [Console] 获取时间流速变量 (默认 1.0)
            // 如果你在控制台输入 set time_scale 0.5，游戏就会变成慢动作
            float timeScale = gm.getVariable("time_scale");

            // 计算“真实”经过的游戏时间
            float gameDelta = delta * timeScale;

            // 把变速后的时间传给 gm 和 cam
            gm.update(gameDelta);

            // 注意：这里需要把 gameDelta 传进去，这样相机的跟随速度也会随时间变慢
            cam.update(gameDelta, gm.getPlayer(), gm);
        }
        // 🔥 关键修复：使用防御性副本避免 ConcurrentModificationException
        List<ExitDoor> exitDoorsCopy = new ArrayList<>(gm.getExitDoors());
        exitDoorsCopy.forEach(d -> d.renderPortalBack(batch));
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
        items.add(new Item(gm.getPlayer(), 100));

        // 🔥 修复：为所有实体集合创建防御性副本
        List<Enemy> enemiesCopy = new ArrayList<>(gm.getEnemies());
        enemiesCopy.forEach(e -> items.add(new Item(e, 50)));

        // 再次使用 exitDoorsCopy（而不是原始集合）
        exitDoorsCopy.forEach(d -> items.add(new Item(d, 45)));

        List<Heart> heartsCopy = new ArrayList<>(gm.getHearts());
        heartsCopy.forEach(h -> {
            if (h.isActive()) items.add(new Item(h, 30));
        });

        List<Treasure> treasuresCopy = new ArrayList<>(gm.getTreasures());
        treasuresCopy.forEach(t -> items.add(new Item(t, 20)));

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
        batch.begin();
        batch.end();
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

        root.add(new Label("PAUSED", game.getSkin(), "title"))
                .padBottom(40).row();

        ButtonFactory bf = new ButtonFactory(game.getSkin());

        root.add(bf.create("RESUME", this::togglePause))
                .width(400).height(80).padBottom(20).row();

        root.add(bf.create("SETTINGS", () -> {
                    // TODO: 打开设置界面（之后单独做）
                }))
                .width(400).height(80).padBottom(20).row();

        root.add(bf.create("BACK TO MENU", () -> {
                    game.goToMenu();
                }))
                .width(400).height(80).padBottom(40).row();

        root.add(new Label(
                "Score: " + gm.getScore(),
                game.getSkin()
        ));

        pauseUIInitialized = true;
        if (game.hasRunningGame()) {
            root.add(bf.create("CONTINUE", game::resumeGame));
        }
    }


    //decoration Wall
    private void renderMazeBorderDecorations(SpriteBatch batch) {
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        int thickness = 1000;

        batch.draw(uiTop,    0, h - thickness, w, thickness);
        batch.draw(uiBottom, 0, 0,             w, thickness);
        batch.draw(uiLeft,   -50, 0,             thickness+400, h);
        batch.draw(uiRight,  w - thickness-200, 0, thickness+300, h);
    }


    private void renderUI() {
        batch.setProjectionMatrix(
                new Matrix4().setToOrtho2D(
                        0, 0,
                        Gdx.graphics.getWidth(),
                        Gdx.graphics.getHeight()
                )
        );

        batch.begin();
        renderMazeBorderDecorations(batch);

        hud.renderInGameUI(batch);
        batch.end();
        hud.renderManaBar();

        if (console != null) {
            console.render();
        }

        batch.setProjectionMatrix(cam.getCamera().combined);
    }

    @Override
    public void dispose() {
        maze.dispose();
        if (console != null) console.dispose();
    }

    @Override
    public void resize(int w, int h) {
        if (console != null) console.resize(w, h);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}
}