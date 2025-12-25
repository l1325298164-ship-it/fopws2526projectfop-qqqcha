package de.tum.cit.fop.maze.audio;

public class AudioConfig {
    private String name;        // 音效名称
    private String filePath;    // 文件路径
    private AudioCategory category; // 音频类别
    private boolean isMusic;    // 是否为音乐
    private float defaultVolume = 1.0f; // 默认音量
    private boolean autoPlay = false;   // 是否自动播放
    private boolean loop = false;       // 是否循环
    private boolean enabled = true;     // 是否启用
    private float pitch = 1.0f;         // 音高
    private float pan = 0.0f;           // 声道平衡
    private boolean priority;           // 是否为优先级音频

    // 新增字段用于统计和内存管理
    private long lastPlayTime = 0;      // 最后播放时间
    private int playCount = 0;          // 播放次数
    private boolean persistent = false; // 是否持久化

    // 构造函数
    public AudioConfig(String name, String filePath, AudioCategory category) {
        this.name = name;
        this.filePath = filePath;
        this.category = category;
        this.isMusic = (category == AudioCategory.MUSIC);
    }

    // Getter 和 Setter 方法
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public AudioCategory getCategory() { return category; }
    public void setCategory(AudioCategory category) {
        this.category = category;
        this.isMusic = (category == AudioCategory.MUSIC);
    }

    public boolean isMusic() { return isMusic; }
    public void setMusic(boolean music) { isMusic = music; }

    public float getDefaultVolume() { return defaultVolume; }
    public void setDefaultVolume(float defaultVolume) {
        this.defaultVolume = Math.max(0, Math.min(1, defaultVolume));
    }

    public boolean isAutoPlay() { return autoPlay; }
    public void setAutoPlay(boolean autoPlay) { this.autoPlay = autoPlay; }

    public boolean isLoop() { return loop; }
    public void setLoop(boolean loop) { this.loop = loop; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public float getPitch() { return pitch; }
    public void setPitch(float pitch) {
        this.pitch = Math.max(0.5f, Math.min(2.0f, pitch));
    }

    public float getPan() { return pan; }
    public void setPan(float pan) {
        this.pan = Math.max(-1, Math.min(1, pan));
    }

    public boolean isPriority() { return priority; }
    public void setPriority(boolean priority) { this.priority = priority; }

    public long getLastPlayTime() { return lastPlayTime; }
    public void setLastPlayTime(long lastPlayTime) { this.lastPlayTime = lastPlayTime; }

    public int getPlayCount() { return playCount; }
    public void setPlayCount(int playCount) { this.playCount = playCount; }

    public boolean isPersistent() { return persistent; }
    public void setPersistent(boolean persistent) { this.persistent = persistent; }

    // 新增方法
    public float getCurrentVolume() {
        return getDefaultVolume(); // 简单实现：使用默认音量作为当前音量
    }

    public void recordPlay() {
        this.playCount++;
        this.lastPlayTime = System.currentTimeMillis();
    }

    public boolean isActive() {
        return System.currentTimeMillis() - lastPlayTime < 5 * 60 * 1000; // 5分钟内播放过
    }
}