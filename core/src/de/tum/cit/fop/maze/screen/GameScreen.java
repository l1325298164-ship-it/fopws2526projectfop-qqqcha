package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.entities.trap.Trap;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.input.KeyBindingManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.tools.DeveloperConsole;
import de.tum.cit.fop.maze.ui.HUD;
import de.tum.cit.fop.maze.utils.CameraManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GameScreen implements Screen {

    private final MazeRunnerGame game;
    private final DifficultyConfig difficultyConfig;
    private GameManager gm;
    private MazeRenderer maze;
    private CameraManager cam;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private HUD hud;
    private PlayerInputHandler input;
    private DeveloperConsole console;
    private Texture uiTop, uiBottom, uiLeft, uiRight;

    enum Type { WALL_BEHIND, ENTITY, WALL_FRONT }

    static class Item {
        float y;
        int priority;
        Type type;
        MazeRenderer.WallGroup wall;
        GameObject entity;

        Item(MazeRenderer.WallGroup w, Type t) { wall = w; y = w.startY; type = t; }
        Item(GameObject e, int p) { entity = e; y = e.getY(); priority = p; type = Type.ENTITY; }
    }

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

        input = new PlayerInputHandler();
        batch = game.getSpriteBatch();
        shapeRenderer = new ShapeRenderer();
        gm = new GameManager(difficultyConfig);
        maze = new MazeRenderer(gm, difficultyConfig);
        cam = new CameraManager(difficultyConfig);
        hud = new HUD(gm);

        cam.centerOnPlayerImmediately(gm.getPlayer());
        console = new DeveloperConsole(gm, game.getSkin());
    }

    @Override
    public void render(float delta) {
        if (KeyBindingManager.getInstance().isJustPressed(KeyBindingManager.GameAction.CONSOLE)) console.toggle();

        if (!console.isVisible() && !gm.isLevelTransitionInProgress()) {
            input.update(delta, new PlayerInputHandler.InputHandlerCallback() {
                @Override public void onMoveInput(int dx, int dy) { gm.onMoveInput(dx, dy); }
                @Override public float getMoveDelayMultiplier() { return gm.getPlayer().getMoveDelayMultiplier(); }
                @Override public boolean onAbilityInput(int slot) { return gm.onAbilityInput(slot); }
                @Override public void onInteractInput() { gm.onInteractInput(); }
                @Override public void onMenuInput() { game.goToMenu(); }
            });
            if (Gdx.input.isKeyJustPressed(Input.Keys.R)) gm.requestReset();
        }

        if (!console.isVisible()) {
            gm.update(delta);
            cam.update(delta, gm.getPlayer());
        }

        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);
        batch.setProjectionMatrix(cam.getCamera().combined);

        /* =========================================================
           Phase 1: SpriteBatch - 渲染地板
           ========================================================= */
        batch.begin();
        maze.renderFloor(batch);

        List<ExitDoor> exitDoorsCopy = new ArrayList<>(gm.getExitDoors());
        exitDoorsCopy.forEach(d -> d.renderPortalBack(batch));

        if (gm.getPlayerSpawnPortal() != null) {
            float px = (gm.getPlayer().getX() + 0.5f) * GameConstants.CELL_SIZE;
            float py = (gm.getPlayer().getY() + 0.5f) * GameConstants.CELL_SIZE;
            gm.getPlayerSpawnPortal().renderBack(batch, px, py);
            gm.getPlayerSpawnPortal().renderFront(batch);
        }

        batch.end(); // 🛑 暂停 SpriteBatch

        /* =========================================================
           Phase 1.5: ShapeRenderer - 渲染陷阱实体 (地雷、泥潭)
           ========================================================= */
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(cam.getCamera().combined);

        // 1. 先画陷阱实体 (泥潭底座、芋圆地雷)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Trap trap : gm.getTraps()) {
            trap.drawShape(shapeRenderer);
        }
        shapeRenderer.end(); // ✅ 必须先结束这一次 begin

        // 2. 再画陷阱特效 (泥潭气泡、爆炸火花) - 它们内部有自己的 begin/end
        if (gm.getTrapEffectManager() != null) {
            gm.getTrapEffectManager().render(shapeRenderer);
        }

        /* =========================================================
           Phase 2: SpriteBatch - 渲染遮挡物体 (墙壁、人物、敌人)
           ========================================================= */
        batch.begin(); // ▶️ 重启 SpriteBatch

        List<Item> items = new ArrayList<>();
        for (var wg : maze.getWallGroups()) {
            boolean front = maze.isWallInFrontOfAnyEntity(wg.startX, wg.startY);
            items.add(new Item(wg, front ? Type.WALL_FRONT : Type.WALL_BEHIND));
        }
        items.add(new Item(gm.getPlayer(), 100));
        gm.getEnemies().forEach(e -> items.add(new Item(e, 50)));
        exitDoorsCopy.forEach(d -> items.add(new Item(d, 45)));
        gm.getHearts().forEach(h -> { if (h.isActive()) items.add(new Item(h, 30)); });
        gm.getTreasures().forEach(t -> items.add(new Item(t, 20)));
        gm.getKeys().forEach(k -> { if (k.isActive()) items.add(new Item(k, 35)); });

        items.sort(Comparator.comparingDouble((Item i) -> -i.y)
                .thenComparingInt(i -> i.type.ordinal())
                .thenComparingInt(i -> i.priority));

        for (Item it : items) {
            if (it.wall != null) maze.renderWallGroup(batch, it.wall);
            else it.entity.drawSprite(batch);
        }

        exitDoorsCopy.forEach(d -> d.renderPortalFront(batch));
        gm.getKeyEffectManager().render(batch);
        gm.getBobaBulletEffectManager().render(batch);
        batch.end();

        /* =========================================================
           Phase 3: ShapeRenderer - 渲染顶层特效 (光效、刀光)
           ========================================================= */
        // 1. 物品收集特效
        if (gm.getItemEffectManager() != null) {
            gm.getItemEffectManager().render(shapeRenderer);
        }

        // 2. 战斗特效 (最上层)
        if (gm.getCombatEffectManager() != null) {
            gm.getCombatEffectManager().render(shapeRenderer);
        }

        /* =========================================================
           Phase 4: UI
           ========================================================= */
        renderUI();
    }

    private void renderUI() {
        batch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
        batch.begin();
        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        int t = 1000;
        batch.draw(uiTop, 0, h - t, w, t);
        batch.draw(uiBottom, 0, 0, w, t);
        batch.draw(uiLeft, -50, 0, t+400, h);
        batch.draw(uiRight, w - t - 200, 0, t+300, h);
        hud.renderInGameUI(batch);
        batch.end();
        hud.renderManaBar();
        if (console != null) console.render();
        batch.setProjectionMatrix(cam.getCamera().combined);
    }

    @Override
    public void dispose() {
        maze.dispose();
        if (console != null) console.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }

    @Override public void resize(int w, int h) { if (console != null) console.resize(w, h); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}