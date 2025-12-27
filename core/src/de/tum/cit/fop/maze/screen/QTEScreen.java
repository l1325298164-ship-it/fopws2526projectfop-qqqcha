package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.qte.QTEMazeData;
import de.tum.cit.fop.maze.qte.QTEMazeRenderer;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

/**
 * QTE Screen（MazeRenderer + 独立 Camera）
 */
public class QTEScreen implements Screen {

    public enum QTEResult {
        SUCCESS,
        FAIL
    }

    private QTEResult result = null;
//成功失败判定时间
    private static final float QTE_TIME_LIMIT = 30.0f;
    private float qteTimer = 0f;

    private final MazeRunnerGame game;
    private final GameManager gameManager;

    // =========================
    // Camera（QTE 专用）
    // =========================
    private OrthographicCamera camera;
    // =========================
// QTE 引导文字
// =========================
    private BitmapFont hintFont;
    private GlyphLayout hintLayout = new GlyphLayout();

    // =========================
    // Maze Renderer
    // =========================
    private QTEMazeRenderer mazeRenderer;


    // =========================
    // 玩家格子坐标
    // =========================
    private int playerGridX = 1;
    private int playerGridY = 2;

    // =========================
    // 世界坐标
    // =========================
    private float playerX;
    private float playerY;
    private float cellSize;

    // =========================
    // 渲染
    // =========================
    private SpriteBatch batch;

    // 动画
    private Animation<TextureRegion> struggleAnim;
    private TextureRegion escapeFrame;
    private float stateTime = 0f;

    // =========================
    // QTE 状态
    // =========================
    private enum QTEState {
        ACTIVE,
        SUCCESS_START,   // 刚成功：爆炸 + 定格
        SUCCESS_MOVE,    // 角色跳出
        DONE
    }
    private QTEState qteState = QTEState.ACTIVE;

    // 连打
    private int mashCount = 0;
    private float mashTimer = 0f;
    private static final float MASH_WINDOW = 1.0f;
    private static final int MASH_REQUIRED = 5;

    private float animationSpeed = 1.0f;

    // 成功移动
    private static final float SUCCESS_DURATION = 1f;
    private float successTimer = 0f;
    private float successStartX;
    private float successTargetX;
    private float successStartY;
    private float successTargetY;

    // =========================
// Progress Bar
// =========================
    private Float lockedProgress = null; // null = 未锁定

    private float progress = 0f;        // 0 ~ 1
    private float displayedProgress = 0f; // 用于平滑动画
    private boolean progressExploding = false;

    // 进度条背景（粉色，20% 透明）
    private static final Color BAR_BG_COLOR =
            new Color(1.0f, 0.4f, 0.7f, 0.2f);

    // 渐变用的两端颜色
    private static final Color BAR_PINK =
            new Color(1.0f, 0.45f, 0.75f, 1f);

    private static final Color BAR_YELLOW =
            new Color(1.0f, 0.95f, 0.4f, 1f);

    // 爆炸粒子
    private static class ProgressParticle {
        float x, y;
        float vx, vy;
        float life;
        Color color;
    }
    private Array<ProgressParticle> particles = new Array<>();

    //progress 美化：黑色边框+高光
    private static final float BAR_BORDER = 2f;
    private static final float METAL_HIGHLIGHT_HEIGHT = 3f;




    // 视觉参数
    private static final float BAR_WIDTH_RATIO = 0.7f;
    private static final float BAR_HEIGHT = 14f;
    private static final float BAR_Y_OFFSET = 18f;
    private ShapeRenderer shapeRenderer;

    private float successFreezeTimer = 0f;

    // ===== Progress Bar layout (GLOBAL) =====
    private float barX;
    private float barY;
    private float barWidth;


    // =========================
    // 构造函数（重点）
    // =========================
    public QTEScreen(MazeRunnerGame game, GameManager gameManager) {
        this.game = game;
        this.gameManager = gameManager;
    }

    // =========================================================
    // 生命周期
    // =========================================================

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        // 👉 引导字体（先用默认，后期可换 TTF）
        hintFont = new BitmapFont();
        hintFont.setUseIntegerPositions(false);

        hintFont.getData().setScale(0.3f);
        hintFont.setColor(1f, 0.9f, 0.95f, 1f);

        TextureManager.getInstance()
                .switchMode(TextureManager.TextureMode.IMAGE);

        cellSize = GameConstants.CELL_SIZE;

        // QTE 专用紧张镜头（只看 4x4 格子）
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 4 * cellSize, 4 * cellSize);

        updatePlayerScreenPos();

        camera.position.set(
                playerX + cellSize / 2f,
                playerY + cellSize / 2f,
                0
        );
        camera.update();

        // MazeRenderer：直接用传进来的 GameManager
        mazeRenderer = new QTEMazeRenderer();
        System.out.println("QTE GameManager = " + gameManager);


        // 动画
        Array<TextureRegion> frames = new Array<>();
        for (int i = 0; i < 4; i++) {
            frames.add(new TextureRegion(
                    new com.badlogic.gdx.graphics.Texture(
                            "qte/player_struggle_00" + i + ".png")
            ));
        }
        struggleAnim = new Animation<>(0.15f, frames, Animation.PlayMode.LOOP);
        escapeFrame = new TextureRegion(
                new com.badlogic.gdx.graphics.Texture("qte/player_escape.png")
        );
    }
//提示词
    private void renderPressSpaceHint() {
        if (qteState != QTEState.ACTIVE) return;

        String text = "PRESS  SPACE";
        hintLayout.setText(hintFont, text);

        // 🌬 呼吸动画（alpha）
        float pulse = 0.6f + 0.4f * MathUtils.sin(stateTime * 4f);

        hintFont.setColor(0.1f, 0.1f, 0.1f, pulse);

        float textX = camera.position.x - hintLayout.width / 2f;
        float textY = barY + BAR_HEIGHT + 9f; // 文字位置 在进度条上方

        batch.begin();
        hintFont.draw(batch, hintLayout, textX, textY);
        batch.end();
    }


    // =========================================================
    // 更新
    // =========================================================

    private void updatePlayerScreenPos() {
        playerX = playerGridX * cellSize;
        playerY = playerGridY * cellSize;
    }



    private void updateSuccess(float delta) {
        if (qteState == QTEState.SUCCESS_START) {
            successFreezeTimer += delta;
            if (successFreezeTimer >= 0.5f) {
                // 进入角色移动阶段
                qteState = QTEState.SUCCESS_MOVE;
                Logger.debug("QTE -> " + qteState);

                successTimer = 0f;
                successStartX = playerX;
                successTargetX = (playerGridX + 1) * cellSize;
            }
            return;
        }

        if (qteState == QTEState.SUCCESS_MOVE) {
            successTimer += delta;
            float t = Math.min(successTimer / SUCCESS_DURATION, 1f);
            playerX = MathUtils.lerp(successStartX, successTargetX, t);

            if (t >= 1f) {
                finishQTE(QTEResult.SUCCESS);
            }
        }
    }


    // =========================================================
    // 渲染
    // =========================================================

    @Override
    public void render(float delta) {
        updateQTE(delta);
        updateSuccess(delta);

        stateTime += delta * animationSpeed;

        // 相机跟随
        camera.position.set(
                playerX + cellSize / 2f,
                playerY + cellSize / 2f,
                0
        );
        camera.update();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        int[][] maze = QTEMazeData.MAZE2;
        int px = playerGridX;
        int py = playerGridY;

// 1️⃣ 地板
        mazeRenderer.renderFloor(batch, maze);

// 2️⃣ 后墙（y > 玩家）
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                if (maze[y][x] == 0 && y > py) {
                    mazeRenderer.renderWall(batch, x, y);
                }
            }
        }

// 3️⃣ 玩家
        drawPlayer();

// 4️⃣ 前墙（y <= 玩家）
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                if (maze[y][x] == 0 && y <= py) {
                    mazeRenderer.renderWall(batch, x, y);
                }
            }
        }


        batch.end();

        renderProgressBar(delta);

        renderPressSpaceHint();
    }

    private void renderProgressBar(float delta) {
        barWidth = camera.viewportWidth * BAR_WIDTH_RATIO;
        barX = camera.position.x - barWidth / 2f;
        barY = camera.position.y - camera.viewportHeight / 2f + BAR_Y_OFFSET;

        float target = (lockedProgress != null)
                ? lockedProgress
                : Math.min(1f, mashCount / (float) MASH_REQUIRED);

        displayedProgress += (target - displayedProgress) * 8f * delta;

        // ===== 唯一 begin =====
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // 1️⃣ 黑色描边
        shapeRenderer.setColor(0f, 0f, 0f, 1f);
        shapeRenderer.rect(
                barX - BAR_BORDER,
                barY - BAR_BORDER,
                barWidth + BAR_BORDER * 2,
                BAR_HEIGHT + BAR_BORDER * 2
        );

        // 2️⃣ 背景
        shapeRenderer.setColor(BAR_BG_COLOR);
        shapeRenderer.rect(barX, barY, barWidth, BAR_HEIGHT);

        // 3️⃣ 填充（渐变）
        if (qteState != QTEState.DONE) {
            drawCandyGradient(barX, barY, barWidth * displayedProgress);
            drawMetalHighlight(barX, barY, barWidth * displayedProgress);
            drawMetalEdges(barX, barY, barWidth * displayedProgress);
        }

        // 4️⃣ 粒子（⚠️ 不允许 begin）
        if (progressExploding) {
            renderExplosionParticles(delta);
        }

        // ===== 唯一 end =====
        shapeRenderer.end();
    }


    private void drawMetalEdges(float x, float y, float width) {
        // 上边缘亮
        shapeRenderer.setColor(1f, 1f, 1f, 0.18f);
        shapeRenderer.rect(x, y + BAR_HEIGHT - 1f, width, 1f);

        // 下边缘暗
        shapeRenderer.setColor(0f, 0f, 0f, 0.15f);
        shapeRenderer.rect(x, y, width, 1f);
    }

    private void drawCandyGradient(float x, float y, float width) {
        int steps = 16; // 越多越顺
        float sliceHeight = BAR_HEIGHT / steps;

        for (int i = 0; i < steps; i++) {
            float t = i / (float) (steps - 1);

            // 🌈 粉 → 黄 + 轻微流动
            float wave = 0.5f + 0.5f *
                    MathUtils.sin(stateTime * 3f + t * 6f);

            Color c = new Color(
                    MathUtils.lerp(BAR_PINK.r, BAR_YELLOW.r, wave),
                    MathUtils.lerp(BAR_PINK.g, BAR_YELLOW.g, wave),
                    MathUtils.lerp(BAR_PINK.b, BAR_YELLOW.b, wave),
                    1f
            );

            shapeRenderer.setColor(c);
            shapeRenderer.rect(
                    x,
                    y + i * sliceHeight,
                    width,
                    sliceHeight + 1f   // 防止缝隙
            );
        }
    }
    private void drawMetalHighlight(float x, float y, float width) {
        // 高光上下浮动
        float wave = 0.5f + 0.5f * MathUtils.sin(stateTime * 2f);
        float highlightY =
                y + BAR_HEIGHT * (0.25f + 0.3f * wave);

        shapeRenderer.setColor(1f, 1f, 1f, 0.22f);
        shapeRenderer.rect(
                x,
                highlightY,
                width,
                METAL_HIGHLIGHT_HEIGHT
        );
    }



    private void spawnProgressExplosion() {
        float barWidth = camera.viewportWidth * BAR_WIDTH_RATIO;
        float barX = camera.position.x - barWidth / 2f;
        float barY = camera.position.y
                - camera.viewportHeight / 2f
                + BAR_Y_OFFSET;

        // 🎯 粒子中心 = 进度条中心
        float cx = barX + barWidth * displayedProgress;
        float cy = barY + BAR_HEIGHT / 2f;
//粒子个数
        for (int i = 0; i < 100; i++) {
            ProgressParticle p = new ProgressParticle();
            p.x = cx;
            p.y = cy;

            float angle = MathUtils.random(0f, 360f);
            float speed = MathUtils.random(40f, 90f);

            p.vx = MathUtils.cosDeg(angle) * speed;
            p.vy = MathUtils.sinDeg(angle) * speed;
            p.life = MathUtils.random(0.5f, 0.8f);

            p.color = new Color(
                    MathUtils.random(0.6f, 1f),
                    MathUtils.random(0.6f, 1f),
                    MathUtils.random(0.6f, 1f),
                    1f
            );

            particles.add(p);
        }
    }
    private void renderExplosionParticles(float delta) {
        for (int i = particles.size - 1; i >= 0; i--) {
            ProgressParticle p = particles.get(i);
            p.life -= delta;

            if (p.life <= 0) {
                particles.removeIndex(i);
                continue;
            }

            p.x += p.vx * delta;
            p.y += p.vy * delta;
            p.vy -= 110 * delta;

            float sparkle = 0.6f + 0.4f *
                    MathUtils.sin(stateTime * 20f + p.life * 10f);

            shapeRenderer.setColor(
                    p.color.r * sparkle,
                    p.color.g * sparkle,
                    p.color.b * sparkle,
                    p.color.a * p.life
            );

            shapeRenderer.circle(p.x, p.y, 0.7f);
        }
    }




    private void drawPlayer() {
        float wobbleX = 0f;
        float wobbleY = 0f;

        if (qteState == QTEState.ACTIVE) {
            float wobble = Math.min(animationSpeed, 3f) * 1.2f;
            wobbleX =  MathUtils.sin(stateTime * 6f) * wobble;
            wobbleY =  MathUtils.cos(stateTime * 5f) * wobble * 0.5f;
        }


        TextureRegion frame =
                (qteState == QTEState.ACTIVE)
                        ? struggleAnim.getKeyFrame(stateTime)
                        : escapeFrame;

        batch.draw(
                frame,
                playerX + wobbleX,
                playerY + wobbleY,
                cellSize,
                cellSize
        );

    }

    private void updateQTE(float delta) {
        if (qteState != QTEState.ACTIVE) return;

        // 总时间限制
        qteTimer += delta;
        if (qteTimer >= QTE_TIME_LIMIT) {
            finishQTE(QTEResult.FAIL);
            return;
        }

        // 连打
        mashTimer += delta;

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            mashCount++;
        }

        if (mashCount >= MASH_REQUIRED) {
            enterSuccessStart();
        }

        if (mashTimer >= MASH_WINDOW && qteState == QTEState.ACTIVE) {
            mashCount = 0;
            mashTimer = 0f;
        }
    }

    private void enterSuccessStart() {
        if (qteState != QTEState.ACTIVE) return;

        qteState = QTEState.SUCCESS_START;
        Logger.debug("QTE -> " + qteState);

        lockedProgress = 1f;
        displayedProgress = 1f;

        progressExploding = true;
        spawnProgressExplosion();

        successFreezeTimer = 0f;

        Logger.debug("ENTER SUCCESS_START, displayedProgress=" + displayedProgress);
    }



    private void finishQTE(QTEResult result) {
        if (qteState == QTEState.DONE) return;

        qteState = QTEState.DONE;
        Logger.debug("QTE -> " + qteState);
        this.result = result;

        Gdx.app.postRunnable(() -> {
            game.onQTEFinished(result);
        });
    }





    // =========================================================
    // 其他
    // =========================================================

    @Override public void resize(int width, int height) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
    }
}
