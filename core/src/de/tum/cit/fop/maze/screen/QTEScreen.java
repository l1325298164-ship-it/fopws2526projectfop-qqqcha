package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.MazeRunnerGame;

/**
 * 连打型 QTE Screen
 * 挣扎 → 成功 → 向右移动一格
 */
public class QTEScreen implements Screen {

    private final MazeRunnerGame game;

    // =========================
    // QTE 逻辑迷宫（仅占位）
    // =========================
    private static final int[][] QTE_MAZE = {
            {0,0,0,0,0,0,0},
            {0,1,1,1,1,0,0},
            {0,1,1,1,1,0,0},
            {0,0,0,1,1,0,0},
            {1,1,1,1,1,0,0},
            {1,1,1,1,1,1,1},
            {0,0,0,0,1,1,0},
    };

    // =========================
    // 玩家格子位置
    // =========================
    private int playerGridX = 1;
    private int playerGridY = 1;

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

    // =========================
    // 屏幕坐标
    // =========================
    private float playerScreenX;
    private float playerScreenY;
    private float drawSize;

    // =========================
    // QTE 状态
    // =========================
    private enum QTEState {
        ACTIVE,
        SUCCESS,
        DONE
    }

    private QTEState qteState = QTEState.ACTIVE;

    // =========================
    // 连打系统
    // =========================
    private int mashCount = 0;
    private float mashTimer = 0f;

    private static final float MASH_WINDOW = 1.0f;
    private static final int MASH_REQUIRED = 12;

    private float animationSpeed = 1.0f;

    // =========================
    // 奶油糖果风抖动
    // =========================
    private float wobbleTime = 0f;

    // =========================
    // 成功移动动画
    // =========================
    private float successAnimTime = 0f;
    private static final float SUCCESS_DURATION = 0.4f;

    private float successStartX;
    private float successEndX;

    public QTEScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        batch = new SpriteBatch();

        background = new Texture("qte/background.png");
        trapTexture = new Texture("qte/trap.png");

        // === 加载挣扎动画 ===
        Array<TextureRegion> frames = new Array<>();
        for (int i = 0; i < 4; i++) {
            frames.add(new TextureRegion(
                    new Texture("qte/player_struggle_00" + i + ".png")
            ));
        }

        struggleAnim = new Animation<>(0.15f, frames, Animation.PlayMode.LOOP);
        escapeFrame = new TextureRegion(new Texture("qte/player_escape.png"));

        // === 屏幕映射 ===
        float cellW = Gdx.graphics.getWidth() / (float) QTE_MAZE[0].length;
        float cellH = Gdx.graphics.getHeight() / (float) QTE_MAZE.length;
        drawSize = Math.min(cellW, cellH);

        updatePlayerScreenPosition();
    }

    // =========================
    // QTE 连打逻辑
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
                successAnimTime = 0f;
                successStartX = playerScreenX;
                successEndX = playerScreenX + drawSize;
            }

            mashCount = 0;
            mashTimer = 0f;
        }
    }

    // =========================
    // 成功移动动画
    // =========================
    private void updateSuccess(float delta) {
        if (qteState != QTEState.SUCCESS) return;

        successAnimTime += delta;
        float t = Math.min(successAnimTime / SUCCESS_DURATION, 1f);

        // smoothstep
        t = t * t * (3 - 2 * t);

        playerScreenX = successStartX + (successEndX - successStartX) * t;

        if (t >= 1f) {
            playerGridX++;
            qteState = QTEState.DONE;
            // TODO: game.exitQTE();
        }
    }

    private void updatePlayerScreenPosition() {
        playerScreenX = playerGridX * drawSize;
        playerScreenY = playerGridY * drawSize;
    }

    @Override
    public void render(float delta) {
        updateQTE(delta);
        updateSuccess(delta);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float drawX = playerScreenX;
        float drawY = playerScreenY;

        // === ACTIVE 阶段才抖动 ===
        if (qteState == QTEState.ACTIVE) {
            wobbleTime += delta * animationSpeed;

            float wobbleStrength = Math.min(animationSpeed, 3f) * 6f;
            float wobbleX = (float) Math.sin(wobbleTime * 8f) * wobbleStrength;
            float wobbleY = (float) Math.cos(wobbleTime * 6f) * wobbleStrength * 0.5f;

            drawX += wobbleX;
            drawY += wobbleY;

            float sinkOffset = Math.min(animationSpeed, 3f) * 3f;
            drawY -= sinkOffset;

            stateTime += delta * animationSpeed;
        }

        batch.begin();

        batch.draw(background, 0, 0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight());

        batch.draw(trapTexture, drawX, drawY, drawSize, drawSize);

        TextureRegion frame =
                (qteState == QTEState.ACTIVE)
                        ? struggleAnim.getKeyFrame(stateTime)
                        : escapeFrame;

        batch.draw(frame, drawX, drawY, drawSize, drawSize);

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
