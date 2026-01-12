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
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.screen.MenuScreen;
import de.tum.cit.fop.maze.utils.BossCamera;
import de.tum.cit.fop.maze.utils.CameraManager;

public class BossFightScreen implements Screen,PlayerInputHandler.InputHandlerCallback  {

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
    private PlayerInputHandler inputHandler;

    private final MazeRunnerGame game;

    private SpriteBatch batch;

    // ===== 占位资源 =====
    private Texture bg;
    private Texture playerTex;
    private Texture bossTex;

    // ===== 简单状态 =====
    private float playerX = 200;
    private float playerY = 120;

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
        playerTex = new Texture(Gdx.files.internal("debug/player.jpg"));
        bossTex = new Texture(Gdx.files.internal("debug/boss.jpg"));
        inputHandler = new PlayerInputHandler();

        // =========================
        // 1️⃣ DifficultyConfig（Boss 专用）
        // =========================
        difficultyConfig = new DifficultyConfig();
        difficultyConfig.mazeWidth = 15;
        difficultyConfig.mazeHeight = 9;
        difficultyConfig.seed = System.currentTimeMillis();

        // =========================
        // 2️⃣ GameManager & Maze
        // =========================
        gameManager = new GameManager(difficultyConfig);
        gameManager.initializeWorld();

        player = gameManager.getPlayer(); // 或 getPlayers().get(0)

        // =========================
        // 3️⃣ CameraManager（下半屏）
        // =========================
        mazeCameraManager = new CameraManager(difficultyConfig);

        mazeViewport = new FitViewport(
                1280,
                360,
                mazeCameraManager.getCamera()
        );

        mazeCameraManager.centerOnPlayerImmediately(player);

        // =========================
        // 4️⃣ MazeRenderer（⚠️ 就是你这份类）
        // =========================
        mazeRenderer = new MazeRenderer(gameManager, difficultyConfig);

        // =========================
        // 5️⃣ Boss Camera（上半屏）
        // =========================
        bossCamera = new BossCamera(1280, 360);
        bossViewport = new FitViewport(
                1280,
                360,
                bossCamera.getCamera()
        );
    }

    @Override
    public void render(float delta) {

        handleInput(delta);
        update(delta);
        inputHandler.update(
                delta,
                this, // Callback 就是 BossFightScreen 自己
                Player.PlayerIndex.P1
        );

        player.update(delta);


        // ===== 清屏 =====
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // =====================================
        // 上半屏：Boss 演出层
        // =====================================
        bossViewport.setScreenBounds(
                0,
                Gdx.graphics.getHeight() / 2,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight() / 2
        );
        bossViewport.apply();

        batch.setProjectionMatrix(bossCamera.getCamera().combined);
        batch.begin();
        batch.draw(bg, 0, 0, 1280, 360);
        batch.draw(bossTex, bossX, bossY);
        batch.end();

        // =====================================
        // 下半屏：Maze 层（暂时占位）
        // =====================================
        mazeViewport.setScreenBounds(
                0,
                0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight() / 2
        );
        mazeViewport.apply();

        // 👉 这里以后会用 GameManager
        // mazeCameraManager.update(delta, gameManager);
        gameManager.update(delta);
        mazeCameraManager.update(delta, gameManager);
        batch.setProjectionMatrix(
                mazeCameraManager.getCamera().combined
        );
        // —— 地板
        mazeRenderer.renderFloor(batch);

// —— 墙
        for (MazeRenderer.WallGroup g : mazeRenderer.getWallGroups()) {
            mazeRenderer.renderWallGroup(batch, g);
        }

// —— 实体（如果你现在有）
// gameManager.getPlayer().render(batch);
// enemy.render(batch);
// exitDoor.render(batch);

        batch.end();
    }

    private void handleInput(float delta) {

        // ===== ESC：立即回菜单（调试期非常重要）=====
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
            return;
        }

        float speed = 300f * delta;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) playerX -= speed;
        if (Gdx.input.isKeyPressed(Input.Keys.D)) playerX += speed;
        if (Gdx.input.isKeyPressed(Input.Keys.W)) playerY += speed;
        if (Gdx.input.isKeyPressed(Input.Keys.S)) playerY -= speed;

        // ===== 临时攻击测试 =====
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            System.out.println("Player attack!");
        }
    }

    private void update(float delta) {
        // 以后放：
        // - Boss 时间轴推进
        // - 碰撞
        // - 受伤闪烁 / 无敌帧
    }

    @Override
    public void resize(int width, int height) {
        bossViewport.update(width, height / 2);
        mazeViewport.update(width, height / 2);
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        bg.dispose();
        playerTex.dispose();
        bossTex.dispose();
    }

    @Override
    public void onMoveInput(Player.PlayerIndex index, int dx, int dy) {

        // Boss 房：连续移动，不走格子
        float speed = 6f; // 每秒速度，随便调

        float newX = player.getWorldX() + dx * speed;
        float newY = player.getWorldY() + dy * speed;

        player.setWorldPosition(newX, newY);
        player.setMovingAnim(true);
        player.updateDirection(dx, dy);
    }

    @Override
    public float getMoveDelayMultiplier() {
        return player.getMoveDelayMultiplier();
    }


    @Override
    public boolean onAbilityInput(Player.PlayerIndex index, int slot) {

        // slot 0 = 攻击 / 主技能
        // slot 1 = Dash
        if (slot == 0) {
            player.startAttack();
            return true;
        }

        if (slot == 1) {
            player.useAbility(1); // Dash
            return true;
        }

        return false;
    }


    @Override
    public void onInteractInput(Player.PlayerIndex index) {

    }

    @Override
    public void onMenuInput() {
        game.setScreen(new MenuScreen(game));
    }
}
