package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.effects.portal.PortalEffectManager;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.entities.Player;


public class MazeGameTutorialScreen implements Screen {


    private Viewport viewport;

    private Pixmap mazeMask;
    private Texture mazeTexture; // 如果你要画背景图
    private boolean debugShowMask = false;
    // === Tutorial Player ===
    private Player tutorialPlayer;
    private float playerX;
    private float playerY;

    public enum MazeGameTutorialResult {
        SUCCESS,
        FAILURE_DEAD,
        EXIT_BY_PLAYER
    }

    private final MazeRunnerGame game;
    private final DifficultyConfig config;
    private GameManager gm;
    private Texture backgroundTexture;
    // === EXIT 提示文字 ===
    private float exitHintTimer = 0f;

    private OrthographicCamera camera;
    private OrthographicCamera hudCamera;
    private ShapeRenderer shapeRenderer;
    // === Player Foot Offset ===
// 美术脚底离贴图底部的偏移（像素）
// 你可以慢慢微调这个值
    private static final float PLAYER_FOOT_OFFSET = 30f;
    private static final float PLAYER_BODY_OFFSET = 20f;
    private static final float PORTAL_Y_OFFSET = 30f;
    // === Tutorial Player Halo ===
    private static final float PLAYER_HALO_RADIUS = 26f;
    private static final float PLAYER_HALO_ALPHA_BASE = 0.38f;


    private boolean finished = false;
    private boolean movedUp, movedDown, movedLeft, movedRight, usedShift; // 增加 usedShift
    private float shiftTimer; // 用于检测是否按够了时长
    private boolean reachedTarget = false;
    private static final float WALK_SPEED = 220f;   // 普通移动速度
    private static final float SPRINT_SPEED = 420f; // 冲刺速度
    // Fixed maze dimensions
    private static final int MAZE_WIDTH = 30;
    private static final int MAZE_HEIGHT = 20;
    private static final float CELL_SIZE = 32f;
    // === GOAL ===
    private Texture goalTexture;
    private final GlyphLayout glyphLayout = new GlyphLayout();

    // 手调终点坐标（屏幕坐标系）
    private float goalX = 1200f;
    private float goalY = 300f;
    private PortalEffectManager goalPortal;
    // 判定半径（可调，越大越宽松）
    private float goalRadius = 40f;


    // Player start position and target position
    private final int targetX = 25;
    private final int targetY = 10;

    // Input handling
    private boolean upPressed, downPressed, leftPressed, rightPressed;
    private float upTimer, downTimer, leftTimer, rightTimer;

    public MazeGameTutorialScreen(MazeRunnerGame game, DifficultyConfig config) {
        this.game = game;
        this.config = config;
        viewport = new ScreenViewport();
        viewport.apply(true);
    }

    @Override
    public void show() {
        viewport = new ScreenViewport();
        viewport.apply(true);

        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );

        shapeRenderer = new ShapeRenderer();


        System.out.println("=== TUTORIAL START ===");
        objectives.clear();

        objectives.add(new ObjectiveItem("Move Up"));
        objectives.add(new ObjectiveItem("Move Down"));
        objectives.add(new ObjectiveItem("Move Left"));
        objectives.add(new ObjectiveItem("Move Right"));
        objectives.add(new ObjectiveItem("SHIFT Sprint"));
        objectives.add(new ObjectiveItem("🎯 Reach Exit"));






        mazeTexture = new Texture(
                Gdx.files.internal("ui/tutorial_bg.png")
        );

        mazeMask = new Pixmap(
                Gdx.files.internal("ui/tutorial_mask.png")
        );
        mazeMask = new Pixmap(Gdx.files.internal("ui/tutorial_mask.png"));
        goalTexture = new Texture(Gdx.files.internal("ui/goal_icon.png"));
        findSpawnByCode(); // 注意：这里用的是「屏幕坐标」

        tutorialPlayer = new Player(0, 0, null); // GameManager 在 Tutorial 里用不到
        tutorialPlayer.setPosition(0, 0);

        // 同步屏幕坐标 → Player world 坐标
        syncPlayerToEntity();


        
        goalPortal = new PortalEffectManager(PortalEffectManager.PortalOwner.DOOR);

        shapeRenderer = new ShapeRenderer();

        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );



        System.out.println("Tutorial Objective: Use WASD or Arrow Keys to move, reach the green target");
        System.out.println("Player Start: (" + playerX + ", " + playerY + ")");
        System.out.println("Target Position: (" + targetX + ", " + targetY + ")");
        System.out.println("Game Stage: STORY_MAZE_GAME_TUTORIAL");
    }

    private void syncPlayerToEntity() {
        tutorialPlayer.setWorldPosition(
                playerX / GameConstants.CELL_SIZE,
                playerY / GameConstants.CELL_SIZE
        );
    }


    private void renderPlayerHalo(float delta) {
        // 呼吸动画
        float pulse = 1.0f + 0.08f * (float) Math.sin(exitHintTimer * 3.0f);

        // 光圈中心 = 玩家脚底
        float haloX = playerX + PLAYER_BODY_OFFSET;
        float haloY = playerY + PLAYER_FOOT_OFFSET;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // 外圈（柔光）
        shapeRenderer.setColor(1.0f, 0.96f, 0.78f, PLAYER_HALO_ALPHA_BASE * 0.6f);
        shapeRenderer.circle(
                haloX,
                haloY,
                PLAYER_HALO_RADIUS * pulse
        );

        // 内圈（亮一点）
        shapeRenderer.setColor(1.0f, 0.93f, 0.65f, PLAYER_HALO_ALPHA_BASE);
        shapeRenderer.circle(
                haloX,
                haloY,
                PLAYER_HALO_RADIUS * 0.65f * pulse
        );

        shapeRenderer.end();
    }


    @Override
    public void render(float delta) {
        viewport.apply();
        game.getSpriteBatch().setProjectionMatrix(
                viewport.getCamera().combined
        );

        handleInput();
        update(delta);

        debugShowMask = Gdx.input.isKeyPressed(Input.Keys.M);
        if (Gdx.input.isKeyPressed(Input.Keys.F)) {
            shapeRenderer.setProjectionMatrix(hudCamera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.GREEN);
            shapeRenderer.circle(goalX, goalY, goalRadius);
            shapeRenderer.end();
        }

        game.getSpriteBatch().begin();
        game.getSpriteBatch().draw(
                mazeTexture,
                0,
                0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );
        game.getSpriteBatch().end();

        // === Tutorial 专属：全屏暗化滤镜（正确版） ===
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

// ⚠️ 颜色其实一直是对的，只是之前 alpha 没生效
        shapeRenderer.setColor(0.05f, 0.08f, 0.12f, 0.48f);

        shapeRenderer.rect(
                0,
                0,
                Gdx.graphics.getWidth(),
                Gdx.graphics.getHeight()
        );

        shapeRenderer.end();


        renderMaskDebugOverlay();
        // 2️⃣ 输入 & 逻辑

        game.getSpriteBatch().begin();

// 终点图标居中绘制
        float size = 64f;
        game.getSpriteBatch().draw(
                goalTexture,
                goalX - size / 2f,
                goalY - size / 2f+PORTAL_Y_OFFSET,
                size,
                size
        );
        game.getSpriteBatch().end();
        game.getSpriteBatch().begin();

// 保存旧矩阵
        Matrix4 oldMatrix = new Matrix4(game.getSpriteBatch().getTransformMatrix());

// 🔥 放大 10 倍（只影响 portal）
        Matrix4 scaled = new Matrix4(oldMatrix);
        scaled.scale(3f, 3f, 1f); // ⚠️ 不是 10，而是视觉 10
        game.getSpriteBatch().setTransformMatrix(scaled);
        float portalDrawX = goalX;
        float portalDrawY = goalY + PORTAL_Y_OFFSET;
// ⚠️ 坐标要反缩放
        goalPortal.renderBack(
                game.getSpriteBatch(),
                portalDrawX / 3.2f,
                portalDrawY / 3.2f
        );

        game.getSpriteBatch().setTransformMatrix(oldMatrix);
        game.getSpriteBatch().end();


        renderPlayerHalo(delta);


        // 3️⃣ 玩家
        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        game.getSpriteBatch().begin();

        tutorialPlayer.drawSprite(game.getSpriteBatch());

        game.getSpriteBatch().end();

        game.getSpriteBatch().begin();
        goalPortal.renderFront(game.getSpriteBatch());
        game.getSpriteBatch().end();

// === EXIT HERE 发光提示 ===
        game.getSpriteBatch().begin();

        var font = game.getSkin().getFont("default-font");

// 上下浮动
        float floatOffset = (float) Math.sin(exitHintTimer * 2.0f) * 6f;

// 基础位置
        float textX = goalX;
        float textY = goalY + PORTAL_Y_OFFSET - 50f + floatOffset;

        String text = "EXIT HERE";

// 计算宽度（只算一次）
        glyphLayout.setText(font, text);
        float textW = glyphLayout.width;

// ===== 1️⃣ 光晕层（多次叠加）=====
        font.setColor(0.6f, 0.85f, 1.0f, 0.18f); // 淡蓝白光

        float glowRadius = 3f; // 光晕扩散半径
        for (int i = 0; i < 6; i++) {
            float angle = i * 60f * MathUtils.degreesToRadians;
            float ox = MathUtils.cos(angle) * glowRadius;
            float oy = MathUtils.sin(angle) * glowRadius;

            font.draw(
                    game.getSpriteBatch(),
                    text,
                    textX - textW / 2f + ox,
                    textY + oy
            );
        }

// ===== 2️⃣ 核心文字 =====
        font.setColor(0.9f, 0.97f, 1.0f, 1.0f);

        font.draw(
                game.getSpriteBatch(),
                text,
                textX - textW / 2f,
                textY
        );

// 还原
        font.setColor(Color.WHITE);
        game.getSpriteBatch().end();




        // 4️⃣ HUD
        renderHUD();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            finishTutorial(MazeGameTutorialResult.EXIT_BY_PLAYER);
        }
    }
    private boolean reachedGoal() {
        float dx = playerX - goalX;
        float dy = playerY - goalY;
        return dx * dx + dy * dy <= goalRadius * goalRadius;
    }

    private void renderMaskDebugOverlay() {
        if (!debugShowMask) return;

        shapeRenderer.setProjectionMatrix(hudCamera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        int maskW = mazeMask.getWidth();
        int maskH = mazeMask.getHeight();

        float cellW = (float) screenW / maskW;
        float cellH = (float) screenH / maskH;

        Color c = new Color();

        for (int y = 0; y < maskH; y++) {
            for (int x = 0; x < maskW; x++) {

                Color.rgba8888ToColor(c, mazeMask.getPixel(x, y));

                boolean walkable =
                        c.a > 0.5f &&
                                c.r > 0.9f &&
                                c.g > 0.9f &&
                                c.b > 0.9f;

                if (walkable) {
                    // 🟢 可走区域
                    shapeRenderer.setColor(0f, 1f, 0f, 0.25f);
                } else {
                    // 🔴 不可走区域
                    shapeRenderer.setColor(1f, 0f, 0f, 0.25f);
                }

                shapeRenderer.rect(
                        x * cellW,
                        (maskH - 1 - y) * cellH,
                        cellW,
                        cellH
                );

            }
        }

        shapeRenderer.end();
    }

    private int screenToMaskX(float screenX) {
        return (int) (screenX / Gdx.graphics.getWidth()
                * mazeMask.getWidth());
    }

    private int screenToMaskY(float screenY) {
        int maskY = (int) (screenY / Gdx.graphics.getHeight()
                * mazeMask.getHeight());

        // 🔥 上下翻转
        return mazeMask.getHeight() - 1 - maskY;
    }



    private boolean canWalk(float screenX, float screenY) {
        int x = screenToMaskX(screenX);
        int y = screenToMaskY(screenY);

        if (x < 0 || y < 0
                || x >= mazeMask.getWidth()
                || y >= mazeMask.getHeight()) {
            return false;
        }

        Color c = new Color();
        Color.rgba8888ToColor(c, mazeMask.getPixel(x, y));

        // ✅ 必须同时满足：
        // 1. 颜色是白
        // 2. alpha 不透明（或至少大于阈值）
        return c.a > 0.5f && c.r > 0.9f && c.g > 0.9f && c.b > 0.9f;
    }

    private boolean exitTriggered = false;



    private void handleInput() {
        upPressed = Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP);
        downPressed = Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN);
        leftPressed = Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT);
        rightPressed = Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT);
    }
    private float getPortalDrawX() {
        return goalX;
    }

    private float getPortalDrawY() {
        return goalY + PORTAL_Y_OFFSET;
    }
    private void update(float delta) {
        if (finished) return;

        // 1. 记录移动状态（修复指示灯不亮的问题）
        if (upPressed) movedUp = true;
        if (downPressed) movedDown = true;
        if (leftPressed) movedLeft = true;
        if (rightPressed) movedRight = true;
        int dx = 0;
        int dy = 0;

        if (upPressed) dy = 1;
        else if (downPressed) dy = -1;
        else if (leftPressed) dx = -1;
        else if (rightPressed) dx = 1;
        if (dx != 0 || dy != 0) {
            tutorialPlayer.updateDirection(dx, dy);
            tutorialPlayer.setMovingAnim(true);
        } else {
            tutorialPlayer.setMovingAnim(false);
        }


        // 2. 检测 Shift 冲刺
        boolean isSprinting = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
        // 只有在移动时按住 Shift 才算完成任务
        if (isSprinting && (upPressed || downPressed || leftPressed || rightPressed)) {
            usedShift = true;
        }
        markObjectiveDone("Move Up", movedUp);
        markObjectiveDone("Move Down", movedDown);
        markObjectiveDone("Move Left", movedLeft);
        markObjectiveDone("Move Right", movedRight);
        markObjectiveDone("SHIFT Sprint", usedShift);
        markObjectiveDone("🎯 Reach Exit", reachedGoal());
        for (int i = objectives.size - 1; i >= 0; i--) {
            ObjectiveItem obj = objectives.get(i);

            if (obj.completed && !obj.removing) {
                // 🟡 完成后停顿 1 秒
                obj.completedTime += delta;
                if (obj.completedTime >= 1.0f) {
                    obj.removing = true; // 开始滑出
                    obj.slide = 0f;
                }
            }

            if (obj.removing) {
                obj.slide += delta * 2.0f; // 滑出速度
                if (obj.slide >= 1f) {
                    objectives.removeIndex(i); // 真正移除
                }
            }
        }

        // 3. 计算位移
        float speed = (isSprinting ? SPRINT_SPEED : WALK_SPEED) * delta;

        float nextX = playerX + dx * speed;
        float nextY = playerY + dy * speed;
        float footY = nextY + PLAYER_FOOT_OFFSET;
        float footX = nextX + PLAYER_BODY_OFFSET;

        if (canWalk(footX, footY)) {
            playerX = nextX;
            playerY = nextY;
        }

        syncPlayerToEntity();
        tutorialPlayer.update(delta);
        // 5. 到达目标逻辑（距离判定）

        goalPortal.setCenter(
                getPortalDrawX(),
                getPortalDrawY()
        );
        goalPortal.update(delta);

        boolean movementTasksDone =
                movedUp && movedDown && movedLeft && movedRight && usedShift;

        boolean onGoal = reachedGoal();
        reachedTarget = onGoal;

        if (!exitTriggered && movementTasksDone && onGoal) {
            exitTriggered = true;
            goalPortal.startExitAnimation(goalX, goalY);
        }


        if (exitTriggered && goalPortal.isFinished()) {
            finishTutorial(MazeGameTutorialResult.SUCCESS);
        }


        exitHintTimer += delta;
    }

    private void markObjectiveDone(String text, boolean condition) {
        if (!condition) return;

        for (ObjectiveItem obj : objectives) {
            if (obj.text.equals(text) && !obj.completed) {
                obj.completed = true;
                obj.completedTime = 0f; // 🔥 开始计时
                break;
            }
        }
    }


    private void findSpawnByCode() {
        int maskW = mazeMask.getWidth();
        int maskH = mazeMask.getHeight();

        for (int y = 0; y < maskH; y++) {
            for (int x = 0; x < maskW; x++) {

                Color c = new Color();
                Color.rgba8888ToColor(c, mazeMask.getPixel(x, y));

                if (c.a > 0.5f && c.r > 0.9f && c.g > 0.9f && c.b > 0.9f) {

                    // 🟢 这是“脚底”的屏幕坐标
                    float footScreenX = (float) x / maskW * Gdx.graphics.getWidth();
                    float footScreenY = (float) (maskH - 1 - y)
                            / maskH * Gdx.graphics.getHeight();

                    // 🔥 关键：身体 = 脚底 + 偏移
                    playerX = footScreenX - PLAYER_BODY_OFFSET;
                    playerY = footScreenY - PLAYER_FOOT_OFFSET;

                    return;
                }
            }
        }

        // 兜底
        playerX = Gdx.graphics.getWidth() / 2f + PLAYER_BODY_OFFSET;
        playerY = Gdx.graphics.getHeight() / 2f + PLAYER_FOOT_OFFSET;
    }




    private void renderHUD() {
        shapeRenderer.setProjectionMatrix(hudCamera.combined);

        float startX = 40f;
        float startY = Gdx.graphics.getHeight() - 80f;
        float spacing = 70f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        game.getSpriteBatch().begin();

        for (int i = 0; i < objectives.size; i++) {
            ObjectiveItem obj = objectives.get(i);

            float y = startY - i * spacing;

            float slideX = obj.removing ? -300f * obj.slide : 0f;
            float alpha = obj.removing ? (1f - obj.slide) : 1f;


            // === 文字颜色 ===
            var font = game.getSkin().getFont("default-font");

            if (obj.completed) {
                font.setColor(0.4f, 0.85f, 1.0f, alpha); // 🔥 高亮文字
            } else {
                font.setColor(1f, 1f, 1f, alpha);
            }

            font.draw(
                    game.getSpriteBatch(),
                    obj.text,
                    startX  + slideX,
                    y + 18f
            );
            font.setColor(Color.WHITE);

        }

        game.getSpriteBatch().end();
        shapeRenderer.end();


    }


    private void drawStatusBox(float x, float y, boolean done) {
        shapeRenderer.setColor(done ? Color.GREEN : Color.RED); // 完成变绿，未完成变红
        shapeRenderer.rect(x, y, 20, 20);
    }

    // 辅助方法：绘制对齐的灯
    private void drawIndicator(float x, float y, boolean active) {
        shapeRenderer.setColor(active ? Color.GREEN : Color.GRAY);
        shapeRenderer.rect(x, y, 18, 18);
    }

    private void finishTutorial(MazeGameTutorialResult result) {
        if (finished) return;

        finished = true;

        System.out.println("=== TUTORIAL END ===");
        System.out.println("Result: " + result);
        System.out.println("Calling game.onTutorialFinished/onTutorialFailed");

        // Delay execution by one frame to avoid rendering issues
        Gdx.app.postRunnable(() -> {
            try {
                if (result == MazeGameTutorialResult.SUCCESS) {
                    System.out.println("Calling game.onTutorialFinished()");
                    game.onTutorialFinished(this);
                } else {
                    System.out.println("Calling game.onTutorialFailed()");
                    game.onTutorialFailed(this, result);
                }
            } catch (Exception e) {
                System.err.println("Tutorial callback error: " + e.getMessage());
                e.printStackTrace();
                // Return to main menu on error
                game.goToMenu();
            }
        });
    }

    private static class ObjectiveItem {
        String text;
        boolean completed = false;

        // 动画参数
        float slide = 0f;        // 0 → 1（滑出）
        float completedTime = 0f; // 已完成停留时间
        boolean removing = false;

        ObjectiveItem(String text) {
            this.text = text;
        }
    }
    private Array<ObjectiveItem> objectives = new Array<>();






















    @Override
    public void resize(int width, int height) {
        if (viewport != null) {
            viewport.update(width, height, true);
        }
        if (hudCamera != null) {
            hudCamera.setToOrtho(false, width, height);
            hudCamera.update();
        }
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        System.out.println("Tutorial screen hidden");
        if (gm != null) {
            gm.setTutorialMode(false);
        }
    }

    @Override
    public void dispose() {
        System.out.println("Tutorial screen resources disposed");

        if (mazeMask != null) mazeMask.dispose();
        if (mazeTexture != null) mazeTexture.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}