package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.TextureManager;

public class CatFollower extends GameObject {

    /* ================== 跟随对象 ================== */

    private final Player player;

    /* ================== 连续坐标 ================== */

    private float worldX;
    private float worldY;

    /* ================== 移动参数 ================== */

    // 猫的基础速度（格 / 秒）
    private static final float BASE_SPEED = 2.5f;

    // 相对于玩家的速度比例
    private static final float PLAYER_SPEED_RATIO = 0.75f;

    // 跟随的“松弛半径”（太近就不动）
    private static final float FOLLOW_EPSILON = 0.05f;


    private enum State {
        FOLLOW_PLAYER,
        IDLE_WANDER
    }
    // ===== idle wandering =====
    private float idleTimer = 0f;
    private float nextIdleDecisionTime = 1.5f; // 多久选一次新点
    private final GameManager gm;
    private float idleTargetX;
    private float idleTargetY;

    private State state = State.FOLLOW_PLAYER;
    /* ================== Animation ================== */

    private static Animation<TextureRegion> animLeft;
    private static Animation<TextureRegion> animRight;
    private static Animation<TextureRegion> animFront;
    private static Animation<TextureRegion> animBack;

    private float animTime = 0f;

    private enum Facing {
        LEFT, RIGHT, FRONT, BACK
    }

    private Facing facing = Facing.FRONT;
    private static void loadAnimations(TextureManager tm) {
        if (animLeft != null) return;

        animLeft = new Animation<>(
                0.18f,
                tm.getCatLeftAtlas().getRegions()
        );

        animRight = new Animation<>(
                0.18f,
                tm.getCatRightAtlas().getRegions()
        );

        animFront = new Animation<>(
                0.18f,
                tm.getCatFrontAtlas().getRegions()
        );

        animBack = new Animation<>(
                0.18f,
                tm.getCatBackAtlas().getRegions()
        );

        animLeft.setPlayMode(Animation.PlayMode.LOOP);
        animRight.setPlayMode(Animation.PlayMode.LOOP);
        animFront.setPlayMode(Animation.PlayMode.LOOP);
        animBack.setPlayMode(Animation.PlayMode.LOOP);
    }

    public CatFollower(Player player, GameManager gm) {
        super(player.getX(), player.getY());
        this.player = player;
        this.gm = gm;

        // ⭐ 初始化为玩家连续坐标（和渲染体系对齐）
        this.worldX = player.getX() + 0.5f;
        this.worldY = player.getY() + 0.2f;

        loadAnimations(TextureManager.getInstance());
    }


    /* ================== Update ================== */

    public void update(float delta) {
        if (!active) return;

        // ① 玩家是否在移动？
        if (player.isMoving()) {
            state = State.FOLLOW_PLAYER;
        } else {
            if (state != State.IDLE_WANDER) {
                enterIdleWander();
            }
        }

        switch (state) {
            case FOLLOW_PLAYER -> updateFollow(delta);
            case IDLE_WANDER -> updateIdle(delta);
        }

        // ⭐ 同步 grid 坐标（给排序用）
        x = (int) worldX;
        y = (int) worldY;
    }

    private void updateFollow(float delta) {
        float targetX = player.getX() + 0.5f;
        float targetY = player.getY() + 0.2f;

        moveToward(targetX, targetY, delta, player.getMoveSpeed() * 0.75f);
    }
    private void enterIdleWander() {
        state = State.IDLE_WANDER;
        idleTimer = 0f;
        pickNewIdleTarget();
    }
    private void updateIdle(float delta) {
        idleTimer += delta;

        // 到时间了，换一个目标
        if (idleTimer >= nextIdleDecisionTime) {
            idleTimer = 0f;
            pickNewIdleTarget();
        }

        moveToward(idleTargetX, idleTargetY, delta, player.getMoveSpeed() * 0.5f);
    }
    private void pickNewIdleTarget() {
        int px = player.getX();
        int py = player.getY();

        // 最多尝试几次，找一个合法格子
        for (int i = 0; i < 10; i++) {

            int dx = MathUtils.random(-2, 2);
            int dy = MathUtils.random(-2, 2);

            int tx = px + dx;
            int ty = py + dy;

            // ① 不和玩家同格
            if (tx == px && ty == py) continue;

            // ② 越界 / 墙直接跳过
            if (gm.getMazeCell(tx, ty) != 1) continue;

            // ✅ 找到一个合法格子
            idleTargetX = tx + 0.5f;
            idleTargetY = ty + 0.2f;
            return;
        }

        // 🔁 如果实在找不到，就退回玩家附近
        idleTargetX = px + 0.5f;
        idleTargetY = py + 0.2f;
    }

    private void moveToward(float targetX, float targetY, float delta, float speed) {

        float dx = targetX - worldX;
        float dy = targetY - worldY;
        if (Math.abs(dx) > Math.abs(dy)) {
            facing = dx > 0 ? Facing.RIGHT : Facing.LEFT;
        } else {
            facing = dy > 0 ? Facing.BACK : Facing.FRONT;
        }
        float distSq = dx * dx + dy * dy;
        if (distSq < 0.0001f) return;

        float dist = (float)Math.sqrt(distSq);
        float step = speed * delta;

        float nextX = worldX;
        float nextY = worldY;

        if (step >= dist) {
            nextX = targetX;
            nextY = targetY;
        } else {
            nextX += dx / dist * step;
            nextY += dy / dist * step;
        }

        // ====== ★ 关键：做墙体碰撞检测 ======

        int curGX = (int)(worldX);
        int curGY = (int)(worldY);

        int nextGX = (int)(nextX);
        int nextGY = (int)(nextY);

        // 如果跨格子，则检测目标格子是否合法
        if (nextGX != curGX || nextGY != curGY) {

            // 猫不能穿墙：mazeCell == 1 才能走
            if (gm.getMazeCell(nextGX, nextGY) != 1) {
                // 不允许跨进墙，停止本帧移动
                return;
            }
        }

        // ====== ★ 允许移动 ======
        worldX = nextX;
        worldY = nextY;
    }






    /* ================== Render ================== */

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;

        float cs = GameConstants.CELL_SIZE;

        float size = cs * 0.8f;
        float drawX = worldX * cs - size * 0.5f;
        float drawY = worldY * cs - size * 0.2f;

        Animation<TextureRegion> anim = switch (facing) {
            case LEFT  -> animLeft;
            case RIGHT -> animRight;
            case BACK  -> animBack;
            case FRONT -> animFront;
        };

        boolean isMoving = player.isMoving() || state == State.IDLE_WANDER;

        TextureRegion frame = isMoving
                ? anim.getKeyFrame(animTime)
                : anim.getKeyFrames()[0]; // ✅ 待机帧（第一帧）

        batch.draw(frame, drawX, drawY, size, size);
    }


    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        // 调试用（可选）
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    /* ================== Getter（给雾用） ================== */

    public float getWorldX() {
        return worldX;
    }

    public float getWorldY() {
        return worldY;
    }
    private int[] getPreferredGrid(Player p) {
        int px = p.getX();
        int py = p.getY();

        return switch (p.getDirection()) {
            case UP    -> new int[]{px, py - 1};
            case DOWN  -> new int[]{px, py + 1};
            case LEFT  -> new int[]{px + 1, py};
            case RIGHT -> new int[]{px - 1, py};
        };
    }

}
