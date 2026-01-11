package de.tum.cit.fop.maze.entities.boss;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.screen.MenuScreen;

public class BossFightScreen implements Screen,PlayerInputHandler.InputHandlerCallback  {
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

        player = new Player(
                0, 0,
                /* context 先不管 */,
                Player.PlayerIndex.P1
        );

        player.setWorldPosition(6f, 3f);

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

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.05f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(bg, 0, 0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());

        batch.draw(playerTex, playerX, playerY);
        batch.draw(bossTex, bossX, bossY);

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

    @Override public void resize(int w, int h) {}
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
