package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

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
        this.detectRange = 6f;

        updateTexture();
    }

    /* ================== 渲染 ================== */

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        super.drawSprite(batch); // ⭐ 直接用 Enemy 的实现
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

        // ⭐ 更新受击闪烁
        updateHitFlash(delta);

        Player player = gm.getPlayer();
        float dist = distanceTo(player);

        shootCooldown -= delta;

        if (dist <= detectRange) {
            state = EnemyState.ATTACK;
        } else {
            state = EnemyState.PATROL;
        }

        switch (state) {
            case PATROL -> patrol(delta, gm);
            case ATTACK -> combat(delta, gm, player, dist);
        }

        moveCooldown -= delta;

    }


    private void patrol(float delta, GameManager gm) {
        tryMoveRandom(delta, gm);
    }

    private void combat(float delta, GameManager gm, Player player, float dist) {
        float idealDistance = detectRange * 0.5f;

        if (dist > idealDistance + 0.5f) {
            // 太远 → 追玩家
            moveToward(player.getX(), player.getY(), gm);

        } else if (dist < idealDistance - 0.5f) {
            // 太近 → 后退
            moveAwayFrom(player.getX(), player.getY(), gm);
        }
        // 在理想距离区间：不移动

        // 射击
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
