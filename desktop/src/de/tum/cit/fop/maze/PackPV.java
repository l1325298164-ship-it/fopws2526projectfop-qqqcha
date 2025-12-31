package de.tum.cit.fop.maze;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings;
import com.badlogic.gdx.graphics.Texture.TextureFilter;

public class PackPV {

    public static void main(String[] args) {

        Settings settings = new Settings();

        // === atlas 最大尺寸 ===
        settings.maxWidth = 4096;
        settings.maxHeight = 4096;

        // === ⭐⭐⭐ 核心修复：禁止旋转 ⭐⭐⭐ ===
        settings.rotation = false;

        // === 推荐设置 ===
        settings.filterMin = TextureFilter.Linear;
        settings.filterMag = TextureFilter.Linear;

        settings.alias = false;

        // 防止裁剪透明边（坐标更直观）
        settings.stripWhitespaceX = false;
        settings.stripWhitespaceY = false;

        String root = System.getProperty("user.dir");
        System.out.println("Working dir = " + root);

        TexturePacker.process(
                settings,
                root + "/assets_raw/tiles_stage_2",
                root + "/assets/menu_bg",
                "tiles_stage_2"
        );

        System.out.println("PV atlas packed successfully.");
    }
}
