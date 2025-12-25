package de.tum.cit.fop.maze.acoustic;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import java.util.ArrayList;
import java.util.List;

public class SoundManager implements Disposable {
    private static SoundManager instance;

    // 存储音效和配置
    private final ObjectMap<String, Sound> sounds;
    private final ObjectMap<String, Music> musicTracks;
    private final ObjectMap<String, SoundConfig> soundConfigs;
    private final ObjectMap<String, SoundConfig> musicConfigs;

    // 全局设置
    private float masterVolume = 1.0f;
    private float soundEffectsVolume = 1.0f;
    private float musicVolume = 0.8f;
    private boolean soundEffectsEnabled = true;
    private boolean musicEnabled = true;
    private boolean masterEnabled = true;

    // 当前播放的音乐
    private Music currentMusic;
    private String currentMusicId;

    // 活跃音效ID列表（用于管理和停止）
    private final List<Long> activeSoundIds;

    private SoundManager() {
        sounds = new ObjectMap<>();
        musicTracks = new ObjectMap<>();
        soundConfigs = new ObjectMap<>();
        musicConfigs = new ObjectMap<>();
        activeSoundIds = new ArrayList<>();
    }

    public static SoundManager getInstance() {
        if (instance == null) {
            instance = new SoundManager();
        }
        return instance;
    }

    // ==================== 音效注册和配置接口 ====================

    /**
     * 注册音效
     */
    public void registerSound(String id, String filePath) {
        SoundConfig config = new SoundConfig(id, filePath);
        soundConfigs.put(id, config);

        // 如果设置了自动播放，加载后立即播放
        if (config.isAutoPlay()) {
            loadAndPlaySound(id);
        }
    }

    /**
     * 注册音乐
     */
    public void registerMusic(String id, String filePath) {
        SoundConfig config = new SoundConfig(id, filePath);
        musicConfigs.put(id, config);
    }

    public void preloadAllSounds() {
        // ====== 背景音乐 ======
        registerMusic("background", "sounds/background.mp3");
        SoundConfig bgConfig = musicConfigs.get("background");
        if (bgConfig != null) {
            bgConfig.setDefaultVolume(0.6f);
            bgConfig.setLoop(true);          // 背景音乐循环
            bgConfig.setAutoPlay(true);      // 自动播放
        }

        // ====== 玩家移动音效 ======
        registerSound("move", "sounds/move01.wav");
        SoundConfig moveConfig = soundConfigs.get("move");
        if (moveConfig != null) {
            moveConfig.setDefaultVolume(0.7f);
            moveConfig.setLoop(true);        // 长按移动时循环
            moveConfig.setEnabled(true);
        }

        // ====== 玩家发起攻击音效 ======
        registerSound("throw-attack", "sounds/throw-attack.wav");
        SoundConfig attackConfig = soundConfigs.get("throw-attack");
        if (attackConfig != null) {
            attackConfig.setDefaultVolume(0.8f);
            attackConfig.setLoop(false);     // 攻击音效不循环
            attackConfig.setEnabled(true);
        }

        // ====== 玩家被击中音效 ======
        registerSound("dazzle-hit", "sounds/dazzle-hit.wav");
        SoundConfig dazzleConfig = soundConfigs.get("dazzle-hit");
        if (dazzleConfig != null) {
            dazzleConfig.setDefaultVolume(0.9f);
            dazzleConfig.setLoop(false);
            dazzleConfig.setEnabled(true);
        }

        // ====== 撞墙音效 ======
        registerSound("hit-wall", "sounds/hit-wall.wav");
        SoundConfig hitWallConfig = soundConfigs.get("hit-wall");
        if (hitWallConfig != null) {
            hitWallConfig.setDefaultVolume(0.7f);
            hitWallConfig.setLoop(false);
            hitWallConfig.setEnabled(true);
        }

        // ====== 钥匙收集音效 ======
        registerSound("get-key", "sounds/get-key.wav");
        SoundConfig keyConfig = soundConfigs.get("get-key");
        if (keyConfig != null) {
            keyConfig.setDefaultVolume(0.8f);
            keyConfig.setLoop(false);        // 单次播放，不循环
            keyConfig.setEnabled(true);
            keyConfig.setAutoPlay(false);    // 不自动播放
        }

        // ====== 成功音效 ======
        registerSound("success", "sounds/success.ogg");
        SoundConfig successConfig = soundConfigs.get("success");
        if (successConfig != null) {
            successConfig.setDefaultVolume(0.9f);  // 成功音效可以稍微响亮一点
            successConfig.setLoop(false);          // 单次播放
            successConfig.setEnabled(true);
            successConfig.setAutoPlay(false);      // 不自动播放，由游戏逻辑触发
        }

        // ====== 失败音效 ======
        registerSound("failure", "sounds/failure-piano.wav");
        SoundConfig failureConfig = soundConfigs.get("failure");
        if (failureConfig != null) {
            failureConfig.setDefaultVolume(0.8f);
            failureConfig.setLoop(false);
            failureConfig.setEnabled(true);
        }

        // ====== 其他通用音效（可根据需要添加） ======
        registerSound("jump", "sounds/jump.wav");      // 如果未来需要
        registerSound("coin", "sounds/coin.wav");      // 如果未来需要
    }
    /**
     * 更新音效配置
     */
    public void updateSoundConfig(String id, SoundConfig newConfig) {
        if (soundConfigs.containsKey(id)) {
            soundConfigs.put(id, newConfig);
        }
    }

    // ==================== 音量控制接口 ====================

    /**
     * 设置主音量（影响所有声音）
     */
    public void setMasterVolume(float volume) {
        this.masterVolume = Math.max(0, Math.min(1, volume));
        updateMusicVolume(); // 更新当前播放音乐的实时音量
    }

    public float getMasterVolume() { return masterVolume; }

    /**
     * 设置音效音量（相对主音量）
     */
    public void setSoundEffectsVolume(float volume) {
        this.soundEffectsVolume = Math.max(0, Math.min(1, volume));
    }

    public float getSoundEffectsVolume() { return soundEffectsVolume; }

    /**
     * 设置音乐音量（相对主音量）
     */
    public void setMusicVolume(float volume) {
        this.musicVolume = Math.max(0, Math.min(1, volume));
        updateMusicVolume(); // 更新当前播放音乐的实时音量
    }

    public float getMusicVolume() { return musicVolume; }

    /**
     * 设置特定音效的音量
     */
    public void setSoundVolume(String id, float volume) {
        if (soundConfigs.containsKey(id)) {
            soundConfigs.get(id).setDefaultVolume(volume);
        }
    }

    /**
     * 设置特定音乐的音量
     */
    public void setMusicVolume(String id, float volume) {
        if (musicConfigs.containsKey(id)) {
            musicConfigs.get(id).setDefaultVolume(volume);
        }
    }

    // ==================== 启用/禁用控制接口 ====================

    /**
     * 全局启用/禁用所有声音
     */
    public void setMasterEnabled(boolean enabled) {
        this.masterEnabled = enabled;
        if (!enabled) {
            stopAllSounds();
            stopAllMusic();
        } else {
            // 如果恢复，重新播放之前的音乐
            if (currentMusic != null && !currentMusic.isPlaying()) {
                currentMusic.play();
            }
        }
    }

    public boolean isMasterEnabled() { return masterEnabled; }

    /**
     * 启用/禁用所有音效
     */
    public void setSoundEffectsEnabled(boolean enabled) {
        this.soundEffectsEnabled = enabled;
        if (!enabled) {
            stopAllSounds();
        }
    }

    public boolean isSoundEffectsEnabled() { return soundEffectsEnabled; }

    /**
     * 启用/禁用所有音乐
     */
    public void setMusicEnabled(boolean enabled) {
        this.musicEnabled = enabled;
        if (!enabled) {
            stopAllMusic();
        } else if (currentMusic != null && !currentMusic.isPlaying()) {
            currentMusic.play();
        }
    }

    public boolean isMusicEnabled() { return musicEnabled; }

    /**
     * 启用/禁用特定音效
     */
    public void setSoundEnabled(String id, boolean enabled) {
        if (soundConfigs.containsKey(id)) {
            soundConfigs.get(id).setEnabled(enabled);
            if (!enabled) {
                stopSound(id);
            }
        }
    }

    /**
     * 启用/禁用特定音乐
     */
    public void setMusicEnabled(String id, boolean enabled) {
        if (musicConfigs.containsKey(id)) {
            musicConfigs.get(id).setEnabled(enabled);
            if (!enabled && currentMusicId != null && currentMusicId.equals(id)) {
                stopMusic();
            }
        }
    }

    // ==================== 播放控制接口 ====================

    /**
     * 播放音效（使用配置中的设置）
     */
    public long playSound(String id) {
        if (!masterEnabled || !soundEffectsEnabled) return -1;

        SoundConfig config = soundConfigs.get(id);
        if (config == null || !config.isEnabled()) return -1;

        // 确保音效已加载
        if (!sounds.containsKey(id)) {
            loadSound(id);
        }

        Sound sound = sounds.get(id);
        if (sound == null) return -1;

        float volume = config.getDefaultVolume() * soundEffectsVolume * masterVolume;
        long soundId;

        if (config.isLoop()) {
            soundId = sound.loop(volume);
        } else {
            soundId = sound.play(volume);
        }

        // 设置音高和声道平衡
        sound.setPitch(soundId, config.getPitch());
        sound.setPan(soundId, config.getPan(), volume);

        activeSoundIds.add(soundId);
        return soundId;
    }

    /**
     * 播放音效（自定义参数）
     */
    public long playSound(String id, float volume, boolean loop, float pitch, float pan) {
        if (!masterEnabled || !soundEffectsEnabled) return -1;

        SoundConfig config = soundConfigs.get(id);
        if (config == null || !config.isEnabled()) return -1;

        if (!sounds.containsKey(id)) {
            loadSound(id);
        }

        Sound sound = sounds.get(id);
        if (sound == null) return -1;

        float finalVolume = volume * soundEffectsVolume * masterVolume;
        long soundId;

        if (loop) {
            soundId = sound.loop(finalVolume);
        } else {
            soundId = sound.play(finalVolume);
        }

        sound.setPitch(soundId, pitch);
        sound.setPan(soundId, pan, finalVolume);

        activeSoundIds.add(soundId);
        return soundId;
    }

    /**
     * 播放音乐（使用配置中的设置）
     */
    public void playMusic(String id) {
        if (!masterEnabled || !musicEnabled) return;

        SoundConfig config = musicConfigs.get(id);
        if (config == null || !config.isEnabled()) return;

        // 如果已经在播放，先停止
        if (currentMusic != null) {
            currentMusic.stop();
        }

        // 加载音乐
        if (!musicTracks.containsKey(id)) {
            loadMusic(id);
        }

        currentMusic = musicTracks.get(id);
        currentMusicId = id;

        if (currentMusic != null) {
            currentMusic.setLooping(config.isLoop());
            updateMusicVolume(); // 设置音量
            currentMusic.play();
        }
    }

    /**
     * 播放音乐（自定义参数）
     */
    public void playMusic(String id, boolean loop, float volume) {
        if (!masterEnabled || !musicEnabled) return;

        SoundConfig config = musicConfigs.get(id);
        if (config == null || !config.isEnabled()) return;

        // 更新配置
        config.setLoop(loop);
        config.setDefaultVolume(volume);

        playMusic(id);
    }

    /**
     * 自动播放所有设置了autoPlay的音效
     */
    public void autoPlayAll() {
        for (SoundConfig config : soundConfigs.values()) {
            if (config.isAutoPlay() && config.isEnabled()) {
                playSound(config.getName());
            }
        }

        for (SoundConfig config : musicConfigs.values()) {
            if (config.isAutoPlay() && config.isEnabled()) {
                playMusic(config.getName());
            }
        }
    }

    // ==================== 停止控制接口 ====================

    /**
     * 停止特定音效
     */
    public void stopSound(String id) {
        if (sounds.containsKey(id)) {
            sounds.get(id).stop();
        }
    }

    /**
     * 停止特定音效实例
     */
    public void stopSoundInstance(long soundId) {
        for (Sound sound : sounds.values()) {
            sound.stop(soundId);
        }
        activeSoundIds.remove(soundId);
    }

    /**
     * 停止所有音效
     */
    public void stopAllSounds() {
        for (Sound sound : sounds.values()) {
            sound.stop();
        }
        activeSoundIds.clear();
    }

    /**
     * 停止当前音乐
     */
    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
            currentMusicId = null;
        }
    }

    /**
     * 停止所有音乐
     */
    public void stopAllMusic() {
        for (Music music : musicTracks.values()) {
            music.stop();
        }
        currentMusic = null;
        currentMusicId = null;
    }

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
        if (currentMusic != null && !currentMusic.isPlaying()) {
            currentMusic.play();
        }
    }

    /**
     * 切换音乐（如果正在播放则停止，否则播放）
     */
    public void toggleMusic(String id) {
        if (currentMusicId != null && currentMusicId.equals(id) &&
                currentMusic != null && currentMusic.isPlaying()) {
            pauseMusic();
        } else {
            playMusic(id);
        }
    }

    // ==================== 循环控制接口 ====================

    /**
     * 设置音效是否循环
     */
    public void setSoundLoop(String id, boolean loop) {
        if (soundConfigs.containsKey(id)) {
            soundConfigs.get(id).setLoop(loop);
        }
    }

    /**
     * 设置音乐是否循环
     */
    public void setMusicLoop(String id, boolean loop) {
        if (musicConfigs.containsKey(id)) {
            musicConfigs.get(id).setLoop(loop);
            if (currentMusic != null && currentMusicId != null && currentMusicId.equals(id)) {
                currentMusic.setLooping(loop);
            }
        }
    }

    // ==================== 辅助方法 ====================

    private void loadSound(String id) {
        try {
            SoundConfig config = soundConfigs.get(id);
            Sound sound = Gdx.audio.newSound(Gdx.files.internal(config.getFilePath()));
            sounds.put(id, sound);

            // 如果设置了自动播放，立即播放
            if (config.isAutoPlay()) {
                playSound(id);
            }
        } catch (Exception e) {
            Gdx.app.error("SoundManager", "Failed to load sound: " + id, e);
        }
    }

    private void loadMusic(String id) {
        try {
            SoundConfig config = musicConfigs.get(id);
            Music music = Gdx.audio.newMusic(Gdx.files.internal(config.getFilePath()));
            musicTracks.put(id, music);
        } catch (Exception e) {
            Gdx.app.error("SoundManager", "Failed to load music: " + id, e);
        }
    }

    private void loadAndPlaySound(String id) {
        if (!sounds.containsKey(id)) {
            loadSound(id);
        }
        playSound(id);
    }

    private void updateMusicVolume() {
        if (currentMusic != null && currentMusicId != null) {
            SoundConfig config = musicConfigs.get(currentMusicId);
            if (config != null) {
                float volume = config.getDefaultVolume() * musicVolume * masterVolume;
                currentMusic.setVolume(volume);
            }
        }
    }

    // ==================== 状态查询接口 ====================

    /**
     * 检查音效是否正在播放
     */
    public boolean isSoundPlaying(String id) {
        // LibGDX 的 Sound 类没有直接的方法检查是否在播放
        // 我们可以通过其他方式跟踪，这里返回是否有活跃实例
        return sounds.containsKey(id) && !activeSoundIds.isEmpty();
    }

    /**
     * 检查音乐是否正在播放
     */
    public boolean isMusicPlaying() {
        return currentMusic != null && currentMusic.isPlaying();
    }

    /**
     * 检查特定音乐是否正在播放
     */
    public boolean isMusicPlaying(String id) {
        return currentMusic != null && currentMusicId != null &&
                currentMusicId.equals(id) && currentMusic.isPlaying();
    }

    /**
     * 获取当前播放的音乐ID
     */
    public String getCurrentMusicId() {
        return currentMusicId;
    }

    /**
     * 清理资源
     */
    @Override
    public void dispose() {
        // 清理音效
        for (Sound sound : sounds.values()) {
            sound.dispose();
        }
        sounds.clear();

        // 清理音乐
        for (Music music : musicTracks.values()) {
            music.dispose();
        }
        musicTracks.clear();

        soundConfigs.clear();
        musicConfigs.clear();
        activeSoundIds.clear();

        currentMusic = null;
        currentMusicId = null;
        instance = null;
    }
}