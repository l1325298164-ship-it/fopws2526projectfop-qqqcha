package de.tum.cit.fop.maze.entities.enemy;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;

public class EnemyE03_CaramelJuggernaut extends Enemy {

    private EnemyState state = EnemyState.IDLE;

    /* ================== AOE ================== */

    private float aoeCooldown = 0f;
    private static final float AOE_INTERVAL = 1.5f;
    private static final int AOE_DAMAGE = 10;

    private Texture aoeTexture;


    public EnemyE03_CaramelJuggernaut(int x, int y) {
        super(x, y);
        size = 1.8f;
        hp = 28;
        collisionDamage = 8;
        attack = AOE_DAMAGE;

        moveSpeed = 1.8f;
        moveInterval = 0.4f;
        changeDirInterval = 999f; // 基本不用随机
        detectRange = 7f;

        aoeTexture = textureManager.getEnemy3AOETexture();

        updateTexture();
    }
    //------------------承伤-----------------
    @Override
    public void takeDamage(int dmg) {
        // 焦糖重装兵可能有护甲
        int armor = 0; // 减伤0点
        int actualDamage = Math.max(0, dmg - armor);

        super.takeDamage(actualDamage);
    }
    /* ================== 渲染 ================== */

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        // ① 只有激活 & AOE 冷却期间才画

        if (active && state == EnemyState.ATTACK) {

            batch.draw(
                    aoeTexture,
                    (worldX - 0.5f) * GameConstants.CELL_SIZE,
                    (worldY - 0.5f) * GameConstants.CELL_SIZE,
                    2 * GameConstants.CELL_SIZE,
                    2 * GameConstants.CELL_SIZE
            );
        }

        // ② 再画敌人本体（含闪烁）
        super.drawSprite(batch);
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        // 不用 Shape
    }

    @Override
    protected void updateTexture() {
        texture = textureManager.getEnemy3Texture();//贴图在这里更改
        needsTextureUpdate = false;
    }

    /* ================== 行为 ================== */

    @Override
    public void update(float delta, GameManager gm) {
        if (!active) return;

        updateHitFlash(delta);

        Player player = gm.getPlayer();
        float dist = distanceTo(player);

        aoeCooldown -= delta;

        // 激活逻辑
        boolean canSeePlayer =
                dist <= detectRange &&
                        !hasWallBetween(player, gm);

        if (canSeePlayer) {
            state = EnemyState.ATTACK;
        } else {
            state = EnemyState.IDLE;
        }

        if (state == EnemyState.ATTACK) {
            chasePlayer(delta, gm, player);
            tryAOEAttack(player, gm);
        }

        moveContinuously(delta);
    }
    private boolean hasWallBetween(Player player, GameManager gm) {

        int px = player.getX();
        int py = player.getY();

        // 只处理同一行或同一列（正交视线）
        if (x == px) {
            int minY = Math.min(y, py);
            int maxY = Math.max(y, py);
            for (int ty = minY + 1; ty < maxY; ty++) {
                if (gm.getMazeCell(x, ty) == 0) {
                    return true; // 有墙
                }
            }
        } else if (y == py) {
            int minX = Math.min(x, px);
            int maxX = Math.max(x, px);
            for (int tx = minX + 1; tx < maxX; tx++) {
                if (gm.getMazeCell(tx, y) == 0) {
                    return true; // 有墙
                }
            }
        }

        return false; // 没被墙挡住
    }

    private void tryAOEAttack(Player player, GameManager gm) {

        if (aoeCooldown > 0f) return;

        if (isPlayerInAOE(player) &&
                !hasWallBetween(player, gm)) {

            player.takeDamage(AOE_DAMAGE);
        }

        aoeCooldown = AOE_INTERVAL;
    }

    /* ================== 追击 ================== */

    private void chasePlayer(float delta, GameManager gm, Player player) {

        if (isMoving) return;

        int dx = Integer.compare(player.getX(), x);
        int dy = Integer.compare(player.getY(), y);

        // 只走正交
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

    /* ================== AOE ================== */

    private boolean isPlayerInAOE(Player player) {
        int px = player.getX();
        int py = player.getY();

        return Math.abs(px - x) <= 1 &&
                Math.abs(py - y) <= 1;
    }


    private float distanceTo(Player p) {
        float dx = p.getX() - x;
        float dy = p.getY() - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }


}
