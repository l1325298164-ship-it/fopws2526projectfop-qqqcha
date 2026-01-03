package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.TimeUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;

/**
 * StoryLoadingScreen
 * 真·异步剧情加载 + 毛玻璃背景
 */
public class StoryLoadingScreen implements Screen {

    private final MazeRunnerGame game;
    private final AssetManager assets;

    private SpriteBatch batch;
    private BitmapFont font;

    // ===== 背景 =====
    private Texture bgTexture;

    // ===== 毛玻璃 =====
    private FrameBuffer blurFbo;
    private TextureRegion blurRegion;

    private long showTime;
    private boolean storyStarted = false;

    public StoryLoadingScreen(MazeRunnerGame game) {
        this.game = game;
        this.assets = game.getAssets();
    }

    // ======================================================
    // 生命周期
    // ======================================================

    @Override
    public void show() {
        batch = game.getSpriteBatch();

        // ⭐⭐⭐【关键修复】重置 SpriteBatch 投影矩阵（屏幕坐标系）
        resetProjection(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // 使用 Skin 字体
        Skin skin = game.getSkin();
        font = skin.getFont("default-font");

        // 吃掉所有输入，防止点到 Menu
        Gdx.input.setInputProcessor(null);

        showTime = TimeUtils.millis();

        // ===== 背景图 =====

        // ===== 毛玻璃 FBO（低分辨率）=====
        createBlurFbo(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // ===== 异步加载 PV =====
        queuePV("pv/1/PV_1.atlas");
        queuePV("pv/2/PV_2.atlas");
        queuePV("pv/3/PV_3.atlas");
        queuePV("pv/4/PV_4.atlas");
    }

    // ======================================================
    // 渲染
    // ======================================================

    @Override
    public void render(float delta) {
        // 非阻塞更新
        assets.update();



        // ===== ② 绘制到屏幕 =====
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float progress = assets.getProgress();

        batch.begin();

        // 放大绘制 → 自然模糊
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(
                blurRegion,
                0, 0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );

        // 半透明遮罩（增强毛玻璃感）
        batch.setColor(0f, 0f, 0f, 0.35f);
        batch.draw(
                blurRegion,
                0, 0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );
        batch.setColor(Color.WHITE);

        // ===== LOADING 文本 =====
        String text = "LOADING " + (int) (progress * 100) + "%";
        float x = Gdx.graphics.getWidth() / 2f;
        float y = Gdx.graphics.getHeight() / 2f;

        font.draw(batch, text, x, y);

        batch.end();

        // ===== 推进剧情 =====
        if (!storyStarted
                && assets.isFinished()
                && TimeUtils.timeSinceMillis(showTime) > 600) {

            storyStarted = true;
            game.startStoryFromBeginning();
        }
    }

    // ======================================================
    // Resize
    // ======================================================

    @Override
    public void resize(int width, int height) {
        resetProjection(width, height);
        createBlurFbo(width, height);
    }

    // ======================================================
    // 工具方法
    // ======================================================

    private void resetProjection(int width, int height) {
        OrthographicCamera cam = new OrthographicCamera(width, height);
        cam.position.set(width / 2f, height / 2f, 0);
        cam.update();
        batch.setProjectionMatrix(cam.combined);
    }

    private void createBlurFbo(int width, int height) {
        if (blurFbo != null) blurFbo.dispose();

        blurFbo = new FrameBuffer(
                Pixmap.Format.RGBA8888,
                width / 4,
                height / 4,
                false
        );

        blurRegion = new TextureRegion(blurFbo.getColorBufferTexture());
        blurRegion.flip(false, true);
    }

    private void queuePV(String path) {
        if (!assets.isLoaded(path, TextureAtlas.class)) {
            assets.load(path, TextureAtlas.class);
        }
    }

    // ======================================================
    // 其余生命周期
    // ======================================================

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (bgTexture != null) bgTexture.dispose();
        if (blurFbo != null) blurFbo.dispose();
    }
}
