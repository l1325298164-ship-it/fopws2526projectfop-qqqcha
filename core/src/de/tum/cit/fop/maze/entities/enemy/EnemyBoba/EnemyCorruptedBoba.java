package de.tum.cit.fop.maze.entities.enemy.EnemyBoba;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.enemy.Enemy;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameManager;

public class EnemyCorruptedBoba extends Enemy {

    private enum State {
        PATROL,
        CHASE,
        PREPARE,
        ATTACK,
        COOLDOWN
    }

    private State state = State.PATROL;
    private float stateTimer = 0f;

    private static final float DETECT_RANGE = 7f;
    private static final float ATTACK_RANGE = 3.5f;

    private static final float PREPARE_TIME = 0.6f;
    private static final float COOLDOWN_TIME = 1.0f;

    // 动画参数（只影响视觉，不改坐标）
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;

    public EnemyCorruptedBoba(int x, int y) {
        super(x, y);
        hp = 8;
        attack = 15;

        moveSpeed = 3.5f;
        moveInterval = 0.25f;
        changeDirInterval = 1.2f;

        updateTexture();
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {

    }

    @Override
    public void update(float delta, GameManager gm) {
        if (!active) return;

        updateHitFlash(delta);

        Player player = gm.getPlayer();
        float dist = distanceTo(player);

        stateTimer += delta;

        switch (state) {
            case PATROL -> {
                tryMoveRandom(delta, gm);
                if (dist <= DETECT_RANGE) {
                    transitionTo(State.CHASE);
                }
            }

            case CHASE -> {
                chasePlayer(player, gm);

                if (dist <= ATTACK_RANGE) {
                    transitionTo(State.PREPARE);
                } else if (dist > DETECT_RANGE * 1.5f) {
                    transitionTo(State.PATROL);
                }
            }

            case PREPARE -> {
                if (stateTimer >= PREPARE_TIME) {
                    transitionTo(State.ATTACK);
                }
            }

            case ATTACK -> {
                shoot(player, gm);
                transitionTo(State.COOLDOWN);
            }

            case COOLDOWN -> {
                if (stateTimer >= COOLDOWN_TIME) {
                    transitionTo(dist <= DETECT_RANGE ? State.CHASE : State.PATROL);
                }
            }
        }

        // ⭐⭐⭐ 连续移动真正执行
        moveContinuously(delta);
    }

    /* ================= 行为 ================= */

    private void chasePlayer(Player player, GameManager gm) {
        if (isMoving) return;

        int dx = Integer.compare(player.getX(), x);
        int dy = Integer.compare(player.getY(), y);

        // 只允许正交方向
        if (Math.abs(dx) > Math.abs(dy)) {
            dy = 0;
        } else {
            dx = 0;
        }

        int nx = x + dx;
        int ny = y + dy;

        if (gm.isEnemyValidMove(nx, ny)) {
            startMoveTo(nx, ny);
        }
    }

    private void shoot(Player player, GameManager gm) {
        float dx = player.getX() - x;
        float dy = player.getY() - y;

        BobaBullet bullet = new BobaBullet(
                x + 0.5f,
                y + 0.5f,
                dx, dy,
                attack
        );

        gm.spawnProjectile(bullet);
    }

    private void transitionTo(State newState) {
        state = newState;
        stateTimer = 0f;

        if (newState == State.ATTACK) {
            scaleX = 1.3f;
            scaleY = 0.7f;
        }
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        // ⭐ 直接复用 Enemy 的连续渲染
        super.drawSprite(batch);
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    @Override
    protected void updateTexture() {
        texture = textureManager.getEnemy1Texture();
        needsTextureUpdate = false;
    }

    private float distanceTo(Player p) {
        float dx = p.getX() - x;
        float dy = p.getY() - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
