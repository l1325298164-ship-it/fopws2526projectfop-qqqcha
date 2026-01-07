package de.tum.cit.fop.maze.entities;

public class TutorialPlayer extends Player {

    public TutorialPlayer(int x, int y) {
        super(x, y);
    }

    @Override
    public void update(float delta) {
        // Tutorial 里只需要动画，不要任何 GameManager 逻辑
        updateAnimation(delta);
    }

    private void updateAnimation(float delta) {
    }

    @Override
    public void useAbility(int slot) {
        // Tutorial 禁用技能（防止误触）
    }

    @Override
    public boolean isDead() {
        return false;
    }
}
