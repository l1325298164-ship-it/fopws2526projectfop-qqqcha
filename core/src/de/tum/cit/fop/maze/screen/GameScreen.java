package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.entities.*;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.ui.HUD;
import de.tum.cit.fop.maze.utils.CameraManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.*;

public class GameScreen implements Screen {

    private final MazeRunnerGame game;
    private final OrthographicCamera camera;
    private final BitmapFont font;

    private GameManager gameManager;
    private MazeRenderer mazeRenderer;
    private CameraManager cameraManager;
    private PlayerInputHandler inputHandler;
    private HUD hud;

    private SpriteBatch worldBatch;
    private SpriteBatch uiBatch;
    private ShapeRenderer shapeRenderer;

    private boolean isPlayerMoving = false;

    // 渲染对象接口
    private interface Renderable {
        float getY();
        int getRenderOrder(); // 0: 后墙, 1: 实体, 2: 前墙
        void render(SpriteBatch batch, ShapeRenderer shapeRenderer);
    }

    // 墙壁渲染对象
    private class WallRenderable implements Renderable {
        private final MazeRenderer.WallGroup wallGroup;
        private final boolean isFront;

        WallRenderable(MazeRenderer.WallGroup wallGroup, boolean isFront) {
            this.wallGroup = wallGroup;
            this.isFront = isFront;
        }

        @Override
        public float getY() {
            return wallGroup.startY;
        }

        @Override
        public int getRenderOrder() {
            return isFront ? 2 : 0; // 前墙=2, 后墙=0
        }

        @Override
        public void render(SpriteBatch batch, ShapeRenderer shapeRenderer) {
            float cellSize = mazeRenderer.getCellSize();
            float wallHeight = cellSize * mazeRenderer.getWallHeightMultiplier();
            int wallOverlap = mazeRenderer.getWallOverlap();

            TextureRegion region = mazeRenderer.getWallRegion(wallGroup.textureIndex);
            if (region != null) {
                float totalWidth = wallGroup.length * cellSize;
                float startXPos = wallGroup.startX * cellSize;
                float startYPos = wallGroup.startY * cellSize - wallOverlap;

                batch.draw(region, startXPos, startYPos, totalWidth, wallHeight);
            }
        }
    }

    // 实体渲染对象
    private class EntityRenderable implements Renderable {
        private final GameObject entity;
        private final int priority;

        EntityRenderable(GameObject entity, int priority) {
            this.entity = entity;
            this.priority = priority;
        }

        @Override
        public float getY() {
            return entity.getY();
        }

        @Override
        public int getRenderOrder() {
            return 1; // 实体=1
        }

        @Override
        public void render(SpriteBatch batch, ShapeRenderer shapeRenderer) {
            if (entity.getRenderType() == GameObject.RenderType.SPRITE) {
                entity.drawSprite(batch);
            } else {
                batch.end();
                entity.drawShape(shapeRenderer);
                batch.begin();
            }
        }
    }

    public GameScreen(MazeRunnerGame game) {
        this.game = game;

        camera = new OrthographicCamera();
        camera.setToOrtho(false);
        camera.zoom = 0.75f;

        font = new BitmapFont();
    }

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
        gameManager = game.getGameManager();
        mazeRenderer = new MazeRenderer(gameManager);
        cameraManager = new CameraManager();
        inputHandler = new PlayerInputHandler();
        hud = new HUD(gameManager);

        cameraManager.centerOnPlayerImmediately(gameManager.getPlayer());
        Gdx.input.setInputProcessor(null);
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

        worldBatch = null;

        if (uiBatch != null) {
            uiBatch.dispose();
            uiBatch = null;
        }

        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }

        if (font != null) font.dispose();

        Logger.debug("GameScreen disposed");
    }

    private void handleInput(float delta) {
        boolean isTryingToMove =
                Gdx.input.isKeyPressed(Input.Keys.UP) ||
                        Gdx.input.isKeyPressed(Input.Keys.DOWN) ||
                        Gdx.input.isKeyPressed(Input.Keys.LEFT) ||
                        Gdx.input.isKeyPressed(Input.Keys.RIGHT);

        // ESC 返回菜单
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.app.postRunnable(() -> game.goToMenu());
            return;
        }

        // R 重开
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            restartGame();
            return;
        }

        // F1-F4 切换纹理模式
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

        // 玩家移动
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

        // 2. 收集所有需要渲染的物体
        List<Renderable> allRenderables = collectAllRenderables();

        // 3. 按深度排序
        allRenderables.sort((a, b) -> {
            // 1️⃣ 先按 y 坐标（从高到低）
            int yCompare = Float.compare(b.getY(), a.getY());
            if (yCompare != 0) return yCompare;

            // 2️⃣ y 相同 → 按渲染类型（后墙->实体->前墙）
            return Integer.compare(a.getRenderOrder(), b.getRenderOrder());
        });

        // 4. 按顺序渲染所有物体
        for (Renderable renderable : allRenderables) {
            renderable.render(worldBatch, shapeRenderer);
        }

        worldBatch.end();
    }

    // 收集所有需要渲染的物体
    private List<Renderable> collectAllRenderables() {
        List<Renderable> renderables = new ArrayList<>();

        // 添加墙壁
        List<MazeRenderer.WallGroup> wallGroups = mazeRenderer.getWallGroups();
        float playerY = gameManager.getPlayer().getY();

        for (MazeRenderer.WallGroup group : wallGroups) {
            boolean isFront = mazeRenderer.isWallInFrontOfAnyEntity(group.startX, group.startY);
            renderables.add(new WallRenderable(group, isFront));
        }

        // 添加玩家
        renderables.add(new EntityRenderable(gameManager.getPlayer(), 100));

        // 添加陷阱
        for (Trap trap : gameManager.getTraps()) {
            if (trap != null && trap.isActive()) {
                renderables.add(new EntityRenderable(trap, 10));
            }
        }

        // 添加敌人
        for (Enemy enemy : gameManager.getEnemies()) {
            if (enemy != null && enemy.isActive()) {
                renderables.add(new EntityRenderable(enemy, 50));
            }
        }

        // 添加子弹
        for (EnemyBullet bullet : gameManager.getBullets()) {
            if (bullet != null && bullet.isActive()) {
                renderables.add(new EntityRenderable(bullet, 100));
            }
        }

        // 添加钥匙
        Key key = gameManager.getKey();
        if (key != null && key.isActive()) {
            renderables.add(new EntityRenderable(key, 20));
        }

        // 添加出口门
        for (ExitDoor door : gameManager.getExitDoors()) {
            if (door != null) {
                renderables.add(new EntityRenderable(door, 0));
            }
        }

        return renderables;
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
        Logger.debug("开始重新启动游戏...");

        // 重置现有的 GameManager
        gameManager.resetGame();

        // 重置 MazeRenderer
        mazeRenderer.setGameManager(gameManager);

        // 重置 HUD
        hud = new HUD(gameManager);

        // 重置输入处理器
        inputHandler = new PlayerInputHandler();

        // 重新居中相机
        cameraManager.centerOnPlayerImmediately(gameManager.getPlayer());

        // 停止并重新开始移动音效
        AudioManager.getInstance().stopPlayerMove();
        isPlayerMoving = false;

        Logger.debug("游戏重新启动完成");
    }
}