package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;

public class IntroScreen implements Screen {

    private final MazeRunnerGame game;
    private final PVExit exitType;
    private final String atlasPath;
    private final String regionName;

    private TextureAtlas pvAtlas;
    private Animation<TextureRegion> pvAnim;
    private Texture backgroundTexture;

    private float stateTime = 0f;
    private SpriteBatch batch;

    // 🔥 1. 定义世界坐标尺寸 (和你图片分辨率一致)
    private static final float WORLD_WIDTH = 2784f;
    private static final float WORLD_HEIGHT = 1536f;

    // 🔥 2. 定义漫画内容的缩放比例
    // 0.85f 表示漫画只显示 85% 大小，留出 15% 给背景边框
    // 如果觉得边框太宽，可以改成 0.90f；如果边框太窄，改成 0.80f
    private static final float CONTENT_SCALE = 0.85f;

    // 视口管理器
    private Viewport viewport;

    private static final float FRAME_DURATION = 1.0f;

    public enum PVExit {
        NEXT_STAGE,
        TO_MENU
    }

    public IntroScreen(MazeRunnerGame game, String atlasPath, String regionName, PVExit exitType) {
        this.game = game;
        this.atlasPath = atlasPath;
        this.regionName = regionName;
        this.exitType = exitType;
        this.batch = game.getSpriteBatch();

        // 🔥 3. 初始化 FitViewport
        // 无论窗口怎么拉伸，都会保持 WORLD_WIDTH x WORLD_HEIGHT 的比例
        // 多余的地方会显示黑边 (Letterboxing)
        this.viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT);
    }

    @Override
    public void show() {

        Gdx.app.debug("IntroScreen", "Loading PV: " + regionName);
        try {
            // 加载背景图 (羊皮纸卷轴)
            // 请确保你的 assets/pv/ 目录下有 background.PNG 这个文件
            backgroundTexture = new Texture(Gdx.files.internal("pv/background.PNG"));

            // 加载漫画 Atlas
            pvAtlas = new TextureAtlas(Gdx.files.internal(atlasPath));

            Array<TextureAtlas.AtlasRegion> frames = pvAtlas.findRegions(regionName);
            if (frames.isEmpty()) {
                Gdx.app.error("IntroScreen", "❌ 找不到图片: " + regionName);
                return;
            }
            pvAnim = new Animation<>(FRAME_DURATION, frames, Animation.PlayMode.NORMAL);

        } catch (Exception e) {
            Gdx.app.error("IntroScreen", "❌ 资源加载错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void render(float delta) {
        stateTime += delta;

        // 清屏 (黑色)
        ScreenUtils.clear(0, 0, 0, 1);

        if (pvAnim == null || backgroundTexture == null) {
            handleExit();
            return;
        }

        // 🔥 4. 应用视口和投影矩阵
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);

        batch.begin();

        // ---------------------------------------------------------
        // 第一层：绘制背景 (羊皮纸)
        // ---------------------------------------------------------
        // 这里的逻辑是：背景图铺满整个世界坐标 (100% 大小)
        batch.draw(backgroundTexture, 0, 0, WORLD_WIDTH, WORLD_HEIGHT);

        // ---------------------------------------------------------
        // 第二层：绘制漫画内容 (叠加在背景上)
        // ---------------------------------------------------------
        int currentFrameIndex = pvAnim.getKeyFrameIndex(stateTime);
        Object[] frames = pvAnim.getKeyFrames();

        // 🔥 5. 计算缩放后的尺寸和居中偏移量
        float scaledWidth = WORLD_WIDTH * CONTENT_SCALE;
        float scaledHeight = WORLD_HEIGHT * CONTENT_SCALE;

        // 让漫画居中显示：(总宽 - 缩放宽) / 2
        float offsetX = (WORLD_WIDTH - scaledWidth) / 2;
        float offsetY = (WORLD_HEIGHT - scaledHeight) / 2;

        for (int i = 0; i <= currentFrameIndex && i < frames.length; i++) {
            TextureRegion region = (TextureRegion) frames[i];

            // 绘制时使用缩放后的坐标 (x, y) 和尺寸 (width, height)
            batch.draw(region, offsetX, offsetY, scaledWidth, scaledHeight);
        }

        batch.end();

        // ---------------------------------------------------------
        // 检查播放结束
        // ---------------------------------------------------------
        if (pvAnim.isAnimationFinished(stateTime)) {
            // 播放完后等待 2 秒再跳转
            if (stateTime > pvAnim.getAnimationDuration() + 2.0f) {
                handleExit();
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        // 🔥 6. 窗口大小改变时更新视口 (true 代表居中)
        viewport.update(width, height, true);
    }

    // 🔥 7. 你的 handleExit 方法 (处理跳转逻辑)
    private void handleExit() {
        switch (exitType) {
            case NEXT_STAGE -> game.nextStage();
            case TO_MENU -> game.goToMenu();
        }
    }

    @Override
    public void dispose() {
        if (pvAtlas != null) pvAtlas.dispose();
        if (backgroundTexture != null) backgroundTexture.dispose();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}