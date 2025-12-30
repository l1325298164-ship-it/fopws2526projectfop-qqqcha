package de.tum.cit.fop.maze.tools;

import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;

public class UiNinePatch {

    /**
     * @param atlas TextureAtlas
     * @param regionName atlas 里的 region 名
     * @param left  左边不拉伸的像素
     * @param right 右边不拉伸的像素
     * @param top   上边不拉伸的像素
     * @param
     */
    public static NinePatchDrawable fromAtlas(
            TextureAtlas atlas,
            String regionName,
            int left, int right, int top, int bottom
    ) {
        return new NinePatchDrawable(
                new NinePatch(
                        atlas.findRegion(regionName),
                        left, right, top, bottom
                )
        );
    }
}
