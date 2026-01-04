package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
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
    private boolean movedUp, movedDown, movedLeft, movedRight;
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

        // Detect movement input
        if (upPressed) {
            if (!movedUp) movedUp = true;
            upTimer += delta;
        }
        if (downPressed) {
            if (!movedDown) movedDown = true;
            downTimer += delta;
        }
        if (leftPressed) {
            if (!movedLeft) movedLeft = true;
            leftTimer += delta;
        }
        if (rightPressed) {
            if (!movedRight) movedRight = true;
            rightTimer += delta;
        }

        // Move player
        float moveSpeed = 3f * delta;
        float newPlayerX = playerX;
        float newPlayerY = playerY;

        if (upPressed) newPlayerY += moveSpeed;
        if (downPressed) newPlayerY -= moveSpeed;
        if (rightPressed) newPlayerX += moveSpeed;
        if (leftPressed) newPlayerX -= moveSpeed;

        // Collision detection
        int cellX = (int) newPlayerX;
        int cellY = (int) newPlayerY;

        if (cellX >= 0 && cellX < MAZE_WIDTH && cellY >= 0 && cellY < MAZE_HEIGHT) {
            if (fixedMaze[cellY][cellX] == 0) {
                playerX = newPlayerX;
                playerY = newPlayerY;
            }
        }

        // Check if reached target
        float distance = (float) Math.sqrt(
                Math.pow(playerX - targetX, 2) + Math.pow(playerY - targetY, 2)
        );

        if (distance < 1.0f && !reachedTarget) {
            reachedTarget = true;
            System.out.println("Reached target!");
        }

        // Check tutorial completion conditions
        boolean allMovesMade = movedUp && movedDown && movedLeft && movedRight;
        if (allMovesMade && reachedTarget && !finished) {
            System.out.println("Tutorial conditions met!");
            System.out.println("Movement: U=" + movedUp + " D=" + movedDown + " L=" + movedLeft + " R=" + movedRight);
            System.out.println("Target reached: " + reachedTarget);
            finishTutorial(MazeGameTutorialResult.SUCCESS);
        }

        // Simple death detection
        if (playerX < 0 || playerX >= MAZE_WIDTH || playerY < 0 || playerY >= MAZE_HEIGHT) {
            finishTutorial(MazeGameTutorialResult.FAILURE_DEAD);
        }
    }

    private void renderGameWorld() {
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Draw floor
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 1);
        for (int y = 0; y < MAZE_HEIGHT; y++) {
            for (int x = 0; x < MAZE_WIDTH; x++) {
                if (fixedMaze[y][x] == 0) {
                    shapeRenderer.rect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        // Draw walls
        shapeRenderer.setColor(0.4f, 0.4f, 0.6f, 1);
        for (int y = 0; y < MAZE_HEIGHT; y++) {
            for (int x = 0; x < MAZE_WIDTH; x++) {
                if (fixedMaze[y][x] == 1) {
                    shapeRenderer.rect(x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        // Draw target
        shapeRenderer.setColor(0, 1, 0, 0.8f);
        shapeRenderer.rect(targetX * CELL_SIZE, targetY * CELL_SIZE, CELL_SIZE, CELL_SIZE);

        // Draw player
        shapeRenderer.setColor(0, 0.8f, 1, 1);
        shapeRenderer.circle(playerX * CELL_SIZE, playerY * CELL_SIZE, CELL_SIZE / 3);

        shapeRenderer.end();
    }

    private void renderHUD() {
        shapeRenderer.setProjectionMatrix(hudCamera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw tutorial status indicators
        shapeRenderer.setColor(movedUp ? 0 : 0.5f, movedUp ? 1 : 0.5f, movedUp ? 0 : 0.5f, 0.8f);
        shapeRenderer.rect(20, Gdx.graphics.getHeight() - 40, 20, 20);

        shapeRenderer.setColor(movedDown ? 0 : 0.5f, movedDown ? 1 : 0.5f, movedDown ? 0 : 0.5f, 0.8f);
        shapeRenderer.rect(20, Gdx.graphics.getHeight() - 70, 20, 20);

        shapeRenderer.setColor(movedLeft ? 0 : 0.5f, movedLeft ? 1 : 0.5f, movedLeft ? 0 : 0.5f, 0.8f);
        shapeRenderer.rect(20, Gdx.graphics.getHeight() - 100, 20, 20);

        shapeRenderer.setColor(movedRight ? 0 : 0.5f, movedRight ? 1 : 0.5f, movedRight ? 0 : 0.5f, 0.8f);
        shapeRenderer.rect(20, Gdx.graphics.getHeight() - 130, 20, 20);

        shapeRenderer.setColor(reachedTarget ? 0 : 0.5f, reachedTarget ? 1 : 0.5f, reachedTarget ? 0 : 0.5f, 0.8f);
        shapeRenderer.rect(200, Gdx.graphics.getHeight() - 70, 20, 20);

        shapeRenderer.end();

        // Draw text
        game.getSpriteBatch().begin();
        game.getSkin().getFont("default-font").draw(game.getSpriteBatch(), "↑ Move Up", 50, Gdx.graphics.getHeight() - 25);
        game.getSkin().getFont("default-font").draw(game.getSpriteBatch(), "↓ Move Down", 50, Gdx.graphics.getHeight() - 55);
        game.getSkin().getFont("default-font").draw(game.getSpriteBatch(), "← Move Left", 50, Gdx.graphics.getHeight() - 85);
        game.getSkin().getFont("default-font").draw(game.getSpriteBatch(), "→ Move Right", 50, Gdx.graphics.getHeight() - 115);
        game.getSkin().getFont("default-font").draw(game.getSpriteBatch(), "🎯 Reach Target", 230, Gdx.graphics.getHeight() - 55);

        if (movedUp && movedDown && movedLeft && movedRight && reachedTarget) {
            game.getSkin().getFont("default-font").draw(game.getSpriteBatch(), "Tutorial Complete!",
                    Gdx.graphics.getWidth() / 2 - 50, 100);
        }

        game.getSpriteBatch().end();
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