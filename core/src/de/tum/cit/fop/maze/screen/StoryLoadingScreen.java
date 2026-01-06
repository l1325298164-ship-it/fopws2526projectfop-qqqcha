package de.tum.cit.fop.maze.screen;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.TimeUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;

public class StoryLoadingScreen implements Screen {
    private TextureRegion starRegion;
    private final MazeRunnerGame game;
    private final AssetManager assets;

    private SpriteBatch batch;
    private BitmapFont font;

    // ===== 背景与毛玻璃 =====
    private FrameBuffer blurFbo;

    // ===== 小猫动画 =====
    private Animation<TextureRegion> catAnim;
    private float stateTime = 0f;
    private final String CAT_ATLAS_PATH = "ani/cat/right/cat_right.atlas"; // 确认你的文件名
    private boolean catInitialized = false;
    private com.badlogic.gdx.utils.Array<Sparkle> sparkles = new com.badlogic.gdx.utils.Array<>();
    private float sparkleTimer = 0;
    private long showTime;
    private boolean storyStarted = false;

    public StoryLoadingScreen(MazeRunnerGame game) {
        this.game = game;
        this.assets = game.getAssets();
    }

    @Override
    public void show() {
        batch = game.getSpriteBatch();
        resetProjection(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        Skin skin = game.getSkin();
        font = skin.getFont("default-font");

        Gdx.input.setInputProcessor(null);
        showTime = TimeUtils.millis();

        createBlurFbo(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // ⭐ 方案 A：让小猫插队到最前面
        if (!assets.isLoaded(CAT_ATLAS_PATH, TextureAtlas.class)) {
            assets.load(CAT_ATLAS_PATH, TextureAtlas.class);
        }
// 加载星星 Atlas
        assets.load("effects/sparkle.atlas", TextureAtlas.class);
        assets.finishLoadingAsset("effects/sparkle.atlas"); // 瞬间加载完

        TextureAtlas atlas = assets.get("effects/sparkle.atlas", TextureAtlas.class);
        starRegion = atlas.findRegion("sparkle"); // 寻找名为 star 的 PNG 帧
        // 然后再加载大块头的 PV 素材
        queuePV("pv/1/PV_1.atlas");
        queuePV("pv/2/PV_2.atlas");
        queuePV("pv/3/PV_3.atlas");
        queuePV("pv/4/PV_4.atlas");
    }

    @Override
    public void render(float delta) {
        assets.update();
        stateTime += delta;

        // --- 逻辑：初始化小猫 (手动兼容版) ---
        if (!catInitialized && assets.isLoaded(CAT_ATLAS_PATH, TextureAtlas.class)) {
            TextureAtlas atlas = assets.get(CAT_ATLAS_PATH, TextureAtlas.class);

            // 创建一个临时的数组来存帧
            com.badlogic.gdx.utils.Array<TextureAtlas.AtlasRegion> catFrames = new com.badlogic.gdx.utils.Array<>();

            // 手动遍历：只要名字里包含 "frame"，就加进来
            for (TextureAtlas.AtlasRegion region : atlas.getRegions()) {
                if (region.name.startsWith("cat_right")) {
                    catFrames.add(region);
                }
            }

            if (catFrames.size > 0) {
                // 关键：按名字后面的数字排序，防止猫瞬移
                catFrames.sort((o1, o2) -> o1.name.compareTo(o2.name));

                catAnim = new Animation<>(0.1f, catFrames, Animation.PlayMode.LOOP);
                catInitialized = true;
                System.out.println("✅ [Animation] 手动抓取成功！共加载帧数: " + catFrames.size);
            } else {
                System.err.println("❌ [Error] 连手动抓取都没找到 frame...");
            }
        }

        // --- 渲染部分 ---
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float progress = assets.getProgress();
        batch.begin();
// --- 4. 定义进度条位置和尺寸 ---
        float barWidth = Gdx.graphics.getWidth() * 0.9f;
        float barHeight = 15f;
        float barX = (Gdx.graphics.getWidth() - barWidth) / 2f;
        // 让进度条高度略低于猫猫 (猫猫在 0.2f，进度条可以设在 0.18f)
        float barY = Gdx.graphics.getHeight() * 0.18f;

        // 获取基础白色贴图 (假设你的 skin 里有白色像素块，这在 LibGDX Skin 中是标配)
        // 如果报错找不到 "white"，请尝试 "pixel" 或者直接用一张纯色小图
        TextureRegion white = game.getSkin().getRegion("white");

        // --- 5. 绘制背景 (深灰色/半透明) ---
        batch.setColor(0.2f, 0.2f, 0.2f, 0.5f); // 深色透明背景
        batch.draw(white, barX, barY, barWidth, barHeight);

        // --- 6. 绘制填充 ---
        batch.setColor(255f/255f, 182f/255f, 193f/255f, 1f);
        float fillWidth = barWidth * progress;
        if (fillWidth > 0) {
            batch.draw(white, barX, barY, fillWidth, barHeight);
        }


        batch.setColor(Color.WHITE);


        if (catInitialized) {
            TextureRegion currentFrame = catAnim.getKeyFrame(stateTime);
            float catSize = 250f;
            float catX = -catSize + (Gdx.graphics.getWidth() + catSize) * progress;
            float catY = Gdx.graphics.getHeight() * 0.2f;

            // --- 闪闪特效逻辑 ---
            sparkleTimer += delta;
            if (sparkleTimer > 0.05f) { // 每 0.1 秒生成一颗新星星
                Color sparkleColor = Math.random() > 0.5 ? Color.WHITE : new Color(1f, 182f/255f, 193f/255f, 1f);
                sparkles.add(new Sparkle(catX + catSize/2, catY + catSize/2, sparkleColor));
                sparkleTimer = 0;
            }
            // 1. 开启发光混合模式
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

            // ❌ 删掉这一行：TextureRegion starRegion = game.getSkin().getRegion("white");
            // ✅ 直接使用类成员变量 starRegion (它已经在 show 里被赋值为 stars.atlas 里的图了)

            for (int i = sparkles.size - 1; i >= 0; i--) {
                Sparkle s = sparkles.get(i);
                s.update(delta);
                if (s.life <= 0) {
                    sparkles.removeIndex(i);
                    continue;
                }

                float alpha = s.life / s.maxLife;
                batch.setColor(s.color.r, s.color.g, s.color.b, alpha);

                // 2. 绘制。如果你想让星星旋转，建议加上旋转参数：
                if (starRegion != null) {
                    batch.draw(starRegion, s.x, s.y, s.size / 2f, s.size / 2f, s.size, s.size, 1f, 1f, stateTime * 100);
                }
            }

            // 3. 恢复混合模式
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            batch.setColor(Color.WHITE);

            // 绘制猫猫
            batch.draw(currentFrame, catX, catY, catSize, catSize);
            for (int i = sparkles.size -1; i >= 0; i--) {
                Sparkle s = sparkles.get(i);
                s.update(delta);
                if (s.life <= 0) {
                    sparkles.removeIndex(i);
                    continue;
                }

                float alpha = s.life / s.maxLife;
                batch.setColor(s.color.r, s.color.g, s.color.b, alpha);

                // 2. 绘制。如果你想让星星旋转，建议加上旋转参数：
                if (starRegion != null) {
                    batch.draw(starRegion, s.x, s.y, s.size / 2f, s.size / 2f, s.size, s.size, 1f, 1f, stateTime * 100);
                }
            }

            // 3. 恢复混合模式
            batch.setColor(Color.WHITE);


        }

        // 文字显示
        font.draw(batch, "LOADING " + (int)(progress * 100) + "%", 0, Gdx.graphics.getHeight() * 0.15f, Gdx.graphics.getWidth(), Align.center, false);

        batch.end();

        // 资源加载完毕后的跳转逻辑
        if (!storyStarted && assets.isFinished() && TimeUtils.timeSinceMillis(showTime) > 2000) {
            storyStarted = true;
            System.out.println("✨ All assets loaded, starting story...");
            game.startStoryFromBeginning();
        }
    }

    private void resetProjection(int width, int height) {
        OrthographicCamera cam = new OrthographicCamera(width, height);
        cam.position.set(width / 2f, height / 2f, 0);
        cam.update();
        batch.setProjectionMatrix(cam.combined);
    }

    private void createBlurFbo(int width, int height) {
        if (blurFbo != null) blurFbo.dispose();
        blurFbo = new FrameBuffer(Pixmap.Format.RGBA8888, width / 4, height / 4, false);
    }

    private void queuePV(String path) {
        if (!assets.isLoaded(path, TextureAtlas.class)) {
            assets.load(path, TextureAtlas.class);
        }
    }

    @Override public void resize(int width, int height) { resetProjection(width, height); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { if (blurFbo != null) blurFbo.dispose(); }



    private class Sparkle {
        float x, y, size, life, maxLife;
        Color color;

        Sparkle(float x, float y, Color color) {
            this.x = x + (float)(Math.random() * 100 - 80);
            this.y = y + (float)(Math.random() * 100 - 50);

            // --- 修改这里：从原来的 (5~15) 改大，比如 (20~40) ---
            this.size = (float)(Math.random() * 20 +2);

            this.maxLife = (float)(Math.random() * 0.5f + 0.8f);
            this.life = maxLife;
            this.color = color;
        }

        void update(float delta) { life -= delta; }
    }
}