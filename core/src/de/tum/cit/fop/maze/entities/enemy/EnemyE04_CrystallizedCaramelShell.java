package de.tum.cit.fop.maze.entities.enemy;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;

public class EnemyE04_CrystallizedCaramelShell extends Enemy {

    /* ================== 状态 ================== */


    /* ================== 构造 ================== */

    public EnemyE04_CrystallizedCaramelShell(int x, int y) {
        super(x, y);

        size = 1.4f;

        hp = 36;
        collisionDamage = 6;
        attack = 6;

        moveInterval = 0.55f;          // 壳状态：慢
        changeDirInterval = 1.2f;
        detectRange = 6f;

        updateTexture();
    }

    /* ================== 受伤逻辑 ================== */

    @Override
    public void takeDamage(int dmg) {

        // Dash 命中：直接破壳 → 死亡
        if (isHitByDash()) {
            dieByShellBreak();
            resetDashHit();
            return;
        }

        // 普通攻击：几乎无效（敲壳）
        int reduced = Math.max(1, dmg / 5);
        super.takeDamage(reduced);
    }

    private void dieByShellBreak() {
        active = false;
        hp = 0; // ⭐ 明确死亡状态

        Logger.debug("E04 Crystallized Shell shattered and died");
    }



    /* ================== 渲染 ================== */

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    @Override
    protected void updateTexture() {
        texture = textureManager.getEnemy4ShellTexture();
        needsTextureUpdate = false;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        super.drawSprite(batch);
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        // 暂不需要
    }

    /* ================== 行为 ================== */

    @Override
    public void update(float delta, GameManager gm) {
        if (!active) return;

        updateHitFlash(delta);

        Player player = gm.getPlayer();
        float dist = distanceTo(player);

        if (dist <= detectRange) {
            chasePlayer(gm, player);
        }

        // ⭐ 必须有
        moveContinuously(delta);
    }


    /* ================== 行为辅助 ================== */

    private void chasePlayer(GameManager gm, Player player) {

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

    private float distanceTo(Player p) {
        float dx = p.getX() - x;
        float dy = p.getY() - y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
