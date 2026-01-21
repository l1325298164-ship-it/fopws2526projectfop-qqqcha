package de.tum.cit.fop.maze.audio;

/**
 * 音频类型枚举 - 所有游戏音频的中央定义
 * 包含了音频的基本配置信息
 */
public enum AudioType {
    // === 音乐 ===
    MUSIC_MENU("sounds_file/BGM/maze_bgm.mp3", AudioCategory.MUSIC, true, 0.7f, true, false),
    MUSIC_MAZE_EASY("sounds_file/BGM/maze_easy.mp3", AudioCategory.MUSIC, true, 0.7f, true, false),
    MUSIC_MAZE_NORMAL("sounds_file/BGM/maze_normal.mp3", AudioCategory.MUSIC, true, 0.7f, true, false),
    MUSIC_MAZE_HARD("sounds_file/BGM/maze_hard.mp3", AudioCategory.MUSIC, true, 0.7f, true, false),
    MUSIC_MAZE_ENDLESS("sounds_file/BGM/maze_endless.mp3", AudioCategory.MUSIC, true, 0.7f, true, false),

    // === 玩家音效 ===
    PLAYER_MOVE("sounds_file/SFX/move01.wav", AudioCategory.PLAYER, false, 0.8f, true, false),
    PLAYER_GET_KEY("sounds_file/SFX/get-key.wav", AudioCategory.PLAYER, false, 1.0f, false, false),
    PLAYER_HIT_WALL("sounds_file/SFX/hit-wall01.wav", AudioCategory.PLAYER, false, 0.9f, false, false),
    PLAYER_ATTACKED("sounds_file/SFX/melee_1.ogg", AudioCategory.PLAYER, false, 1.0f, false, false),
    // === PV ===
    PV_1("sounds_file/BGM/pv/PV1.mp3", AudioCategory.MUSIC, true, 0.7f, false, false),
    PV_2("sounds_file/BGM/pv/PV2.mp3", AudioCategory.MUSIC, true, 0.7f, false, false),
    PV_3("sounds_file/BGM/pv/PV3.mp3", AudioCategory.MUSIC, true, 0.7f, false, false),
    PV_4("sounds_file/BGM/pv/PV4.mp3", AudioCategory.MUSIC, true, 0.7f, false, false),



    // === 敌人音效 ===
    ENEMY_ATTACKED("sounds_file/SFX/attacked02.wav", AudioCategory.ENEMY, false, 0.8f, false, false),
    ENEMY_ATTACKED_E01("sounds_file/SFX/attack_E01.ogg", AudioCategory.ENEMY, false, 0.8f, false, false),
    ENEMY_ATTACKED_E02("sounds_file/SFX/attack_E02.ogg", AudioCategory.ENEMY, false, 0.8f, false, false),
    ENEMY_ATTACKED_E03("sounds_file/SFX/attack_E03.ogg", AudioCategory.ENEMY, false, 0.8f, false, false),


    // === UI音效 ===
    UI_CLICK("sounds_file/SFX/click01.wav", AudioCategory.UI, false, 0.6f, false, true),
    UI_SUCCESS("sounds_file/SFX/btn_3.mp3", AudioCategory.UI, false, 0.8f, false, true),
    UI_FAILURE("sounds_file/SFX/btn_1.ogg", AudioCategory.UI, false, 0.8f, false, true),
    UI_HIT_DAZZLE("sounds_file/SFX/click02.wav", AudioCategory.UI, false, 1.0f, false, true),
    UI_THROW_ATTACK("sounds_file/SFX/btn_2.mp3", AudioCategory.UI, false, 1.0f, false, true),
//revision |||| sword

    // === ✨ 新增：技能与动作 ===
    // 冲刺音效 (Whoosh声)
    SKILL_DASH("sounds_file/SFX/dash_01.ogg", AudioCategory.PLAYER, false, 0.9f, false, false),
    // 挥剑音效 (Slash声)
    SKILL_SLASH("sounds_file/SFX/sword_swing.wav", AudioCategory.PLAYER, false, 0.8f, false, false),
    // Buff获取音效 (强化声)
    BUFF_GAIN("sounds_file/SFX/buff_01.wav", AudioCategory.UI, false, 0.9f, false, false),

    // === 💀 新增：战斗反馈 ===
    ENEMY_DEATH("sounds_file/SFX/enemy_death.wav", AudioCategory.ENEMY, false, 0.9f, false, false),
    // 敌人发现玩家的音效
    ENEMY_ALERT("sounds_file/SFX/alert.wav", AudioCategory.ENEMY, false, 1.0f, false, false),
    // 蓄力音效
    ENEMY_CHARGE("sounds_file/SFX/charge.wav", AudioCategory.ENEMY, false, 0.8f, false, false),

    // === 物品拾取 ===
    ITEM_HEAL("sounds_file/SFX/item_heart.wav", AudioCategory.UI, false, 0.9f, false, false),     // 拾取红心
    ITEM_TREASURE("sounds_file/SFX/item_treasure.wav", AudioCategory.UI, false, 0.9f, false, false), // 拾取宝箱
    ITEM_POWERUP("sounds_file/SFX/item_powerup.wav", AudioCategory.UI, false, 1.0f, false, false),   // 无尽模式强化道具

    //Tutorial
    TUTORIAL_MAIN_BGM(
            "sounds_file/BGM/tutorial_bgm.mp3",
            AudioCategory.MUSIC,
            true,   // isMusic
            0.6f,
            true,   // loop
            false
    ),
    BOSS_BGM("sounds_file/BGM/boss_bgm.mp3",AudioCategory.MUSIC, true, 0.7f, false, false),
    MUSIC_MENU_END("sounds_file/BGM/menu_bgm2.mp3",AudioCategory.MUSIC, true, 0.7f, true, false),
    BOSS_LOADING("sounds_file/BGM/BOSS_loading.mp3",AudioCategory.MUSIC, true, 0.7f, true, false);





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