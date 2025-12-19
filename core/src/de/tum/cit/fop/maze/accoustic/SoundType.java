package de.tum.cit.fop.maze.accoustic;

    public enum SoundType {
        // ========== 背景音乐 ==========
        BACKGROUND_MUSIC("background"),

        // ========== 玩家动作音效 ==========
        MOVE("move"),
        THROW_ATTACK("throw-attack"),

        // ========== 交互音效 ==========
        DAZZLE_HIT("dazzle-hit"),
        HIT_WALL("hit-wall"),

        // ========== 收集音效 ==========
        GET_KEY("get-key"),

        // ========== 游戏状态音效 ==========
        FAILURE("failure"),

        // ========== 预留/未来扩展 ==========
        JUMP("jump"),
        COIN("coin"),
        VICTORY("victory"),
        BUTTON_CLICK("button-click"),
        DOOR_OPEN("door-open"),
        ENEMY_HIT("enemy-hit"),
        POWER_UP("power-up");

        private final String soundId;

        SoundType(String soundId) {
            this.soundId = soundId;
        }

        public String getSoundId() {
            return soundId;
        }

        /**
         * 根据字符串获取对应的 SoundType
         */
        public static SoundType fromString(String soundId) {
            for (SoundType type : values()) {
                if (type.getSoundId().equals(soundId)) {
                    return type;
                }
            }
            return null;
        }
    }
