package de.tum.cit.fop.maze.game.score;

/**
 * 游戏数值常量定义
 * 包括：分数、成就计数阈值、物品分值等
 */
public class ScoreConstants {

    // === 击杀得分 ===
    public static final int SCORE_E01_PEARL = 150;
    public static final int SCORE_E02_COFFEE = 100;
    public static final int SCORE_E03_CARAMEL = 600;
    public static final int SCORE_E04_SHELL = 600;
    public static final int SCORE_BOSS = 5000;

    // === 物品得分 ===
    public static final int SCORE_HEART = 50;
    public static final int SCORE_TREASURE = 800; // 任意宝箱
    public static final int SCORE_FOG_CLEARED = 500;

    // === 成就阈值 (Target Counts) ===
    public static final int TARGET_KILLS_E01 = 60;      // ACH_04
    public static final int TARGET_KILLS_E02 = 40;      // ACH_05
    public static final int TARGET_KILLS_E03 = 50;      // ACH_06
    public static final int TARGET_KILLS_E04_DASH = 50; // ACH_07
    public static final int TARGET_KILLS_GLOBAL = 500;  // ACH_08

    public static final int TARGET_HEARTS_COLLECTED = 50;     // ACH_09
    public static final int TARGET_TREASURE_TYPES = 3;        // ACH_10
    public static final int TARGET_NO_DAMAGE_LIMIT = 3;       // ACH_11 (受击<=3)
}
