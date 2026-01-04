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


    /* ===== 陷阱数量 ===== */
    public final int trapT01GeyserCount;
    public final int trapT02PearlMineCount;
    public final int trapT03TeaShardCount;
    public final int trapT04MudTileCount;
    public int enemyE04ShellCount;
    /* ===== 战斗参数 ===== */
    public final int initialLives;
    public final float enemyHpMultiplier;
    public final float enemyDamageMultiplier;


    /* ===== 构造器（私有，强制走工厂） ===== */
    public DifficultyConfig(
            Difficulty difficulty, int mazeWidth,
            int mazeHeight,
            int exitCount,

            int enemyE01PearlCount,
            int enemyE02CoffeeBeanCount,
            int enemyE03CaramelCount,
            int enemyE04ShellCount,

            int trapT01GeyserCount,
            int trapT02PearlMineCount,
            int trapT03TeaShardCount,
            int trapT04MudTileCount,

            int initialLives,
            float enemyHpMultiplier,
            float enemyDamageMultiplier
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
    }

    /* ===== 难度工厂 ===== */
    public static DifficultyConfig  of(Difficulty d) {
        return switch (d) {

            case EASY -> new DifficultyConfig(
                    /* 地图 */
                    Difficulty.EASY, 40, 40, 1,

                    /* 敌人 */
                    4, 2, 0,0,

                    /* 陷阱 */
                    0, 0, 0, 0,

                    /* 战斗 */
                    5,
                    0.7f,
                    0.6f
            );

            case NORMAL -> new DifficultyConfig(
                    /* 地图 */
                    Difficulty.NORMAL, 50, 50, 3,

                    /* 敌人 */
                    8, 6, 2,0,

                    /* 陷阱 */
                    2, 1, 1, 10,

                    /* 战斗 */
                    3,
                    1.0f,
                    1.0f
            );

            case HARD -> new DifficultyConfig(
                    /* 地图 */
                    Difficulty.HARD, 100, 100, 4,

                    /* 敌人 */
                    12, 10, 6,1,

                    /* 陷阱 */
                    4, 3, 3, 20,

                    /* 战斗 */
                    2,
                    1.4f,
                    1.3f
            );
            case TUTORIAL -> new DifficultyConfig(
                    /* 地图 */
                    Difficulty.TUTORIAL, 40, 40, 1,

                    /* 敌人 */
                    0, 0, 0,0,

                    /* 陷阱 */
                    0, 0, 0, 0,

                    /* 战斗 */
                    5,
                    1.4f,
                    1.3f
            );
        };
    }
}

