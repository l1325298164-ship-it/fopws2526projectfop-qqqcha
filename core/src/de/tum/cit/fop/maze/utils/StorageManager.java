package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonWriter;
import de.tum.cit.fop.maze.game.GameSaveData;
import de.tum.cit.fop.maze.game.achievement.CareerData;

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

    // ==========================================
    // 单例模式实现
    // ==========================================
    private static StorageManager instance;

    /**
     * 获取单例实例
     * @return StorageManager 单例对象
     */
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

    /**
     * 私有构造函数，防止外部直接实例化
     */
    private StorageManager() {
        this.json = new Json();
        this.json.setOutputType(JsonWriter.OutputType.json);
        this.json.setUsePrototypes(false);
        
        // 创建单线程执行器，确保保存操作的顺序性
        this.saveExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "StorageManager-SaveThread");
            t.setDaemon(true);  // 守护线程，不会阻止JVM退出
            return t;
        });
        
        // 注册关闭钩子，确保退出时等待所有保存完成
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                waitForAllSaves(5, TimeUnit.SECONDS);
            } catch (Exception e) {
                Logger.warning("Error during shutdown save: " + e.getMessage());
            }
        }));
    }
    
    /**
     * ✨ [新增] 设置是否启用压缩
     */
    public void setCompressionEnabled(boolean enabled) {
        this.compressionEnabled = enabled;
    }
    
    /**
     * ✨ [新增] 设置是否启用异步保存
     */
    public void setAsyncEnabled(boolean enabled) {
        this.asyncEnabled = enabled;
    }
    
    /**
     * ✨ [新增] 等待所有异步保存任务完成
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return 是否所有任务都已完成
     */
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
    
    /**
     * ✨ [新增] 强制同步等待所有保存完成（用于游戏退出时）
     */
    public void flushAllSaves() {
        Logger.info("Flushing all pending saves...");
        waitForAllSaves(10, TimeUnit.SECONDS);
        Logger.info("All saves flushed.");
    }

    // ==========================================
    // 压缩/解压工具方法
    // ==========================================
    
    /**
     * ✨ [新增] 压缩数据
     */
    private byte[] compressData(String jsonStr) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(jsonStr.getBytes("UTF-8"));
        }
        return baos.toByteArray();
    }
    
    /**
     * ✨ [新增] 解压数据
     */
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
    
    // ==========================================
    // 通用原子写入方法（同步版本）
    // ==========================================
    private void writeJsonSafelySync(String fileName, Object data, boolean useCompression) {
        if (data == null) return;
        FileHandle tmpFile = null;
        try {
            // 1. 清理可能存在的旧临时文件
            FileHandle oldTmpFile = getFile(fileName + ".tmp");
            if (oldTmpFile.exists()) {
                try {
                    oldTmpFile.delete();
                } catch (Exception e) {
                    Logger.warning("Failed to delete old temp file: " + e.getMessage());
                }
            }
            
            // 2. 序列化数据
            String jsonStr = json.toJson(data);
            
            // 3. 写入临时文件（压缩或原始）
            tmpFile = getFile(fileName + ".tmp");
            if (useCompression) {
                byte[] compressed = compressData(jsonStr);
                tmpFile.writeBytes(compressed, false);
            } else {
                tmpFile.writeString(jsonStr, false);
            }

            // 4. 移动/重命名临时文件覆盖正式文件 (原子操作)
            FileHandle targetFile = getFile(fileName);
            tmpFile.moveTo(targetFile);
            tmpFile = null; // 移动成功后，tmpFile 不再指向有效文件

            Logger.debug("Data saved safely to " + fileName + (useCompression ? " (compressed)" : ""));
        } catch (Exception e) {
            Logger.error("Failed to save data to " + fileName + ": " + e.getMessage());
            e.printStackTrace();
            
            // 清理失败的临时文件
            if (tmpFile != null && tmpFile.exists()) {
                try {
                    tmpFile.delete();
                } catch (Exception cleanupEx) {
                    Logger.warning("Failed to cleanup temp file: " + cleanupEx.getMessage());
                }
            }
        }
    }
    
    /**
     * ✨ [新增] 异步写入方法
     */
    private void writeJsonSafelyAsync(String fileName, Object data, boolean useCompression) {
        if (data == null) return;
        
        // 深拷贝数据，避免在异步保存时数据被修改
        Object dataCopy = deepCopy(data);
        
        Future<?> future = saveExecutor.submit(() -> {
            writeJsonSafelySync(fileName, dataCopy, useCompression);
        });
        
        pendingSaves.offer(future);
        
        // 清理已完成的任务（避免内存泄漏）
        while (!pendingSaves.isEmpty()) {
            Future<?> first = pendingSaves.peek();
            if (first.isDone()) {
                pendingSaves.poll();
            } else {
                break;
            }
        }
    }
    
    /**
     * ✨ [新增] 深拷贝对象（通过JSON序列化/反序列化）
     */
    @SuppressWarnings("unchecked")
    private <T> T deepCopy(T obj) {
        try {
            String jsonStr = json.toJson(obj);
            return (T) json.fromJson(obj.getClass(), jsonStr);
        } catch (Exception e) {
            Logger.warning("Failed to deep copy object, using original: " + e.getMessage());
            return obj;  // 如果拷贝失败，返回原对象（风险较低，因为通常保存很快）
        }
    }

    // ==========================================
    // 1. 单局存档 (GameSaveData)
    // ==========================================
    
    /**
     * 保存游戏进度（异步，压缩）
     */
    public void saveGame(GameSaveData data) {
        if (asyncEnabled) {
            writeJsonSafelyAsync(SAVE_FILE_NAME, data, compressionEnabled);
            Logger.debug("Game progress queued for async save.");
        } else {
            writeJsonSafelySync(SAVE_FILE_NAME, data, compressionEnabled);
            Logger.info("Game progress saved.");
        }
    }
    
    /**
     * ✨ [新增] 同步保存游戏进度（用于关键节点，如关卡结束）
     */
    public void saveGameSync(GameSaveData data) {
        writeJsonSafelySync(SAVE_FILE_NAME, data, compressionEnabled);
        Logger.info("Game progress saved (sync).");
    }

    /**
     * 加载游戏进度（支持压缩和旧格式）
     */
    public GameSaveData loadGame() {
        // 优先尝试加载压缩文件
        FileHandle file = getFile(SAVE_FILE_NAME);
        boolean isCompressed = true;
        
        // 如果压缩文件不存在，尝试加载旧格式
        if (!file.exists()) {
            file = getFile(SAVE_FILE_NAME_LEGACY);
            isCompressed = false;
        }
        
        if (!file.exists()) {
            Logger.info("No save file found.");
            return null;
        }
        
        try {
            String jsonStr;
            
            if (isCompressed) {
                // 读取并解压
                byte[] compressed = file.readBytes();
                jsonStr = decompressData(compressed);
            } else {
                // 读取原始JSON
                jsonStr = file.readString();
            }
            
            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                Logger.warning("Save file is empty, treating as no save.");
                return null;
            }
            
            GameSaveData data = json.fromJson(GameSaveData.class, jsonStr);
            
            // 数据验证
            if (data == null) {
                Logger.error("Failed to parse save data: data is null");
                return null;
            }
            
            // 验证关键字段的合理性
            if (data.currentLevel < 1) {
                Logger.warning("Invalid level in save data: " + data.currentLevel + ", resetting to 1");
                data.currentLevel = 1;
            }
            if (data.score < 0) {
                Logger.warning("Invalid score in save data: " + data.score + ", resetting to 0");
                data.score = 0;
            }
            if (data.lives < 0) {
                Logger.warning("Invalid lives in save data: " + data.lives + ", resetting to 0");
                data.lives = 0;
            }
            
            Logger.info("Game progress loaded successfully (" + (isCompressed ? "compressed" : "legacy") + ").");
            return data;
        } catch (Exception e) {
            Logger.error("Failed to load save data: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void deleteSave() {
        // 删除压缩和旧格式文件
        FileHandle file = getFile(SAVE_FILE_NAME);
        if (file.exists()) {
            file.delete();
        }
        FileHandle legacyFile = getFile(SAVE_FILE_NAME_LEGACY);
        if (legacyFile.exists()) {
            legacyFile.delete();
        }
        Logger.info("Save file deleted.");
    }

    public boolean hasSaveFile() {
        return getFile(SAVE_FILE_NAME).exists() || getFile(SAVE_FILE_NAME_LEGACY).exists();
    }

    // ==========================================
    // 2. 生涯档案 (CareerData)
    // ==========================================
    
    /**
     * 保存生涯数据（异步，压缩）
     */
    public void saveCareer(CareerData data) {
        if (asyncEnabled) {
            writeJsonSafelyAsync(CAREER_FILE_NAME, data, compressionEnabled);
            Logger.debug("Career data queued for async save.");
        } else {
            writeJsonSafelySync(CAREER_FILE_NAME, data, compressionEnabled);
        }
    }
    
    /**
     * ✨ [新增] 同步保存生涯数据（用于关键节点）
     */
    public void saveCareerSync(CareerData data) {
        writeJsonSafelySync(CAREER_FILE_NAME, data, compressionEnabled);
    }

    /**
     * 加载生涯数据（支持压缩和旧格式）
     */
    public CareerData loadCareer() {
        // 优先尝试加载压缩文件
        FileHandle file = getFile(CAREER_FILE_NAME);
        boolean isCompressed = true;
        
        // 如果压缩文件不存在，尝试加载旧格式
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
                // 读取并解压
                byte[] compressed = file.readBytes();
                jsonStr = decompressData(compressed);
            } else {
                // 读取原始JSON
                jsonStr = file.readString();
            }
            
            if (jsonStr == null || jsonStr.trim().isEmpty()) {
                Logger.warning("Career file is empty, creating new profile.");
                return new CareerData();
            }
            
            CareerData data = json.fromJson(CareerData.class, jsonStr);
            
            // 数据验证
            if (data == null) {
                Logger.warning("Failed to parse career data: data is null, creating new profile.");
                return new CareerData();
            }
            
            // 验证数据的合理性（防止负数或异常大的值）
            if (data.totalKills_E01 < 0) data.totalKills_E01 = 0;
            if (data.totalKills_E02 < 0) data.totalKills_E02 = 0;
            if (data.totalKills_E03 < 0) data.totalKills_E03 = 0;
            if (data.totalDashKills_E04 < 0) data.totalDashKills_E04 = 0;
            if (data.totalKills_Global < 0) data.totalKills_Global = 0;
            if (data.totalHeartsCollected < 0) data.totalHeartsCollected = 0;
            
            // 确保 HashSet 不为 null
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
}