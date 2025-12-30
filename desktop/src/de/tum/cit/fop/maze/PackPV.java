package de.tum.cit.fop.maze;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings;
import com.badlogic.gdx.graphics.Texture.TextureFilter;

public class PackPV {

    public static void main(String[] args) {

        // === TexturePacker 配置 ===
        Settings settings = new Settings();

        // 1. 尺寸设置：允许 4096，确保能装下你的高清大图
        settings.maxWidth = 4096;
        settings.maxHeight = 4096;

        // 2. 过滤设置：线性过滤，保证播放时画质平滑
        settings.filterMin = TextureFilter.Linear;
        settings.filterMag = TextureFilter.Linear;

        // 3. 【绝对关键】路径扁平化 (Flatten Paths)
        // 你的图片虽然放在 assets_raw/pv/1/pv1_000.png
        // 开启这个后，Atlas 里的名字直接就是 "pv1_000" (自动去掉前面的 "1/" 文件夹名)
        // 这样代码里写 "pv1" 就能找到，完全不用管它在哪个文件夹里！
        settings.flattenPaths = true;

        // 可选：防止重名报错
        settings.alias = false;


        // === 路径准备 ===
        String root = System.getProperty("user.dir");
        System.out.println("当前工作目录: " + root);

        // 输入路径：只要指向 pv 这一层，它会自动把里面 1, 2, 3 文件夹里的图全抓出来
        String inputDir = root + "/assets_raw/pv";
        String outputDir = root + "/assets/pv";     // 输出位置
        String packFileName = "pv";                 // 生成文件名 (pv.atlas)


        // === 执行打包 ===
        System.out.println(">>> 正在打包 PV 序列 (合并所有子文件夹)...");

        try {
            TexturePacker.process(settings, inputDir, outputDir, packFileName);
            System.out.println("✅ 打包成功！请检查 assets/pv/pv.atlas");
        } catch (Exception e) {
            System.err.println("❌ 打包失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}