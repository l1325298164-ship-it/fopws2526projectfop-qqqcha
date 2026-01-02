package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.input.PlayerInputHandler;
import de.tum.cit.fop.maze.utils.CameraManager;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.ArrayDeque;
import java.util.Queue;

public class MazeGameTutorialScreen implements Screen {

    public enum MazeGameTutorialResult {
        SUCCESS,
        FAILURE_DEAD,
        EXIT_BY_PLAYER
    }

    private final MazeRunnerGame game;
    private final DifficultyConfig config;

    private GameManager gm;
    private CameraManager cameraManager;
    private Texture controlsTexture;
    private OrthographicCamera hudCamera;

    private float uiScale, uiWidth, uiHeight, uiX, uiY;
    private float glowTimer = 0f;

    private boolean finished = false;
    private boolean movedUp, movedDown, movedLeft, movedRight;
    private boolean reachedTarget = false;

    private static final float CELL_SIZE = 32f;
    private static final float PLAYER_RADIUS = CELL_SIZE / 2f;
    private int targetX, targetY;

    public MazeGameTutorialScreen(MazeRunnerGame game, DifficultyConfig config) {
        this.game = game;
        this.config = config;
    }

    @Override
    public void show() {
        gm = game.getGameManager();
        gm.setTutorialMode(true);

        // === 世界相机 ===
        cameraManager = new CameraManager(config);
        cameraManager.setClampToMap(false);
        // 立即居中到玩家位置
        cameraManager.centerOnPlayerImmediately(gm.getPlayer());

        // === HUD 相机 ===
        hudCamera = new OrthographicCamera();
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        // === HUD 图片 ===
        controlsTexture = new Texture(Gdx.files.internal("tutorial/keyboard4directions.png"));

        uiScale = 0.35f;
        uiWidth = controlsTexture.getWidth() * uiScale;
        uiHeight = controlsTexture.getHeight() * uiScale;
        uiX = 20f;

        // 确保指南针是激活状态
        if (gm.getCompass() != null) {
            gm.getCompass().setActive(true);
        }

        // 设置目标位置 - 找一个可达的点
        findReachableTarget();
    }

    private void findReachableTarget() {
        int[][] maze = gm.getMaze();
        int w = maze[0].length;
        int h = maze.length;

        int startX = (int) gm.getPlayer().getX();
        int startY = (int) gm.getPlayer().getY();

        boolean[][] visited = new boolean[h][w];
        Queue<int[]> queue = new ArrayDeque<>();
        int[][] directions = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

        queue.add(new int[]{startX, startY, 0}); // 第三个参数是距离
        visited[startY][startX] = true;

        // 存储所有可达点的列表
        java.util.List<int[]> reachablePoints = new java.util.ArrayList<>();

        // 最短距离要求：至少离玩家3格远
        final int MIN_DISTANCE = 10;
        // 最大距离限制：避免太远
        final int MAX_DISTANCE = 10;

        while (!queue.isEmpty()) {
            int[] point = queue.poll();
            int x = point[0];
            int y = point[1];
            int dist = point[2];

            // 如果距离合适（不太近也不太远），添加到候选列表
            if (dist >= MIN_DISTANCE && dist <= MAX_DISTANCE && maze[y][x] == 0) {
                reachablePoints.add(new int[]{x, y, dist});
            }

            // 继续BFS搜索，但限制最大距离
            if (dist < MAX_DISTANCE) {
                for (int[] dir : directions) {
                    int nx = x + dir[0];
                    int ny = y + dir[1];

                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
                    if (visited[ny][nx] || maze[ny][nx] != 0) continue;

                    visited[ny][nx] = true;
                    queue.add(new int[]{nx, ny, dist + 1});
                }
            }
        }

        // 如果找到合适的点，随机选择一个
        if (!reachablePoints.isEmpty()) {
            // 按距离排序，选择中等距离的点（既不太近也不太远）
            reachablePoints.sort((a, b) -> Integer.compare(a[2], b[2]));

            // 选择中间距离的点（避免选择最近或最远的）
            int middleIndex = reachablePoints.size() / 2;
            int[] selected = reachablePoints.get(middleIndex);
            targetX = selected[0];
            targetY = selected[1];

            System.out.println("教程目标点: (" + targetX + ", " + targetY +
                    ") 距离玩家: " + selected[2] + " 格");
        } else {
            // 如果没有找到合适的点，找一个可达的最远点（但不是玩家当前位置）
            targetX = startX;
            targetY = startY;

            // 尝试在4个方向找最近的通路
            for (int[] dir : directions) {
                int nx = startX + dir[0] * 2; // 2格远
                int ny = startY + dir[1] * 2;

                if (nx >= 0 && ny >= 0 && nx < w && ny < h && maze[ny][nx] == 0) {
                    targetX = nx;
                    targetY = ny;
                    break;
                }
            }

            System.out.println("警告：未找到合适距离的目标点，使用备用点: (" + targetX + ", " + targetY + ")");
        }

        System.out.println("玩家起始点: (" + startX + ", " + startY + ")");
        System.out.println("目标点是否为通路: " + (maze[targetY][targetX] == 0 ? "是" : "否"));

        // 调试：检查目标点是否可达
        if (maze[targetY][targetX] != 0) {
            System.out.println("警告：目标点在墙上！尝试重新选择...");
            // 尝试找最近的通路
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    if (maze[y][x] == 0) {
                        targetX = x;
                        targetY = y;
                        break;
                    }
                }
                if (maze[targetY][targetX] == 0) break;
            }
        }
    }

    @Override
    public void render(float delta) {
        glowTimer += delta;

        // 先更新游戏逻辑
        update(delta);

        // 重要：更新相机位置，确保相机跟随玩家
        if (!finished) {
            // 获取玩家位置
            float playerX = gm.getPlayer().getX() * CELL_SIZE;
            float playerY = gm.getPlayer().getY() * CELL_SIZE;

            // 使用CameraManager更新相机
            cameraManager.update(delta, gm.getPlayer());
        }

        ScreenUtils.clear(0, 0, 0, 1);
        renderGame();
        renderHUD();
        renderCompass(); // 使用已有的指南针渲染方法
    }

    private void update(float delta) {
        if (finished) return;

        // 更新游戏管理器
        gm.getInputHandler().update(delta, gm);
        gm.update(delta);

        // 检查移动输入
        PlayerInputHandler input = gm.getInputHandler();
        movedUp |= input.hasMovedUp();
        movedDown |= input.hasMovedDown();
        movedLeft |= input.hasMovedLeft();
        movedRight |= input.hasMovedRight();

        // 使用网格坐标进行位置判断（简化版）
        float playerGridX = gm.getPlayer().getX();
        float playerGridY = gm.getPlayer().getY();

        // 计算玩家到目标的曼哈顿距离
        float manhattanDist = Math.abs(playerGridX - targetX) + Math.abs(playerGridY - targetY);

        // 调试信息
        if (!reachedTarget && manhattanDist <= 1.5f) {
            System.out.println("玩家靠近目标点！玩家: (" + playerGridX + ", " + playerGridY +
                    "), 目标: (" + targetX + ", " + targetY +
                    "), 距离: " + manhattanDist);
        }

        // 使用更大的容差（1.5个网格单位）
        if (manhattanDist <= 1.5f) {
            reachedTarget = true;
            System.out.println("玩家到达目标区域！");
        }

        // 检查是否完成教程条件
        if (movedUp && movedDown && movedLeft && movedRight && reachedTarget) {
            System.out.println("教程完成！");
            finishTutorial(MazeGameTutorialResult.SUCCESS);
            return;
        }

        if (gm.isPlayerDead()) {
            System.out.println("玩家死亡！");
            finishTutorial(MazeGameTutorialResult.FAILURE_DEAD);
        }
    }

    private void finishTutorial(MazeGameTutorialResult result) {
        if (finished) return; // 防止重复调用

        finished = true;
        gm.setTutorialMode(false);

        if (result == MazeGameTutorialResult.SUCCESS) {
            game.onTutorialFinished(this);
        } else {
            game.onTutorialFailed(this, result);
        }
    }

    private void renderGame() {
        SpriteBatch batch = game.getSpriteBatch();

        // 重要：确保使用正确的相机矩阵
        batch.setProjectionMatrix(cameraManager.getCamera().combined);
        batch.begin();

        int[][] maze = gm.getMaze();
        Texture whitePixel = TextureManager.getInstance().getWhitePixel();

        // 绘制迷宫墙壁
        batch.setColor(0.5f, 0.5f, 0.5f, 1);
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[0].length; x++) {
                if (maze[y][x] == 1) {
                    batch.draw(whitePixel, x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        // 绘制可通行区域（浅色）
        batch.setColor(0.2f, 0.2f, 0.2f, 0.5f);
        for (int y = 0; y < maze.length; y++) {
            for (int x = 0; x < maze[0].length; x++) {
                if (maze[y][x] == 0) {
                    batch.draw(whitePixel, x * CELL_SIZE, y * CELL_SIZE, CELL_SIZE, CELL_SIZE);
                }
            }
        }

        // 绘制目标点 - 绿色块（比正常格子稍大）
        batch.setColor(0, 1, 0, 0.8f);
        float targetDrawSize = CELL_SIZE * 1.2f;
        float targetDrawX = targetX * CELL_SIZE - (targetDrawSize - CELL_SIZE) / 2f;
        float targetDrawY = targetY * CELL_SIZE - (targetDrawSize - CELL_SIZE) / 2f;
        batch.draw(whitePixel, targetDrawX, targetDrawY, targetDrawSize, targetDrawSize);

        // 绘制玩家（青色）
        batch.setColor(0, 0.8f, 1, 1);
        float playerDrawX = gm.getPlayer().getX() * CELL_SIZE;
        float playerDrawY = gm.getPlayer().getY() * CELL_SIZE;
        batch.draw(whitePixel, playerDrawX, playerDrawY, CELL_SIZE, CELL_SIZE);

        batch.setColor(1, 1, 1, 1);
        batch.end();
    }

    private void renderHUD() {
        // 只在需要时显示控制提示
        if (movedUp && movedDown && movedLeft && movedRight) return;

        SpriteBatch batch = game.getSpriteBatch();
        batch.setProjectionMatrix(hudCamera.combined);

        float glowAlpha = 0.4f + 0.5f * (float) Math.sin(glowTimer * 3f);

        batch.begin();
        // 绘制发光背景
        batch.setColor(1f, 0.7f, 0.9f, glowAlpha * 0.6f);
        batch.draw(controlsTexture, uiX - 6, uiY - 6, uiWidth + 12, uiHeight + 12);

        // 绘制控制图
        batch.setColor(1, 1, 1, 1);
        batch.draw(controlsTexture, uiX, uiY, uiWidth, uiHeight);

        batch.end();
    }

    // 使用已有的指南针渲染方法
    private void renderCompass() {
        if (gm.getCompass() == null || !gm.getCompass().isActive()) {
            return;
        }

        SpriteBatch batch = game.getSpriteBatch();
        batch.setProjectionMatrix(hudCamera.combined);
        batch.begin();

        // 使用Compass类自带的绘制方法
        gm.getCompass().drawAsUI(batch);

        batch.end();
    }

    @Override
    public void resize(int width, int height) {
        if (cameraManager != null) {
            cameraManager.resize(width, height);
        }
        hudCamera.setToOrtho(false, width, height);
        uiY = hudCamera.viewportHeight - uiHeight - 20f;
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        // 隐藏时停用指南针
        if (gm.getCompass() != null) {
            gm.getCompass().setActive(false);
        }
    }

    @Override
    public void dispose() {
        if (controlsTexture != null) {
            controlsTexture.dispose();
            controlsTexture = null;
        }
    }
}