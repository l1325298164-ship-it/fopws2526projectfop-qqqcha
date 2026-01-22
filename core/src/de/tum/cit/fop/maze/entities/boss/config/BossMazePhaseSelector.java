package de.tum.cit.fop.maze.entities.boss.config;

import com.badlogic.gdx.utils.Array;

public class BossMazePhaseSelector {

    private final Array<BossMazeConfig.Phase> phases;

    private int index = 0;
    private float timer = 0f;

    // ⭐ 是否已经触发“准备切换”（防止重复触发）
    private boolean prepareTriggered = false;

    public BossMazePhaseSelector(Array<BossMazeConfig.Phase> phases) {
        this.phases = phases;
    }

    public BossMazeConfig.Phase getCurrent() {
        return phases.get(index);
    }

    public boolean isLastPhase() {
        return index >= phases.size - 1;
    }

    /**
     * ⭐ 只用于判断：是否该开始倒计时提示
     * ❌ 不切换 phase
     */
    public boolean shouldPrepareNextPhase(float delta) {
        if (isLastPhase()) return false;

        timer += delta;

        if (!prepareTriggered && timer >= getCurrent().duration) {
            prepareTriggered = true;
            return true;
        }

        return false;
    }

    /**
     * ⭐ 真正推进到下一个 phase（只在 SWITCHING 调用）
     */
    public BossMazeConfig.Phase advanceAndGet() {
        if (!isLastPhase()) {
            index++;
        }

        timer = 0f;
        prepareTriggered = false;

        return getCurrent();
    }
}
