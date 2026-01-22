package de.tum.cit.fop.maze.entities.boss.config;

public class BossHpController {

    private float bossHp;
    private float maxHp;

    public void onEnemyDamaged(float dmg) {
        bossHp -= dmg * 0.2f; // 比例你定
    }

    public float getHpPercent() {
        return bossHp / maxHp;
    }
}
