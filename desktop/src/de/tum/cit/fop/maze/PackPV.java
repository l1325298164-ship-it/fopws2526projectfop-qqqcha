package de.tum.cit.fop.maze;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings;
import com.badlogic.gdx.graphics.Texture.TextureFilter;

public class PackPV {

    public static void main(String[] args) {

        Settings settings = new Settings();

        // === ⭐ 新增：自动缩小比例 ⭐ ===
        // 1248 * 0.25 = 312 像素，非常完美
        settings.scale = new float[]{1f};

        // === 因为缩小了，最大尺寸也可以调小了 ===
        // 1024x1024 足够装下缩小后的 9 帧
        settings.maxWidth = 1024;
        settings.maxHeight = 1024;

        // === 其余保持不变 ===
        settings.rotation = false;
        settings.filterMin = TextureFilter.Linear;
        settings.filterMag = TextureFilter.Linear;
        settings.alias = false;
        settings.stripWhitespaceX = false;
        settings.stripWhitespaceY = false;

        String root = System.getProperty("user.dir");

        // 执行打包（记得把路径改成你小猫素材的路径）
        TexturePacker.process(
                settings,
                root + "/assets_raw/1", // 假设你的大猫原图在这里
                root + "/assets/Skin", // 输出到 assets
                "skin"                // 输出文件名
        );

        System.out.println("Cat atlas packed successfully with 1x scale.");
    }
}