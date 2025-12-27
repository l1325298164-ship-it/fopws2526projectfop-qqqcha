package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;

public class IntroScreen implements Screen {
    private final MazeRunnerGame game;
    private TextureAtlas pvAtlas;
    private Animation<TextureAtlas.AtlasRegion> pvAnim;
    private float stateTime = 0f;
    private SpriteBatch batch;

    public IntroScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        pvAtlas = new TextureAtlas(Gdx.files.internal("pv/pre1.atlas"));

        Array<TextureAtlas.AtlasRegion> frames =
                pvAtlas.findRegions("pre1"); // 自动按 _0000, _0001 排序

        pvAnim = new Animation<>(
                0.1f,          // 每帧 0.1 秒（10 FPS，可调）
                frames,
                Animation.PlayMode.NORMAL
        );

        stateTime = 0f;

    }

    @Override
    public void render(float delta) {
        stateTime += delta;

        ScreenUtils.clear(0, 0, 0, 1);

        batch.begin();
        TextureRegion frame = pvAnim.getKeyFrame(stateTime);
        batch.draw(frame, 0, 0, 1920, 1080);
        batch.end();

        // ✅ 只切 Screen，不 dispose
        if (pvAnim.isAnimationFinished(stateTime)) {
            game.goToQTE(); // 或 setScreen(new ...)
        }
    }


    @Override
    public void resize(int i, int i1) {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void dispose() {
        batch.dispose();
        pvAtlas.dispose();
    }
}
