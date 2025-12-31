package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.ScreenUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.entities.GameObject;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.ui.HUD;
import de.tum.cit.fop.maze.utils.CameraManager;

import java.util.*;

public class GameScreen implements Screen {

    private final MazeRunnerGame game;
    private GameManager gm;
    private MazeRenderer maze;
    private CameraManager cam;
    private SpriteBatch batch;
    private HUD hud;
    private PlayerInputHandler input;

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

    public GameScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        input = new PlayerInputHandler();

        batch = game.getSpriteBatch();
        gm = new GameManager();
        maze = new MazeRenderer(gm);
        cam = new CameraManager();
        hud = new HUD(gm);

        cam.centerOnPlayerImmediately(gm.getPlayer());
    }

    @Override
    public void render(float delta) {

        /* ================= 输入 ================= */
        input.update(delta, new PlayerInputHandler.InputHandlerCallback() {
            @Override public void onMoveInput(int dx, int dy) { gm.onMoveInput(dx, dy); }
            @Override public float getMoveDelayMultiplier() { return gm.getPlayer().getMoveDelayMultiplier(); }
            @Override public boolean onAbilityInput(int slot) { return gm.onAbilityInput(slot); }
            @Override public void onInteractInput() { gm.onInteractInput(); }
            @Override public void onMenuInput() { game.goToMenu(); }
        });

        // R 重置
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            gm.resetGame();
            cam.centerOnPlayerImmediately(gm.getPlayer());
        }

        /* ================= 更新 ================= */
        gm.update(delta);
        cam.update(delta, gm.getPlayer());

        /* ================= 清屏 ================= */
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1f);
        batch.setProjectionMatrix(cam.getCamera().combined);

        /* =========================================================
           ① 地板 + 门背后呼吸光（Portal Back）
           ========================================================= */
        batch.begin();
        maze.renderFloor(batch);
        gm.getExitDoors().forEach(d -> d.renderPortalBack(batch));
        batch.end();

        /* =========================================================
           ② 世界实体排序渲染
           ========================================================= */
        List<Item> items = new ArrayList<>();

        for (var wg : maze.getWallGroups()) {
            boolean front = maze.isWallInFrontOfAnyEntity(wg.startX, wg.startY);
            items.add(new Item(wg, front ? Type.WALL_FRONT : Type.WALL_BEHIND));
        }

        items.add(new Item(gm.getPlayer(), 100));
        gm.getEnemies().forEach(e -> items.add(new Item(e, 50)));
        gm.getExitDoors().forEach(d -> items.add(new Item(d, 45)));
        gm.getHearts().forEach(h -> { if (h.isActive()) items.add(new Item(h, 30)); });
        gm.getTreasures().forEach(t -> items.add(new Item(t, 20)));
        if (gm.getKey() != null && gm.getKey().isActive()) {
            items.add(new Item(gm.getKey(), 35));
        }

        items.sort(
                Comparator
                        .comparingDouble((Item i) -> -i.y)
                        .thenComparingInt(i -> i.type.ordinal())
                        .thenComparingInt(i -> i.priority)
        );

        batch.begin();
        for (Item it : items) {
            if (it.wall != null) maze.renderWallGroup(batch, it.wall);
            else it.entity.drawSprite(batch);
        }
        batch.end();

        /* =========================================================
           ③ 门前龙卷风粒子（Portal Front）
           ========================================================= */
        batch.begin();
        gm.getExitDoors().forEach(d -> d.renderPortalFront(batch));
        gm.getKeyEffectManager().render(batch);
        gm.getBobaBulletEffectManager().render(batch);
        batch.end();

        /* =========================================================
           ④ UI（正交相机）
           ========================================================= */
        renderUI();
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
        hud.renderInGameUI(batch);
        batch.end();

        hud.renderManaBar();

        batch.setProjectionMatrix(cam.getCamera().combined);
    }

    @Override public void dispose() { maze.dispose(); }
    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}
