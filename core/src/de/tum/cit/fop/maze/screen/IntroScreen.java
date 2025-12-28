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

    public void show() {
        batch = new SpriteBatch();

        // 添加调试信息
        Gdx.app.debug("IntroScreen", "Loading atlas from: " + atlasPath);

        try {
            pvAtlas = new TextureAtlas(Gdx.files.internal(atlasPath));

            // 检查图集是否加载成功
            if (pvAtlas == null) {
                Gdx.app.error("IntroScreen", "Failed to load texture atlas!");
                return;
            }

            // 列出图集中的所有区域，用于调试
            Gdx.app.debug("IntroScreen", "Atlas regions found: ");
            for (TextureAtlas.AtlasRegion region : pvAtlas.getRegions()) {
                Gdx.app.debug("IntroScreen", "- " + region.name);
            }

            Array<TextureAtlas.AtlasRegion> frames = pvAtlas.findRegions(regionName);

            // 重要：检查是否找到了帧
            Gdx.app.debug("IntroScreen", "Looking for region: " + regionName);
            Gdx.app.debug("IntroScreen", "Found frames count: " + frames.size);

            if (frames.size == 0) {
                Gdx.app.error("IntroScreen", "No frames found for region: " + regionName);

                // 尝试寻找类似名称的区域（常见命名差异）
                for (TextureAtlas.AtlasRegion region : pvAtlas.getRegions()) {
                    if (region.name.contains(regionName) || regionName.contains(region.name)) {
                        Gdx.app.debug("IntroScreen", "Found similar region: " + region.name);
                    }
                }

                return;
            }

            pvAnim = new Animation<>(
                    0.5f,
                    frames,
                    Animation.PlayMode.NORMAL
            );

            stateTime = 0f;

        } catch (Exception e) {
            Gdx.app.error("IntroScreen", "Error loading intro animation: " + e.getMessage());
        }
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
