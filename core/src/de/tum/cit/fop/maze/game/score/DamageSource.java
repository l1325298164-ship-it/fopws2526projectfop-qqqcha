package de.tum.cit.fop.maze.game.score;

/**
 * 伤害来源枚举
 * <p>
 * 用于标识玩家受到的伤害类型，以便 ScoreManager 根据《分数系统.pdf》计算相应的扣分。
 */
public enum DamageSource {
    // === 敌人攻击 ===
    /** 腐败珍珠 (-50 pts) */
    ENEMY_E01(50),
    /** 小包咖啡豆 (-50 pts) */
    ENEMY_E02(50),
    /** 焦糖重装 (-100 pts) */
    ENEMY_E03(100),
    /** 结晶焦糖壳 (-100 pts) */
    ENEMY_E04(100),
    /** Boss 攻击 (假设值，文档未明确，设定较高惩罚) */
    BOSS(200),

    // === 陷阱伤害 ===
    /** 茶叶碎 (-50 pts) */
    TRAP_SPIKE(50),
    /** 焦糖地喷 (-100 pts) */
    TRAP_GEYSER(100),
    /** 过期珍珠雷 (-150 pts) */
    TRAP_MINE(150),
    /** 泥潭/其他 (-20 pts，轻微惩罚) */
    TRAP_MUD(20),

    // === 环境/机制 ===
    /** 移动墙挤压 (-200 pts，重大失误) */
    OBSTACLE_WALL(200),
    /** 未知来源 (不扣分或默认扣分) */
    UNKNOWN(0);

    /** 该伤害源的基础扣分值 (未乘倍率) */
    public final int penaltyScore;

    DamageSource(int score) {
        this.penaltyScore = score;
    }
}