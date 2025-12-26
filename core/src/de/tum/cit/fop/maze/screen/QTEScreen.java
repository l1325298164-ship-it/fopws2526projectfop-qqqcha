package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.CameraManager;

public class QTEScreen implements Screen {

    private final MazeRunnerGame game;

    // =========================
    // 固定迷宫逻辑（不渲染）
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
    // 玩家格子坐标
    // =========================
    private int playerGridX = 1;
    private int playerGridY = 1;

    // =========================
    // 世界坐标（真实）
    // =========================
    private float playerWorldX;
    private float playerWorldY;

    // =========================
    // 渲染
    // =========================
    private SpriteBatch batch;
    private Texture background;
    private Texture trapTexture;

    private Animation<TextureRegion> struggleAnim;
    private TextureRegion escapeFrame;
    private float stateTime = 0f;

    // =========================
    // Camera
    // =========================
    private CameraManager cameraManager;
    private OrthographicCamera camera;

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
    private static final int MASH_REQUIRED = 12;

    private float animationSpeed = 1.0f;

    // =========================
    // 成功移动
    // =========================
    private static final float SUCCESS_DURATION = 0.4f;
    private float successTimer = 0f;

    public QTEScreen(MazeRunnerGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera.zoom = 0.7f;

        camera.position.set(
                camera.viewportWidth / 2f,
                camera.viewportHeight / 2f,
                0
        );
        camera.update();

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

        // === 世界坐标 ===
        float margin = GameConstants.CELL_SIZE * 0.5f;

        playerWorldX = margin;
        playerWorldY = margin;

        // === Camera ===
        cameraManager = new CameraManager();
        camera = cameraManager.getCamera();

        // 放大角色（数值越小越近）
        camera.zoom = 0.6f;

        camera.position.set(
                playerWorldX + GameConstants.CELL_SIZE / 2f,
                playerWorldY + GameConstants.CELL_SIZE / 2f,
                0
        );
        camera.update();
    }

    // =========================
    // Camera 只跟随真实坐标（不抖）
    // =========================
    private void updateCamera(float delta) {
        float targetX = playerWorldX + GameConstants.CELL_SIZE / 2f;
        float targetY = playerWorldY + GameConstants.CELL_SIZE / 2f;

        camera.position.x += (targetX - camera.position.x) * 6f * delta;
        camera.position.y += (targetY - camera.position.y) * 6f * delta;
        camera.update();
    }

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
            }
            mashCount = 0;
            mashTimer = 0f;
        }
    }

    private void updateSuccess(float delta) {
        if (qteState != QTEState.SUCCESS) return;

        successTimer += delta;
        float targetX = (playerGridX + 1) * GameConstants.CELL_SIZE;
        float speed = GameConstants.CELL_SIZE / SUCCESS_DURATION;

        playerWorldX = Math.min(playerWorldX + speed * delta, targetX);

        if (successTimer >= SUCCESS_DURATION) {
            playerGridX++;
            qteState = QTEState.DONE;
            // TODO: 切回 GameScreen
        }
    }

    @Override
    public void render(float delta) {
        updateQTE(delta);
        updateSuccess(delta);
//        updateCamera(delta);

        stateTime += delta * animationSpeed;

        Gdx.gl.glClearColor(0,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        // 背景（世界坐标铺）
        batch.draw(background,
                camera.position.x - camera.viewportWidth / 2,
                camera.position.y - camera.viewportHeight / 2,
                camera.viewportWidth,
                camera.viewportHeight
        );

        // 陷阱（不抖）
        batch.draw(trapTexture,
                playerWorldX,
                playerWorldY,
                GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE
        );

        // === 人物抖动 ===
        float wobble = Math.min(animationSpeed, 3f) * 1.5f;
        float wobbleX = (float)Math.sin(stateTime * 5f) * wobble;
        float wobbleY = (float)Math.cos(stateTime * 4f) * wobble * 0.6f;
        TextureRegion frame = (qteState == QTEState.ACTIVE)
                ? struggleAnim.getKeyFrame(stateTime)
                : escapeFrame;

        batch.draw(frame,
                playerWorldX + wobbleX,
                playerWorldY + wobbleY,
                GameConstants.CELL_SIZE* 1.8f,
                GameConstants.CELL_SIZE* 1.8f
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
