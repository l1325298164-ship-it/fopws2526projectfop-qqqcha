package de.tum.cit.fop.maze;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings;
import com.badlogic.gdx.graphics.Texture.TextureFilter;

public class PackPV {

    public static void main(String[] args) {

        // === TexturePacker 配置 ===
        Settings settings = new Settings();

        // 关键：允许放下 1920x1080 的大图
        settings.maxWidth = 4096;
        settings.maxHeight = 4096;

        // 线性过滤，PV 看起来更平滑
        settings.filterMin = TextureFilter.Linear;
        settings.filterMag = TextureFilter.Linear;

        // 可选：防止重名时报错（PV 序列一般安全）
        settings.alias = false;

        // === 执行打包 ===
        String root = System.getProperty("user.dir");
        System.out.println("Working dir = " + root);

        TexturePacker.process(
                settings,
                root + "/assets_raw/chip_back",
                root + "/assets/chip_back",
                "chip_back"
        );


        System.out.println("PV atlas packed successfully.");
    }
}
