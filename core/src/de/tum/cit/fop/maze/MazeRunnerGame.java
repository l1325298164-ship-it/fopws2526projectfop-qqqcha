package de.tum.cit.fop.maze;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import de.tum.cit.fop.maze.screen.GameScreen;
import de.tum.cit.fop.maze.screen.MenuScreen;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;
// 添加 Audio导入
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.audio.AudioConfig;

/**
 * The MazeRunnerGame class represents the core of the Maze Runner game.
 * It manages the screens and global resources like SpriteBatch and Skin.
 */
public class MazeRunnerGame extends Game {

    private SpriteBatch spriteBatch;
    private Skin skin;
    private AudioManager audioManager;  // 添加音效管理器字段

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
        if (audioManager != null) {
            audioManager.resumeMusic();
        }
    }

    public void goToMenu() {
        Screen old = getScreen();
        setScreen(new MenuScreen(this));
        if (old != null) old.dispose();
        // 切换到菜单屏幕时确保音乐继续播放
        if (audioManager != null) {
            audioManager.resumeMusic();
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
            // 获取音频管理器实例（单例模式）
            audioManager = AudioManager.getInstance();

            // 不再需要调用 preloadAllSounds()，因为 AudioManager 构造函数中已经初始化

            // 设置全局音量
            audioManager.setMasterVolume(1.0f);
            audioManager.setMusicVolume(0.6f);      // 背景音乐音量
            audioManager.setSfxVolume(0.8f);        // 音效音量（注意方法名已改为 setSfxVolume）

            // 启用音乐和音效
            audioManager.setMusicEnabled(true);
            audioManager.setSfxEnabled(true);

            // 播放背景音乐
            // 注意：根据您的AudioType，背景音乐的枚举是MUSIC_MENU
            // 而不是"background"，所以使用AudioType枚举
            audioManager.playMusic(AudioType.MUSIC_MENU);

            // 可选：设置UI音效为持久化，避免被清理
            AudioConfig uiConfig = audioManager.getAudioConfig(AudioType.UI_CLICK);
            if (uiConfig != null) {
                uiConfig.setPersistent(true);
            }

            // 可选：设置移动音效循环（已在AudioType中设置，这里确保）
            AudioConfig moveConfig = audioManager.getAudioConfig(AudioType.PLAYER_MOVE);
            if (moveConfig != null) {
                moveConfig.setLoop(true);
                moveConfig.setPersistent(true); // 移动音效也设为持久化
            }

            Logger.debug("音效系统初始化完成");
            Logger.debug("当前音量设置: 主音量=" + audioManager.getMasterVolume() +
                    ", 音乐音量=" + audioManager.getMusicVolume() +
                    ", 音效音量=" + audioManager.getSfxVolume());
        } catch (Exception e) {
            Logger.error("音效系统初始化失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取音效管理器
     */
    public AudioManager getSoundManager() {
        return audioManager;
    }

    @Override
    public void dispose() {
        super.dispose();

        if (spriteBatch != null) spriteBatch.dispose();
        if (skin != null) skin.dispose();
        // 清理音效资源
        if (audioManager != null) {
            audioManager.dispose();
        }

        TextureManager.getInstance().dispose();

        Logger.debug("Game disposed");
    }

}

