package de.tum.cit.fop.maze.game.achievement;

import java.util.HashSet;

/**
 * 生涯数据 (Career Data / Global Profile)
 * <p>
 * 职责：
 * 记录跨越多次游戏的累积数据，如总击杀数、已解锁成就等。
 * 此数据应由 StorageManager 独立保存为 json 文件 (如 career_data.json)。
 */
public class CareerData {

    // === 累计击杀计数 (对应成就 ACH_04 ~ ACH_08) ===
    public int totalKills_E01 = 0; // 目标 60
    public int totalKills_E02 = 0; // 目标 40
    public int totalKills_E03 = 0; // 目标 50
    public int totalKills_E04 = 0; // 目标 50
    public int totalKills_Global = 0; // 目标 500

    // === 收集类计数 ===
    public int totalHeartsCollected = 0; // 目标 50 (ACH_09)

    // 记录已获得过的宝箱Buff类型 (如 "BUFF_ATK", "BUFF_SPEED")
    // 用于判定 "寻宝大师" (ACH_10)
    public HashSet<String> collectedBuffTypes = new HashSet<>();

    // === 状态标记 (引导类/唯一类) ===
    public boolean hasWatchedPV = false;   // ACH_01
    public boolean hasHealedOnce = false;  // ACH_03
    public boolean hasClearedHardMode = false; // ACH_14
    public boolean hasKilledBoss = false;  // ACH_15

    // === 已解锁成就列表 ===
    // 存储的是 AchievementType.id
    public HashSet<String> unlockedAchievements = new HashSet<>();
}