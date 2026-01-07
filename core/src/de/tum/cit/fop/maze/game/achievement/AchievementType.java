package de.tum.cit.fop.maze.game.achievement;

/**
 * 成就类型枚举
 * <p>
 * 定义了游戏中所有的成就 ID、名称和描述。
 * 参考来源:
 */
public enum AchievementType {

    // === 复建 (引导类) ===
    ACH_01_TRAINING("ACH_01", "培训:背诵配方", "完整观看一次PV"),
    ACH_02_FIRST_CUP("ACH_02", "出餐:迷宫第一杯", "通关第1小关"),
    ACH_03_BOBA_RESCUE("ACH_03", "加料:脆波波救急", "首次拾取 晶莹柠檬脆波波 (回血道具)"),

    // === 每日备料 (击杀计数类) ===
    ACH_04_PEARL_SWEEPER("ACH_04", "筛除废料:腐败珍珠", "累计击杀60个 E01 腐败珍珠"),
    ACH_05_COFFEE_GRINDER("ACH_05", "研磨:小包咖啡豆", "累计击杀40个 E02 小包咖啡豆"),
    ACH_06_CARAMEL_MELT("ACH_06", "融糖:焦糖重装", "累计击杀50个 E03 焦糖重装"),
    ACH_07_SHELL_BREAKER("ACH_07", "破壳:结晶焦糖", "累计冲刺击杀50个 E04 结晶焦糖壳"),
    ACH_08_BEST_SELLER("ACH_08", "爆单王:销量领先", "累计击杀任意敌人总数达到500个"),

    // === 进阶调茶 (收集与机制类) ===
    ACH_09_FREE_TOPPING("ACH_09", "每周二小料免费", "累计拾取50次 Heart"),
    ACH_10_TREASURE_MASTER("ACH_10", "寻宝大师", "累计获得过3种不同的宝箱增益"),
    ACH_11_SEALED_TIGHT("ACH_11", "封口:滴水不漏", "在任意关卡结算时，受击次数<=3"),
    ACH_12_MINE_EXPERT("ACH_12", "扫雷专家", "在不受伤的情况下诱导过期珍珠雷爆炸10次 (未实装/可选)"),

    // === 金牌店长 (通关与挑战类) ===
    ACH_13_TRUE_RECIPE("ACH_13", "“真相”- 至尊果茶", "获得全部至尊果茶隐藏配方 (未实装/可选)"),
    ACH_14_RENAISSANCE("ACH_14", "“复兴”", "在困难模式下通关游戏"),
    ACH_15_SUCCESS("ACH_15", "“成功?”", "首次击杀 Boss");

    public final String id;
    public final String displayName;
    public final String description;

    AchievementType(String id, String displayName, String description) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
    }
}