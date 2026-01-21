package de.tum.cit.fop.maze.game.achievement;

import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.game.EnemyTier;
import de.tum.cit.fop.maze.game.save.GameSaveData;
import de.tum.cit.fop.maze.game.event.GameListener;
import de.tum.cit.fop.maze.game.score.DamageSource;
import de.tum.cit.fop.maze.game.score.ScoreConstants;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.game.save.StorageManager;

import java.util.LinkedList;
import java.util.Queue;

/**
 * 成就管理器
 */
public class AchievementManager implements GameListener {

    private final CareerData careerData;
    // 🔥 FIX: 去掉 final，允许在读档时更新引用
    private GameSaveData gameSaveData;
    private final StorageManager storageManager;
    private final Difficulty currentDifficulty;

    private static final int MAX_NOTIFICATION_QUEUE_SIZE = 50;
    private final Queue<AchievementType> notificationQueue = new LinkedList<>();

    private boolean needsSave = false;
    private int currentLevelDamageTaken = 0;

    public AchievementManager(CareerData careerData,
                              GameSaveData gameSaveData,
                              StorageManager storageManager,
                              Difficulty currentDifficulty) {
        this.careerData = careerData;
        this.gameSaveData = gameSaveData;
        this.storageManager = storageManager;
        this.currentDifficulty = currentDifficulty;
    }

    // 🔥 FIX: 提供更新 GameSaveData 引用的方法
    public void updateGameSaveData(GameSaveData newData) {
        this.gameSaveData = newData;
        Logger.info("AchievementManager: GameSaveData reference updated.");
    }

    public AchievementType pollNotification() {
        return notificationQueue.poll();
    }

    @Override
    public void onEnemyKilled(EnemyTier tier, boolean isDashKill) {
        // 🔥 FIX: 记录本局击杀数据，确保结算界面能读取到 Kill 数量
        if (gameSaveData != null) {
            gameSaveData.addSessionKill(tier.name());
        }

        careerData.totalKills_Global++;
        if (careerData.totalKills_Global >= ScoreConstants.TARGET_KILLS_GLOBAL) {
            unlock(AchievementType.ACH_08_BEST_SELLER);
        }

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
                    careerData.totalDashKills_E04++;
                    if (careerData.totalDashKills_E04 >= ScoreConstants.TARGET_KILLS_E04_DASH)
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
    }

    @Override
    public void onPlayerDamage(int currentHp, DamageSource source) {
        currentLevelDamageTaken++;
    }

    @Override
    public void onItemCollected(String itemType) {
        if (itemType == null) return;

        if ("HEART".equals(itemType) || "BOBA".equals(itemType)) {
            careerData.totalHeartsCollected++;
            if (careerData.totalHeartsCollected >= ScoreConstants.TARGET_HEARTS_COLLECTED) {
                unlock(AchievementType.ACH_09_FREE_TOPPING);
            }

            if (!careerData.hasHealedOnce) {
                careerData.hasHealedOnce = true;
                unlock(AchievementType.ACH_03_BOBA_RESCUE);
            }
        }
        else if (itemType.startsWith("TREASURE")) {
            careerData.collectedBuffTypes.add(itemType);
            if (careerData.collectedBuffTypes.size() >= ScoreConstants.TARGET_TREASURE_TYPES) {
                unlock(AchievementType.ACH_10_TREASURE_MASTER);
            }
        }
    }

    @Override
    public void onLevelFinished(int levelNumber) {
        if (levelNumber == 1) {
            unlock(AchievementType.ACH_02_FIRST_CUP);
        }

        if (currentLevelDamageTaken <= ScoreConstants.TARGET_NO_DAMAGE_LIMIT) {
            unlock(AchievementType.ACH_11_SEALED_TIGHT);
        }

        currentLevelDamageTaken = 0;

        if (levelNumber >= 3 && currentDifficulty == Difficulty.HARD) {
            if (!careerData.hasClearedHardMode) {
                careerData.hasClearedHardMode = true;
                unlock(AchievementType.ACH_14_RENAISSANCE);
            }
        }

        saveCareerSync();
    }

    public void onPVWatched() {
        if (!careerData.hasWatchedPV) {
            careerData.hasWatchedPV = true;
            unlock(AchievementType.ACH_01_TRAINING);
            saveCareerSync();
        }
    }

    public void forceSave() {
        saveCareer();
    }

    public void saveIfNeeded() {
        if (needsSave) {
            saveCareer();
            needsSave = false;
        }
    }

    private void unlock(AchievementType type) {
        if (!careerData.unlockedAchievements.contains(type.id)) {
            careerData.unlockedAchievements.add(type.id);
            // 这里现在是安全的，因为 gameSaveData 引用是最新的
            gameSaveData.recordNewAchievement(type.id);

            if (notificationQueue.size() < MAX_NOTIFICATION_QUEUE_SIZE) {
                notificationQueue.add(type);
            } else {
                notificationQueue.poll();
                notificationQueue.add(type);
            }

            Logger.info("🏆 Achievement Unlocked: " + type.displayName);
            needsSave = true;
        }
    }

    private void saveCareer() {
        if (storageManager != null) {
            storageManager.saveCareer(careerData);
        }
    }

    private void saveCareerSync() {
        if (storageManager != null) {
            storageManager.saveCareerSync(careerData);
        }
    }
}