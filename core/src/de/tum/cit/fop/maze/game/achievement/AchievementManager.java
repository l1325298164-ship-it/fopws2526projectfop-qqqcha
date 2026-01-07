package de.tum.cit.fop.maze.game.achievement;

import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.game.EnemyTier;
import de.tum.cit.fop.maze.game.GameSaveData;
import de.tum.cit.fop.maze.game.event.GameListener;
import de.tum.cit.fop.maze.game.score.DamageSource;
import de.tum.cit.fop.maze.game.score.ScoreConstants;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.StorageManager;

/**
 * 成就管理器
 * <p>
 * 修正注记：
 * 1. 移除了高频 I/O 操作 (saveCareer)，仅在关卡结束或重要节点保存。
 * 2. 使用 ScoreConstants 统一阈值。
 */
public class AchievementManager implements GameListener {

    private final CareerData careerData;
    private final GameSaveData gameSaveData; // 当前关卡快照
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
        // 1. 更新全局总击杀
        careerData.totalKills_Global++;
        if (careerData.totalKills_Global >= ScoreConstants.TARGET_KILLS_GLOBAL) {
            unlock(AchievementType.ACH_08_BEST_SELLER);
        }

        // 2. 更新特定怪物击杀
        switch (tier) {
            case E01 -> {
                careerData.totalKills_E01++;
                if (careerData.totalKills_E01 >= ScoreConstants.TARGET_KILLS_E01)
                    unlock(AchievementType.ACH_04_PEARL_SWEEPER);
            }
            case E02 -> {
                careerData.totalKills_E02++;
                if (careerData.totalKills_E02 >= ScoreConstants.TARGET_KILLS_E02)
                    unlock(AchievementType.ACH_05_COFFEE_GRINDER);
            }
            case E03 -> {
                careerData.totalKills_E03++;
                if (careerData.totalKills_E03 >= ScoreConstants.TARGET_KILLS_E03)
                    unlock(AchievementType.ACH_06_CARAMEL_MELT);
            }
            case E04 -> {
                if (isDashKill) {
                    careerData.totalKills_E04++;
                    if (careerData.totalKills_E04 >= ScoreConstants.TARGET_KILLS_E04_DASH)
                        unlock(AchievementType.ACH_07_SHELL_BREAKER);
                }
            }
            case BOSS -> {
                if (!careerData.hasKilledBoss) {
                    careerData.hasKilledBoss = true;
                    unlock(AchievementType.ACH_15_SUCCESS);
                }
            }
        }
        // 注意：此处不再调用 saveCareer()，避免战斗中频繁 I/O
    }

    @Override
    public void onPlayerDamage(int currentHp, DamageSource source) {
        // 记录本关受击数 (GameSaveData 是当前关卡快照)
        gameSaveData.sessionDamageTaken++;
    }

    @Override
    public void onItemCollected(String itemType) {
        if ("HEART".equals(itemType)) {
            careerData.totalHeartsCollected++;
            if (careerData.totalHeartsCollected >= ScoreConstants.TARGET_HEARTS_COLLECTED) {
                unlock(AchievementType.ACH_09_FREE_TOPPING);
            }
        }
        else if (itemType != null && itemType.startsWith("TREASURE")) {
            careerData.collectedBuffTypes.add(itemType);
            if (careerData.collectedBuffTypes.size() >= ScoreConstants.TARGET_TREASURE_TYPES) {
                unlock(AchievementType.ACH_10_TREASURE_MASTER);
            }
        }
        else if ("BOBA".equals(itemType)) {
            if (!careerData.hasHealedOnce) {
                careerData.hasHealedOnce = true;
                unlock(AchievementType.ACH_03_BOBA_RESCUE);
            }
        }
        // 注意：此处不再调用 saveCareer()
    }

    @Override
    public void onLevelFinished(int levelNumber) {
        // ACH_02: 通关第1小关
        if (levelNumber == 1) {
            unlock(AchievementType.ACH_02_FIRST_CUP);
        }

        // ACH_11: 滴水不漏 (本关受击<=3)
        // 依赖 GameSaveData (当前关卡快照) 的准确性
        if (gameSaveData.sessionDamageTaken <= ScoreConstants.TARGET_NO_DAMAGE_LIMIT) {
            unlock(AchievementType.ACH_11_SEALED_TIGHT);
        }

        // ACH_14: 复兴 (困难模式通关) - 假设 level 3 是最后一关
        if (levelNumber >= 3 && currentDifficulty == Difficulty.HARD) {
            if (!careerData.hasClearedHardMode) {
                careerData.hasClearedHardMode = true;
                unlock(AchievementType.ACH_14_RENAISSANCE);
            }
        }

        // 关卡结束是非常好的保存时机
        saveCareer();
    }

    public void onPVWatched() {
        if (!careerData.hasWatchedPV) {
            careerData.hasWatchedPV = true;
            unlock(AchievementType.ACH_01_TRAINING);
            saveCareer();
        }
    }

    /**
     * 强制手动保存 (建议在 暂停菜单 或 退出游戏 时调用)
     */
    public void forceSave() {
        saveCareer();
    }

    private void unlock(AchievementType type) {
        if (!careerData.unlockedAchievements.contains(type.id)) {
            careerData.unlockedAchievements.add(type.id);
            // 通知当前关卡UI显示弹窗
            gameSaveData.recordNewAchievement(type.id);
            Logger.info("🏆 Achievement Unlocked: " + type.displayName);

            // 重要成就可以立即保存防止丢失
            saveCareer();
        }
    }

    private void saveCareer() {
        if (storageManager != null) {
            storageManager.saveCareer(careerData);
        }
    }
}