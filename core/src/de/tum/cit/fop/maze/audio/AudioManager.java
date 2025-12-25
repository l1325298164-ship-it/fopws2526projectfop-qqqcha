package de.tum.cit.fop.maze.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 音频管理器 - 简化但功能完整
 */
public class AudioManager implements Disposable {
    // ==================== 单例模式 ====================
    private static AudioManager instance;

    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    // ==================== 音频资源存储 ====================
    private final ObjectMap<String, Sound> sounds;
    private final ObjectMap<String, Music> musicTracks;
    private final ObjectMap<String, AudioConfig> configs;

    // ==================== 活跃状态跟踪 ====================
    private static class ActiveSound {
        String id;
        long soundId;
        long startTime;

        ActiveSound(String id, long soundId) {
            this.id = id;
            this.soundId = soundId;
            this.startTime = System.currentTimeMillis();
        }
    }

    private final Map<String, ActiveSound> activeSounds;
    private final Map<String, Music> activeMusic;

    // ==================== 全局设置 ====================
    private float masterVolume = 1.0f;
    private float musicVolume = 0.7f;
    private float sfxVolume = 0.8f;
    private boolean masterEnabled = true;
    private boolean musicEnabled = true;
    private boolean sfxEnabled = true;

    // ==================== 当前状态 ====================
    private String currentMusicId;
    private Music currentMusic;
    private AudioConfig currentMusicConfig;

    // ==================== 构造函数和初始化 ====================
    private AudioManager() {
        sounds = new ObjectMap<>();
        musicTracks = new ObjectMap<>();
        configs = new ObjectMap<>();
        activeSounds = new HashMap<>();
        activeMusic = new HashMap<>();

        initialize();
    }

    /**
     * 初始化所有音频
     */
    private void initialize() {
        // 注册所有AudioType
        for (AudioType type : AudioType.values()) {
            registerAudio(type);
        }

        // 预加载核心音频
        preloadCoreAudio();
    }

    // ==================== 音频注册与加载 ====================

    /**
     * 注册单个音频类型
     */
    public void registerAudio(AudioType type) {
        AudioConfig config = type.getConfig();
        configs.put(type.name(), config);
    }

    /**
     * 手动注册音频（用于动态添加）
     */
    public void registerAudio(String id, AudioConfig config) {
        configs.put(id, config);
    }

    /**
     * 预加载核心音频
     */
    private void preloadCoreAudio() {
        // 预加载所有音乐
        for (AudioType type : AudioType.values()) {
            if (type.isMusic()) {
                loadMusic(type.name());
            }
        }

        // 预加载常用音效
        loadSound(AudioType.UI_CLICK.name());
        loadSound(AudioType.UI_SUCCESS.name());
        loadSound(AudioType.UI_FAILURE.name());
        loadSound(AudioType.PLAYER_MOVE.name());
    }

    /**
     * 加载音效（延迟加载）
     */
    private Sound loadSound(String id) {
        if (sounds.containsKey(id)) {
            return sounds.get(id);
        }

        AudioConfig config = configs.get(id);
        if (config == null) {
            Gdx.app.error("AudioManager", "Config not found: " + id);
            return null;
        }

        try {
            Sound sound = Gdx.audio.newSound(Gdx.files.internal(config.getFilePath()));
            sounds.put(id, sound);
            return sound;
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Failed to load sound: " + id + " - " + e.getMessage());
            return null;
        }
    }

    /**
     * 加载音乐（延迟加载）
     */
    private Music loadMusic(String id) {
        if (musicTracks.containsKey(id)) {
            return musicTracks.get(id);
        }

        AudioConfig config = configs.get(id);
        if (config == null || !config.isMusic()) {
            Gdx.app.error("AudioManager", "Music config not found or not music: " + id);
            return null;
        }

        try {
            Music music = Gdx.audio.newMusic(Gdx.files.internal(config.getFilePath()));
            musicTracks.put(id, music);
            return music;
        } catch (Exception e) {
            Gdx.app.error("AudioManager", "Failed to load music: " + id + " - " + e.getMessage());
            return null;
        }
    }

    // ==================== 播放控制 ====================

    /**
     * 播放音频（通用方法）
     */
    public long play(String id) {
        AudioConfig config = configs.get(id);
        if (config == null) {
            Gdx.app.error("AudioManager", "Audio not registered: " + id);
            return -1;
        }

        if (config.isMusic()) {
            playMusic(id);
            return 0;
        } else {
            return playSound(id);
        }
    }

    /**
     * 播放AudioType音频
     */
    public long play(AudioType type) {
        return play(type.name());
    }

    /**
     * 播放音效
     */
    public long playSound(String id) {
        return playSound(id, 1.0f);
    }

    public long playSound(String id, float volumeMultiplier) {
        return playSound(id, volumeMultiplier, 1.0f, 0.0f);
    }

    public long playSound(String id, float volumeMultiplier, float pitch, float pan) {
        // 检查全局开关
        if (!masterEnabled || !sfxEnabled) return -1;

        AudioConfig config = configs.get(id);
        if (config == null || !config.isEnabled()) {
            Gdx.app.debug("AudioManager", "Sound disabled or not found: " + id);
            return -1;
        }

        // 确保音效已加载
        Sound sound = loadSound(id);
        if (sound == null) return -1;

        // 计算最终音量
        float volume = config.getDefaultVolume() * sfxVolume * masterVolume * volumeMultiplier;
        volume = Math.max(0, Math.min(1, volume));

        // 记录播放
        config.recordPlay();

        // 播放音效
        long soundId;
        if (config.isLoop()) {
            soundId = sound.loop(volume);
        } else {
            soundId = sound.play(volume);
        }

        // 设置音高和声道
        if (pitch != 1.0f) sound.setPitch(soundId, pitch);
        if (pan != 0.0f) sound.setPan(soundId, pan, volume);

        // 跟踪活跃音效（如果是循环音效）
        if (config.isLoop()) {
            ActiveSound activeSound = new ActiveSound(id, soundId);
            activeSounds.put(id, activeSound);
        }

        return soundId;
    }

    /**
     * 播放音乐
     */
    public void playMusic(String id) {
        playMusic(id, true);
    }

    public void playMusic(String id, boolean loop) {
        // 检查全局开关
        if (!masterEnabled || !musicEnabled) return;

        AudioConfig config = configs.get(id);
        if (config == null || !config.isEnabled() || !config.isMusic()) {
            Gdx.app.error("AudioManager", "Music not found or disabled: " + id);
            return;
        }

        // 如果已经在播放同一首音乐
        if (id.equals(currentMusicId) && currentMusic != null && currentMusic.isPlaying()) {
            return;
        }

        // 停止当前音乐
        if (currentMusic != null && !id.equals(currentMusicId)) {
            currentMusic.stop();
        }

        // 确保音乐已加载
        Music music = loadMusic(id);
        if (music == null) return;

        // 记录播放
        config.recordPlay();

        // 设置音乐参数
        float volume = config.getDefaultVolume() * musicVolume * masterVolume;
        volume = Math.max(0, Math.min(1, volume));

        music.setVolume(volume);
        music.setLooping(loop);
        music.play();

        // 更新当前音乐状态
        currentMusicId = id;
        currentMusic = music;
        currentMusicConfig = config;
        activeMusic.put(id, music);
    }

    /**
     * 播放AudioType音乐
     */
    public void playMusic(AudioType type) {
        playMusic(type.name(), type.isLoop());
    }

    // ==================== 特殊播放方法 ====================

    /**
     * 播放玩家移动音效（特殊处理）
     */
    public void playPlayerMove() {
        String moveId = AudioType.PLAYER_MOVE.name();

        // 如果已经在播放移动音效，先停止
        if (activeSounds.containsKey(moveId)) {
            stopSound(moveId);
        }

        // 播放循环移动音效
        playSound(moveId, 1.0f, 1.0f, 0.0f);
    }

    /**
     * 停止玩家移动音效
     */
    public void stopPlayerMove() {
        stopSound(AudioType.PLAYER_MOVE.name());
    }

    /**
     * 播放UI点击音效（带冷却时间）
     */
    private long lastClickTime = 0;
    private static final long CLICK_COOLDOWN = 50; // 50ms冷却

    public void playUIClick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClickTime > CLICK_COOLDOWN) {
            playSound(AudioType.UI_CLICK.name(), 1.0f);
            lastClickTime = currentTime;
        }
    }

    // ==================== 停止控制 ====================

    /**
     * 停止音效
     */
    public void stopSound(String id) {
        if (sounds.containsKey(id)) {
            sounds.get(id).stop();
            activeSounds.remove(id);
        }
    }

    /**
     * 停止AudioType音效
     */
    public void stopSound(AudioType type) {
        stopSound(type.name());
    }

    /**
     * 停止特定音效实例
     */
    public void stopSoundInstance(String id, long soundId) {
        if (sounds.containsKey(id)) {
            sounds.get(id).stop(soundId);
            ActiveSound activeSound = activeSounds.get(id);
            if (activeSound != null && activeSound.soundId == soundId) {
                activeSounds.remove(id);
            }
        }
    }

    /**
     * 停止所有音效
     */
    public void stopAllSounds() {
        for (Sound sound : sounds.values()) {
            sound.stop();
        }
        activeSounds.clear();
    }

    /**
     * 停止当前音乐
     */
    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
            currentMusicId = null;
            currentMusicConfig = null;
        }
    }

    /**
     * 停止所有音乐
     */
    public void stopAllMusic() {
        for (Music music : musicTracks.values()) {
            music.stop();
        }
        activeMusic.clear();
        stopMusic();
    }

    /**
     * 停止所有音频
     */
    public void stopAll() {
        stopAllSounds();
        stopAllMusic();
    }

    // ==================== 暂停/恢复控制 ====================

    /**
     * 暂停当前音乐
     */
    public void pauseMusic() {
        if (currentMusic != null && currentMusic.isPlaying()) {
            currentMusic.pause();
        }
    }

    /**
     * 恢复当前音乐
     */
    public void resumeMusic() {
        if (currentMusic != null && !currentMusic.isPlaying() &&
                masterEnabled && musicEnabled) {
            currentMusic.play();
        }
    }

    /**
     * 暂停所有音频
     */
    public void pauseAll() {
        pauseMusic();
        // 注意：Sound没有暂停方法，只能停止
    }

    /**
     * 恢复所有音频
     */
    public void resumeAll() {
        resumeMusic();
        // 音效无法恢复，需要重新播放
    }

    // ==================== 音量控制 ====================

    public float getMasterVolume() { return masterVolume; }
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0, Math.min(1, volume));
        updateAllVolumes();
    }

    public float getMusicVolume() { return musicVolume; }
    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0, Math.min(1, volume));
        updateMusicVolumes();
    }

    public float getSfxVolume() { return sfxVolume; }
    public void setSfxVolume(float volume) {
        this.sfxVolume = Math.max(0, Math.min(1, volume));
    }

    public boolean isMasterEnabled() { return masterEnabled; }
    public void setMasterEnabled(boolean enabled) {
        this.masterEnabled = enabled;
        if (!enabled) {
            stopAll();
        } else {
            resumeAll();
        }
    }

    public boolean isMusicEnabled() { return musicEnabled; }
    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        if (!enabled) {
            pauseMusic();
        } else if (masterEnabled) {
            resumeMusic();
        }
    }

    public boolean isSfxEnabled() { return sfxEnabled; }
    public void setSfxEnabled(boolean enabled) {
        this.sfxEnabled = enabled;
        if (!enabled) {
            stopAllSounds();
        }
    }

    /**
     * 设置音频配置
     */
    public void setAudioConfig(String id, AudioConfig config) {
        configs.put(id, config);
    }

    public AudioConfig getAudioConfig(String id) {
        return configs.get(id);
    }

    public AudioConfig getAudioConfig(AudioType type) {
        return configs.get(type.name());
    }

    // ==================== 内存管理 ====================

    /**
     * 清理未使用的音频资源
     */
    public void cleanupUnusedAudio() {
        Gdx.app.debug("AudioManager", "Cleaning up unused audio...");
        int unloadedCount = 0;

        // 清理长时间未使用的音效
        // 使用临时数组来避免 ConcurrentModificationException
        com.badlogic.gdx.utils.Array<String> soundKeys = sounds.keys().toArray();
        for (String id : soundKeys) {
            AudioConfig config = configs.get(id);
            if (config != null && !config.isPersistent() && !config.isActive()) {
                // 检查是否正在使用
                if (!activeSounds.containsKey(id)) {
                    Sound sound = sounds.remove(id);
                    if (sound != null) {
                        sound.dispose();
                        unloadedCount++;
                        Gdx.app.debug("AudioManager", "Unloaded sound: " + id);
                    }
                }
            }
        }

        // 清理未使用的音乐（除了当前播放的）
        com.badlogic.gdx.utils.Array<String> musicKeys = musicTracks.keys().toArray();
        for (String id : musicKeys) {
            if (!id.equals(currentMusicId)) {
                AudioConfig config = configs.get(id);
                if (config != null && !config.isPersistent() && !config.isActive()) {
                    Music music = musicTracks.remove(id);
                    if (music != null) {
                        music.dispose();
                        unloadedCount++;
                        Gdx.app.debug("AudioManager", "Unloaded music: " + id);
                    }
                }
            }
        }

        Gdx.app.debug("AudioManager", "Cleaned up " + unloadedCount + " unused audio resources");
    }

    /**
     * 获取内存使用统计
     */
    public String getMemoryStats() {
        int soundCount = sounds.size;
        int musicCount = musicTracks.size;
        int activeSoundCount = activeSounds.size();
        int activeMusicCount = activeMusic.size();

        return String.format(
                "Audio Memory Stats: Sounds=%d, Music=%d, ActiveSounds=%d, ActiveMusic=%d",
                soundCount, musicCount, activeSoundCount, activeMusicCount
        );
    }

    // ==================== 状态查询 ====================

    public boolean isPlaying(String id) {
        AudioConfig config = configs.get(id);
        if (config == null) return false;

        if (config.isMusic()) {
            return id.equals(currentMusicId) && currentMusic != null && currentMusic.isPlaying();
        } else {
            return activeSounds.containsKey(id);
        }
    }

    public boolean isPlaying(AudioType type) {
        return isPlaying(type.name());
    }

    public boolean isMusicPlaying() {
        return currentMusic != null && currentMusic.isPlaying();
    }

    public String getCurrentMusicId() {
        return currentMusicId;
    }

    public AudioType getCurrentMusicType() {
        if (currentMusicId == null) return null;
        try {
            return AudioType.valueOf(currentMusicId);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ==================== 辅助方法 ====================

    private void updateMusicVolumes() {
        for (Music music : musicTracks.values()) {
            String musicId = musicTracks.findKey(music, false);
            if (musicId != null) {
                AudioConfig config = configs.get(musicId);
                if (config != null) {
                    float volume = config.getDefaultVolume() * musicVolume * masterVolume;
                    volume = Math.max(0, Math.min(1, volume));
                    music.setVolume(volume);
                }
            }
        }
    }

    private void updateAllVolumes() {
        updateMusicVolumes();
    }

    @Override
    public void dispose() {
        Gdx.app.debug("AudioManager", "Disposing AudioManager...");

        // 停止所有音频
        stopAll();

        // 释放音效资源
        for (Sound sound : sounds.values()) {
            sound.dispose();
        }
        sounds.clear();

        // 释放音乐资源
        for (Music music : musicTracks.values()) {
            music.dispose();
        }
        musicTracks.clear();

        // 清理活跃列表
        activeSounds.clear();
        activeMusic.clear();
        configs.clear();

        currentMusic = null;
        currentMusicId = null;
        currentMusicConfig = null;

        instance = null;

        Gdx.app.debug("AudioManager", "AudioManager disposed");
    }
}