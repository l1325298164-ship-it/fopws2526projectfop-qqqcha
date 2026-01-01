package de.tum.cit.fop.maze;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.qte.QTEResult;
import de.tum.cit.fop.maze.screen.*;
import de.tum.cit.fop.maze.tools.ChapterContext;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;
// 添加 Audio的导入
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
    private GameManager gameManager;
    private DifficultyConfig difficultyConfig;
    private GameScreen activeGameScreen;
    public void startNewGame(Difficulty difficulty) {
        Logger.debug("Start new game with difficulty = " + difficulty);
        this.difficultyConfig = DifficultyConfig.of(difficulty);
        this.gameManager = new GameManager(this.difficultyConfig);
    }

    public void onTutorialFailed(
            MazeGameTutorialScreen tutorial,
            MazeGameTutorialScreen.MazeGameTutorialResult result
    ) {
        // ❌ 教程失败或退出：不存档、不推进剧情
        stage = StoryStage.MAIN_MENU;
        setScreen(new MenuScreen(this));
    }

    public enum PV4Result {
        START,
        EXIT
    }

    public enum StoryStage {
        PV1,
        QTE1,

        PV2_SUCCESS,
        PV2_FAIL,   // ❗失败后不再推进
        QTE2,

        PV3_SUCCESS,
        PV3_FAIL,   // ❗失败后不再推进

        MAZE_GAME_TUTORIAL,
        PV4,
        MODE_MENU,
        MAZE_GAME,
        MAIN_MENU
    }


    private StoryStage stage = StoryStage.PV1;
    // ⚠️ advanceStory 永远不会存档
    public void advanceStory() {
        Logger.debug("advanceStory called, stage = " + stage);
        Screen old = getScreen();

        switch (stage) {

            // =====================
            // 第一段
            // =====================
            case PV1 -> {
                stage = StoryStage.QTE1;
                setScreen(new QTEScreen_single(this, gameManager));
            }

            // ⚠️ QTE1 不在这里处理
            // QTE1 → onQTEFinished()

            // =====================
            // 第二段（分支）
            // =====================
            case PV2_SUCCESS -> {
                stage = StoryStage.QTE2;
                setScreen(new QTEScreen_double(this, gameManager));
            }

            case PV2_FAIL, PV3_FAIL -> {
                stage = StoryStage.MAIN_MENU;
                setScreen(new MenuScreen(this));
            }

            // =====================
            // 第三段（分支）
            // =====================
            case PV3_SUCCESS -> {
                stage = StoryStage.MAZE_GAME_TUTORIAL;
                setScreen(new MazeGameTutorialScreen(this,difficultyConfig));
            }

            // =====================
            // Maze → Menu → 正式游戏
            // =====================
            case MAZE_GAME_TUTORIAL -> {
                stage = StoryStage.PV4;
                setScreen(new IntroScreen(this, "pv/4/pv_4.atlas",
                        "pv_4",
                        IntroScreen.PVExit.PV4_CHOICE));
            }

            case PV4 -> {
                stage = StoryStage.MODE_MENU;
                setScreen(new ChapterSelectScreen(this));
            }


            case MODE_MENU -> {
                stage = StoryStage.MAZE_GAME;
                setScreen(new GameScreen(this,difficultyConfig));
            }

            default -> {
                // 防御性兜底（防止卡死）
                Gdx.app.log("Stage", "Unhandled stage: " + stage);
            }
        }

        if (old != null) old.dispose();
    }


    @Override
    public void create() {
        this.difficultyConfig = DifficultyConfig.of(Difficulty.NORMAL);
        this.gameManager = new GameManager( this.difficultyConfig);
        spriteBatch = new SpriteBatch();

        // ✅ 先加载 atlas
        TextureAtlas uiAtlas =
                new TextureAtlas(Gdx.files.internal("ui/button.atlas"));

        // ✅ 把 atlas 注册进 Skin，再解析 json
        skin = new Skin(
                Gdx.files.internal("ui/skinbutton.json"),
                uiAtlas
        );

        System.out.println("FONT = " + skin.getFont("default-font"));


        initializeSoundManager();
        goToMenu();
    }




    public void goToPV() {
        Screen old = getScreen();

        setScreen(new IntroScreen(
                this,
                "pv/1/pv_1.atlas",
                "pv_1",
                IntroScreen.PVExit.NEXT_STAGE
        ));

        if (old != null) old.dispose();
        audioManager.stopAll();
    }

    public void goToGame() {
        Screen old = getScreen();
        Logger.debug("TextureManager CONSTRUCTOR");
        //临时更改为pv播放
        setScreen(new GameScreen(this,difficultyConfig));
        if (old != null) old.dispose();
        // 切换到游戏屏幕时确保音乐继续播放
        if (audioManager != null) {
            audioManager.resumeMusic();
        }
    }
    public void setActiveGameScreen(GameScreen gs) {
        this.activeGameScreen = gs;
    }

    public boolean hasRunningGame() {
        return activeGameScreen != null;
    }

    public void resumeGame() {
        setScreen(activeGameScreen);
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

    public GameManager getGameManager() {
        return gameManager;
    }

    public void onQTEFinished(QTEResult result) {
        Screen old = getScreen();

        switch (stage) {

            // =====================
            // QTE1 结果
            // =====================
            case QTE1 -> {
                if (result == QTEResult.SUCCESS) {
                    stage = StoryStage.PV2_SUCCESS;
                    setScreen(new IntroScreen(
                            this,
                            "pv/2/pv_2.atlas",
                            "pv_2",IntroScreen.PVExit.NEXT_STAGE
                    ));
                } else {
                    // ❌ 失败：直接进失败 PV
                    stage = StoryStage.PV2_FAIL;
                    setScreen(new IntroScreen(
                            this,
                            "pv/5/pre1.atlas",
                            "pre1",IntroScreen.PVExit.TO_MENU
                    ));
                }
            }

            // =====================
            // QTE2 结果
            // =====================
            case QTE2 -> {
                if (result == QTEResult.SUCCESS) {
                    stage = StoryStage.PV3_SUCCESS;
                    setScreen(new IntroScreen(
                            this,
                            "pv/5/pre1.atlas",
                            "pre1",IntroScreen.PVExit.NEXT_STAGE
                    ));
                } else {
                    // ❌ 失败：直接进失败 PV
                    stage = StoryStage.PV3_FAIL;
                    setScreen(new IntroScreen(
                            this,
                            "pv/5/pre1.atlas",
                            "pre1",IntroScreen.PVExit.TO_MENU
                    ));
                }
            }
        }

        if (old != null) old.dispose();
    }


    public void onTutorialFinished(MazeGameTutorialScreen tutorial) {
        if (stage == StoryStage.MAZE_GAME_TUTORIAL) {
            stage = StoryStage.PV4;
            setScreen(new IntroScreen(
                    this,
                    "pv/4/pv_4.atlas",
                    "pv_4",
                    IntroScreen.PVExit.PV4_CHOICE
            ));
        }
    }



    public void onPV4Choice(PV4Result result) {
        if (stage != StoryStage.PV4) return;

        Screen old = getScreen();

        if (result == PV4Result.START) {
            saveProgress();              // ✅唯一一次
            stage = StoryStage.MODE_MENU;
            setScreen(new ChapterSelectScreen(this));
        } else {
            stage = StoryStage.MAIN_MENU;
            setScreen(new MenuScreen(this));
        }

        if (old != null) old.dispose();
    }

    private void saveProgress() {
        // TODO: 以后真正写存档逻辑
        Logger.debug("Progress saved (PV4 confirmation)");
    }


//
//    public void startChapter(Chapter chapter) {
//        ChapterContext ctx = new ChapterContext(chapter);
//        setScreen(new GameScreen(this, ctx));
//    }
}


