package de.tum.cit.fop.maze.acoustic;

public class SoundConfig {
    private String name;        // 音效名称
    private String filePath;    // 文件路径
    private float defaultVolume = 1.0f; // 默认音量
    private boolean autoPlay = false;   // 是否自动播放
    private boolean loop = false;       // 是否循环
    private boolean enabled = true;     // 是否启用
    private float pitch = 1.0f;         // 音高
    private float pan = 0.0f;           // 声道平衡

    // 构造函数
    public SoundConfig(String name, String filePath) {
        this.name = name;
        this.filePath = filePath;
    }

    // Getter 和 Setter 方法
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

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
}