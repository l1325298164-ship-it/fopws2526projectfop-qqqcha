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
    private final PVExit exitType;

    public enum PVExit {
        NEXT_STAGE,
        TO_MENU
    }
    // ⭐ 新增：由外部传入
    private final String atlasPath;
    private final String regionName;

    private TextureAtlas pvAtlas;
    private Animation<TextureAtlas.AtlasRegion> pvAnim;
    private float stateTime = 0f;
    private SpriteBatch batch;

    // ✅ 新构造函数（核心）
    public IntroScreen(
            MazeRunnerGame game,
            String atlasPath,
            String regionName,
            PVExit exitType
    ) {
        this.game = game;
        this.atlasPath = atlasPath;
        this.regionName = regionName;
        this.exitType = exitType;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        pvAtlas = new TextureAtlas(Gdx.files.internal(atlasPath));

        Array<TextureAtlas.AtlasRegion> frames =
                pvAtlas.findRegions(regionName); // 自动按 _0000, _0001

        pvAnim = new Animation<>(
                0.1f,                  // FPS 可调
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

        if (pvAnim.isAnimationFinished(stateTime)) {

            switch (exitType) {
                case NEXT_STAGE -> game.nextStage();
                case TO_MENU -> game.goToMenu();
            }
        }
    }

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        pvAtlas.dispose();
    }
}
