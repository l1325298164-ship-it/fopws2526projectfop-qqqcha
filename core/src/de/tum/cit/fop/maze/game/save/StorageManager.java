package de.tum.cit.fop.maze.game.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import de.tum.cit.fop.maze.game.achievement.CareerData;
import de.tum.cit.fop.maze.utils.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * 统一存储管理器 (Storage Manager) - 单例模式
 * <p>
 * 功能增强：
 * 1. 异步存档（后台线程保存，不阻塞主线程）
 * 2. 存档压缩（GZIP压缩JSON，减少文件大小）
 * 3. 原子写入机制（Write-to-temp -> Move），防止存档损坏
 * 4. 线程安全，支持等待所有异步任务完成
 */
public class StorageManager {

    public enum SaveTarget {
        AUTO,
        SLOT_1,
        SLOT_2,
        SLOT_3;

        public static SaveTarget fromSlot(int slot) {
            return switch (slot) {
                case 1 -> SLOT_1;
                case 2 -> SLOT_2;
                case 3 -> SLOT_3;
                default -> AUTO;
            };
        }
    }

    // ===== 主存档 Slot =====
    public static final int MAX_SAVE_SLOTS = 3;
    private static final String AUTO_SAVE_FILE = "save_auto.json.gz";
    private static final String SAVE_SLOT_PATTERN = "save_slot_%d.json.gz";

    // ==========================================
    // 单例模式实现
    // ==========================================
    private static StorageManager instance;

    public static StorageManager getInstance() {
        if (instance == null) {
            instance = new StorageManager();
        }
        return instance;
    }

    // ==========================================
    // 文件配置
    // ==========================================
    private static final String SAVE_FILE_NAME = "save_data.json.gz";  // 压缩后文件名
    private static final String CAREER_FILE_NAME = "career_data.json.gz";
    private static final String SAVE_FILE_NAME_LEGACY = "save_data.json";  // 兼容旧存档
    private static final String CAREER_FILE_NAME_LEGACY = "career_data.json";

    // ==========================================
    // 异步保存配置
    // ==========================================
    private final ExecutorService saveExecutor;
    private final Json json;

    // 用于跟踪异步任务
    private final ConcurrentLinkedQueue<Future<?>> pendingSaves = new ConcurrentLinkedQueue<>();

    // 是否启用压缩（默认启用）
    private boolean compressionEnabled = true;

    // 是否启用异步保存（默认启用）
    private boolean asyncEnabled = true;

    private StorageManager() {
        this.json = new Json();
        this.json.setOutputType(JsonWriter.OutputType.json);
        this.json.setUsePrototypes(false);

        this.saveExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "StorageManager-SaveThread");
            t.setDaemon(true);  // 守护线程，不会阻止JVM退出
            return t;
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                waitForAllSaves(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                Logger.warning("Error during shutdown save: " + e.getMessage());
            }
        }));
    }

    private String getSlotFileName(int slot) {
        if (slot < 1 || slot > MAX_SAVE_SLOTS) {
            throw new IllegalArgumentException("Invalid save slot: " + slot);
        }
        return String.format(SAVE_SLOT_PATTERN, slot);
    }

    public void saveGameToSlot(int slot, GameSaveData data) {
        if (data == null) return;

        String fileName = getSlotFileName(slot);

        if (asyncEnabled) {
            writeJsonSafelyAsync(fileName, data, compressionEnabled);
            Logger.debug("Game saved to slot " + slot + " (async)");
        } else {
            writeJsonSafelySync(fileName, data, compressionEnabled);
            Logger.info("Game saved to slot " + slot);
        }
    }

    // 🔥 [新增] 专门用于保存自动存档的方法
    public void saveAuto(GameSaveData data) {
        if (data == null) return;

        if (asyncEnabled) {
            writeJsonSafelyAsync(AUTO_SAVE_FILE, data, compressionEnabled);
            // Logger.debug("Game auto-saved (async)");
        } else {
            writeJsonSafelySync(AUTO_SAVE_FILE, data, compressionEnabled);
            // Logger.info("Game auto-saved (sync)");
        }
    }

    // [修改] 修复了原有的 saveGameAuto 逻辑
    public void saveGameAuto(SaveTarget target, GameSaveData data) {
        if (target != SaveTarget.AUTO) {
            return;
        }
        saveAuto(data);
    }

    public GameSaveData loadGameFromSlot(int slot) {
        String fileName = getSlotFileName(slot);
        return loadGameInternal(fileName);
    }

    public boolean hasSaveInSlot(int slot) {
        String fileName = getSlotFileName(slot);
        return getFile(fileName).exists();
    }

    public boolean[] getSaveSlotStates() {
        boolean[] result = new boolean[MAX_SAVE_SLOTS + 1];
        for (int i = 1; i <= MAX_SAVE_SLOTS; i++) {
            result[i] = hasSaveInSlot(i);
        }
        return result;
    }

    private GameSaveData loadGameInternal(String fileName) {
        FileHandle file = getFile(fileName);
        boolean isCompressed = fileName.endsWith(".gz");

        if (!file.exists()) return null;

        try {
            String jsonStr;

            if (isCompressed) {
                byte[] compressed = file.readBytes();
                jsonStr = decompressData(compressed);
            } else {
                jsonStr = file.readString();
            }

            if (jsonStr == null || jsonStr.isBlank()) return null;

            GameSaveData data = json.fromJson(GameSaveData.class, jsonStr);
            return data;

        } catch (Exception e) {
            Logger.error("Failed to load save: " + fileName);
            e.printStackTrace();
            return null;
        }
    }

    public void setCompressionEnabled(boolean enabled) {
        this.compressionEnabled = enabled;
    }

    public void setAsyncEnabled(boolean enabled) {
        this.asyncEnabled = enabled;
    }

    public boolean waitForAllSaves(long timeout, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (!pendingSaves.isEmpty() && System.currentTimeMillis() < deadline) {
            Future<?> future = pendingSaves.poll();
            if (future != null) {
                try {
                    future.get(Math.max(1, deadline - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    Logger.warning("Save task failed: " + e.getMessage());
                }
            }
        }
        return pendingSaves.isEmpty();
    }

    public void flushAllSaves() {
        Logger.info("Flushing all pending saves...");
        waitForAllSaves(10, TimeUnit.SECONDS);
        Logger.info("All saves flushed.");
    }

    private byte[] compressData(String jsonStr) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(jsonStr.getBytes("UTF-8"));
        }
        return baos.toByteArray();
    }

    private String decompressData(byte[] compressed) throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        try (GZIPInputStream gzis = new GZIPInputStream(bais)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = gzis.read(buffer)) != -1) {
                baos.write(buffer, 0, len);
            }
            return baos.toString("UTF-8");
        }
    }

    private void writeJsonSafelySync(String fileName, Object data, boolean useCompression) {
        if (data == null) return;
        FileHandle tmpFile = null;
        try {
            FileHandle oldTmpFile = getFile(fileName + ".tmp");
            if (oldTmpFile.exists()) {
                try {
                    oldTmpFile.delete();
                } catch (Exception e) {
                    Logger.warning("Failed to delete old temp file: " + e.getMessage());
                }
            }

            String jsonStr = json.toJson(data);

            tmpFile = getFile(fileName + ".tmp");
            if (useCompression) {
                byte[] compressed = compressData(jsonStr);
                tmpFile.writeBytes(compressed, false);
            } else {
                tmpFile.writeString(jsonStr, false);
            }

            FileHandle targetFile = getFile(fileName);
            tmpFile.moveTo(targetFile);
            tmpFile = null;

            // Logger.debug("Data saved safely to " + fileName + (useCompression ? " (compressed)" : ""));
        } catch (Exception e) {
            Logger.error("Failed to save data to " + fileName + ": " + e.getMessage());
            e.printStackTrace();

            if (tmpFile != null && tmpFile.exists()) {
                try {
                    tmpFile.delete();
                } catch (Exception cleanupEx) {
                    Logger.warning("Failed to cleanup temp file: " + cleanupEx.getMessage());
                }
            }
        }
    }

    private void writeJsonSafelyAsync(String fileName, Object data, boolean useCompression) {
        if (data == null) return;

        Object dataCopy = deepCopy(data);

        Future<?> future = saveExecutor.submit(() -> {
            writeJsonSafelySync(fileName, dataCopy, useCompression);
        });

        pendingSaves.offer(future);

        while (!pendingSaves.isEmpty()) {
            Future<?> first = pendingSaves.peek();
            if (first.isDone()) {
                pendingSaves.poll();
            } else {
                break;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T deepCopy(T obj) {
        try {
            String jsonStr = json.toJson(obj);
            return (T) json.fromJson(obj.getClass(), jsonStr);
        } catch (Exception e) {
            Logger.warning("Failed to deep copy object, using original: " + e.getMessage());
            return obj;
        }
    }

    public GameSaveData loadGame() {
        GameSaveData auto = loadAutoSave();
        if (auto != null) {
            Logger.info("Loaded auto save");
            return auto;
        }

        GameSaveData slot1 = loadGameFromSlot(1);
        if (slot1 != null) {
            Logger.info("Loaded save from slot 1");
            return slot1;
        }

        FileHandle legacy = getFile(SAVE_FILE_NAME);
        if (legacy.exists()) {
            Logger.warning("Legacy save detected");
            return loadGameInternal(SAVE_FILE_NAME);
        }

        Logger.info("No save file found");
        return null;
    }

    public void deleteSave() {
        for (int i = 1; i <= MAX_SAVE_SLOTS; i++) {
            FileHandle slot = getFile(getSlotFileName(i));
            if (slot.exists()) slot.delete();
        }

        FileHandle legacy = getFile(SAVE_FILE_NAME);
        if (legacy.exists()) legacy.delete();

        FileHandle legacyRaw = getFile(SAVE_FILE_NAME_LEGACY);
        if (legacyRaw.exists()) legacyRaw.delete();

        Logger.info("All save files deleted.");
    }

    public boolean hasAnySave() {
        if (hasAutoSave()) return true;
        for (int i = 1; i <= MAX_SAVE_SLOTS; i++) {
            if (hasSaveInSlot(i)) return true;
        }
        return getFile(SAVE_FILE_NAME).exists();
    }

    public void saveCareer(CareerData data) {
        if (asyncEnabled) {
            writeJsonSafelyAsync(CAREER_FILE_NAME, data, compressionEnabled);
            Logger.debug("Career data queued for async save.");
        } else {
            writeJsonSafelySync(CAREER_FILE_NAME, data, compressionEnabled);
        }
    }

    public void saveCareerSync(CareerData data) {
        writeJsonSafelySync(CAREER_FILE_NAME, data, compressionEnabled);
    }

    public CareerData loadCareer() {
        FileHandle file = getFile(CAREER_FILE_NAME);
        boolean isCompressed = true;

        if (!file.exists()) {
            file = getFile(CAREER_FILE_NAME_LEGACY);
            isCompressed = false;
        }

        if (!file.exists()) {
            Logger.info("No career data found, creating new profile.");
            return new CareerData();
        }

        try {
            String jsonStr;

            if (isCompressed) {
                byte[] compressed = file.readBytes();
                jsonStr = decompressData(compressed);
            } else {
                jsonStr = file.readString();
            }

            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                Logger.warning("Career file is empty, creating new profile.");
                return new CareerData();
            }

            CareerData data = json.fromJson(CareerData.class, jsonStr);

            if (data == null) {
                Logger.warning("Failed to parse career data: data is null, creating new profile.");
                return new CareerData();
            }

            if (data.totalKills_E01 < 0) data.totalKills_E01 = 0;
            if (data.totalKills_E02 < 0) data.totalKills_E02 = 0;
            if (data.totalKills_E03 < 0) data.totalKills_E03 = 0;
            if (data.totalDashKills_E04 < 0) data.totalDashKills_E04 = 0;
            if (data.totalKills_Global < 0) data.totalKills_Global = 0;
            if (data.totalHeartsCollected < 0) data.totalHeartsCollected = 0;

            if (data.collectedBuffTypes == null) {
                data.collectedBuffTypes = new java.util.HashSet<>();
            }
            if (data.unlockedAchievements == null) {
                data.unlockedAchievements = new java.util.HashSet<>();
            }

            Logger.info("Career data loaded successfully (" + (isCompressed ? "compressed" : "legacy") + ").");
            return data;
        } catch (Exception e) {
            Logger.error("Failed to load career data, resetting: " + e.getMessage());
            e.printStackTrace();
            return new CareerData();
        }
    }

    private FileHandle getFile(String fileName) {
        return Gdx.files.local(fileName);
    }

    public boolean deleteSaveSlot(int slot) {
        if (slot < 1 || slot > MAX_SAVE_SLOTS) {
            Logger.warning("Attempted to delete invalid save slot: " + slot);
            return false;
        }

        FileHandle file = getFile(getSlotFileName(slot));
        if (file.exists()) {
            boolean success = file.delete();
            if (success) {
                Logger.info("Save slot " + slot + " deleted.");
            } else {
                Logger.warning("Failed to delete save slot " + slot);
            }
            return success;
        }

        Logger.info("Save slot " + slot + " does not exist.");
        return false;
    }

    public GameSaveData loadAutoSave() {
        return loadGameInternal(AUTO_SAVE_FILE);
    }

    public boolean hasAutoSave() {
        return getFile(AUTO_SAVE_FILE).exists();
    }

    public void deleteAutoSave() {
        FileHandle f = getFile(AUTO_SAVE_FILE);
        if (f.exists()) f.delete();
    }

    public void saveGameSync(GameSaveData data) {
        if (data == null) return;
        writeJsonSafelySync(AUTO_SAVE_FILE, data, compressionEnabled);
    }
}