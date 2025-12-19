package de.tum.cit.fop.maze;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.screen.GameScreen;
import de.tum.cit.fop.maze.screen.MenuScreen;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;
import games.spooky.gdx.nativefilechooser.NativeFileChooser;
// 添加 SoundManager 导入
import de.tum.cit.fop.maze.accoustic.SoundManager;

/**
 * The MazeRunnerGame class represents the core of the Maze Runner game.
 * It manages the screens and global resources like SpriteBatch and Skin.
 */
public class MazeRunnerGame extends Game {

    private SpriteBatch spriteBatch;
    private Skin skin;
    private SoundManager soundManager;  // 添加音效管理器字段

    @Override
    public void create() {
        spriteBatch = new SpriteBatch();
        skin = new Skin(Gdx.files.internal("craft/craftacular-ui.json"));
        TextureManager.getInstance().switchMode(TextureManager.TextureMode.IMAGE);
        initializeSoundManager();// 初始化音效系统

        goToMenu();
    }

    public void goToGame() {
        Screen old = getScreen();
        setScreen(new GameScreen(this));
        if (old != null) old.dispose();
        // 切换到游戏屏幕时确保音乐继续播放
        if (soundManager != null) {
            soundManager.resumeMusic();
        }
    }

    public void goToMenu() {
        Screen old = getScreen();
        setScreen(new MenuScreen(this));
        if (old != null) old.dispose();
        // 切换到菜单屏幕时确保音乐继续播放
        if (soundManager != null) {
            soundManager.resumeMusic();
        }
    }


    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
    }

    public Skin getSkin() {
        return skin;
    }

    /**
     * 初始化音效管理器
     */
    private void initializeSoundManager() {
        try {
            soundManager = SoundManager.getInstance();
            soundManager.preloadAllSounds();

            // 设置全局音量
            soundManager.setMasterVolume(1.0f);
            soundManager.setMusicVolume(0.6f);      // 背景音乐音量
            soundManager.setSoundEffectsVolume(0.8f); // 音效音量

            // 确保背景音乐播放（如果 SoundManager 不支持 autoPlay，可以手动调用）
            soundManager.playMusic("background");

            Logger.debug("音效系统初始化完成");
        } catch (Exception e) {
            Logger.error("音效系统初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取音效管理器
     */
    public SoundManager getSoundManager() {
        return soundManager;
    }

    @Override
    public void dispose() {
        super.dispose();

        if (spriteBatch != null) spriteBatch.dispose();
        if (skin != null) skin.dispose();
        // 清理音效资源
        if (soundManager != null) {
            soundManager.dispose();
        }

        TextureManager.getInstance().dispose();

        Logger.debug("Game disposed");
    }

}

