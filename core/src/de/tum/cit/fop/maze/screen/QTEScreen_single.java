//package de.tum.cit.fop.maze.screen;
//
//import com.badlogic.gdx.Gdx;
//import com.badlogic.gdx.Input;
//import com.badlogic.gdx.Screen;
//import com.badlogic.gdx.graphics.Color;
//import com.badlogic.gdx.graphics.GL20;
//import com.badlogic.gdx.graphics.OrthographicCamera;
//import com.badlogic.gdx.graphics.g2d.*;
//import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
//import com.badlogic.gdx.math.MathUtils;
//import com.badlogic.gdx.utils.Array;
//
//import de.tum.cit.fop.maze.MazeRunnerGame;
//import de.tum.cit.fop.maze.effects.QTE.QTERippleManager;
//import de.tum.cit.fop.maze.game.GameConstants;
//import de.tum.cit.fop.maze.game.GameManager;
//import de.tum.cit.fop.maze.qte.QTEMazeData;
//import de.tum.cit.fop.maze.qte.QTEMazeRenderer;
//import de.tum.cit.fop.maze.tools.PVAnimationCache;
//import de.tum.cit.fop.maze.utils.Logger;
//import de.tum.cit.fop.maze.utils.TextureManager;
//import de.tum.cit.fop.maze.qte.QTEResult;
//
///**
// * QTE Screen（MazeRenderer + 独立 Camera）
// */
//public class QTEScreen_single implements Screen {
//
//
//
//    private QTEResult result = null;
//    //成功失败判定时间
//    private static final float QTE_TIME_LIMIT = 30.0f;
//    private float qteTimer = 0f;
//
//    private final MazeRunnerGame game;
//    private final GameManager gameManager;
//
//    // =========================
//    private OrthographicCamera camera;
//    // =========================
//    // QTE 引导文字
//    // =========================
//    private BitmapFont hintFont;
//    private GlyphLayout hintLayout = new GlyphLayout();
//
//    // =========================
//    // Countdown Font
//    // =========================
//    private BitmapFont countdownFont;
//    private GlyphLayout countdownLayout = new GlyphLayout();
//
//
//
//
//    private float playerWorldX;
//    private float playerWorldY;
//    private float cellSize;
//
//    // =========================
//    // 渲染
//    // =========================
//    private SpriteBatch batch;
//    private BitmapFont successFont;
//    // 动画
//    private Animation<TextureRegion> struggleAnim;
//    private TextureRegion escapeFrame;
//    private float stateTime = 0f;
//
//    // =========================
//    // QTE 状态
//    // =========================
//    private enum QTEState {
//        PREPARE,
//        ACTIVE,
//        SUCCESS_START,   // 刚成功：爆炸 + 定格
//        SUCCESS_MOVE,    // 角色跳出动画
//        SUCCESS_STAY,    // 动画完成后的短暂停留
//        DONE
//    }
//    // 添加停留计时器
//    private float successStayTimer = 0f;
//    // 延长移动时间
//    private static final float SUCCESS_DURATION = 0.3f; // 从1秒延长到1.5秒
//
//    // =========================
//    // QTE 倒计时
//    // =========================
//    private static final float PREPARE_DURATION = 3f;
//    private float prepareTimer = 0f;
//
//    //初始状态
//    private QTEState qteState = QTEState.PREPARE;
//
//    // 连打
//    private int mashCount = 0;
//    private float mashTimer = 0f;
//    private static final float MASH_WINDOW = 1.0f;
//    private static final int MASH_REQUIRED = 5;
//
//    private float animationSpeed = 1.0f;
//
//    // 成功移动
//    private float successTimer = 0f;
//    private float successStartX;
//    private float successTargetX;
//
//    // =========================
//    // de.tum.cit.fop.maze.entities.trap.Trap（QTE 陷阱）
//    // =========================
//    private int trapGridX;
//    private int trapGridY;
//    private float trapWorldX;
//    private float trapWorldY;
//    private TextureRegion trapRegion;
//
//    // =========================
//    // Progress Bar & Effects
//    // =========================
//    private Float lockedProgress = null; // null = 未锁定
//    private float progress = 0f;        // 0 ~ 1
//    private float displayedProgress = 0f; // 用于平滑动画
//    private boolean progressExploding = false;
//
//    // 🔥【新增】波纹管理器
//    private QTERippleManager rippleManager;
//
//    // 进度条背景（粉色，20% 透明）
//    private static final Color BAR_BG_COLOR = new Color(1.0f, 0.4f, 0.7f, 0.2f);
//    // 渐变用的两端颜色
//    private static final Color BAR_PINK = new Color(1.0f, 0.45f, 0.75f, 1f);
//    private static final Color BAR_YELLOW = new Color(1.0f, 0.95f, 0.4f, 1f);
//
//    // 爆炸粒子
//    private static class ProgressParticle {
//        float x, y;
//        float vx, vy;
//        float life;
//        Color color;
//    }
//    private Array<ProgressParticle> particles = new Array<>();
//
//    //progress 美化：黑色边框+高光
//    private static final float BAR_BORDER = 2f;
//    private static final float METAL_HIGHLIGHT_HEIGHT = 3f;
//
//    // 视觉参数
//    private static final float BAR_WIDTH_RATIO = 0.7f;
//    private static final float BAR_HEIGHT = 14f;
//    private static final float BAR_Y_OFFSET = 18f;
//    private ShapeRenderer shapeRenderer;
//
//    private float successFreezeTimer = 0f;
//
//    // ===== Progress Bar layout (GLOBAL) =====
//    private float barX;
//    private float barY;
//    private float barWidth;
//
//    // =========================
//    // 构造函数（重点）
//    // =========================
//    public QTEScreen_single(MazeRunnerGame game, GameManager gameManager) {
//        this.game = game;
//        this.gameManager = gameManager;
//    }
//
//    // =========================================================
//    // 生命周期
//    // =========================================================
//
//    @Override
//    public void show() {
//        successFont = new BitmapFont();
//        successFont.getData().setScale(1.2f);
//        successFont.setColor(Color.GOLD);
//        camera = new OrthographicCamera();
//        camera.setToOrtho(
//                false,
//                Gdx.graphics.getWidth(),
//                Gdx.graphics.getHeight()
//        );
//        camera.update();
//
//// 角色世界坐标 = 屏幕中心
//        playerWorldX = camera.viewportWidth / 2f;
//        playerWorldY = camera.viewportHeight / 2f;
//
//
//        batch = new SpriteBatch();
//        shapeRenderer = new ShapeRenderer();
//
//        // 🔥【新增】初始化波纹管理器
//        rippleManager = new QTERippleManager();
//
//        // 👉 引导字体（先用默认，后期可换 TTF）
//        hintFont = new BitmapFont();
//        hintFont.setUseIntegerPositions(false);
//        hintFont.getData().setScale(0.3f);
//        hintFont.setColor(1f, 0.9f, 0.95f, 1f);
//
//        //倒计时字体
//        countdownFont = new BitmapFont(); // 先用默认
//        countdownFont.setUseIntegerPositions(false);
//        countdownFont.getData().setScale(0.9f);
//        countdownFont.setColor(0f, 0f, 0f, 1f);
//        countdownFont.getData().markupEnabled = false;
//
//        TextureManager.getInstance().switchMode(TextureManager.TextureMode.IMAGE);
//
//        cellSize = GameConstants.CELL_SIZE;
//
//
//        camera.position.set(
//                playerWorldX + cellSize / 2f,
//                playerWorldY + cellSize / 2f,
//                0
//        );
//        camera.update();
//
//        System.out.println("QTE GameManager = " + gameManager);
//
//        // 动画
//        Array<TextureRegion> frames = new Array<>();
//        for (int i = 0; i < 4; i++) {
//            frames.add(new TextureRegion(
//                    new com.badlogic.gdx.graphics.Texture("qte/player_struggle_00" + i + ".png")
//            ));
//        }
//        struggleAnim = new Animation<>(0.15f, frames, Animation.PlayMode.LOOP);
//        escapeFrame = new TextureRegion(
//                new com.badlogic.gdx.graphics.Texture("qte/player_escape.png")
//        );
//
//
//
//    }
//    private void renderSuccessText() {
//        if (qteState != QTEState.SUCCESS_STAY && qteState != QTEState.DONE) return;
//
//        String text = "ACCOMPLISHED!";
//        GlyphLayout layout = new GlyphLayout(successFont, text);
//
//        float x = camera.viewportWidth / 2f - layout.width / 2f;
//        float y = camera.viewportHeight / 2f + 120f;
//
//        batch.begin();
//        successFont.draw(batch, layout, x, y);
//        batch.end();
//    }
//
//
//
//
//    private void drawPlayer() {
//        TextureRegion frame;
//
//        if (qteState == QTEState.ACTIVE || qteState == QTEState.PREPARE) {
//            frame = struggleAnim.getKeyFrame(stateTime);
//        } else {
//            frame = escapeFrame;
//        }
//
//        float size = 96f; // 角色显示大小（像素）
//        float x = playerWorldX - size / 2f;
//        float y = playerWorldY - size / 2f;
//
//        batch.draw(frame, x, y, size, size);
//    }
//
//
//    //提示词
//    private void renderPressSpaceHint() {
//        if (qteState != QTEState.ACTIVE) return;
//
//        String text = "PRESS  SPACE";
//        hintLayout.setText(hintFont, text);
//
//        // 🌬 呼吸动画（alpha）
//        float pulse = 0.6f + 0.4f * MathUtils.sin(stateTime * 4f);
//        hintFont.setColor(0.1f, 0.1f, 0.1f, pulse);
//
//        float textX = camera.viewportWidth / 2f - hintLayout.width / 2f;
//        float textY = barY + BAR_HEIGHT + 24f;
//
//        batch.begin();
//        hintFont.draw(batch, hintLayout, textX, textY);
//        batch.end();
//    }
//
//    // =========================================================
//    // 主渲染方法
//    // =========================================================
//
//    // 在QTEScreen中修改主渲染方法（最终方案）
//    @Override
//    public void render(float delta) {
//        updatePrepare(delta);
//        updateQTE(delta);
//        updateSuccess(delta);
//
//        stateTime += delta;
//
//        Gdx.gl.glClearColor(0, 0, 0, 1);
//        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
//
//        // 1️⃣ 游戏画面
//        batch.setProjectionMatrix(camera.combined);
//        batch.begin();
//        drawPlayer();
//        batch.end();
//
//// 2️⃣ UI（全部用 shapeRenderer / font）
//        renderProgressBar(delta);
//        renderPressSpaceHint();
//        renderPrepareText();
//        renderSuccessText();
//    }
//
//    private void updateSuccess(float delta) {
//        // 只在成功相关状态下处理
//        if (qteState != QTEState.SUCCESS_START
//                && qteState != QTEState.SUCCESS_STAY) {
//            return;
//        }
//
//        // ① 成功瞬间冻结（给爆炸 / 反馈用）
//        if (qteState == QTEState.SUCCESS_START) {
//            successFreezeTimer += delta;
//
//            // 冻结 0.5 秒后进入展示阶段
//            if (successFreezeTimer >= 0.5f) {
//                qteState = QTEState.SUCCESS_STAY;
//                successStayTimer = 0f;
//                Logger.debug("QTE -> SUCCESS_STAY");
//            }
//            return;
//        }
//
//        // ② 成功展示阶段（ACCOMPLISHED!）
//        if (qteState == QTEState.SUCCESS_STAY) {
//            successStayTimer += delta;
//
//            // 停留 1 秒后结束 QTE
//            if (successStayTimer >= 1.0f) {
//                finishQTE(QTEResult.SUCCESS);
//            }
//        }
//    }
//
//
//    // =========================================================
//    // 进度条渲染（保持不变）
//    // =========================================================
//
//    private void renderProgressBar(float delta) {
//        barWidth = camera.viewportWidth * BAR_WIDTH_RATIO;
//        barX = camera.position.x - barWidth / 2f;
//        barY = camera.position.y - camera.viewportHeight / 2f + BAR_Y_OFFSET;
//
//        float target = (lockedProgress != null)
//                ? lockedProgress
//                : Math.min(1f, mashCount / (float) MASH_REQUIRED);
//
//        displayedProgress += (target - displayedProgress) * 8f * delta;
//
//        // ===== 唯一 begin =====
//        shapeRenderer.setProjectionMatrix(camera.combined);
//        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
//
//        // 1️⃣ 黑色描边
//        shapeRenderer.setColor(0f, 0f, 0f, 1f);
//        shapeRenderer.rect(
//                barX - BAR_BORDER,
//                barY - BAR_BORDER,
//                barWidth + BAR_BORDER * 2,
//                BAR_HEIGHT + BAR_BORDER * 2
//        );
//
//        // 2️⃣ 背景
//        shapeRenderer.setColor(BAR_BG_COLOR);
//        shapeRenderer.rect(barX, barY, barWidth, BAR_HEIGHT);
//
//        // 3️⃣ 填充（渐变）
//        if (qteState != QTEState.DONE) {
//            drawCandyGradient(barX, barY, barWidth * displayedProgress);
//            drawMetalHighlight(barX, barY, barWidth * displayedProgress);
//            drawMetalEdges(barX, barY, barWidth * displayedProgress);
//        }
//
//        // 4️⃣ 粒子
//        if (progressExploding) {
//            renderExplosionParticles(delta);
//        }
//
//        shapeRenderer.end();
//    }
//
//    private void drawMetalEdges(float x, float y, float width) {
//        // 上边缘亮
//        shapeRenderer.setColor(1f, 1f, 1f, 0.18f);
//        shapeRenderer.rect(x, y + BAR_HEIGHT - 1f, width, 1f);
//
//        // 下边缘暗
//        shapeRenderer.setColor(0f, 0f, 0f, 0.15f);
//        shapeRenderer.rect(x, y, width, 1f);
//    }
//
//    private void drawCandyGradient(float x, float y, float width) {
//        int steps = 16;
//        float sliceHeight = BAR_HEIGHT / steps;
//
//        for (int i = 0; i < steps; i++) {
//            float t = i / (float) (steps - 1);
//            float wave = 0.5f + 0.5f * MathUtils.sin(stateTime * 3f + t * 6f);
//            Color c = new Color(
//                    MathUtils.lerp(BAR_PINK.r, BAR_YELLOW.r, wave),
//                    MathUtils.lerp(BAR_PINK.g, BAR_YELLOW.g, wave),
//                    MathUtils.lerp(BAR_PINK.b, BAR_YELLOW.b, wave),
//                    1f
//            );
//
//            shapeRenderer.setColor(c);
//            shapeRenderer.rect(
//                    x,
//                    y + i * sliceHeight,
//                    width,
//                    sliceHeight + 1f
//            );
//        }
//    }
//
//    private void drawMetalHighlight(float x, float y, float width) {
//        float wave = 0.5f + 0.5f * MathUtils.sin(stateTime * 2f);
//        float highlightY = y + BAR_HEIGHT * (0.25f + 0.3f * wave);
//        shapeRenderer.setColor(1f, 1f, 1f, 0.22f);
//        shapeRenderer.rect(x, highlightY, width, METAL_HIGHLIGHT_HEIGHT);
//    }
//
//    private void spawnProgressExplosion() {
//        float barWidth = camera.viewportWidth * BAR_WIDTH_RATIO;
//        float barX = camera.position.x - barWidth / 2f;
//        float barY = camera.position.y - camera.viewportHeight / 2f + BAR_Y_OFFSET;
//
//        // 🎯 粒子中心 = 进度条中心
//        float cx = barX + barWidth * displayedProgress;
//        float cy = barY + BAR_HEIGHT / 2f;
//
//        for (int i = 0; i < 100; i++) {
//            ProgressParticle p = new ProgressParticle();
//            p.x = cx;
//            p.y = cy;
//
//            float angle = MathUtils.random(0f, 360f);
//            float speed = MathUtils.random(40f, 90f);
//
//            p.vx = MathUtils.cosDeg(angle) * speed;
//            p.vy = MathUtils.sinDeg(angle) * speed;
//            p.life = MathUtils.random(0.5f, 0.8f);
//
//            p.color = new Color(
//                    MathUtils.random(0.6f, 1f),
//                    MathUtils.random(0.6f, 1f),
//                    MathUtils.random(0.6f, 1f),
//                    1f
//            );
//
//            particles.add(p);
//        }
//    }
//
//    private void renderExplosionParticles(float delta) {
//        for (int i = particles.size - 1; i >= 0; i--) {
//            ProgressParticle p = particles.get(i);
//            p.life -= delta;
//
//            if (p.life <= 0) {
//                particles.removeIndex(i);
//                continue;
//            }
//
//            p.x += p.vx * delta;
//            p.y += p.vy * delta;
//            p.vy -= 110 * delta;
//
//            float sparkle = 0.6f + 0.4f * MathUtils.sin(stateTime * 20f + p.life * 10f);
//            shapeRenderer.setColor(
//                    p.color.r * sparkle,
//                    p.color.g * sparkle,
//                    p.color.b * sparkle,
//                    p.color.a * p.life
//            );
//
//            shapeRenderer.circle(p.x, p.y, 0.7f);
//        }
//    }
//
//    // =========================================================
//    // QTE 逻辑（保持不变）
//    // =========================================================
//
//    private void updateQTE(float delta) {
//        if (qteState != QTEState.ACTIVE) return;
//
//        // 总时间限制
//        qteTimer += delta;
//        if (qteTimer >= QTE_TIME_LIMIT) {
//            finishQTE(QTEResult.FAIL);
//            return;
//        }
//
//        // 连打
//        mashTimer += delta;
//
//        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
//            mashCount++;
//
//            // 🔥【新增】播放波纹特效
//            if (rippleManager != null) {
//                // 计算进度条中心点（黑框的几何中心）
//                float centerX = barX + barWidth / 2f;
//                float centerY = barY + BAR_HEIGHT / 2f;
//
//                rippleManager.spawnRipple(centerX, centerY);
//            }
//        }
//
//        if (mashCount >= MASH_REQUIRED) {
//            enterSuccessStart();
//        }
//
//        if (mashTimer >= MASH_WINDOW && qteState == QTEState.ACTIVE) {
//            mashCount = 0;
//            mashTimer = 0f;
//        }
//    }
//
//    private void enterSuccessStart() {
//        if (qteState != QTEState.ACTIVE) return;
//
//        qteState = QTEState.SUCCESS_START;
//        Logger.debug("QTE -> " + qteState);
//
//        lockedProgress = 1f;
//        displayedProgress = 1f;
//
//        progressExploding = true;
//        spawnProgressExplosion();
//
//        successFreezeTimer = 0f;
//        Logger.debug("ENTER SUCCESS_START, displayedProgress=" + displayedProgress);
//    }
//
//    private void finishQTE(QTEResult result) {
//        if (qteState == QTEState.DONE) return;
//
//        qteState = QTEState.DONE;
//        this.result = result;
//
//        Gdx.app.postRunnable(() -> {
//            game.onQTEFinished(result);
//        });
//    }
//
//    private void updatePrepare(float delta) {
//        if (qteState != QTEState.PREPARE) return;
//
//        prepareTimer += delta;
//        if (prepareTimer >= PREPARE_DURATION) {
//            // 倒计时结束，正式开始 QTE
//            qteState = QTEState.ACTIVE;
//            prepareTimer = 0f;
//
//            // 清空所有 QTE 相关状态，确保"干净开局"
//            qteTimer = 0f;
//            mashCount = 0;
//            mashTimer = 0f;
//            displayedProgress = 0f;
//
//            Logger.debug("QTE -> ACTIVE");
//        }
//    }
//
//    private void renderPrepareText() {
//        if (qteState != QTEState.PREPARE) return;
//
//        String text;
//        int second = 3 - (int) prepareTimer;
//
//        switch (second) {
//            case 3:
//                text = "GET";
//                break;
//            case 2:
//                text = "READY";
//                break;
//            default:
//                text = "GO!";
//                break;
//        }
//
//        countdownLayout.setText(countdownFont, text);
//
//        // 中央 + 轻微缩放呼吸
//        float pulse = 0.85f + 0.15f * MathUtils.sin(stateTime * 6f);
//        countdownFont.setColor(0f, 0f, 0f, pulse);
//
//        float textX = camera.viewportWidth / 2f - hintLayout.width / 2f;
//        float textY = barY + BAR_HEIGHT + 24f;
//
//        batch.begin();
//        countdownFont.draw(batch, countdownLayout, textX, textY);
//        batch.end();
//    }
//
//    // =========================================================
//    // 其他生命周期方法
//    // =========================================================
//
//    @Override
//    public void resize(int width, int height) {}
//
//    @Override
//    public void pause() {}
//
//    @Override
//    public void resume() {}
//
//    @Override
//    public void hide() {}
//
//    @Override
//    public void dispose() {
//        if (batch != null) {
//            batch.dispose();
//            batch = null;
//        }
//        if (shapeRenderer != null) {
//            shapeRenderer.dispose();
//            shapeRenderer = null;
//        }
//        if (hintFont != null) {
//            hintFont.dispose();
//            hintFont = null;
//        }
//        if (countdownFont != null) {
//            countdownFont.dispose();
//            countdownFont = null;
//        }
//        // 🔥【新增】清理波纹管理器
//        if (rippleManager != null) {
//            rippleManager.dispose();
//            rippleManager = null;
//        }
//    }
//}