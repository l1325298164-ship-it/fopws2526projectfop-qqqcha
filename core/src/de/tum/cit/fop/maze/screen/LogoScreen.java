package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.MazeRunnerGame;

public class LogoScreen implements Screen {

    private final MazeRunnerGame game;
    private SpriteBatch batch;

    private TextureAtlas logoAtlas;
    private Animation<TextureRegion> logoAnim;

    private float stateTime = 0f;
    private static final float TOTAL_DURATION = 6f;

    public LogoScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = game.getSpriteBatch();

        // 1️⃣ 加载 atlas
        logoAtlas = new TextureAtlas(
                Gdx.files.internal("logo/logo.atlas")
        );

        // 2️⃣ 拿到所有 logo 帧
        Array<TextureAtlas.AtlasRegion> atlasRegions =
                logoAtlas.findRegions("logo");

        Array<TextureRegion> frames = new Array<>();
        for (TextureAtlas.AtlasRegion r : atlasRegions) {
            frames.add(r);
        }

        // 3️⃣ 计算每帧时长（6 秒播完）
        float frameDuration = TOTAL_DURATION / frames.size;

        // 4️⃣ 创建动画
        logoAnim = new Animation<>(
                frameDuration,
                frames,
                Animation.PlayMode.NORMAL
        );
    }

    @Override
    public void render(float delta) {
        stateTime += delta;

        // 🔹 任意键 / 触屏跳过
        if (Gdx.input.justTouched() ||
                Gdx.input.isKeyJustPressed(Input.Keys.ANY_KEY)) {
            finish();
            return;
        }

        TextureRegion frame = logoAnim.getKeyFrame(stateTime);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(com.badlogic.gdx.graphics.GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        batch.draw(
                frame,
                0,
                0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );
        batch.end();

        // 播放完成
        if (logoAnim.isAnimationFinished(stateTime)) {
            finish();
        }
    }

    private void finish() {
        dispose();
        game.setScreen(new MenuScreen(game));
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (logoAtlas != null) {
            logoAtlas.dispose();
        }
    }
}
