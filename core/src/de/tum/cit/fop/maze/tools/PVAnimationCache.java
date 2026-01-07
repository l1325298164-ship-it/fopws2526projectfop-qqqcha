package de.tum.cit.fop.maze.tools;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public final class PVAnimationCache {

    private static final ObjectMap<String, Animation<TextureRegion>> CACHE =
            new ObjectMap<>();

    private PVAnimationCache() {}

    public static Animation<TextureRegion> get(
            String atlasPath,
            String regionName
    ) {
        AssetManager assets =
                MazeRunnerGameHolder.get().getAssets();

        TextureAtlas atlas =
                assets.get(atlasPath, TextureAtlas.class);

        Array<TextureAtlas.AtlasRegion> frames =
                atlas.findRegions(regionName);

        return new Animation<>(1f / 24f, frames);
    }
}
