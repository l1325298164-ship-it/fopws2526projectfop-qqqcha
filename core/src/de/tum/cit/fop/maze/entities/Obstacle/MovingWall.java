package de.tum.cit.fop.maze.entities.Obstacle;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.entities.PushSource;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

/**
 * MovingWall
 * - 会沿着一条直线路径来回移动
 * - SINGLE : 单面墙（000）
 * - DOUBLE : 双面墙（001）
 * - 遇到玩家会尝试推开玩家
 */
public class MovingWall extends DynamicObstacle implements PushSource {
    @Override
    public int getPushStrength() {
        return 1;
    }

    @Override
    public boolean isLethal() {
        return false;
    }

    /* ================= 墙类型 ================= */

    public enum WallType {
        SINGLE, // 000
        DOUBLE  // 001
    }

    /* ================= 路径参数 ================= */

    private final int dirX;
    private final int dirY;

    private final int startX, startY;
    private final int endX, endY;

    private boolean forward = true;

    /* ================= 渲染 ================= */

    private final WallType wallType;
    private TextureRegion wallRegion;

    /* ================= 构造 ================= */

    public MovingWall(int startX, int startY, int endX, int endY, WallType type) {

        super(startX, startY);
        // 验证起点和终点在同一水平或垂直线上
        if (startX != endX && startY != endY) {
            throw new IllegalArgumentException("MovingWall must move horizontally or vertically, not diagonally");
        }

        // 验证移动距离
        int dist = Math.abs(endX - startX) + Math.abs(endY - startY);
        if (dist < 2) {
            throw new IllegalArgumentException("MovingWall move distance must be at least 2 cells");
        }
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.wallType = type;

        // 初始化坐标系统
        this.x = startX;  // 整数网格坐标
        this.y = startY;
        this.worldX = startX;  // 浮点数渲染坐标
        this.worldY = startY;
        this.targetX = startX;  // 目标坐标
        this.targetY = startY;

        this.dirX = Integer.compare(endX, startX);
        this.dirY = Integer.compare(endY, startY);

        this.moveInterval = 0.6f;
        this.moveCooldown = 0f;
        this.isMoving = false;  // 初始不移动

        loadTexture();

        // 调试输出
        Logger.debug("MovingWall constructor: grid=(" + x + "," + y +
                ") world=(" + worldX + "," + worldY + ")");
    }


    /* ================= 纹理 ================= */

    private void loadTexture() {
        TextureAtlas atlas = TextureManager.getInstance().getWallAtlas();
        if (atlas == null) return;

        int index = (wallType == WallType.SINGLE) ? 0 : 1;
        wallRegion = atlas.findRegion("Wallpaper", index);

        if (wallRegion == null) {
            Logger.error("MovingWall texture missing: Wallpaper index=" + index);
        }
    }

    /* ================= 更新 ================= */

    @Override
    public void update(float delta, GameManager gm) {
        if (moveCooldown < 0) moveCooldown = 0;
//        debugState("BEGIN UPDATE");

        // 减少冷却时间
        moveCooldown -= delta;

        // 正在平滑移动
        if (isMoving) {
            moveContinuously(delta);

            if (!isMoving) {
//                debugState("MOVE_DONE");
                // 移动完成后，确保坐标同步
                x = (int)Math.round(worldX);
                y = (int)Math.round(worldY);
            }
            return;
        }

        // 冷却期间不处理
        if (moveCooldown > 0f) return;

        // 到达端点 → 掉头
        float tolerance = 0.1f; // 容差
        if (forward) {
            float dx = endX - worldX;
            float dy = endY - worldY;
            if (dx * dx + dy * dy < tolerance) {
                forward = !forward;
//                debugState("REACHED_END -> REVERSE");
                // 对齐到终点
                worldX = endX;
                worldY = endY;
                x = endX;
                y = endY;
                moveCooldown = moveInterval * 2; // 在端点暂停一会儿
                return;
            }
        } else {
            float dx = startX - worldX;
            float dy = startY - worldY;
            if (dx * dx + dy * dy < tolerance) {
                forward = !forward;
//                debugState("REACHED_START -> REVERSE");
                // 对齐到起点
                worldX = startX;
                worldY = startY;
                x = startX;
                y = startY;
                moveCooldown = moveInterval * 2; // 在端点暂停一会儿
                return;
            }
        }

        // 计算下一个目标
        int nx = x + (forward ? dirX : -dirX);
        int ny = y + (forward ? dirY : -dirY);

//        debugState("TRY_MOVE next=(" + nx + "," + ny + ")");

        // ===== 玩家挡路 → 尝试推（支持双人）=====
        boolean playerBlocked = false;

        for (Player p : gm.getPlayers()) {
            if (p == null || p.isDead()) continue;

            if (p.getX() == nx && p.getY() == ny) {

                int pushDirX = Integer.compare(nx, x);
                int pushDirY = Integer.compare(ny, y);

                boolean pushed = p.onPushedBy(this, pushDirX, pushDirY, gm);

                if (pushed) {
                    startMoveTo(nx, ny);
                    moveCooldown = moveInterval;
                } else {
                    forward = !forward;
                    moveCooldown = moveInterval;
                }

                playerBlocked = true;
                break; // ⚠️ 只处理一个玩家即可
            }
        }

        if (playerBlocked) {
            return;
        }

        // 正常移动
        if (gm.isObstacleValidMove(nx, ny)) {
//            debugState("MOVE_START (" + nx + "," + ny + ")");
            startMoveTo(nx, ny);
            moveCooldown = moveInterval;
        } else {
//            debugState("BLOCKED -> REVERSE");
            forward = !forward;
            moveCooldown = moveInterval; // 被阻挡时也等待一段时间
        }
    }

    /**
     * 移动过程中：同时占用当前格子和目标格子
     */
    public boolean occupiesCell(int cx, int cy) {
        // 当前格子
        if (x == cx && y == cy) return true;

        // 正在移动时，占用目标格子
        if (isMoving && targetX == cx && targetY == cy) return true;

        return false;
    }


    /* ================= 渲染 ================= */

    @Override
    public void draw(SpriteBatch batch) {
        if (wallRegion == null) return;

        float cs = GameConstants.CELL_SIZE;
        float height = cs * 2.4f;
        int overlap = 6;

        // 主体（向上）
        batch.draw(
                wallRegion,
                worldX * cs,
                worldY * cs - overlap,
                cs,
                height
        );

        // 双面墙：向下再补一段
        if (wallType == WallType.DOUBLE) {
            batch.draw(
                    wallRegion,
                    worldX * cs,
                    worldY * cs - height + overlap,
                    cs,
                    height
            );
        }
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        float cs = GameConstants.CELL_SIZE;

        // 绘制移动路径
        shapeRenderer.setColor(1, 0, 0, 0.3f);  // 红色半透明
        shapeRenderer.rectLine(
                startX * cs + cs/2, startY * cs + cs/2,
                endX * cs + cs/2, endY * cs + cs/2,
                3
        );

        // 绘制当前位置
        shapeRenderer.setColor(0, 1, 0, 0.5f);  // 绿色半透明
        shapeRenderer.circle(
                worldX * cs + cs/2,
                worldY * cs + cs/2,
                cs/4
        );
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (wallRegion == null) return;

        float cs = GameConstants.CELL_SIZE;
        float height = cs * 2.4f;
        int overlap = 6;

        // 主体（向上）
        batch.draw(
                wallRegion,
                worldX * cs,
                worldY * cs - overlap,
                cs,
                height
        );

        // 双面墙渲染第二面
        if (wallType == WallType.DOUBLE) {
            batch.draw(
                    wallRegion,
                    worldX * cs,
                    worldY * cs - height + overlap,
                    cs,
                    height
            );
        }

    }

//    private void debugState(String tag) {
//        if (!Logger.isDebugEnabled()) return;
//
//        Logger.debug("[MovingWall] " + tag +
//                " | pos=(" + x + "," + y + ")" +
//                " world=(" + String.format("%.2f", worldX) + "," + String.format("%.2f", worldY) + ")" +
//                " target=(" + targetX + "," + targetY + ")" +
//                " forward=" + forward +
//                " dir=(" + dirX + "," + dirY + ")" +
//                " cooldown=" + String.format("%.2f", moveCooldown)
//        );
//    }

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    @Override
    public boolean isPassable() {
        return false;
    }

    /* ================= 内部移动 ================= */

    protected void startMoveTo(int nx, int ny) {
        // 重要：先更新网格坐标
        // 但注意：这里x,y是起点，target是终点

        // 实际上，在平滑移动期间，grid坐标应该保持为起点
        // 移动到终点后才更新grid坐标
        // 所以这里不要更新x,y

        targetX = nx;
        targetY = ny;
        isMoving = true;

        Logger.debug("startMoveTo: from=(" + x + "," + y +
                ") to target=(" + targetX + "," + targetY + ")");
    }

    protected void moveContinuously(float delta) {
        float speed = 1f / moveInterval;

        float dx = targetX - worldX;
        float dy = targetY - worldY;

        float distSq = dx * dx + dy * dy;

        if (distSq < 1e-4f) {
            worldX = targetX;
            worldY = targetY;
            isMoving = false;

            x = (int) Math.round(worldX);
            y = (int) Math.round(worldY);

            Logger.debug("moveContinuously DONE: grid=(" + x + "," + y +
                    ") world=(" + worldX + "," + worldY + ")");
            return;
        }

        float dist = (float) Math.sqrt(distSq);

        float nx = dx / dist;
        float ny = dy / dist;

        float step = speed * delta;

        if (step >= dist) {
            worldX = targetX;
            worldY = targetY;
            isMoving = false;

            x = (int) Math.round(worldX);
            y = (int) Math.round(worldY);

            Logger.debug("moveContinuously DONE (snap): grid=(" + x + "," + y +
                    ") world=(" + worldX + "," + worldY + ")");
            return;
        }

        worldX += nx * step;
        worldY += ny * step;
    }

}
