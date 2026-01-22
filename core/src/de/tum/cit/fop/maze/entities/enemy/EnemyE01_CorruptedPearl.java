package de.tum.cit.fop.maze.entities.enemy;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.entities.enemy.EnemyBoba.BobaBullet;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.utils.Logger;

public class EnemyE01_CorruptedPearl extends Enemy {

    private EnemyState state = EnemyState.PATROL;

    private float shootCooldown = 0f;
    private static final float SHOOT_INTERVAL = 1.2f;
    // ===== 攻击表现用 =====
    private boolean isAttacking = false;
    private float attackTimer = 0f;
    protected Animation<TextureRegion> anim;

    // 攻击节奏（远程怪推荐）
    private static final float ATTACK_WINDUP = 0.25f;   // 蓄力
    private static final float ATTACK_FLASH = 0.08f;    // 开火瞬间
    @Override
    protected AudioType getAttackSound() {
        return AudioType.ENEMY_ATTACK_E01;
    }

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
        Logger.debug("E01 constructed, needsTextureUpdate=" + needsTextureUpdate);
    }
    @Override
    public void takeDamage(int dmg) {
        // 先调用父类的通用伤害处理
        super.takeDamage(dmg);
        AudioManager.getInstance().play(AudioType.ENEMY_ATTACKED_E01);


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
        super.drawSprite(batch); // ✅ 现在是对的
    }




    @Override
    protected void updateTexture() {
        Logger.debug("E01 updateTexture CALLED");
        // 1️⃣ 前（DOWN）
        TextureAtlas frontAtlas =
                textureManager.getEnemy1AtlasFront();
        Logger.debug("E01 atlas = " + frontAtlas);
        frontAnim = new Animation<>(
                0.12f,
                frontAtlas.findRegions("E01_front"),
                Animation.PlayMode.LOOP
        );

        // 2️⃣ 后（UP）
        TextureAtlas backAtlas =
                textureManager.getEnemy1AtlasBack();
        Logger.debug("E01 atlas = " + backAtlas);
        backAnim = new Animation<>(
                0.12f,
                backAtlas.findRegions("E01_back"),
                Animation.PlayMode.LOOP
        );

        // 3️⃣ 左右（SIDE）
        TextureAtlas sideAtlas =
                textureManager.getEnemy1AtlasRL();
        Logger.debug("E01 atlas = " + sideAtlas);
        leftAnim = new Animation<>(
                0.12f,
                sideAtlas.findRegions("E01_left"),
                Animation.PlayMode.LOOP
        );
        rightAnim = new Animation<>(
                0.12f,
                sideAtlas.findRegions("E01_right"),
                Animation.PlayMode.LOOP
        );

        needsTextureUpdate = false;
    }

    /* ================== 行为 ================== */

    @Override
    public void update(float delta, GameManager gm) {

        if (isMoving) {
            stateTime += delta;
        } else {
            stateTime = 0f;
        }

        if (!active) return;

        updateHitFlash(delta);

        // ✅ 唯一合法目标来源
        Player target = gm.getNearestAlivePlayer(x, y);
        if (target == null) return;

        float dist = distanceTo(target);

        shootCooldown -= delta;

        state = (dist <= detectRange)
                ? EnemyState.ATTACK
                : EnemyState.PATROL;

        // ===== 攻击表现驱动 =====
        if (isAttacking) {
            attackTimer += delta;

            if (attackTimer >= ATTACK_WINDUP) {
                shootAt(target, gm);
                AudioManager.getInstance().play(AudioType.ENEMY_ATTACK_E01);
                shootCooldown = SHOOT_INTERVAL;
                attackTimer = -ATTACK_FLASH;
            }

            if (attackTimer >= 0f) {
                isAttacking = false;
                attackTimer = 0f;
            }

            return;
        }

        switch (state) {
            case PATROL -> patrol(delta, gm);
            case ATTACK -> combat(delta, gm, target, dist);
        }

        moveContinuously(delta);
    }


    private void patrol(float delta, GameManager gm) {
        tryMoveRandom(delta, gm);
    }

    private void combat(float delta, GameManager gm, Player target, float dist) {

        float idealDistance = detectRange * 0.5f;

        if (shootCooldown <= 0f && !isAttacking) {
            isAttacking = true;
            attackTimer = 0f;
        }

        if (!isMoving) {
            int dx = Integer.compare(target.getX(), x);
            int dy = Integer.compare(target.getY(), y);

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
            shootAt(target, gm);
            shootCooldown = SHOOT_INTERVAL;
        }
    }


    private void shootAt(Player target, GameManager gm) {
        float dx = target.getX() - x;
        float dy = target.getY() - y;

        BobaBullet bullet = new BobaBullet(
                x + 0.5f, y + 0.5f, dx, dy, attack
        );

        gm.spawnProjectile(bullet);
        Logger.debug("✅ E01 发射 BobaBullet → " + target.getPlayerIndex());
    }


    private float distanceTo(Player p) {
        float dx = p.getX() - x;
        float dy = p.getY() - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
