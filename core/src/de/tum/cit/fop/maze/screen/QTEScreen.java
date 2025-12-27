package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.MazeRunnerGame;

/**
 * 极简 QTE Screen（无 Camera）
 * - 左下角坐标
 * - 连打加速动画
 * - 成功后向右移动一格
 */
public class QTEScreen implements Screen {

    private final MazeRunnerGame game;
    private float trapX;
    private float trapY;

    // =========================
    // 迷宫逻辑尺寸（仅用于算格子）
    // =========================
    private static final int MAZE_WIDTH = 7;
    private static final int MAZE_HEIGHT = 7;

    // =========================
    // 玩家格子坐标（左下角）
    // =========================
    private int playerGridX = 1;
    private int playerGridY = 1;

    // =========================
    // 屏幕坐标
    // =========================
    private float playerX;
    private float playerY;
    private float cellSize;

    // =========================
    // 渲染
    // =========================
    private SpriteBatch batch;
    private Texture background;
    private Texture trapTexture;

    // 玩家动画
    private Animation<TextureRegion> struggleAnim;
    private TextureRegion escapeFrame;
    private float stateTime = 0f;

    public MazeRunnerGame getGame() {
        return game;
    }

    // =========================
    // QTE 状态
    // =========================
    private enum QTEState { ACTIVE, SUCCESS, DONE }
    private QTEState qteState = QTEState.ACTIVE;

    // =========================
    // 连打判定
    // =========================
    private int mashCount = 0;
    private float mashTimer = 0f;
    private static final float MASH_WINDOW = 1.0f;
    private static final int MASH_REQUIRED = 5;

    private float animationSpeed = 1.0f;

    // =========================
    // 成功移动
    // =========================
    private static final float SUCCESS_DURATION = 0.4f;
    private float successTimer = 0f;
    private float successStartX;
    private float successTargetX;

    public QTEScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        background = new Texture("qte/background.png");
        trapTexture = new Texture("qte/trap.png");

        // === 计算格子在屏幕中的大小 ===
        float cellW = Gdx.graphics.getWidth() / (float) MAZE_WIDTH;
        float cellH = Gdx.graphics.getHeight() / (float) MAZE_HEIGHT;
        cellSize = Math.min(cellW, cellH);

        updatePlayerScreenPos();
        trapX = playerX;
        trapY = playerY;

        // === 加载挣扎动画 ===
        Array<TextureRegion> frames = new Array<>();
        for (int i = 0; i < 4; i++) {
            frames.add(new TextureRegion(
                    new Texture("qte/player_struggle_00" + i + ".png")
            ));
        }
        struggleAnim = new Animation<>(0.15f, frames, Animation.PlayMode.LOOP);
        escapeFrame = new TextureRegion(new Texture("qte/player_escape.png"));
    }

    private void updatePlayerScreenPos() {
        playerX = playerGridX * cellSize;
        playerY = playerGridY * cellSize;
    }

    // =========================
    // QTE 更新
    // =========================
    private void updateQTE(float delta) {
        if (qteState != QTEState.ACTIVE) return;

        mashTimer += delta;

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            mashCount++;
        }

        if (mashTimer >= MASH_WINDOW) {
            animationSpeed = Math.min(3f, 1f + mashCount / 4f);

            if (mashCount >= MASH_REQUIRED) {
                qteState = QTEState.SUCCESS;
                successTimer = 0f;
                successStartX = playerX;
                successTargetX = (playerGridX + 1) * cellSize;
            }

            mashCount = 0;
            mashTimer = 0f;
        }
    }

    private void updateSuccess(float delta) {
        if (qteState != QTEState.SUCCESS) return;

        successTimer += delta;
        float t = Math.min(successTimer / SUCCESS_DURATION, 1f);

        playerX = successStartX + (successTargetX - successStartX) * t;

        if (t >= 1f) {
            playerGridX++;
            qteState = QTEState.DONE;
        }
    }

    @Override
    public void render(float delta) {
        updateQTE(delta);
        updateSuccess(delta);

        stateTime += delta * animationSpeed;

        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();

        // 背景铺满
        batch.draw(
                background,
                0, 0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );




        // 陷阱（固定）
        batch.draw(
                trapTexture,
                trapX,
                trapY,
                cellSize,
                cellSize
        );

        // 玩家抖动（仅人物）
        float wobble = Math.min(animationSpeed, 3f) * 1.2f;
        float wobbleX = (float)Math.sin(stateTime * 6f) * wobble;
        float wobbleY = (float)Math.cos(stateTime * 5f) * wobble * 0.5f;

        TextureRegion frame =
                (qteState == QTEState.ACTIVE)
                        ? struggleAnim.getKeyFrame(stateTime)
                        : escapeFrame;

        batch.draw(
                frame,
                playerX + wobbleX,
                playerY + wobbleY,
                cellSize * 1.6f,
                cellSize * 1.6f
        );

        batch.end();
    }

    @Override public void resize(int w, int h) {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        background.dispose();
        trapTexture.dispose();
    }
}
