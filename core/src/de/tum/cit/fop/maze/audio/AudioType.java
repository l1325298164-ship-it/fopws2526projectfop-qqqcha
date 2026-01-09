package de.tum.cit.fop.maze.audio;

/**
 * 音频类型枚举 - 所有游戏音频的中央定义
 * 包含了音频的基本配置信息
 */
public enum AudioType {
    // === 音乐 ===
    MUSIC_MENU("sounds/music/menu-background.mp3", AudioCategory.MUSIC, true, 0.7f, true, false),

    // === 玩家音效 ===
    PLAYER_MOVE("sounds/sfx/player/move01.wav", AudioCategory.PLAYER, false, 0.8f, true, false),
    PLAYER_GET_KEY("sounds/sfx/player/get-key.wav", AudioCategory.PLAYER, false, 1.0f, false, false),
    PLAYER_HIT_WALL("sounds/sfx/player/hit-wall01.wav", AudioCategory.PLAYER, false, 0.9f, false, false),
    PLAYER_ATTACKED("sounds/sfx/player/attacked01.wav", AudioCategory.PLAYER, false, 1.0f, false, false),
    // === PV ===
    PV_1("sounds/pv/PV1.mp3", AudioCategory.MUSIC, true, 0.7f, false, false),
    PV_2("sounds/pv/PV2.mp3", AudioCategory.MUSIC, true, 0.7f, false, false),
    PV_3("sounds/pv/PV3.mp3", AudioCategory.MUSIC, true, 0.7f, false, false),
    PV_4("sounds/pv/PV4.mp3", AudioCategory.MUSIC, true, 0.7f, false, false),



    // === 敌人音效 ===
    ENEMY_ATTACKED("sounds/sfx/enemy/attacked01.wav", AudioCategory.ENEMY, false, 0.8f, false, false),

    // === UI音效 ===
    UI_CLICK("sounds/sfx/ui/click01.wav", AudioCategory.UI, false, 0.6f, false, true),
    UI_SUCCESS("sounds/sfx/ui/enter-next-level.ogg", AudioCategory.UI, false, 0.8f, false, true),
    UI_FAILURE("sounds/sfx/ui/game-failure.wav", AudioCategory.UI, false, 0.8f, false, true),
    UI_HIT_DAZZLE("sounds/sfx/ui/click02.wav", AudioCategory.UI, false, 1.0f, false, true),
    UI_THROW_ATTACK("sounds/sfx/ui/throw-attack.wav", AudioCategory.UI, false, 1.0f, false, true),
    SWORD_SWING("sounds/sfx/ui/throw-attack.wav", AudioCategory.UI, false, 1.0f, false, true),
//revision |||| sword


    //Tutorial
    TUTORIAL_MAIN_BGM(
            "sounds/pv/Crystal Sugarquake.mp3",
            AudioCategory.MUSIC,
            true,   // isMusic
            0.6f,
            true,   // loop
            false
    ),
    TUTORIAL_IDLE_HINT(
            "sounds/temprorary/attacked03.wav",
            AudioCategory.AMBIENT,
            false,  // ❗ 不是 Music
            0.7f,
            false,
            true
    ),

    TUTORIAL_SLOW_HINT(
            "sounds/temprorary/attacked03.wav",
            AudioCategory.AMBIENT,
            false,
            0.7f,
            false,
            true
    ),

    TUTORIAL_FAST_FEEDBACK(
            "sounds/temprorary/attacked03.wav",
            AudioCategory.AMBIENT,
            false,
            0.8f,
            false,
            true
    ),

    TUTORIAL_TARGET_HINT(
            "sounds/temprorary/attacked03.wav",
            AudioCategory.AMBIENT,
            false,
            0.7f,
            false,
            true
    );





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