package de.tum.cit.fop.maze.entities.boss;

package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.MazeRunnerGame;

public class BossFightScreen implements Screen {

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
        bg = new Texture(Gdx.files.internal("debug/boss_bg.png"));
        playerTex = new Texture(Gdx.files.internal("debug/player.png"));
        bossTex = new Texture(Gdx.files.internal("debug/boss.png"));
    }

    @Override
    public void render(float delta) {

        handleInput(delta);
        update(delta);

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
}
