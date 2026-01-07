package de.tum.cit.fop.maze.game.achievement;

import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.game.EnemyTier;
import de.tum.cit.fop.maze.game.GameSaveData;
import de.tum.cit.fop.maze.game.event.GameListener;
import de.tum.cit.fop.maze.game.score.DamageSource;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.StorageManager;

/**
 * 成就管理器
 * <p>
 * 职责：
 * 1. 监听游戏事件 (击杀、拾取、通关)。
 * 2. 更新生涯数据 (CareerData)。
 * 3. 判定成就条件是否满足，若满足则解锁并通知 UI。
 * 4. 实时保存生涯数据，防止丢失。
 */
public class AchievementManager implements GameListener {

    private final CareerData careerData;
    private final GameSaveData gameSaveData; // 用于通知单局UI (SettlementScreen)
    private final StorageManager storageManager;
    private final Difficulty currentDifficulty;

    public AchievementManager(CareerData careerData,
                              GameSaveData gameSaveData,
                              StorageManager storageManager,
                              Difficulty currentDifficulty) {
        this.careerData = careerData;
        this.gameSaveData = gameSaveData;
        this.storageManager = storageManager;
        this.currentDifficulty = currentDifficulty;
    }

    @Override
    public void onEnemyKilled(EnemyTier tier, boolean isDashKill) {
        // 1. 更新全局总击杀 (ACH_08)
        careerData.totalKills_Global++;
        if (careerData.totalKills_Global >= 500) {
            unlock(AchievementType.ACH_08_BEST_SELLER);
        }

        // 2. 更新特定怪物击杀
        switch (tier) {
            case E01 -> {
                careerData.totalKills_E01++;
                if (careerData.totalKills_E01 >= 60) unlock(AchievementType.ACH_04_PEARL_SWEEPER);
            }
            case E02 -> {
                careerData.totalKills_E02++;
                if (careerData.totalKills_E02 >= 40) unlock(AchievementType.ACH_05_COFFEE_GRINDER);
            }
            case E03 -> {
                careerData.totalKills_E03++;
                if (careerData.totalKills_E03 >= 50) unlock(AchievementType.ACH_06_CARAMEL_MELT);
            }
            case E04 -> {
                // E04 需要冲刺击杀才能判定 ACH_07
                if (isDashKill) {
                    careerData.totalKills_E04++;
                    if (careerData.totalKills_E04 >= 50) unlock(AchievementType.ACH_07_SHELL_BREAKER);
                }
            }
            case BOSS -> {
                if (!careerData.hasKilledBoss) {
                    careerData.hasKilledBoss = true;
                    unlock(AchievementType.ACH_15_SUCCESS);
                }
            }
        }

        // 3. 保存生涯数据
        saveCareer();
    }

    @Override
    public void onPlayerDamage(int currentHp, DamageSource source) {
        // 记录单局受击数 (用于 ACH_11 判定)
        gameSaveData.sessionDamageTaken++;
    }

    @Override
    public void onItemCollected(String itemType) {
        if ("HEART".equals(itemType)) {
            careerData.totalHeartsCollected++;
            if (careerData.totalHeartsCollected >= 50) {
                unlock(AchievementType.ACH_09_FREE_TOPPING);
            }
        }
        else if (itemType.startsWith("TREASURE")) {
            // itemType 例如 "TREASURE_ATK", "TREASURE_SPEED"
            careerData.collectedBuffTypes.add(itemType);
            if (careerData.collectedBuffTypes.size() >= 3) {
                unlock(AchievementType.ACH_10_TREASURE_MASTER);
            }
        }
        else if ("BOBA".equals(itemType)) {
            if (!careerData.hasHealedOnce) {
                careerData.hasHealedOnce = true;
                unlock(AchievementType.ACH_03_BOBA_RESCUE);
            }
        }

        saveCareer();
    }

    @Override
    public void onLevelFinished(int levelNumber) {
        // ACH_02: 通关第1小关
        if (levelNumber == 1) {
            unlock(AchievementType.ACH_02_FIRST_CUP);
        }

        // ACH_11: 滴水不漏 (单局受击<=3)
        // 注意：这是单局结算，需要在每关结束时检查，或者在通关时检查
        if (gameSaveData.sessionDamageTaken <= 3) {
            unlock(AchievementType.ACH_11_SEALED_TIGHT);
        }

        // ACH_14: 复兴 (困难模式通关)
        // 假设 levelNumber 3 是最后一关
        if (levelNumber >= 3 && currentDifficulty == Difficulty.HARD) {
            if (!careerData.hasClearedHardMode) {
                careerData.hasClearedHardMode = true;
                unlock(AchievementType.ACH_14_RENAISSANCE);
            }
        }

        saveCareer();
    }

    /**
     * PV 播放完毕时调用 (需手动调用此方法)
     */
    public void onPVWatched() {
        if (!careerData.hasWatchedPV) {
            careerData.hasWatchedPV = true;
            unlock(AchievementType.ACH_01_TRAINING);
            saveCareer();
        }
    }

    /**
     * 核心解锁逻辑
     */
    private void unlock(AchievementType type) {
        // 如果尚未解锁
        if (!careerData.unlockedAchievements.contains(type.id)) {
            // 1. 标记生涯解锁
            careerData.unlockedAchievements.add(type.id);

            // 2. 通知单局数据 (用于结算界面展示 "New Achievements!")
            gameSaveData.recordNewAchievement(type.id);

            Logger.info("🏆 Achievement Unlocked: " + type.displayName);

            // TODO: 这里可以调用 HUD 显示实时 Toast 弹窗
        }
    }

    private void saveCareer() {
        if (storageManager != null) {
            storageManager.saveCareer(careerData);
        }
    }
}