package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;

import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.maze.MazeRenderer;
import de.tum.cit.fop.maze.qte.QTEMazeData;
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
    private static final float QTE_TIME_LIMIT = 3.0f;
    private float qteTimer = 0f;

    private final MazeRunnerGame game;
    private final GameManager gameManager;

    // =========================
    // Camera（QTE 专用）
    // =========================
    private OrthographicCamera camera;

    // =========================
    // Maze Renderer
    // =========================
    private MazeRenderer mazeRenderer;


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
    private enum QTEState { ACTIVE, SUCCESS, DONE }
    private QTEState qteState = QTEState.ACTIVE;

    // 连打
    private int mashCount = 0;
    private float mashTimer = 0f;
    private static final float MASH_WINDOW = 1.0f;
    private static final int MASH_REQUIRED = 5;

    private float animationSpeed = 1.0f;

    // 成功移动
    private static final float SUCCESS_DURATION = 0.4f;
    private float successTimer = 0f;
    private float successStartX;
    private float successTargetX;
    private float successStartY;
    private float successTargetY;


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
        mazeRenderer = new MazeRenderer(gameManager);
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

    // =========================================================
    // 更新
    // =========================================================

    private void updatePlayerScreenPos() {
        playerX = playerGridX * cellSize;
        playerY = playerGridY * cellSize;
    }



    private void updateSuccess(float delta) {
        if (qteState != QTEState.SUCCESS) return;

        successTimer += delta;
        float t = Math.min(successTimer / SUCCESS_DURATION, 1f);

        playerX = successStartX + (successTargetX - successStartX) * t;

        if (t >= 1f) {
            // ⛔ 成功后不再晃、不再更新
            animationSpeed = 1f;
            finishQTE(QTEResult.SUCCESS);
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

        // =========================
        // 固定 QTE 迷宫
        // =========================
        int[][] maze = QTEMazeData.MAZE2;

        int px = playerGridX;
        int py = playerGridY;

        // =========================
        // 1️⃣ 地板（全部）
        // =========================
        mazeRenderer.renderFloor(batch, maze);

        // =========================
        // 2️⃣ 后墙（y > 玩家）
        // =========================
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                if (maze[y][x] == 0 && y > py) {
                    mazeRenderer.renderWallAtPosition(batch, x, y, maze);
                }
            }
        }

        // =========================
        // 3️⃣ 玩家（抖动 + 动画）
        // =========================
        float wobbleX = 0f;
        float wobbleY = 0f;

        if (qteState == QTEState.ACTIVE) {
            float wobble = Math.min(animationSpeed, 3f) * 1.2f;
            wobbleX = (float) Math.sin(stateTime * 6f) * wobble;
            wobbleY = (float) Math.cos(stateTime * 5f) * wobble * 0.5f;
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

        // =========================
        // 4️⃣ 前墙（y <= 玩家）
        // =========================
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[y].length; x++) {
                if (maze[y][x] == 0 && y <= py) {
                    mazeRenderer.renderWallAtPosition(batch, x, y, maze);
                }
            }
        }

        batch.end();
    }

    private void updateQTE(float delta) {
        if (qteState != QTEState.ACTIVE) return;

        // =====================
        // 1️⃣ 总时间限制（失败）
        // =====================
        qteTimer += delta;
        if (qteTimer >= QTE_TIME_LIMIT) {
            finishQTE(QTEResult.FAIL);
            return;
        }

        // =====================
        // 2️⃣ 连打检测（成功）
        // =====================
        mashTimer += delta;

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            mashCount++;
        }

        if (mashTimer >= MASH_WINDOW) {

            // 动画加速反馈
            animationSpeed = Math.min(3f, 1f + mashCount / 4f);

            if (mashCount >= MASH_REQUIRED) {
                // ✅ 成功
                qteState = QTEState.SUCCESS;
                successTimer = 0f;
                successStartX = playerX;
                successTargetX = (playerGridY + 1) * cellSize;
                //成功后往上走一步，根据地图调整
//                successStartY = playerY;
//                successTargetY = (playerGridY + 1) * cellSize;

                // ⭐ 这里不要立刻跳转，让成功动画播完
                return;
            }

            // 重置连打窗口
            mashCount = 0;
            mashTimer = 0f;
        }
    }
    private void finishQTE(QTEResult result) {
        if (qteState == QTEState.DONE) return;

        qteState = QTEState.DONE;
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
