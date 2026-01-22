package de.tum.cit.fop.maze.game.event;

import de.tum.cit.fop.maze.game.EnemyTier;
import de.tum.cit.fop.maze.game.score.DamageSource;

/**
 * 游戏事件监听器接口 (Observer Interface)
 * <p>
 * 职责：
 * 定义游戏核心逻辑 (GameManager) 向外部系统 (分数、成就) 发送通知的标准协议。
 * <p>
 * 使用方式：
 * 1. ScoreManager 和 AchievementManager 实现此接口。
 * 2. GameManager 持有 List&lt;GameListener&gt; 并在事件发生时遍历调用。
 */
public interface GameListener {

    /**
     * 当敌人被击杀时触发。
     * 对应需求：
     * - 分数系统：根据 EnemyTier 增加不同分数 (+150/+600 等)。
     * - 成就系统：累积击杀计数 (如"筛除废料"需60个E01)。
     *
     * @param tier       敌人的阶级/类型 (E01, E02, E03, E04, BOSS)
     * @param isDashKill 是否是被玩家的冲刺技能 (Dash) 击杀的。
     * (用于判定成就 "破壳:结晶焦糖"，要求必须冲刺击杀 E04)
     */
    void onEnemyKilled(EnemyTier tier, boolean isDashKill);

    /**
     * 当玩家受伤时触发。
     * 对应需求：
     * - 分数系统：根据伤害来源 (Source) 扣除相应分数 (如被陷阱伤扣50，被移动墙伤扣200)。
     * - 成就系统：统计单局受击次数 (用于判定 "封口:滴水不漏" 无伤成就)。
     *
     * @param currentHp 玩家受伤后的剩余血量。
     * @param source    伤害来源枚举，标识是哪种怪物或陷阱造成的伤害。
     */
    void onPlayerDamage(int currentHp, DamageSource source);

    /**
     * 当物品被拾取、宝箱开启或特殊互动发生时触发。
     * 对应需求：
     * - 分数系统：拾取 Heart (+50)，开启宝箱 (+800)，驱散迷雾 (+500)。
     * - 成就系统：累计 Heart 拾取数，记录宝箱 Buff 种类 (寻宝大师)。
     *
     * @param itemType 物品或事件的类型标识字符串。
     * 建议使用常量或约定字符串，例如:
     * - "HEART" (红心)
     * - "TREASURE_ATK" / "TREASURE_SPEED" (宝箱Buff)
     * - "BOBA" (脆波波)
     * - "FOG_CLEARED" (驱散迷雾)
     */
    void onItemCollected(String itemType);

    /**
     * 当关卡完成（玩家到达出口并触发结算）时触发。
     * 对应需求：
     * - 分数系统：进行本关评级结算 (S/A/B/C/D)。
     * - 成就系统：检查通关类成就 (如"迷宫第一杯")。
     *
     * @param levelNumber 刚刚完成的关卡编号 (例如 1 代表 Level 1)。
     */
    void onLevelFinished(int levelNumber);
}