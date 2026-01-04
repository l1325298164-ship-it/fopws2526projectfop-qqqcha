package de.tum.cit.fop.maze.entities.trap;

import de.tum.cit.fop.maze.entities.GameObject;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameManager;

public abstract class Trap extends GameObject {

    protected boolean active = true;

    public Trap(int x, int y) {
        super(x, y);
    }

    public abstract void update(float delta);

    /** 每帧更新 */
    public abstract void update(float delta, GameManager gameManager);

    /** 玩家踩上时 */
    public abstract void onPlayerStep(Player player);

    /** 是否还需要参与渲染 */
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean isPassable() {
        // 陷阱默认不可通过
        return false;
    }
}
