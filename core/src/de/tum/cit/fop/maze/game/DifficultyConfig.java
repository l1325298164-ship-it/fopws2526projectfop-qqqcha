package de.tum.cit.fop.maze.game;

public class DifficultyConfig {
    public final Difficulty difficulty;

    /* ===== 地图 ===== */
    public final int mazeWidth;
    public final int mazeHeight;
    public final int exitCount;

    /* ===== 敌人数量 ===== */
    public final int enemyE01PearlCount;
    public final int enemyE02CoffeeBeanCount;
    public final int enemyE03CaramelCount;
    public int enemyE04ShellCount;

    /* ===== 陷阱数量 ===== */
    public final int trapT01GeyserCount;
    public final int trapT02PearlMineCount;
    public final int trapT03TeaShardCount;
    public final int trapT04MudTileCount;
    /* ===== 战斗参数 ===== */
    public final int initialLives;
    public final float enemyHpMultiplier;
    public final float enemyDamageMultiplier;
    public final int keyCount;

    /* ===== 【新增】 分数与惩罚倍率 ===== */
    public final float scoreMultiplier;
    public final float penaltyMultiplier;

    public DifficultyConfig(
            Difficulty difficulty, int mazeWidth, int mazeHeight, int exitCount,
            int enemyE01PearlCount, int enemyE02CoffeeBeanCount, int enemyE03CaramelCount, int enemyE04ShellCount,
            int trapT01GeyserCount, int trapT02PearlMineCount, int trapT03TeaShardCount, int trapT04MudTileCount,
            int initialLives, float enemyHpMultiplier, float enemyDamageMultiplier, int keyCount,
            // 【新增】 构造参数
            float scoreMultiplier, float penaltyMultiplier

    ) {
        this.difficulty = difficulty;
        this.mazeWidth = mazeWidth;
        this.mazeHeight = mazeHeight;
        this.exitCount = exitCount;

        this.enemyE01PearlCount = enemyE01PearlCount;
        this.enemyE02CoffeeBeanCount = enemyE02CoffeeBeanCount;
        this.enemyE03CaramelCount = enemyE03CaramelCount;
        this.enemyE04ShellCount = enemyE04ShellCount;

        this.trapT01GeyserCount = trapT01GeyserCount;
        this.trapT02PearlMineCount = trapT02PearlMineCount;
        this.trapT03TeaShardCount = trapT03TeaShardCount;
        this.trapT04MudTileCount = trapT04MudTileCount;

        this.initialLives = initialLives;
        this.enemyHpMultiplier = enemyHpMultiplier;
        this.enemyDamageMultiplier = enemyDamageMultiplier;
        this.keyCount = keyCount;

        // 【新增】 赋值
        this.scoreMultiplier = scoreMultiplier;
        this.penaltyMultiplier = penaltyMultiplier;
    }

    /* ===== 难度工厂 ===== */
    public static DifficultyConfig of(Difficulty d) {
        return switch (d) {

            case EASY -> new DifficultyConfig(
                    Difficulty.EASY, 40, 40, 1,
                    0, 0, 0, 10,  // 敌人
                    0, 0, 0, 0,  // 陷阱
                    200, 0.7f, 0.6f, 2, // 战斗
                    1.0f, 0.5f // 【新增】 scoreMultiplier, penaltyMultiplier

            );

            case NORMAL -> new DifficultyConfig(
                    Difficulty.NORMAL, 50, 50, 3,
                    8, 6, 2, 0,

                    /* 敌人 */
                    8, 6, 2,0,

                    /* 陷阱 */
                    2, 1, 1, 10,
                    200, 1.0f
            );

            case HARD -> new DifficultyConfig(
                    /* 地图 */
                    Difficulty.HARD, 100, 100, 4,
                    12, 10, 6, 1,

                    /* 敌人 */
                    12, 10, 6,1,

                    /* 陷阱 */
                    4, 3, 3, 20,
                    200, 1.4f
            );

            case TUTORIAL -> new DifficultyConfig(
                    /* 地图 */
                    Difficulty.TUTORIAL, 40, 40, 1,
                    0, 0, 0, 0,

                    /* 敌人 */
                    0, 0, 0,0,

                    /* 陷阱 */
                    0, 0, 0, 0,
                    5, 1.4f
            );
            case ENDLESS -> new DifficultyConfig(
                    Difficulty.ENDLESS, 40, 40, 0,
                    7, 5, 4, 4,
                    10, 5, 3, 2, // 修正了这里原本的语法错误 (原代码有一堆奇怪的逗号)
                    400, 1.3f, 1.3f, 0, // 修正了原本缺少的参数
                    2.0f, 1.2f
            );
            case BOSS -> new DifficultyConfig(
                    Difficulty.BOSS, 40, 40, 0,

                    5, 3, 0, 0,   // enemies
                    0, 0, 0, 0,   // traps

                    200,
                    1.0f,
                    1.0f,
                    0,1,1
            );


        };
    }
}