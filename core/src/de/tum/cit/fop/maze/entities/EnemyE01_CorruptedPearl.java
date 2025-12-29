package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.entities.Player;

public class EnemyE01_CorruptedPearl extends Enemy {

    private EnemyState state = EnemyState.PATROL;

    private float shootCooldown = 0f;
    private static final float SHOOT_INTERVAL = 1.2f;

    public EnemyE01_CorruptedPearl(int x, int y) {
        super(x, y);

        hp = 5;
        collisionDamage = 5;
        attack = 10;

        moveSpeed = 2.5f;
        moveInterval = 0.35f;
        changeDirInterval = 2.0f;
        detectRange = 6f;

        updateTexture();
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        // 不用 Shape 渲染
    }

    /* ================== 渲染 ================== */

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        super.drawSprite(batch);
    }

    @Override
    protected void updateTexture() {
        texture = textureManager.getEnemy1Texture();
        needsTextureUpdate = false;
    }

    /* ================== 行为 ================== */

    @Override
    public void update(float delta, GameManager gm) {
        if (!active) return;

        updateHitFlash(delta);

        Player player = gm.getPlayer();
        float dist = distanceTo(player);

        shootCooldown -= delta;

        state = (dist <= detectRange)
                ? EnemyState.ATTACK
                : EnemyState.PATROL;

        switch (state) {
            case PATROL -> patrol(delta, gm);
            case ATTACK -> combat(delta, gm, player, dist);
        }

        // ⭐⭐⭐ 连续移动真正执行在这里
        moveContinuously(delta);
    }

    private void patrol(float delta, GameManager gm) {
        tryMoveRandom(delta, gm);
    }

    private void combat(float delta, GameManager gm, Player player, float dist) {

        float idealDistance = detectRange * 0.5f;

        // ❗ 这里先用“简单版追逐”，避免瞬移问题
        if (!isMoving) {
            int dx = Integer.compare(player.getX(), x);
            int dy = Integer.compare(player.getY(), y);

            // 只走正交方向
            if (Math.abs(dx) > Math.abs(dy)) {
                dy = 0;
            } else {
                dx = 0;
            }

            int nx = x + dx;
            int ny = y + dy;

            if (dist > idealDistance + 0.5f && gm.isEnemyValidMove(nx, ny)) {
                startMoveTo(nx, ny);
            } else if (dist < idealDistance - 0.5f) {
                int bx = x - dx;
                int by = y - dy;
                if (gm.isEnemyValidMove(bx, by)) {
                    startMoveTo(bx, by);
                }
            }
        }

        if (shootCooldown <= 0f) {
            shootAt(player, gm);
            shootCooldown = SHOOT_INTERVAL;
        }
    }

    private void shootAt(Player player, GameManager gm) {
        float dx = player.getX() - x;
        float dy = player.getY() - y;

        EnemyBullet bullet = new EnemyBullet(
                x + 0.5f,
                y + 0.5f,
                dx,
                dy,
                attack
        );

        gm.spawnProjectile(bullet);
    }

    private float distanceTo(Player p) {
        float dx = p.getX() - x;
        float dy = p.getY() - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
