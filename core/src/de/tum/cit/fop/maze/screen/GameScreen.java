package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.effects.boba.BobaBulletManager;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.entities.EnemyBoba.BobaBullet;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.ui.HUD;
import de.tum.cit.fop.maze.utils.CameraManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

import de.tum.cit.fop.maze.effects.key.KeyEffectManager;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.effects.portal.PortalEffectManager;

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

    private boolean isPlayerMoving = false;
//===新增特效===
    private BobaBulletManager bobaBulletManager;
    private KeyEffectManager keyEffectManager;
    private PortalEffectManager portalEffectManager;



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

        // 1️⃣ 先记录旧状态 (必须在 update 之前)
        boolean playerHadKey = gameManager.getPlayer().hasKey();

        // 2️⃣ 更新游戏逻辑 (只调用一次！)
        gameManager.update(delta);
        cameraManager.update(delta, gameManager.getPlayer());

        // === 子弹逻辑 ===
        for (EnemyBullet bullet : gameManager.getBullets()) {
            if (bullet instanceof BobaBullet) {
                var bobaBullet = (BobaBullet) bullet;
                if (!bobaBullet.isManagedByEffectManager()) {
                    bobaBulletManager.addBullet(bobaBullet);
                }
            }
        }

        if (bobaBulletManager != null) {
            bobaBulletManager.update(delta);
        }

        // 3️⃣ 检查钥匙收集 (从无 -> 有)
        if (!playerHadKey && gameManager.getPlayer().hasKey()) {
            var key = gameManager.getKey();

            float pixelX = key.getX() * GameConstants.CELL_SIZE + 4;
            float pixelY = key.getY() * GameConstants.CELL_SIZE + 4;

            Texture keyTexture = TextureManager.getInstance().getKeyTexture();

            // 调试日志
            System.out.println("✨ 触发钥匙特效！坐标: " + pixelX + "," + pixelY);

            keyEffectManager.spawnKeyEffect(pixelX, pixelY, keyTexture);
            AudioManager.getInstance().play(AudioType.PLAYER_GET_KEY);
        }

        // 4️⃣ 更新特效
        if (keyEffectManager != null) {
            keyEffectManager.update(delta);
        }

        // ============ [新增] 传送门特效逻辑开始 ============
        // 检测是否触发了退出流程 (GameManager 里的 isExitingLevel 为 true)
        if (gameManager.isExitingLevel() && !portalEffectManager.isActive()) {
            // 获取玩家中心点坐标 (格坐标 -> 像素坐标 + 半个格子偏移)
            float px = (gameManager.getPlayer().getX() + 0.5f) * GameConstants.CELL_SIZE;
            float py = (gameManager.getPlayer().getY() + 0.5f) * GameConstants.CELL_SIZE;

            // 启动龙卷风特效
            portalEffectManager.startExitAnimation(px, py);

            // 播放音效
           // AudioManager.getInstance().play(AudioType.ENTER_NEXT_LEVEL);
        }

        // 更新特效状态 (呼吸灯、粒子运动等)
        portalEffectManager.update(delta);

        // 检查动画是否播放完毕，如果完毕则通知 GameManager 正式切关
        if (portalEffectManager.isFinished()) {
            gameManager.completeLevelTransition();
            // 重置相机位置到新关卡的玩家位置
            cameraManager.centerOnPlayerImmediately(gameManager.getPlayer());
        }
        // ============ [新增] 传送门特效逻辑结束 ============

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

        // 初始化特效管理器
        bobaBulletManager = new BobaBulletManager();
        bobaBulletManager.setRenderMode(BobaBulletManager.RenderMode.MANAGED); // 让管理器全权负责子弹渲染
        keyEffectManager = new KeyEffectManager();
        portalEffectManager = new PortalEffectManager();

        cameraManager.centerOnPlayerImmediately(gameManager.getPlayer());

        Gdx.input.setInputProcessor(null); // 不用 Scene2D

        // 调试日志：确认这一行确实执行了
        System.out.println("🔥🔥🔥 GameScreen SHOW executed, Manager created!");
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

        //===新增===
        if (bobaBulletManager != null) {
            bobaBulletManager.dispose();
        }

        if (keyEffectManager != null) {
            keyEffectManager.dispose();
        }

        if (portalEffectManager != null) {
            portalEffectManager.dispose();
        }

        Logger.debug("GameScreen disposed");
    }



    // Additional methods and logic can be added as needed for the game screen
    private void handleInput(float delta) {
        boolean isTryingToMove =
                Gdx.input.isKeyPressed(Input.Keys.UP) ||
                        Gdx.input.isKeyPressed(Input.Keys.DOWN) ||
                        Gdx.input.isKeyPressed(Input.Keys.LEFT) ||
                        Gdx.input.isKeyPressed(Input.Keys.RIGHT);

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
            }
        });
        if (isTryingToMove) {
            if (!isPlayerMoving) {
                AudioManager.getInstance().playPlayerMove();
                isPlayerMoving = true;
            }
        } else {
            if (isPlayerMoving) {
                AudioManager.getInstance().stopPlayerMove();
                isPlayerMoving = false;
            }
        }

    }

    private void renderWorld() {
        worldBatch.setProjectionMatrix(cameraManager.getCamera().combined);
        shapeRenderer.setProjectionMatrix(cameraManager.getCamera().combined);

        worldBatch.begin();

        // 1. 地板
        mazeRenderer.renderFloor(worldBatch);

        // ============ [新增 1] 渲染门后的蓝色呼吸光晕 ============
        // 遍历所有出口，为解锁的门绘制背景光
        for (ExitDoor door : gameManager.getExitDoors()) {
            if (!door.isLocked()) {
                // 计算中心点像素坐标 (格坐标 + 0.5f 偏移量) * 格子大小
                float dx = (door.getX() + 0.5f) * GameConstants.CELL_SIZE;
                float dy = (door.getY() + 0.5f) * GameConstants.CELL_SIZE;

                // 确保 portalEffectManager 已初始化
                if (portalEffectManager != null) {
                    portalEffectManager.renderBack(worldBatch, dx, dy);
                }
            }
        }
        // ====================================================

        // 2. 收集并排序渲染对象
        var renderItems = collectAllRenderItems();
        renderItems.sort((a, b) -> {
            // 1️⃣ 先按 y（视觉深度）从上到下渲染
            int yCompare = Float.compare(b.y, a.y);
            if (yCompare != 0) return yCompare;

            // 2️⃣ y 相同 → 按 priority 排序
            return Integer.compare(a.priority, b.priority);
        });


        // 3. 渲染实体和墙壁
        for (var item : renderItems) {
            if (item.type == RenderItemType.ENTITY) {

                // ============ [新增 2] 玩家消失逻辑 ============
                // 如果特效管理器说“该隐藏玩家了”，就跳过玩家的绘制
                if (portalEffectManager != null &&
                        item.entity == gameManager.getPlayer() &&
                        portalEffectManager.shouldHidePlayer()) {
                    continue;
                }
                // ============================================

                GameObject entity = item.entity;

                if (entity.getRenderType() == GameObject.RenderType.SPRITE) {
                    entity.drawSprite(worldBatch);
                } else {
                    worldBatch.end();
                    entity.drawShape(shapeRenderer);
                    worldBatch.begin();
                }

            } else {
                // 渲染墙壁
                mazeRenderer.renderWallAtPosition(
                        worldBatch,
                        (int) item.x,
                        (int) item.y
                );
            }
        }

        // 子弹特效
        if (bobaBulletManager != null) {
            bobaBulletManager.render(worldBatch);
        }

        // 钥匙特效
        if (keyEffectManager != null) {
            keyEffectManager.render(worldBatch);
        }

        // ============ [新增 3] 渲染前景：蓝色光条龙卷风 ============
        // 这一步要在所有物体之后画，保证粒子在最上层
        if (portalEffectManager != null) {
            portalEffectManager.renderFront(worldBatch);
        }
        // ====================================================

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
