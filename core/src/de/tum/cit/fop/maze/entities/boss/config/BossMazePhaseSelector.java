package de.tum.cit.fop.maze.entities.boss.config;

import com.badlogic.gdx.utils.Array;

public class BossMazePhaseSelector {

    private final Array<BossMazeConfig.Phase> phases;
    private int index = 0;
    private float timer = 0f;

    public BossMazePhaseSelector(Array<BossMazeConfig.Phase> phases) {
        this.phases = phases;
    }

    public BossMazeConfig.Phase getCurrent() {
        return phases.get(index);
    }

    public boolean update(float delta) {
        timer += delta;
        if (timer >= getCurrent().duration) {
            timer = 0f;
            if (index < phases.size - 1) {
                index++;
                return true;
            }
        }
        return false;
    }
    public boolean isLastPhase() {
        return index >= phases.size - 1;
    }
}
