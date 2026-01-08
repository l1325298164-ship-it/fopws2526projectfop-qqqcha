// 修改后的 ScoreConstants.java
package de.tum.cit.fop.maze.game.score;

public class ScoreConstants {

    // === 击杀得分 ===
    public static final int SCORE_E01_PEARL = 150;
    public static final int SCORE_E02_COFFEE = 100;
    public static final int SCORE_E03_CARAMEL = 600;
    public static final int SCORE_E04_SHELL = 600;
    public static final int SCORE_BOSS = 5000;

    // === 物品得分 ===
    public static final int SCORE_HEART = 50;
    public static final int SCORE_TREASURE = 800;
    public static final int SCORE_KEY = 50;  // 钥匙得分
    public static final int SCORE_FOG_CLEARED = 500;

    // === 【新增】 解决报错的别名映射 ===
    // 其他代码引用了 ScoreConstants.E01，所以这里我们需要补充定义
    public static final int E01 = SCORE_E01_PEARL;
    public static final int E02 = SCORE_E02_COFFEE;
    public static final int E03 = SCORE_E03_CARAMEL;
    public static final int E04 = SCORE_E04_SHELL;
    public static final int BOSS = SCORE_BOSS;

    // ... (保留原本的 TARGET_... 代码不变)
    public static final int TARGET_KILLS_E01 = 60;
    public static final int TARGET_KILLS_E02 = 40;
    public static final int TARGET_KILLS_E03 = 50;
    public static final int TARGET_KILLS_E04_DASH = 50;
    public static final int TARGET_KILLS_GLOBAL = 500;
    public static final int TARGET_HEARTS_COLLECTED = 50;
    public static final int TARGET_TREASURE_TYPES = 3;
    public static final int TARGET_NO_DAMAGE_LIMIT = 3;
}