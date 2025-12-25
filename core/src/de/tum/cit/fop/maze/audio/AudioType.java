package de.tum.cit.fop.maze.audio;

/**
 * 音频类型枚举 - 所有游戏音频的中央定义
 * 包含了音频的基本配置信息
 */
public enum AudioType {
    // === 音乐 ===
    MUSIC_MENU("music/menu-background.mp3", AudioCategory.MUSIC, true, 0.7f, true, false),

    // === 玩家音效 ===
    PLAYER_MOVE("sfx/player/move01.wav", AudioCategory.PLAYER, false, 0.8f, true, false),
    PLAYER_GET_KEY("sfx/player/get-key.wav", AudioCategory.PLAYER, false, 1.0f, false, false),
    PLAYER_HIT_WALL("sfx/player/hit-wall01.wav", AudioCategory.PLAYER, false, 0.9f, false, false),
    PLAYER_ATTACKED("sfx/player/attacked01.wav", AudioCategory.PLAYER, false, 1.0f, false, false),

    // === UI音效 ===
    UI_CLICK("sfx/ui/click01.wav", AudioCategory.UI, false, 0.6f, false, true),
    UI_SUCCESS("sfx/ui/enter-next-level.ogg", AudioCategory.UI, false, 0.8f, false, true),
    UI_FAILURE("sfx/ui/game-failure.wav", AudioCategory.UI, false, 0.8f, false, true),
    UI_HIT_DAZZLE("sfx/ui/hit-dazzle.wav", AudioCategory.UI, false, 1.0f, false, true),
    UI_THROW_ATTACK("sfx/ui/throw-attack.wav", AudioCategory.UI, false, 1.0f, false, true);

    private final String path;
    private final AudioCategory category;
    private final boolean isMusic;
    private final float defaultVolume;
    private final boolean loop;           // 是否循环
    private final boolean isPriority;     // 是否为优先级音频（UI音效等，不会被中断）

    AudioType(String path, AudioCategory category, boolean isMusic,
              float defaultVolume, boolean loop, boolean isPriority) {
        this.path = path;
        this.category = category;
        this.isMusic = isMusic;
        this.defaultVolume = Math.max(0, Math.min(1, defaultVolume));
        this.loop = loop;
        this.isPriority = isPriority;
    }

    /**
     * 获取对应的AudioConfig
     */
    public AudioConfig getConfig() {
        AudioConfig config = new AudioConfig(this.name(), this.path, this.category);
        config.setDefaultVolume(this.defaultVolume);
        config.setLoop(this.loop); // 使用枚举中定义的loop值
        config.setMusic(this.isMusic); // 添加这个方法到AudioConfig
        config.setPriority(this.isPriority); // 添加这个方法到AudioConfig
        return config;
    }

    public String getPath() { return path; }
    public AudioCategory getCategory() { return category; }
    public boolean isMusic() { return isMusic; }
    public float getDefaultVolume() { return defaultVolume; }
    public boolean isLoop() { return loop; }
    public boolean isPriority() { return isPriority; }
}