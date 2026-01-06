package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;

public class MazeGameTutorialScreen implements Screen {

    public enum MazeGameTutorialResult {
        SUCCESS,
        FAILURE_DEAD,
        EXIT_BY_PLAYER
    }

    private final MazeRunnerGame game;
    private final DifficultyConfig config;
    private GameManager gm;

    private OrthographicCamera camera;
    private OrthographicCamera hudCamera;
    private ShapeRenderer shapeRenderer;

    private boolean finished = false;
    private boolean movedUp, movedDown, movedLeft, movedRight, usedShift; // 增加 usedShift
    private float shiftTimer; // 用于检测是否按够了时长
    private boolean reachedTarget = false;

    // Fixed maze dimensions
    private static final int MAZE_WIDTH = 30;
    private static final int MAZE_HEIGHT = 20;
    private static final float CELL_SIZE = 32f;

    // Fixed simple maze (0=path, 1=wall)
    private final int[][] fixedMaze = {
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,0,1,1,0,1},
            {1,0,1,0,0,0,0,0,1,0,1,0,0,0,0,0,1,0,1,0,0,0,0,0,1,0,1,0,0,1},
            {1,0,1,0,1,1,1,0,1,0,1,0,1,1,1,0,1,0,1,0,1,1,1,0,1,0,1,0,1,1},
            {1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,1},
            {1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,1},
            {1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,1},
            {1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,0,1,1},
            {1,0,1,0,1,0,1,0,0,0,1,0,1,0,1,0,0,0,1,0,1,0,1,0,0,0,1,0,0,1},
            {1,0,1,0,1,0,1,1,1,1,1,0,1,0,1,1,1,1,1,0,1,0,1,1,1,1,1,1,0,1},
            {1,0,1,0,1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,1,0,0,0,0,0,0,0,0,1},
            {1,0,1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1},
            {1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0,1},
            {1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1},
            {1,0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1},
            {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1}
    };

    // Player start position and target position
    private float playerX = 1.5f;
    private float playerY = 1.5f;
    private final int targetX = 25;
    private final int targetY = 10;

    // Input handling
    private boolean upPressed, downPressed, leftPressed, rightPressed;
    private float upTimer, downTimer, leftTimer, rightTimer;

    public MazeGameTutorialScreen(MazeRunnerGame game, DifficultyConfig config) {
        this.game = game;
        this.config = config;
    }

    @Override
    public void show() {
        System.out.println("=== TUTORIAL START ===");

        // Initialize GameManager
        gm = game.getGameManager();
        if (gm == null) {
            System.err.println("Warning: GameManager is null, creating new one");
            gm = new GameManager(config);
        }
        gm.setTutorialMode(true);

        // Create cameras
        camera = new OrthographicCamera();
        hudCamera = new OrthographicCamera();
        shapeRenderer = new ShapeRenderer();

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        centerCameraOnPlayer();

        System.out.println("Tutorial Objective: Use WASD or Arrow Keys to move, reach the green target");
        System.out.println("Player Start: (" + playerX + ", " + playerY + ")");
        System.out.println("Target Position: (" + targetX + ", " + targetY + ")");
        System.out.println("Game Stage: STORY_MAZE_GAME_TUTORIAL");
    }

    private void centerCameraOnPlayer() {
        float worldWidth = MAZE_WIDTH * CELL_SIZE;
        float worldHeight = MAZE_HEIGHT * CELL_SIZE;
        float cameraWidth = camera.viewportWidth;
        float cameraHeight = camera.viewportHeight;

        float targetCamX = Math.max(cameraWidth / 2,
                Math.min(worldWidth - cameraWidth / 2, playerX * CELL_SIZE));
        float targetCamY = Math.max(cameraHeight / 2,
                Math.min(worldHeight - cameraHeight / 2, playerY * CELL_SIZE));

        camera.position.set(targetCamX, targetCamY, 0);
        camera.update();
    }

    @Override
    public void render(float delta) {
        // Handle input
        handleInput();

        // Update game logic
        update(delta);

        // Update camera
        centerCameraOnPlayer();

        // Render
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);
        renderGameWorld();
        renderHUD();

        // Check ESC to exit
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            finishTutorial(MazeGameTutorialResult.EXIT_BY_PLAYER);
        }
    }

    private void handleInput() {
        upPressed = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        downPressed = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        leftPressed = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        rightPressed = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
    }

    private void update(float delta) {
        if (finished) return;

        // 1. 记录移动状态（修复指示灯不亮的问题）
        if (upPressed) movedUp = true;
        if (downPressed) movedDown = true;
        if (leftPressed) movedLeft = true;
        if (rightPressed) movedRight = true;

        // 2. 检测 Shift 冲刺
        boolean isSprinting = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        // 只有在移动时按住 Shift 才算完成任务
        if (isSprinting && (upPressed || downPressed || leftPressed || rightPressed)) {
            usedShift = true;
        }

        // 3. 计算位移
        float moveSpeed = (isSprinting ? 6.0f : 3.0f) * delta;
        float nextX = playerX;
        float nextY = playerY;

        if (upPressed) nextY += moveSpeed;
        if (downPressed) nextY -= moveSpeed;
        if (rightPressed) nextX += moveSpeed;
        if (leftPressed) nextX -= moveSpeed;

        // 4. 碰撞检测
        if (nextX >= 0 && nextX < MAZE_WIDTH && nextY >= 0 && nextY < MAZE_HEIGHT) {
            // 注意：这里转换为 int 检查迷宫数组
            if (fixedMaze[(int)nextY][(int)nextX] == 0) {
                playerX = nextX;
                playerY = nextY;
            }
        }

        // 5. 到达目标逻辑（距离判定）
        float distToTarget = (float) Math.sqrt(Math.pow(playerX - targetX, 2) + Math.pow(playerY - targetY, 2));
        if (distToTarget < 0.8f) { // 稍微给一点余量，不需要完全重合
            reachedTarget = true;
        }

        // 6. 核心：条件达成逻辑
        // 必须：上下左右都动过 + 用过冲刺 + 站到了目标点
        boolean allTasksDone = movedUp && movedDown && movedLeft && movedRight && usedShift && reachedTarget;

        if (allTasksDone) {
            finishTutorial(MazeGameTutorialResult.SUCCESS);
        }

        // 7. 边界死亡判定（可选）
        if (playerX < 0 || playerX >= MAZE_WIDTH || playerY < 0 || playerY >= MAZE_HEIGHT) {
            finishTutorial(MazeGameTutorialResult.FAILURE_DEAD);
        }
    }

    private void renderGameWorld() {
        shapeRenderer.setProjectionMatrix(camera.combined);

        // 计算居中偏移量
        float mazePixelWidth = MAZE_WIDTH * CELL_SIZE;
        float mazePixelHeight = MAZE_HEIGHT * CELL_SIZE;
        float offsetX = (Gdx.graphics.getWidth() - mazePixelWidth) / 2f;
        float offsetY = (Gdx.graphics.getHeight() - mazePixelHeight) / 2f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // --- 1. 绘制地板 (浅粉色) ---
        shapeRenderer.setColor(1.0f, 0.85f, 0.9f, 1);
        for (int y = 0; y < MAZE_HEIGHT; y++) {
            for (int x = 0; x < MAZE_WIDTH; x++) {
                if (fixedMaze[y][x] == 0) {
                    shapeRenderer.rect(offsetX + x * CELL_SIZE, offsetY + y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        // --- 2. 绘制墙壁 (深粉色/草莓粉) ---
        shapeRenderer.setColor(0.9f, 0.4f, 0.6f, 1);
        for (int y = 0; y < MAZE_HEIGHT; y++) {
            for (int x = 0; x < MAZE_WIDTH; x++) {
                if (fixedMaze[y][x] == 1) {
                    // 绘制墙壁主体
                    shapeRenderer.rect(offsetX + x * CELL_SIZE, offsetY + y * CELL_SIZE, CELL_SIZE, CELL_SIZE);

                    // 可选：给墙壁加一个白色的“糖霜”边缘，增加体积感
                    shapeRenderer.setColor(1f, 1f, 1f, 0.3f);
                    shapeRenderer.rect(offsetX + x * CELL_SIZE, offsetY + y * CELL_SIZE + CELL_SIZE - 4, CELL_SIZE, 4);
                    shapeRenderer.setColor(0.9f, 0.4f, 0.6f, 1); // 还原颜色继续画
                }
            }
        }

        // --- 3. 绘制目标点 (金色糖果色) ---
        shapeRenderer.setColor(1.0f, 0.85f, 0.2f, 0.8f);
        shapeRenderer.rect(offsetX + targetX * CELL_SIZE, offsetY + targetY * CELL_SIZE, CELL_SIZE, CELL_SIZE);

        // --- 4. 绘制玩家 (白色小球) ---
        shapeRenderer.setColor(1f, 1f, 1f, 1);
        shapeRenderer.circle(offsetX + playerX * CELL_SIZE, offsetY + playerY * CELL_SIZE, CELL_SIZE / 2.5f);

        shapeRenderer.end();
    }

    private void renderHUD() {
        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        float startX = Gdx.graphics.getWidth() / 2f - 120; // 居中对齐起点
        float startY = Gdx.graphics.getHeight() - 60;
        float spacing = 35f; // 行距

        // 绘制指示方块
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawStatusBox(startX, startY, movedUp);
        drawStatusBox(startX, startY - spacing, movedDown);
        drawStatusBox(startX, startY - spacing * 2, movedLeft);
        drawStatusBox(startX, startY - spacing * 3, movedRight);
        drawStatusBox(startX, startY - spacing * 4, usedShift); // 新增 Shift 指示

        // 目标指示灯放在右边一点
        drawStatusBox(startX + 240, startY - spacing * 2, reachedTarget);
        shapeRenderer.end();

        // 绘制对齐的文字
        game.getSpriteBatch().begin();
        var font = game.getSkin().getFont("default-font");
        float textX = startX + 30; // 文字跟在方块后面
        float textYOffset = 18;     // 修正文字垂直对齐偏移

        font.draw(game.getSpriteBatch(), "W / UP - Move Up", textX, startY + textYOffset);
        font.draw(game.getSpriteBatch(), "S / DOWN - Move Down", textX, startY - spacing + textYOffset);
        font.draw(game.getSpriteBatch(), "A / LEFT - Move Left", textX, startY - spacing * 2 + textYOffset);
        font.draw(game.getSpriteBatch(), "D / RIGHT - Move Right", textX, startY - spacing * 3 + textYOffset);
        font.draw(game.getSpriteBatch(), "SHIFT - Sprint", textX, startY - spacing * 4 + textYOffset);

        font.draw(game.getSpriteBatch(), "🎯 Reach Exit", startX + 275, startY - spacing * 2 + textYOffset);
        game.getSpriteBatch().end();
    }

    private void drawStatusBox(float x, float y, boolean done) {
        shapeRenderer.setColor(done ? Color.GREEN : Color.RED); // 完成变绿，未完成变红
        shapeRenderer.rect(x, y, 20, 20);
    }

    // 辅助方法：绘制对齐的灯
    private void drawIndicator(float x, float y, boolean active) {
        shapeRenderer.setColor(active ? Color.GREEN : Color.GRAY);
        shapeRenderer.rect(x, y, 18, 18);
    }

    private void finishTutorial(MazeGameTutorialResult result) {
        if (finished) return;

        finished = true;

        System.out.println("=== TUTORIAL END ===");
        System.out.println("Result: " + result);
        System.out.println("Calling game.onTutorialFinished/onTutorialFailed");

        // Delay execution by one frame to avoid rendering issues
        Gdx.app.postRunnable(() -> {
            try {
                if (result == MazeGameTutorialResult.SUCCESS) {
                    System.out.println("Calling game.onTutorialFinished()");
                    game.onTutorialFinished(this);
                } else {
                    System.out.println("Calling game.onTutorialFailed()");
                    game.onTutorialFailed(this, result);
                }
            } catch (Exception e) {
                System.err.println("Tutorial callback error: " + e.getMessage());
                e.printStackTrace();
                // Return to main menu on error
                game.goToMenu();
            }
        });
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();

        hudCamera.setToOrtho(false, width, height);
        hudCamera.update();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        System.out.println("Tutorial screen hidden");
        if (gm != null) {
            gm.setTutorialMode(false);
        }
    }

    @Override
    public void dispose() {
        System.out.println("Tutorial screen resources disposed");
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}