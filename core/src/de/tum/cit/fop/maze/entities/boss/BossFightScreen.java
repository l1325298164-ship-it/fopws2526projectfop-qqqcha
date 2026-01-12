package de.tum.cit.fop.maze.entities.boss;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.BossMazeRenderer;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.screen.MenuScreen;
import de.tum.cit.fop.maze.utils.BossCamera;
import de.tum.cit.fop.maze.utils.CameraManager;

public class BossFightScreen implements Screen {

    // ===== Cameras =====
    private BossCamera bossCamera;
    private CameraManager mazeCameraManager;

    // ===== Viewports =====
    private FitViewport bossViewport;
    private FitViewport mazeViewport;

    // ===== 下半屏迷宫 =====
    private GameManager gameManager;
    private DifficultyConfig difficultyConfig;
    private MazeRenderer mazeRenderer;


    private Player player;

    private final MazeRunnerGame game;

    private SpriteBatch batch;

    // ===== 占位资源 =====
    private Texture bg;
    private Texture bossTex;

    // ===== 简单状态 =====
    private float bossX = 800;
    private float bossY = 300;

    public BossFightScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        // 占位贴图，之后随时换
        bg = new Texture(Gdx.files.internal("debug/boss_bg.jpg"));
        bossTex = new Texture(Gdx.files.internal("debug/boss.jpg"));

        // ===== Boss 专用 DifficultyConfig =====
        difficultyConfig = new DifficultyConfig(
                Difficulty.BOSS,
                22, 20,   // ✅ 包含 border 的完整尺寸
                0,

                5, 3, 0, 0,   // 敌人
                0, 0, 0, 0,   // 陷阱

                1,
                1.0f,
                1.0f,
                0
        );


// 1️⃣ World
        gameManager = new GameManager(difficultyConfig, false);
        player = gameManager.getPlayer();

// 2️⃣ CameraManager
        mazeCameraManager = new CameraManager(difficultyConfig);
        mazeCameraManager.centerOnPlayerImmediately(player);

// 3️⃣ Viewport（不要手动改 Y）
        mazeViewport = new FitViewport(
                GameConstants.CAMERA_VIEW_WIDTH,
                GameConstants.CAMERA_VIEW_HEIGHT / 2f,
                mazeCameraManager.getCamera()
        );

// 4️⃣ Renderer
        mazeRenderer = new BossMazeRenderer(gameManager, difficultyConfig);

// 5️⃣ Boss Camera / Viewport
        bossCamera = new BossCamera(1280, 360);
        bossViewport = new FitViewport(1280, 360, bossCamera.getCamera());
    }

    @Override
    public void render(float delta) {

        // ===== 测试期：ESC 直接回主菜单 =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
            return;
        }
        update(delta);

        gameManager.update(delta);
        // ===== 清屏 =====
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // =====================================
        // 上半屏：Boss 演出层
        // =====================================

        bossViewport.apply();

        batch.setProjectionMatrix(bossCamera.getCamera().combined);
        batch.begin();
        batch.draw(bg, 0, 0, 1280, 360);
        batch.draw(bossTex, bossX, bossY);
        batch.end();

        // ================= 下半屏：Maze =================

        mazeViewport.apply();

// 更新世界 & 相机
        mazeCameraManager.update(delta, gameManager);

        batch.setProjectionMatrix(
                mazeCameraManager.getCamera().combined
        );
        batch.begin();

        mazeRenderer.renderFloor(batch);

        for (MazeRenderer.WallGroup g : mazeRenderer.getWallGroups()) {
            mazeRenderer.renderWallGroup(batch, g);
        }

// 玩家 / 敌人 / 陷阱
        gameManager.getPlayer().drawSprite(batch);


        for (Enemy e : gameManager.getEnemies()) {
            if (e.isActive()) {
                e.drawSprite(batch);
            }
        }


        batch.end();
    }



    private void update(float delta) {
        // 以后放：
        // - Boss 时间轴推进
        // - 碰撞
        // - 受伤闪烁 / 无敌帧
    }

    @Override
    public void resize(int width, int height) {
        // 上半屏
        bossViewport.update(width, height / 2, true);

        // 下半屏
        mazeViewport.update(width, height / 2, true);

        // 🔥 关键：把 mazeViewport 挪到屏幕底部
        mazeViewport.setScreenPosition(0, 0);
        bossViewport.setScreenPosition(0, height / 2);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        bg.dispose();
        bossTex.dispose();
        if (gameManager != null) {
            gameManager.dispose();
        }
    }


}
