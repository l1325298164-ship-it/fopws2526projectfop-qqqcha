package de.tum.cit.fop.maze.entities.boss;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.screen.MenuScreen;

public class BossFailScreen implements Screen {
    private static final float INPUT_ENABLE_TIME = 1.0f;

    private final MazeRunnerGame game;
    private final BossFailType failType;

    private OrthographicCamera camera;
    private SpriteBatch batch;
    private BitmapFont font;

    private float timer = 0f;
    private static final float AUTO_RETURN_TIME = 3.0f;

    public BossFailScreen(MazeRunnerGame game, BossFailType failType) {
        this.game = game;
        this.failType = failType;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );
        camera.update();

        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.6f);
    }

    @Override
    public void render(float delta) {
        timer += delta;

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
// ===== Fade-in alpha（0 ~ 1，0.5 秒完成）=====
        float alpha = Math.min(1f, timer / 0.5f);
        font.setColor(1f, 1f, 1f, alpha);
        // ===== 主失败文案 =====
        String text = getFailText();
        GlyphLayout layout = new GlyphLayout(font, text);

        float x = (Gdx.graphics.getWidth() - layout.width) / 2f;
        float y = Gdx.graphics.getHeight() / 2f + layout.height / 2f;

        font.draw(batch, layout, x, y);

        // ===== 1 秒后才显示提示 =====
        if (timer >= INPUT_ENABLE_TIME) {
            font.getData().setScale(0.9f);
            String hint = "[Press ENTER to return to menu]";
            GlyphLayout hintLayout = new GlyphLayout(font, hint);

            float hx = (Gdx.graphics.getWidth() - hintLayout.width) / 2f;
            float hy = y - 50f;

            font.draw(batch, hintLayout, hx, hy);

            // 画完记得恢复 scale
            font.getData().setScale(1.6f);
        }

        batch.end();
        font.setColor(1f, 1f, 1f, 1f);

        // ===== 输入判断：1 秒后才允许 =====
        if (timer >= INPUT_ENABLE_TIME &&
                (timer >= AUTO_RETURN_TIME
                        || Gdx.input.justTouched()
                        || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                        || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))) {

            game.setScreen(new MenuScreen(game));
        }
    }


    private String getFailText() {
        return switch (failType) {
            case PLAYER_DEAD ->
                    "You are bound to this place forever.";
            case DAMAGE_NOT_ENOUGH ->
                    "The maze closes. You were not strong enough.";
        };
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
