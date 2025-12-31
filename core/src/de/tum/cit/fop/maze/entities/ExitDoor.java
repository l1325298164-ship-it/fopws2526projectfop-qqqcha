package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import de.tum.cit.fop.maze.effects.portal.PortalEffectManager;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.utils.Logger;

public class ExitDoor extends GameObject {
    private final PortalEffectManager portalEffect = new PortalEffectManager();
    private boolean triggered = false;

    private boolean locked = true;

    public ExitDoor(int x, int y, int index) {
        super(x, y);
        this.active = true;

        Logger.debug("ExitDoor created at " + getPositionString());
    }

    /* ================= 状态 ================= */

    public boolean isLocked() {
        return locked;
    }

    public void unlock() {
        locked = false;
        Logger.gameEvent("Exit unlocked at " + getPositionString());
    }
    public void update(float delta, GameManager gm) {
        portalEffect.update(delta);

        if (portalEffect.isFinished()) {
            gm.nextLevel();   // ✅ 真正推进关卡
        }
    }

    /* ================= 行为 ================= */

    @Override
    public boolean isPassable() {
        // 🔥 关键：没钥匙前 = 墙
        return !locked;
    }
    public void onPlayerStep(Player player) {
        if (locked || triggered) return;

        triggered = true;

        // 🔥 启动龙卷风 + 呼吸灯
        portalEffect.startExitAnimation(
                x * GameConstants.CELL_SIZE,
                y * GameConstants.CELL_SIZE
        );
    }


    @Override
    public boolean isInteractable() {
        return !locked;
    }

    @Override
    public void onInteract(Player player) {
        if (locked) return;

        // 只做标记，不跳关
        this.active = false;
        Logger.gameEvent("Player stepped on exit at " + getPositionString());
    }


    /* ================= 渲染 ================= */

    @Override
    public void drawSprite(SpriteBatch batch) {
        // 如果你用的是 MazeRenderer 墙系统，这里可以留空
        // 出口本来就是墙的一部分
    }

    @Override
    public RenderType getRenderType() {
        return null;
    }

    @Override
    public void drawShape(ShapeRenderer shapeRenderer) {
        if (locked) return;

        // 解锁后，用绿色标识可进入区域（调试用）
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 1, 0, 0.5f);
        shapeRenderer.rect(
                x * GameConstants.CELL_SIZE,
                y * GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE,
                GameConstants.CELL_SIZE
        );
        shapeRenderer.end();
    }

    public void renderPortalFront(SpriteBatch batch) {
        portalEffect.renderFront(batch);
    }

    public void renderPortalBack(SpriteBatch batch) {
        portalEffect.renderBack(batch,
                x * GameConstants.CELL_SIZE,
                y * GameConstants.CELL_SIZE);
    }
}
