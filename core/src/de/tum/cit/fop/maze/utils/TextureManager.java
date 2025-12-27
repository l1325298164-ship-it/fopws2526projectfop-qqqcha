// TextureManager.java
package de.tum.cit.fop.maze.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import de.tum.cit.fop.maze.game.GameConstants;

import java.util.HashMap;
import java.util.Map;

public class TextureManager implements Disposable {
    private static TextureManager instance;
    private Map<String, Texture> textures;



    // 纹理模式
    public enum TextureMode {
        COLOR,      // 纯色模式（现有功能）
        IMAGE,      // 图片模式
        PIXEL,      // 像素风格
        MINIMAL     // 极简模式
    }

    // 纹理键名常量
    public static final String FLOOR = "floor";
    public static final String WALL = "wall";
    public static final String PLAYER = "player";
    public static final String KEY = "key";
    public static final String DOOR = "door";
    public static final String LOCKED_DOOR = "locked_door";
    public static final String ENEMY1 = "enemy1";
    public static final String HEART = "heart";
    public static final String TRAP = "trap";


    // 默认颜色（备用）
    private static final Color DEFAULT_COLOR = Color.WHITE;

    // 图片文件映射（不同模式使用不同图片）
    private Map<TextureMode, Map<String, String>> textureFileMap;
    private TextureMode currentMode = TextureMode.IMAGE; // 默认纯色模式

    private TextureManager() {
        textures = new HashMap<>();
        textureFileMap = new HashMap<>();

        // 初始化各模式的图片映射
        initializeTextureMappings();

        Logger.debug("TextureManager initialized, mode: " + currentMode);
    }

    public static TextureManager getInstance() {
        if (instance == null) {
            instance = new TextureManager();
        }
        return instance;
    }

    /**
     * 初始化各模式的纹理映射
     */
    private void initializeTextureMappings() {
        // COLOR 模式：使用颜色纹理（不映射图片）
        textureFileMap.put(TextureMode.COLOR, new HashMap<>());

        // IMAGE 模式：映射到实际的图片文件
        Map<String, String> imageMode = new HashMap<>();

        imageMode.put(FLOOR, "floor/780.jpg");
        imageMode.put(WALL, "Walls/wall_2.png");

        imageMode.put(PLAYER, "Character/mainCharacter.png");
        imageMode.put(ENEMY1, "Character/Enemy1.png");
        imageMode.put(TRAP, "Traps/spike.png"); // 路径示例


        imageMode.put(KEY, "Items/key_1.gif");

        imageMode.put(DOOR, "Walls/wall_1.png");
        imageMode.put(LOCKED_DOOR, "Walls/wall_3.png");

// 心心如果你有图可以补，没有也没关系
// imageMode.put(HEART, "Items/heart.png");

        textureFileMap.put(TextureMode.IMAGE, imageMode);

        // PIXEL 模式：可以映射到像素风格的图片（如果没有，使用IMAGE模式的图片）
        Map<String, String> pixelMode = new HashMap<>();
        // 这里可以添加专门像素风格的图片，暂时使用IMAGE模式的图片
        pixelMode.putAll(imageMode);
        textureFileMap.put(TextureMode.PIXEL, pixelMode);

        // MINIMAL 模式：极简模式，只加载必要的纹理或使用颜色
        Map<String, String> minimalMode = new HashMap<>();
        // 极简模式可以只使用颜色纹理
        textureFileMap.put(TextureMode.MINIMAL, minimalMode);
    }

    /**
     * 切换纹理模式
     */
    public void switchMode(TextureMode mode) {
        if (this.currentMode == mode) return;

        Logger.debug("Switching texture mode from " + currentMode + " to " + mode);

        // 1️⃣ 先清理所有非颜色纹理
        clearAllNonColorTextures();

        // 2️⃣ 再切换模式
        this.currentMode = mode;

        // 3️⃣ 预加载新模式需要的图片
        preloadTexturesForMode(mode);
    }


    /**
     * 预加载指定模式的纹理
     */
    private void preloadTexturesForMode(TextureMode mode) {
        Map<String, String> fileMap = textureFileMap.get(mode);

        if (fileMap != null && !fileMap.isEmpty()) {
            for (Map.Entry<String, String> entry : fileMap.entrySet()) {
                loadImageTexture(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * 加载图片纹理
     */
    private void loadImageTexture(String key, String filePath) {
        try {
            FileHandle file = Gdx.files.internal(filePath);
            if (file.exists()) {
                Texture texture = new Texture(file);
                textures.put(key, texture);
                Logger.debug("Loaded image texture: " + key + " from " + filePath);
            } else {
                Logger.warning("Image file not found: " + filePath + ", using fallback");
                // 文件不存在时，创建颜色纹理作为备用
                createFallbackTexture(key);
            }
        } catch (Exception e) {
            Logger.error("Failed to load texture: " + key + " - " + e.getMessage());
            createFallbackTexture(key);
        }
    }

    /**
     * 创建备用颜色纹理
     */
    private void createFallbackTexture(String key) {
        Color fallbackColor = getFallbackColor(key);
        Texture colorTexture = createColorTexture(fallbackColor);
        textures.put(key, colorTexture);
    }

    /**
     * 根据键名获取备用颜色
     */
    private Color getFallbackColor(String key) {
        switch (key) {
            case FLOOR: return GameConstants.FLOOR_COLOR;
            case WALL: return GameConstants.WALL_COLOR;
            case PLAYER: return GameConstants.PLAYER_COLOR;
            case KEY: return GameConstants.KEY_COLOR;
            case DOOR: return GameConstants.DOOR_COLOR;
            case LOCKED_DOOR: return GameConstants.LOCKED_DOOR_COLOR;
            case ENEMY1: return Color.PURPLE; // 敌人备用颜色
            case HEART: return GameConstants.HEART_COLOR;
            case TRAP: return Color.RED; // 或你想要的陷阱颜色

            default: return DEFAULT_COLOR;
        }
    }

    /**
     * 清理图片纹理（保留颜色纹理）
     */
    private void clearAllNonColorTextures() {
        Map<String, Texture> toKeep = new HashMap<>();

        for (Map.Entry<String, Texture> entry : textures.entrySet()) {
            String key = entry.getKey();

            // 只保留颜色纹理
            if (key.startsWith("color_")) {
                toKeep.put(key, entry.getValue());
            } else {
                entry.getValue().dispose();
            }
        }

        textures = toKeep;
        Logger.debug("Cleared all non-color textures, kept " + textures.size());
    }



    /**
     * 获取纹理 - 智能选择
     */
    public Texture getTexture(String key) {
        // 1. 首先尝试获取已加载的纹理
        if (textures.containsKey(key)) {
            return textures.get(key);
        }

        // 2. 根据当前模式处理
        switch (currentMode) {
            case COLOR:
                // 纯色模式：生成颜色纹理
                return getColorTextureByKey(key);

            case IMAGE:
            case PIXEL:
                // 图片模式：尝试从映射中加载
                Map<String, String> fileMap = textureFileMap.get(currentMode);
                if (fileMap != null && fileMap.containsKey(key)) {
                    loadImageTexture(key, fileMap.get(key));
                    return textures.get(key);
                }
                // 如果映射中没有，生成颜色纹理作为备用
                return getColorTextureByKey(key);

            case MINIMAL:
                // 极简模式：使用简单的颜色纹理
                return getColorTextureByKey(key);

            default:
                return getColorTextureByKey(key);
        }
    }

    /**
     * 根据键名获取颜色纹理
     */
    private Texture getColorTextureByKey(String key) {
        String colorKey = "color_" + key;
        if (!textures.containsKey(colorKey)) {
            Color color = getFallbackColor(key);
            textures.put(colorKey, createColorTexture(color));
        }
        return textures.get(colorKey);
    }

    /**
     * 创建纯色纹理（原有功能保留）
     */
    public Texture getColorTexture(Color color) {
        String key = "color_custom_" + color.toString();
        if (!textures.containsKey(key)) {
            textures.put(key, createColorTexture(color));
        }
        return textures.get(key);
    }

    private Texture createColorTexture(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    // 便捷方法（保持向后兼容）
    public Texture getFloorTexture() {
        return getTexture(FLOOR);
    }

    public Texture getWallTexture() {
        return getTexture(WALL);
    }

    public Texture getPlayerTexture() {
        return getTexture(PLAYER);
    }
    public Texture getTrapTexture() {
        return getTexture(TRAP);
    }

    public Texture getKeyTexture() {
        return getTexture(KEY);
    }

    public Texture getDoorTexture() {
        return getTexture(DOOR);
    }

    public Texture getLockedDoorTexture() {
        return getTexture(LOCKED_DOOR);
    }

    public Texture getEnemy1Texture() {
        return getTexture(ENEMY1);
    }

    public Texture getHeartTexture() {
        return getTexture(HEART);
    }

    /**
     * 获取当前模式
     */
    public TextureMode getCurrentMode() {
        return currentMode;
    }

    /**
     * 预加载所有模式的主要纹理（可选）
     */
    public void preloadAllModes() {
        Logger.debug("Preloading textures for all modes...");

        // 确保基础颜色纹理已加载
        getFloorTexture();
        getWallTexture();
        getPlayerTexture();
        getKeyTexture();
        getDoorTexture();

        // 保存当前模式
        TextureMode originalMode = currentMode;

        // 预加载其他模式
        for (TextureMode mode : TextureMode.values()) {
            if (mode != originalMode) {
                switchMode(mode);
                // 加载一些主要纹理
                getPlayerTexture();
                getWallTexture();
            }
        }

        // 切换回原始模式
        switchMode(originalMode);
    }

    public void dispose() {
        Logger.debug("Disposing TextureManager");
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
        textureFileMap.clear();
        instance = null;
    }
}
