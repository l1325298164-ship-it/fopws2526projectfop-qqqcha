// TextureManager.java
package de.tum.cit.fop.maze.utils;

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

    private TextureManager() {
        textures = new HashMap<>();
        Logger.debug("TextureManager initialized");
    }

    public static TextureManager getInstance() {
        if (instance == null) {
            instance = new TextureManager();
        }
        return instance;
    }

    public Texture getColorTexture(Color color) {
        String key = color.toString();
        if (!textures.containsKey(key)) {
            textures.put(key, createColorTexture(color));
        }
        return textures.get(key);
    }

    public Texture getFloorTexture() {
        return getColorTexture(GameConstants.FLOOR_COLOR);
    }

    public Texture getWallTexture() {
        return getColorTexture(GameConstants.WALL_COLOR);
    }

    public Texture getPlayerTexture() {
        return getColorTexture(GameConstants.PLAYER_COLOR);
    }

    public Texture getKeyTexture() {
        return getColorTexture(GameConstants.KEY_COLOR);
    }

    public Texture getDoorTexture() {
        return getColorTexture(GameConstants.DOOR_COLOR);
    }

    public Texture getLockedDoorTexture() {
        return getColorTexture(GameConstants.LOCKED_DOOR_COLOR);
    }

    private Texture createColorTexture(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        Logger.debug("Created texture for color: " + color);
        return texture;
    }

    public void dispose() {
        Logger.debug("Disposing TextureManager");
        for (Texture texture : textures.values()) {
            texture.dispose();
        }
        textures.clear();
        instance = null;
    }
}
