package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.ui.HUD;
import de.tum.cit.fop.maze.utils.CameraManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.List;

/**
 * The GameScreen class is responsible for rendering the gameplay screen.
 * It handles the game logic and rendering of the game elements.
 */
public class GameScreen implements Screen {

    private final MazeRunnerGame game;
    private final OrthographicCamera camera;
    private final BitmapFont font;
    // === 新增 ===
    private GameManager gameManager;
    private MazeRenderer mazeRenderer;
    private CameraManager cameraManager;
    private PlayerInputHandler inputHandler;
    private HUD hud;

    private SpriteBatch worldBatch;
    private SpriteBatch uiBatch;
    private ShapeRenderer shapeRenderer;




    /**
     * Constructor for GameScreen. Sets up the camera and font.
     *
     * @param game The main game class, used to access global resources and methods.
     */
    public GameScreen(MazeRunnerGame game) {
        this.game = game;

        // Create and configure the camera for the game view
        camera = new OrthographicCamera();
        camera.setToOrtho(false);
        camera.zoom = 0.75f;

        // Get the font from the game's skin
        font = game.getSkin().getFont("font");
    }


    // Screen interface methods with necessary functionality
    @Override
    public void render(float delta) {
        handleInput(delta);

        gameManager.update(delta);
        cameraManager.update(delta, gameManager.getPlayer());

        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);

        renderWorld();
        renderUI();
    }


    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void show() {
        worldBatch = game.getSpriteBatch();
        uiBatch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        gameManager = new GameManager();
        mazeRenderer = new MazeRenderer(gameManager);
        cameraManager = new CameraManager();
        inputHandler = new PlayerInputHandler();
        hud = new HUD(gameManager);


        cameraManager.centerOnPlayerImmediately(gameManager.getPlayer());

        Gdx.input.setInputProcessor(null); // 不用 Scene2D
    }


    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        if (hud != null) {
            hud.dispose();
            hud = null;
        }

        // ❌ 不要 dispose game.getSpriteBatch()
        worldBatch = null;

        if (uiBatch != null) {
            uiBatch.dispose();
            uiBatch = null;
        }

        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }

        Logger.debug("GameScreen disposed");
    }



    // Additional methods and logic can be added as needed for the game screen
    private void handleInput(float delta) {

        // === 1. ESC 返回菜单 ===
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.postRunnable(() -> {
                game.goToMenu();
            });
            return;
        }

        // === 2. R 重开 ===
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restartGame();
            return;
        }

        // === 3. F1-F4 切换纹理模式 ===
        TextureManager textureManager = TextureManager.getInstance();

        if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
            textureManager.switchMode(TextureManager.TextureMode.COLOR);
            gameManager.onTextureModeChanged();
            mazeRenderer.onTextureModeChanged();
            Logger.gameEvent("Texture mode switched to COLOR");
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            textureManager.switchMode(TextureManager.TextureMode.IMAGE);
            gameManager.onTextureModeChanged();
            mazeRenderer.onTextureModeChanged();
            Logger.gameEvent("Texture mode switched to IMAGE");
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            textureManager.switchMode(TextureManager.TextureMode.PIXEL);
            gameManager.onTextureModeChanged();
            mazeRenderer.onTextureModeChanged();
            Logger.gameEvent("Texture mode switched to PIXEL");
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
            textureManager.switchMode(TextureManager.TextureMode.MINIMAL);
            gameManager.onTextureModeChanged();
            mazeRenderer.onTextureModeChanged();
            Logger.gameEvent("Texture mode switched to MINIMAL");
        }

        // === 4. 玩家移动 ===
        inputHandler.update(delta, (dx, dy) -> {
            int nx = gameManager.getPlayer().getX() + dx;
            int ny = gameManager.getPlayer().getY() + dy;

            if (gameManager.isValidMove(nx, ny)) {
                gameManager.getPlayer().move(dx, dy);
                AudioManager.getInstance().playPlayerMove();
            }
        });
    }

    private void renderWorld() {
        worldBatch.setProjectionMatrix(cameraManager.getCamera().combined);
        shapeRenderer.setProjectionMatrix(cameraManager.getCamera().combined);

        worldBatch.begin();

        // 1. 地板
        mazeRenderer.renderFloor(worldBatch);

        // 2. 收集并排序
        var renderItems = collectAllRenderItems();
        renderItems.sort((a, b) -> {
            // 1️⃣ 先按 y（视觉深度）
            int yCompare = Float.compare(b.y, a.y);
            if (yCompare != 0) return yCompare;

            // 2️⃣ y 相同 → 按 priority
            return Integer.compare(a.priority, b.priority);
        });


        // 3. 渲染
        for (var item : renderItems) {
            if (item.type == RenderItemType.ENTITY) {

                GameObject entity = item.entity;

                if (entity.getRenderType() == GameObject.RenderType.SPRITE) {
                    entity.drawSprite(worldBatch);
                } else {
                    worldBatch.end();
                    entity.drawShape(shapeRenderer);
                    worldBatch.begin();
                }

            } else {
                mazeRenderer.renderWallAtPosition(
                        worldBatch,
                        (int) item.x,
                        (int) item.y
                );
            }
        }


        worldBatch.end();
    }
    private enum RenderItemType {
        WALL_BEHIND,
        ENTITY,
        WALL_FRONT
    }
    private class RenderItem {
        float x, y;
        int priority; // ⭐ 新增
        RenderItemType type;
        GameObject entity;

        RenderItem(GameObject entity, int priority) {
            this.entity = entity;
            this.x = entity.getX();
            this.y = entity.getY();
            this.priority = priority;
            this.type = RenderItemType.ENTITY;
        }

        RenderItem(float x, float y, RenderItemType type) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.priority = 0;
        }
    }
    private java.util.List<RenderItem> collectAllRenderItems() {
        java.util.List<RenderItem> items = new java.util.ArrayList<>();

        addAllWalls(items);
        addAllEntities(items);

        return items;
    }
    private void addAllWalls(java.util.List<RenderItem> items) {
        int[][] maze = gameManager.getMazeForRendering();

        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                if (maze[y][x] == 0) {
                    boolean isFront = isWallInFrontOfAnyEntity(x, y);
                    items.add(new RenderItem(
                            x,
                            y,
                            isFront ? RenderItemType.WALL_FRONT : RenderItemType.WALL_BEHIND
                    ));
                }
            }
        }
    }
    private boolean isWallInFrontOfAnyEntity(int wallX, int wallY) {
        var player = gameManager.getPlayer();
        if (wallY > player.getY()) return true;

        var key = gameManager.getKey();
        if (key != null && key.isActive() && wallY > key.getY()) return true;

        for (var door : gameManager.getExitDoors()) {
            if (door != null && wallY > door.getY()) return true;
        }

        // ⭐⭐⭐ 加这一段
        for (Enemy enemy : gameManager.getEnemies()) {
            if (enemy != null && enemy.isActive() && wallY > enemy.getY()) {
                return true;
            }
        }

        return false;
    }
    private void addAllEntities(List<RenderItem> items) {

        // Player
        items.add(new RenderItem(gameManager.getPlayer(),100));

        // Traps
        for (Trap trap : gameManager.getTraps()) {
            if (trap != null && trap.isActive()) {
                items.add(new RenderItem(trap,10));
            }
        }

        // ⭐⭐⭐ Enemies（你之前缺的就是这一段）
        for (Enemy enemy : gameManager.getEnemies()) {
            if (enemy != null && enemy.isActive()) {
                items.add(new RenderItem(enemy,50));
            }
        }

        // ⭐⭐⭐ Enemy Bullets
        for (EnemyBullet bullet : gameManager.getBullets()) {
            if (bullet != null && bullet.isActive()) {
                items.add(new RenderItem(bullet,100));
            }
        }

        // Key
        Key key = gameManager.getKey();
        if (key != null && key.isActive()) {
            items.add(new RenderItem(key,20));
        }

        // Exit Doors
        for (ExitDoor door : gameManager.getExitDoors()) {
            if (door != null) {
                items.add(new RenderItem(door,0));
            }
        }
    }






    private void renderUI() {
        uiBatch.begin();

        if (gameManager.isGameComplete()) {
            hud.renderGameComplete(uiBatch);
        } else {
            hud.renderInGameUI(uiBatch);
        }

        uiBatch.end();

    }
    private void restartGame() {
        // 重新创建游戏状态
        gameManager = new GameManager();
        mazeRenderer.setGameManager(gameManager);
        cameraManager.centerOnPlayerImmediately(gameManager.getPlayer());
    }


}
