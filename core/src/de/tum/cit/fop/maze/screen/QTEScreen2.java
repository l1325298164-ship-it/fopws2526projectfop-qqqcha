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
 * 双人QTE Screen
 */
public class QTEScreen2 implements Screen {


    private QTEResult result = null;

    // QTE时间限制
    private static final float QTE_TIME_LIMIT = 30.0f;
    private float qteTimer = 0f;

    private final MazeRunnerGame game;
    private final GameManager gameManager;

    // =========================
    // Camera（QTE 专用）
    // =========================
    private OrthographicCamera camera;

    // =========================
    // 双人QTE相关
    // =========================
    private enum PlayerType {
        PLAYER_A,  // 按空格键
        PLAYER_B   // 按回车键
    }

    private int playerAGridX = 3;
    private int playerAGridY = 4;
    private int playerBGridX = 5;  // 玩家B初始位置在玩家A右边
    private int playerBGridY = 4;

    private float playerAWorldX, playerAWorldY;
    private float playerBWorldX, playerBWorldY;

    // 玩家敲击计数
    private int mashCountA = 0;
    private int mashCountB = 0;
    private float mashTimer = 0f;

    // 连打参数
    private static final float MASH_WINDOW = 1.0f;
    private static final int MASH_REQUIRED_TOTAL = 15;  // 双人总需求
    private static final int MASH_MINIMUM_PER_PLAYER = 3; // 每人最低要求

    // =========================
    // 字体
    // =========================
    private BitmapFont hintFont;
    private GlyphLayout hintLayout = new GlyphLayout();

    private BitmapFont countdownFont;
    private GlyphLayout countdownLayout = new GlyphLayout();

    private BitmapFont playerStatsFont;

    // =========================
    // Maze Renderer
    // =========================
    private QTEMazeRenderer mazeRenderer;

    private float cellSize;

    // =========================
    // 渲染
    // =========================
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    // 动画
    private Animation<TextureRegion> struggleAnimA;  // 玩家A挣扎动画
    private Animation<TextureRegion> struggleAnimB;  // 玩家B挣扎动画
    private TextureRegion rescueFrameA;  // 玩家A被救出后的帧
    private TextureRegion rescueFrameB;  // 玩家B正常的帧

    private float stateTime = 0f;

    // =========================
    // QTE 状态
    // =========================
    private enum QTEState {
        PREPARE,
        ACTIVE,
        SUCCESS_START,   // 成功开始：爆炸+定格
        SUCCESS_MOVE,    // 玩家A移动到玩家B位置
        SUCCESS_STAY,    // 动画完成后的短暂停留
        DONE
    }

    private QTEState qteState = QTEState.PREPARE;
    private float successStayTimer = 0f;
    private static final float SUCCESS_DURATION = 0.3f;

    // =========================
    // 准备倒计时
    // =========================
    private static final float PREPARE_DURATION = 3f;
    private float prepareTimer = 0f;

    // =========================
    // 成功移动相关
    // =========================
    private float successTimer = 0f;
    private float successStartX;
    private float successTargetX;
    private float successStartY;
    private float successTargetY;
    private float successFreezeTimer = 0f;

    // =========================
    // 陷阱
    // =========================
    private TextureRegion trapRegion;
    private float trapWorldX, trapWorldY;

    // =========================
    // 进度条
    // =========================
    private Float lockedProgress = null;
    private float progress = 0f;
    private float displayedProgress = 0f;
    private boolean progressExploding = false;

    private QTERippleManager rippleManager;

    // 进度条颜色
    private static final Color BAR_BG_COLOR = new Color(1.0f, 0.4f, 0.7f, 0.2f);
    private static final Color BAR_PINK = new Color(1.0f, 0.45f, 0.75f, 1f);
    private static final Color BAR_YELLOW = new Color(1.0f, 0.95f, 0.4f, 1f);
    private static final Color PLAYER_A_COLOR = new Color(0.2f, 0.6f, 1f, 1f);    // 蓝色 - 玩家A
    private static final Color PLAYER_B_COLOR = new Color(1f, 0.4f, 0.2f, 1f);    // 橙色 - 玩家B

    // 进度条粒子
    private static class ProgressParticle {
        float x, y;
        float vx, vy;
        float life;
        Color color;
    }
    private Array<ProgressParticle> particles = new Array<>();

    // 进度条参数
    private static final float BAR_BORDER = 2f;
    private static final float METAL_HIGHLIGHT_HEIGHT = 3f;
    private static final float BAR_WIDTH_RATIO = 0.7f;
    private static final float BAR_HEIGHT = 14f;
    private static final float BAR_Y_OFFSET = 18f;

    private float barX, barY, barWidth;

    public QTEScreen2(MazeRunnerGame game, GameManager gameManager) {
        this.game = game;
        this.gameManager = gameManager;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // 初始化波纹管理器
        rippleManager = new QTERippleManager();

        // 初始化字体
        hintFont = new BitmapFont();
        hintFont.setUseIntegerPositions(false);
        hintFont.getData().setScale(0.25f);
        hintFont.setColor(1f, 0.9f, 0.95f, 1f);

        countdownFont = new BitmapFont();
        countdownFont.setUseIntegerPositions(false);
        countdownFont.getData().setScale(0.9f);
        countdownFont.setColor(0f, 0f, 0f, 1f);
        countdownFont.getData().markupEnabled = false;

        playerStatsFont = new BitmapFont();
        playerStatsFont.setUseIntegerPositions(false);
        playerStatsFont.getData().setScale(0.2f);

        TextureManager.getInstance().switchMode(TextureManager.TextureMode.IMAGE);

        cellSize = GameConstants.CELL_SIZE;

        // 设置相机
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 6 * cellSize, 4 * cellSize);

        // 初始化迷宫渲染器
        mazeRenderer = new QTEMazeRenderer();
        int[][] maze = QTEMazeData.MAZE2;
        mazeRenderer.setMazeDimensions(maze[0].length, maze.length);

        // 更新玩家世界坐标
        updatePlayerWorldPositions();

        // 设置相机初始位置（在两个玩家中间）
        float centerX = (playerAWorldX + playerBWorldX + cellSize) / 2f;
        camera.position.set(centerX, playerAWorldY + cellSize / 2f, 0);
        camera.update();

        // 加载动画
        loadAnimations();

        // 加载陷阱
        trapRegion = new TextureRegion(
                new com.badlogic.gdx.graphics.Texture("qte/trap.png")
        );

        // 陷阱在玩家A的位置
        trapWorldX = playerAGridX * cellSize;
        trapWorldY = mazeRenderer.getInvertedWorldY(playerAGridY);
    }

    private void loadAnimations() {
        // 玩家A的挣扎动画
        Array<TextureRegion> framesA = new Array<>();
        for (int i = 0; i < 4; i++) {
            framesA.add(new TextureRegion(
                    new com.badlogic.gdx.graphics.Texture("qte/playerA_struggle_00" + i + ".png")
            ));
        }
        struggleAnimA = new Animation<>(0.15f, framesA, Animation.PlayMode.LOOP);
        rescueFrameA = new TextureRegion(
                new com.badlogic.gdx.graphics.Texture("qte/playerA_rescue.png")
        );

        // 玩家B的挣扎动画
        Array<TextureRegion> framesB = new Array<>();
        for (int i = 0; i < 4; i++) {
            framesB.add(new TextureRegion(
                    new com.badlogic.gdx.graphics.Texture("qte/playerB_struggle_00" + i + ".png")
            ));
        }
        struggleAnimB = new Animation<>(0.15f, framesB, Animation.PlayMode.LOOP);
        rescueFrameB = new TextureRegion(
                new com.badlogic.gdx.graphics.Texture("qte/playerB_normal.png")
        );
    }

    private void updatePlayerWorldPositions() {
        playerAWorldX = playerAGridX * cellSize;
        playerAWorldY = mazeRenderer.getInvertedWorldY(playerAGridY);

        playerBWorldX = playerBGridX * cellSize;
        playerBWorldY = mazeRenderer.getInvertedWorldY(playerBGridY);
    }

    @Override
    public void render(float delta) {
        // 更新波纹管理器
        if (rippleManager != null) {
            rippleManager.update(delta);
        }

        updatePrepare(delta);
        updateQTE(delta);
        updateSuccess(delta);

        stateTime += delta;

        // 更新玩家世界坐标
        updatePlayerWorldPositions();

        // 相机平滑跟随（两个玩家中间位置）
        float targetX = (playerAWorldX + playerBWorldX + cellSize) / 2f;
        float targetY = (playerAWorldY + playerBWorldY + cellSize) / 2f;

        float followSpeed = (qteState == QTEState.SUCCESS_MOVE) ? 10f : 5f;
        camera.position.x += (targetX - camera.position.x) * followSpeed * delta;
        camera.position.y += (targetY - camera.position.y) * followSpeed * delta;
        camera.update();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 1️⃣ 渲染迷宫背景
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        int[][] maze = QTEMazeData.MAZE2;

        // 渲染地板
        mazeRenderer.renderFloor(batch, maze);

        // 渲染墙壁
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                if (maze[y][x] == 0) {
                    mazeRenderer.renderWall(batch, x, y);
                }
            }
        }

        // 渲染陷阱（在玩家A位置）
        drawTrap();

        // 渲染玩家
        drawPlayers();

        batch.end();

        // 2️⃣ 渲染波纹特效
        shapeRenderer.setProjectionMatrix(camera.combined);
        if (rippleManager != null) {
            rippleManager.render(shapeRenderer);
        }

        // 3️⃣ 渲染UI
        renderProgressBar(delta);
        renderPlayerStats();
        renderPressHint();
        renderPrepareText();
    }

    private void drawTrap() {
        if (trapRegion == null) return;

        float scale = 3.4f;
        float scaledSize = cellSize * scale;
        float offset = (cellSize - scaledSize) / 2f;

        batch.draw(
                trapRegion,
                trapWorldX + offset,
                trapWorldY + offset,
                scaledSize,
                scaledSize
        );
    }

    private void drawPlayers() {
        // 渲染玩家B（救援者）
        drawPlayerB();

        // 渲染玩家A（被救援者）
        drawPlayerA();
    }

    private void drawPlayerA() {
        float wobbleX = 0f;
        float wobbleY = 0f;
        Color tintColor = new Color(1f, 1f, 1f, 1f);

        if (qteState == QTEState.ACTIVE || qteState == QTEState.PREPARE) {
            float wobble = Math.min(1.0f, 3f) * 1.2f;
            wobbleX = MathUtils.sin(stateTime * 6f) * wobble;
            wobbleY = MathUtils.cos(stateTime * 5f) * wobble * 0.5f;
        } else if (qteState == QTEState.SUCCESS_MOVE) {
            // 移动特效
            float moveEffect = MathUtils.sin(stateTime * 20f) * 0.15f;
            wobbleY = moveEffect;
            tintColor = new Color(1f, 1f, 0.7f, 1f);

            float scale = 1f + MathUtils.sin(stateTime * 15f) * 0.1f;
            batch.setColor(tintColor);

            float offset = (cellSize - cellSize * scale) / 2f;
            TextureRegion frame = rescueFrameA;

            batch.draw(
                    frame,
                    playerAWorldX + offset,
                    playerAWorldY + offset,
                    cellSize * scale,
                    cellSize * scale
            );

            batch.setColor(1f, 1f, 1f, 1f);
            return;
        } else if (qteState == QTEState.SUCCESS_STAY) {
            // 停留状态
            float breathe = MathUtils.sin(stateTime * 3f) * 0.05f;
            float scale = 1f + breathe;
            tintColor = new Color(0.9f, 1f, 0.9f, 1f);

            batch.setColor(tintColor);
            float offset = (cellSize - cellSize * scale) / 2f;

            batch.draw(
                    rescueFrameA,
                    playerAWorldX + offset,
                    playerAWorldY + offset,
                    cellSize * scale,
                    cellSize * scale
            );

            batch.setColor(1f, 1f, 1f, 1f);
            return;
        }

        TextureRegion frame = (qteState == QTEState.ACTIVE || qteState == QTEState.PREPARE)
                ? struggleAnimA.getKeyFrame(stateTime)
                : rescueFrameA;

        batch.draw(frame, playerAWorldX, playerAWorldY, cellSize, cellSize);
    }

    private void drawPlayerB() {
        // 玩家B始终显示正常/挣扎状态
        TextureRegion frame = (qteState == QTEState.ACTIVE || qteState == QTEState.PREPARE)
                ? struggleAnimB.getKeyFrame(stateTime + 0.5f) // 偏移时间让动画不同步
                : rescueFrameB;

        // 如果是活跃状态，给玩家B添加轻微动画
        float wobbleX = 0f;
        float wobbleY = 0f;

        if (qteState == QTEState.ACTIVE) {
            wobbleX = MathUtils.sin(stateTime * 4f) * 0.5f;
            wobbleY = MathUtils.cos(stateTime * 3f) * 0.3f;
        }

        batch.draw(
                frame,
                playerBWorldX + wobbleX,
                playerBWorldY + wobbleY,
                cellSize,
                cellSize
        );
    }

    private void updatePrepare(float delta) {
        if (qteState != QTEState.PREPARE) return;

        prepareTimer += delta;
        if (prepareTimer >= PREPARE_DURATION) {
            qteState = QTEState.ACTIVE;
            prepareTimer = 0f;

            // 重置所有QTE状态
            qteTimer = 0f;
            mashCountA = 0;
            mashCountB = 0;
            mashTimer = 0f;
            displayedProgress = 0f;

            Logger.debug("双人QTE -> ACTIVE");
        }
    }

    private void updateQTE(float delta) {
        if (qteState != QTEState.ACTIVE) return;

        // 时间限制
        qteTimer += delta;
        if (qteTimer >= QTE_TIME_LIMIT) {
            checkFailCondition();
            return;
        }

        // 连打窗口
        mashTimer += delta;

        // 玩家A输入（空格）
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            mashCountA++;
            spawnRippleForPlayer(PlayerType.PLAYER_A);
            Logger.debug("玩家A敲击: " + mashCountA);
        }

        // 玩家B输入（回车）
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            mashCountB++;
            spawnRippleForPlayer(PlayerType.PLAYER_B);
            Logger.debug("玩家B敲击: " + mashCountB);
        }

        // 检查成功条件
        int totalMash = mashCountA + mashCountB;
        if (mashCountA >= MASH_MINIMUM_PER_PLAYER &&
                mashCountB >= MASH_MINIMUM_PER_PLAYER &&
                totalMash >= MASH_REQUIRED_TOTAL) {
            enterSuccessStart();
        }

        // 重置连打窗口
        if (mashTimer >= MASH_WINDOW) {
            mashCountA = 0;
            mashCountB = 0;
            mashTimer = 0f;
        }
    }

    private void checkFailCondition() {
        int totalMash = mashCountA + mashCountB;

        // 失败条件：
        // 1. 时间到了，但总次数不足
        // 2. 或者任何一个玩家没达到最低要求
        if (totalMash < MASH_REQUIRED_TOTAL ||
                mashCountA < MASH_MINIMUM_PER_PLAYER ||
                mashCountB < MASH_MINIMUM_PER_PLAYER) {
            finishQTE(QTEResult.FAIL);
        } else {
            // 时间到了但满足条件也算成功
            enterSuccessStart();
        }
    }

    private void spawnRippleForPlayer(PlayerType player) {
        if (rippleManager == null) return;

        // 根据玩家类型在对应位置生成波纹
        float centerX, centerY;

        if (player == PlayerType.PLAYER_A) {
            centerX = barX + barWidth * (0.25f); // 左边1/4处
        } else {
            centerX = barX + barWidth * (0.75f); // 右边3/4处
        }

        centerY = barY + BAR_HEIGHT / 2f;

        rippleManager.spawnRipple(centerX, centerY);
    }

    private void enterSuccessStart() {
        if (qteState != QTEState.ACTIVE) return;

        qteState = QTEState.SUCCESS_START;
        Logger.debug("双人QTE -> SUCCESS_START");

        lockedProgress = 1f;
        displayedProgress = 1f;

        progressExploding = true;
        spawnProgressExplosion();

        successFreezeTimer = 0f;
    }

    private void updateSuccess(float delta) {
        if (qteState == QTEState.SUCCESS_START) {
            successFreezeTimer += delta;
            if (successFreezeTimer >= 0.5f) {
                qteState = QTEState.SUCCESS_MOVE;
                Logger.debug("双人QTE -> SUCCESS_MOVE");

                successTimer = 0f;
                successStartX = playerAWorldX;
                successStartY = playerAWorldY;
                successTargetX = playerBWorldX;
                successTargetY = playerBWorldY;
            }
            return;
        }

        if (qteState == QTEState.SUCCESS_MOVE) {
            successTimer += delta;
            float t = Math.min(successTimer / SUCCESS_DURATION, 1f);

            // 更新玩家A位置（向玩家B移动）
            playerAWorldX = MathUtils.lerp(successStartX, successTargetX, t);
            playerAWorldY = MathUtils.lerp(successStartY, successTargetY, t);

            if (t >= 1f) {
                // 更新网格坐标
                playerAGridX = playerBGridX;
                playerAGridY = playerBGridY;
                updatePlayerWorldPositions();

                qteState = QTEState.SUCCESS_STAY;
                successStayTimer = 0f;
                Logger.debug("双人QTE -> SUCCESS_STAY");
            }
        }

        if (qteState == QTEState.SUCCESS_STAY) {
            successStayTimer += delta;
            if (successStayTimer >= 0.8f) {
                finishQTE(QTEResult.SUCCESS);
            }
        }
    }

    private void renderProgressBar(float delta) {
        barWidth = camera.viewportWidth * BAR_WIDTH_RATIO;
        barX = camera.position.x - barWidth / 2f;
        barY = camera.position.y - camera.viewportHeight / 2f + BAR_Y_OFFSET;

        // 计算进度（两个玩家的总进度）
        float target;
        if (lockedProgress != null) {
            target = lockedProgress;
        } else {
            int totalMash = mashCountA + mashCountB;
            target = Math.min(1f, totalMash / (float) MASH_REQUIRED_TOTAL);

            // 检查是否满足最低要求
            float minA = Math.min(1f, mashCountA / (float) MASH_MINIMUM_PER_PLAYER);
            float minB = Math.min(1f, mashCountB / (float) MASH_MINIMUM_PER_PLAYER);
            float minProgress = Math.min(minA, minB);
            target = Math.min(target, minProgress);
        }

        displayedProgress += (target - displayedProgress) * 8f * delta;

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

        // 3️⃣ 填充（双色渐变）
        if (qteState != QTEState.DONE) {
            drawDualPlayerGradient(barX, barY, barWidth * displayedProgress);
            drawMetalHighlight(barX, barY, barWidth * displayedProgress);
            drawMetalEdges(barX, barY, barWidth * displayedProgress);

            // 绘制玩家分隔线
            drawPlayerSeparator(barX, barY);
        }

        // 4️⃣ 粒子
        if (progressExploding) {
            renderExplosionParticles(delta);
        }

        shapeRenderer.end();
    }

    private void drawDualPlayerGradient(float x, float y, float width) {
        int steps = 16;
        float sliceHeight = BAR_HEIGHT / steps;

        for (int i = 0; i < steps; i++) {
            float t = i / (float) (steps - 1);
            float wave = 0.5f + 0.5f * MathUtils.sin(stateTime * 3f + t * 6f);

            // 根据位置选择颜色（左半边玩家A，右半边玩家B）
            Color baseColor;
            float progressPos = width / (barWidth * displayedProgress);

            if (progressPos < 0.5f) {
                // 玩家A区域（蓝色渐变）
                baseColor = PLAYER_A_COLOR;
            } else {
                // 玩家B区域（橙色渐变）
                baseColor = PLAYER_B_COLOR;
            }

            // 添加动态效果
            Color c = new Color(
                    baseColor.r * (0.8f + 0.2f * wave),
                    baseColor.g * (0.8f + 0.2f * wave),
                    baseColor.b * (0.8f + 0.2f * wave),
                    1f
            );

            shapeRenderer.setColor(c);
            shapeRenderer.rect(x, y + i * sliceHeight, width, sliceHeight + 1f);
        }
    }

    private void drawPlayerSeparator(float x, float y) {
        // 在进度条中间画一条细线分隔两个玩家
        float separatorX = x + barWidth / 2f;
        shapeRenderer.setColor(1f, 1f, 1f, 0.5f);
        shapeRenderer.rect(separatorX - 0.5f, y - BAR_BORDER, 1f, BAR_HEIGHT + BAR_BORDER * 2);
    }

    private void drawMetalEdges(float x, float y, float width) {
        shapeRenderer.setColor(1f, 1f, 1f, 0.18f);
        shapeRenderer.rect(x, y + BAR_HEIGHT - 1f, width, 1f);

        shapeRenderer.setColor(0f, 0f, 0f, 0.15f);
        shapeRenderer.rect(x, y, width, 1f);
    }

    private void drawMetalHighlight(float x, float y, float width) {
        float wave = 0.5f + 0.5f * MathUtils.sin(stateTime * 2f);
        float highlightY = y + BAR_HEIGHT * (0.25f + 0.3f * wave);
        shapeRenderer.setColor(1f, 1f, 1f, 0.22f);
        shapeRenderer.rect(x, highlightY, width, METAL_HIGHLIGHT_HEIGHT);
    }

    private void renderPlayerStats() {
        if (qteState != QTEState.ACTIVE) return;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 玩家A统计（左边）
        String playerAText = "PLAYER A: " + mashCountA + "/" + MASH_MINIMUM_PER_PLAYER;
        playerStatsFont.setColor(PLAYER_A_COLOR);
        float playerAX = barX;
        float playerAY = barY + BAR_HEIGHT + 5f;
        playerStatsFont.draw(batch, playerAText, playerAX, playerAY);

        // 玩家B统计（右边）
        String playerBText = "PLAYER B: " + mashCountB + "/" + MASH_MINIMUM_PER_PLAYER;
        playerStatsFont.setColor(PLAYER_B_COLOR);
        float playerBX = barX + barWidth - getTextWidth(playerStatsFont, playerBText);
        float playerBY = barY + BAR_HEIGHT + 5f;
        playerStatsFont.draw(batch, playerBText, playerBX, playerBY);

        // 总需求
        String totalText = "TOTAL: " + (mashCountA + mashCountB) + "/" + MASH_REQUIRED_TOTAL;
        playerStatsFont.setColor(1f, 1f, 1f, 1f);
        float totalX = barX + (barWidth - getTextWidth(playerStatsFont, totalText)) / 2f;
        float totalY = barY - 5f;
        playerStatsFont.draw(batch, totalText, totalX, totalY);

        batch.end();
    }
    private float getTextWidth(BitmapFont font, String text) {
        GlyphLayout layout = new GlyphLayout();
        layout.setText(font, text);
        return layout.width;
    }

    private void renderPressHint() {
        if (qteState != QTEState.ACTIVE) return;

        String text = "PLAYER A: SPACE  |  PLAYER B: ENTER";
        hintLayout.setText(hintFont, text);

        float pulse = 0.6f + 0.4f * MathUtils.sin(stateTime * 4f);
        hintFont.setColor(0.1f, 0.1f, 0.1f, pulse);

        float textX = camera.position.x - hintLayout.width / 2f;
        float textY = barY + BAR_HEIGHT + 15f; // 在玩家统计上方

        batch.begin();
        hintFont.draw(batch, hintLayout, textX, textY);
        batch.end();
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

        float pulse = 0.85f + 0.15f * MathUtils.sin(stateTime * 6f);
        countdownFont.setColor(0f, 0f, 0f, pulse);

        float x = camera.position.x - countdownLayout.width / 2f;
        float y = camera.position.y + countdownLayout.height / 2f;

        batch.begin();
        countdownFont.draw(batch, countdownLayout, x, y);
        batch.end();
    }

    private void spawnProgressExplosion() {
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

            // 随机选择玩家A或B的颜色
            if (MathUtils.randomBoolean()) {
                p.color = new Color(PLAYER_A_COLOR);
            } else {
                p.color = new Color(PLAYER_B_COLOR);
            }

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

    private void finishQTE(QTEResult result) {
        if (qteState == QTEState.DONE) return;

        qteState = QTEState.DONE;
        Logger.debug("双人QTE -> DONE, 结果: " + result);
        this.result = result;

        Gdx.app.postRunnable(() -> {
            game.onQTEFinished(result);
        });
    }

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
        if (playerStatsFont != null) {
            playerStatsFont.dispose();
            playerStatsFont = null;
        }
        if (rippleManager != null) {
            rippleManager.dispose();
            rippleManager = null;
        }
    }
}