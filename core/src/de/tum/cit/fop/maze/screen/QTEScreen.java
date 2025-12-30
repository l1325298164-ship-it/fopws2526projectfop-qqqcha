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
import de.tum.cit.fop.maze.effects.QTE.QTERippleManager;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.qte.QTEMazeData;
import de.tum.cit.fop.maze.qte.QTEMazeRenderer;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;
import de.tum.cit.fop.maze.qte.QTEResult;

/**
 * QTE Screen（MazeRenderer + 独立 Camera）
 */
public class QTEScreen implements Screen {



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
    // Countdown Font
    // =========================
    private BitmapFont countdownFont;
    private GlyphLayout countdownLayout = new GlyphLayout();


    // =========================
    // Maze Renderer
    // =========================
    private QTEMazeRenderer mazeRenderer;

    // =========================
    // 玩家格子坐标
    // =========================
    private int playerGridX = 3;
    private int playerGridY = 4;

    // =========================
    // 世界坐标
    // =========================
    private float playerWorldX;
    private float playerWorldY;
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
        PREPARE,
        ACTIVE,
        SUCCESS_START,   // 刚成功：爆炸 + 定格
        SUCCESS_MOVE,    // 角色跳出动画
        SUCCESS_STAY,    // 动画完成后的短暂停留
        DONE
    }
    // 添加停留计时器
    private float successStayTimer = 0f;
    // 延长移动时间
    private static final float SUCCESS_DURATION = 0.3f; // 从1秒延长到1.5秒

    // =========================
    // QTE 倒计时
    // =========================
    private static final float PREPARE_DURATION = 3f;
    private float prepareTimer = 0f;

    //初始状态
    private QTEState qteState = QTEState.PREPARE;

    // 连打
    private int mashCount = 0;
    private float mashTimer = 0f;
    private static final float MASH_WINDOW = 1.0f;
    private static final int MASH_REQUIRED = 5;

    private float animationSpeed = 1.0f;

    // 成功移动
    private float successTimer = 0f;
    private float successStartX;
    private float successTargetX;

    // =========================
    // de.tum.cit.fop.maze.entities.trap.Trap（QTE 陷阱）
    // =========================
    private int trapGridX;
    private int trapGridY;
    private float trapWorldX;
    private float trapWorldY;
    private TextureRegion trapRegion;

    // =========================
    // Progress Bar & Effects
    // =========================
    private Float lockedProgress = null; // null = 未锁定
    private float progress = 0f;        // 0 ~ 1
    private float displayedProgress = 0f; // 用于平滑动画
    private boolean progressExploding = false;

    // 🔥【新增】波纹管理器
    private QTERippleManager rippleManager;

    // 进度条背景（粉色，20% 透明）
    private static final Color BAR_BG_COLOR = new Color(1.0f, 0.4f, 0.7f, 0.2f);
    // 渐变用的两端颜色
    private static final Color BAR_PINK = new Color(1.0f, 0.45f, 0.75f, 1f);
    private static final Color BAR_YELLOW = new Color(1.0f, 0.95f, 0.4f, 1f);

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

        // 🔥【新增】初始化波纹管理器
        rippleManager = new QTERippleManager();

        // 👉 引导字体（先用默认，后期可换 TTF）
        hintFont = new BitmapFont();
        hintFont.setUseIntegerPositions(false);
        hintFont.getData().setScale(0.3f);
        hintFont.setColor(1f, 0.9f, 0.95f, 1f);

        //倒计时字体
        countdownFont = new BitmapFont(); // 先用默认
        countdownFont.setUseIntegerPositions(false);
        countdownFont.getData().setScale(0.9f);
        countdownFont.setColor(0f, 0f, 0f, 1f);
        countdownFont.getData().markupEnabled = false;

        TextureManager.getInstance().switchMode(TextureManager.TextureMode.IMAGE);

        cellSize = GameConstants.CELL_SIZE;

        // QTE 专用紧张镜头（只看 4x4 格子）
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 4* cellSize, 4 * cellSize);

        // MazeRenderer：直接用传进来的 GameManager
        mazeRenderer = new QTEMazeRenderer();

        // 设置迷宫尺寸（重要！）
        int[][] maze = QTEMazeData.MAZE2;
        mazeRenderer.setMazeDimensions(maze[0].length, maze.length);

        // 更新玩家世界坐标（使用倒置后的y坐标）
        updatePlayerWorldPos();

        camera.position.set(
                playerWorldX + cellSize / 2f,
                playerWorldY + cellSize / 2f,
                0
        );
        camera.update();

        System.out.println("QTE GameManager = " + gameManager);

        // 动画
        Array<TextureRegion> frames = new Array<>();
        for (int i = 0; i < 4; i++) {
            frames.add(new TextureRegion(
                    new com.badlogic.gdx.graphics.Texture("qte/player_struggle_00" + i + ".png")
            ));
        }
        struggleAnim = new Animation<>(0.15f, frames, Animation.PlayMode.LOOP);
        escapeFrame = new TextureRegion(
                new com.badlogic.gdx.graphics.Texture("qte/player_escape.png")
        );

        // =========================
        // QTE de.tum.cit.fop.maze.entities.trap.Trap - 固定在玩家初始位置
        // =========================
        trapRegion = new TextureRegion(
                new com.badlogic.gdx.graphics.Texture("qte/trap.png")
        );

        // 陷阱固定在玩家初始位置
        trapGridX = playerGridX-1;
        trapGridY = playerGridY;
        trapWorldX = trapGridX * cellSize;
        trapWorldY = mazeRenderer.getInvertedWorldY(trapGridY);
    }

    // =========================================================
    // 坐标更新方法
    // =========================================================

    private void updatePlayerWorldPos() {
        playerWorldX = playerGridX * cellSize;
        // 使用倒置后的y坐标
        playerWorldY = mazeRenderer.getInvertedWorldY(playerGridY);
    }

    // =========================================================
    // 渲染方法
    // =========================================================

    private void drawTrap() {
        if (trapRegion == null) return;
        // 放大比例
        float scale = 3.4f; // 放大陷阱
        float scaledSize = cellSize * scale;
        float offset = (cellSize - scaledSize) / 2f; // 居中偏移
        // 始终绘制在初始位置，但尺寸放大
        batch.draw(
                trapRegion,
                trapWorldX + offset,    // X位置居中
                trapWorldY + offset,    // Y位置居中
                scaledSize,
                scaledSize
        );
    }

    private void drawPlayer() {
        float wobbleX = 0f;
        float wobbleY = 0f;
        Color tintColor = new Color(1f, 1f, 1f, 1f);

        if (qteState == QTEState.ACTIVE || qteState == QTEState.PREPARE) {
            float wobble = Math.min(animationSpeed, 3f) * 1.2f;
            wobbleX = MathUtils.sin(stateTime * 6f) * wobble;
            wobbleY = MathUtils.cos(stateTime * 5f) * wobble * 0.5f;
        } else if (qteState == QTEState.SUCCESS_MOVE) {
            // 移动时添加特效
            float moveEffect = MathUtils.sin(stateTime * 20f) * 0.15f;
            wobbleY = moveEffect;
            tintColor = new Color(1f, 1f, 0.7f, 1f); // 金色

            // 移动时轻微变大
            float scale = 1f + MathUtils.sin(stateTime * 15f) * 0.1f;
            batch.setColor(tintColor);

            float renderX = playerWorldX;
            float renderY = mazeRenderer.getInvertedWorldY(playerGridY);
            float offset = (cellSize - cellSize * scale) / 2f;

            TextureRegion frame = escapeFrame; // 移动时使用逃脱帧

            batch.draw(
                    frame,
                    renderX + offset,
                    renderY + offset,
                    cellSize * scale,
                    cellSize * scale
            );

            batch.setColor(1f, 1f, 1f, 1f);
            return; // 提前返回，不使用下面的通用绘制
        } else if (qteState == QTEState.SUCCESS_STAY) {
            // 停留状态：使用逃脱帧，轻微呼吸效果
            float breathe = MathUtils.sin(stateTime * 3f) * 0.05f;
            float scale = 1f + breathe;
            tintColor = new Color(0.9f, 1f, 0.9f, 1f); // 淡绿色

            batch.setColor(tintColor);

            float renderX = playerWorldX;
            float renderY = mazeRenderer.getInvertedWorldY(playerGridY);
            float offset = (cellSize - cellSize * scale) / 2f;

            batch.draw(
                    escapeFrame,
                    renderX + offset,
                    renderY + offset,
                    cellSize * scale,
                    cellSize * scale
            );

            batch.setColor(1f, 1f, 1f, 1f);
            return; // 提前返回
        }

        TextureRegion frame =
                (qteState == QTEState.ACTIVE || qteState == QTEState.PREPARE)
                        ? struggleAnim.getKeyFrame(stateTime)
                        : escapeFrame;

        float renderX = playerWorldX + wobbleX;
        float renderY = mazeRenderer.getInvertedWorldY(playerGridY) + wobbleY;

        batch.draw(
                frame,
                renderX,
                renderY,
                cellSize,
                cellSize
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
        float textY = barY + BAR_HEIGHT + 9f; // 文字位置在进度条上方

        batch.begin();
        hintFont.draw(batch, hintLayout, textX, textY);
        batch.end();
    }

    // =========================================================
    // 更新逻辑
    // =========================================================

    private void updateSuccess(float delta) {
        if (qteState == QTEState.SUCCESS_START) {
            successFreezeTimer += delta;
            if (successFreezeTimer >= 0.5f) {
                // 进入角色移动阶段
                qteState = QTEState.SUCCESS_MOVE;
                Logger.debug("QTE -> SUCCESS_MOVE (开始移动动画)");

                successTimer = 0f;
                successStartX = playerWorldX;
                successTargetX = (playerGridX + 1) * cellSize; // 向右移动3格

                Logger.debug("移动动画开始: 从 " + playerGridX + " 到 " + (playerGridX + 1) +
                        ", 持续时间: " + SUCCESS_DURATION + "秒");
            }
            return;
        }

        if (qteState == QTEState.SUCCESS_MOVE) {
            successTimer += delta;
            float t = Math.min(successTimer / SUCCESS_DURATION, 1f);

//            // 使用缓动效果
//            t = 1f - (float)Math.pow(1f - t, 3); // easeOutCubic

            // 更新世界坐标
            playerWorldX = MathUtils.lerp(successStartX, successTargetX, t);

            if (t >= 1f) {
                // 移动动画完成，更新网格坐标
                playerGridX += 1;
                playerWorldX = successTargetX;
                updatePlayerWorldPos();

                Logger.debug("移动动画完成! 新位置: (" + playerGridX + ", " + playerGridY + ")");

                // 重要：不要立即结束QTE，让玩家在新位置停留一会儿
                // 添加一个短暂的停留时间
                qteState = QTEState.SUCCESS_STAY;
                successStayTimer = 0f;
                Logger.debug("QTE -> SUCCESS_STAY (停留展示)");
            }
        }

        // 处理停留状态
        if (qteState == QTEState.SUCCESS_STAY) {
            successStayTimer += delta;
            if (successStayTimer >= 0.8f) { // 停留0.8秒
                finishQTE(QTEResult.SUCCESS);
            }
        }
    }

    // =========================================================
    // 主渲染方法
    // =========================================================

    // 在QTEScreen中修改主渲染方法（最终方案）
    @Override
    public void render(float delta) {
        // 🔥【新增】波纹逻辑更新
        if (rippleManager != null) {
            rippleManager.update(delta);
        }

        updatePrepare(delta);
        updateQTE(delta);
        updateSuccess(delta);

        stateTime += delta * animationSpeed;

        // 更新玩家世界坐标
        updatePlayerWorldPos();

        // 相机跟随 - 添加平滑效果
        float targetX = playerWorldX + cellSize / 2f;
        float targetY = playerWorldY + cellSize / 2f;

        // 平滑跟随（移动时更快跟随）
        float followSpeed = (qteState == QTEState.SUCCESS_MOVE) ? 10f : 5f;
        camera.position.x += (targetX - camera.position.x) * followSpeed * delta;
        camera.position.y += (targetY - camera.position.y) * followSpeed * delta;

        camera.update();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 1️⃣ 渲染地板/墙壁/实体（背景层）
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        int[][] maze = QTEMazeData.MAZE2;

        // 渲染地板
        mazeRenderer.renderFloor(batch, maze);

        // 按行从下往上渲染
        // 从最下面一行开始（y=0），到最上面一行（y=maze.length-1）
        for (int y = 0; y < maze.length; y++) {
            // 渲染这一行的所有墙壁
            for (int x = 0; x < maze[y].length; x++) {
                if (maze[y][x] == 0) { // 墙壁
                    mazeRenderer.renderWall(batch, x, y);
                }
            }

            // 如果这一行是玩家所在的行，渲染陷阱和玩家
            if (y == playerGridY) {
                // 先渲染这一行中玩家左边的墙壁
                for (int x = 0; x < playerGridX; x++) {
                    if (maze[y][x] == 0) {
                        mazeRenderer.renderWall(batch, x, y);
                    }
                }

                // 渲染陷阱（在玩家位置）
                drawTrap();

                // 渲染玩家
                drawPlayer();

                // 渲染这一行中玩家右边的墙壁
                for (int x = playerGridX + 1; x < maze[y].length; x++) {
                    if (maze[y][x] == 0) {
                        mazeRenderer.renderWall(batch, x, y);
                    }
                }
            }
        }

        batch.end();

        // 2️⃣ 渲染波纹特效（在背景之上，UI之下）
        // 🔥【新增】调整层级：波纹先画，会被后面的 UI 黑框盖住中心
        shapeRenderer.setProjectionMatrix(camera.combined);
        if (rippleManager != null) {
            rippleManager.render(shapeRenderer);
        }

        // 3️⃣ 渲染 UI（进度条、文字）
        renderProgressBar(delta);
        renderPressSpaceHint();
        renderPrepareText();
    }

    // =========================================================
    // 进度条渲染（保持不变）
    // =========================================================

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

        // 4️⃣ 粒子
        if (progressExploding) {
            renderExplosionParticles(delta);
        }

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
        int steps = 16;
        float sliceHeight = BAR_HEIGHT / steps;

        for (int i = 0; i < steps; i++) {
            float t = i / (float) (steps - 1);
            float wave = 0.5f + 0.5f * MathUtils.sin(stateTime * 3f + t * 6f);
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
                    sliceHeight + 1f
            );
        }
    }

    private void drawMetalHighlight(float x, float y, float width) {
        float wave = 0.5f + 0.5f * MathUtils.sin(stateTime * 2f);
        float highlightY = y + BAR_HEIGHT * (0.25f + 0.3f * wave);
        shapeRenderer.setColor(1f, 1f, 1f, 0.22f);
        shapeRenderer.rect(x, highlightY, width, METAL_HIGHLIGHT_HEIGHT);
    }

    private void spawnProgressExplosion() {
        float barWidth = camera.viewportWidth * BAR_WIDTH_RATIO;
        float barX = camera.position.x - barWidth / 2f;
        float barY = camera.position.y - camera.viewportHeight / 2f + BAR_Y_OFFSET;

        // 🎯 粒子中心 = 进度条中心
        float cx = barX + barWidth * displayedProgress;
        float cy = barY + BAR_HEIGHT / 2f;

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

            float sparkle = 0.6f + 0.4f * MathUtils.sin(stateTime * 20f + p.life * 10f);
            shapeRenderer.setColor(
                    p.color.r * sparkle,
                    p.color.g * sparkle,
                    p.color.b * sparkle,
                    p.color.a * p.life
            );

            shapeRenderer.circle(p.x, p.y, 0.7f);
        }
    }

    // =========================================================
    // QTE 逻辑（保持不变）
    // =========================================================

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

            // 🔥【新增】播放波纹特效
            if (rippleManager != null) {
                // 计算进度条中心点（黑框的几何中心）
                float centerX = barX + barWidth / 2f;
                float centerY = barY + BAR_HEIGHT / 2f;

                rippleManager.spawnRipple(centerX, centerY);
            }
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
        Logger.debug("QTE -> DONE, 最终位置: (" + playerGridX + ", " + playerGridY + ")");
        this.result = result;

        Gdx.app.postRunnable(() -> {
            game.onQTEFinished(result);
        });
    }

    private void updatePrepare(float delta) {
        if (qteState != QTEState.PREPARE) return;

        prepareTimer += delta;
        if (prepareTimer >= PREPARE_DURATION) {
            // 倒计时结束，正式开始 QTE
            qteState = QTEState.ACTIVE;
            prepareTimer = 0f;

            // 清空所有 QTE 相关状态，确保"干净开局"
            qteTimer = 0f;
            mashCount = 0;
            mashTimer = 0f;
            displayedProgress = 0f;

            Logger.debug("QTE -> ACTIVE");
        }
    }

    private void renderPrepareText() {
        if (qteState != QTEState.PREPARE) return;

        String text;
        int second = 3 - (int) prepareTimer;

        switch (second) {
            case 3:
                text = "GET";
                break;
            case 2:
                text = "READY";
                break;
            default:
                text = "GO!";
                break;
        }

        countdownLayout.setText(countdownFont, text);

        // 中央 + 轻微缩放呼吸
        float pulse = 0.85f + 0.15f * MathUtils.sin(stateTime * 6f);
        countdownFont.setColor(0f, 0f, 0f, pulse);

        float x = camera.position.x - countdownLayout.width / 2f;
        float y = camera.position.y + countdownLayout.height / 2f;

        batch.begin();
        countdownFont.draw(batch, countdownLayout, x, y);
        batch.end();
    }

    // =========================================================
    // 其他生命周期方法
    // =========================================================

    @Override
    public void resize(int width, int height) {}

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
            shapeRenderer = null;
        }
        if (hintFont != null) {
            hintFont.dispose();
            hintFont = null;
        }
        if (countdownFont != null) {
            countdownFont.dispose();
            countdownFont = null;
        }
        // 🔥【新增】清理波纹管理器
        if (rippleManager != null) {
            rippleManager.dispose();
            rippleManager = null;
        }
    }
}