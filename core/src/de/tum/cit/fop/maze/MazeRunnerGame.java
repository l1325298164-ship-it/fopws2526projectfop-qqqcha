package de.tum.cit.fop.maze;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import de.tum.cit.fop.maze.audio.AudioConfig;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.game.Difficulty;
import de.tum.cit.fop.maze.game.DifficultyConfig;
import de.tum.cit.fop.maze.game.GameManager;
import de.tum.cit.fop.maze.screen.*;
import de.tum.cit.fop.maze.tools.MazeRunnerGameHolder;
import de.tum.cit.fop.maze.tools.PVAnimationCache;
import de.tum.cit.fop.maze.tools.PVNode;
import de.tum.cit.fop.maze.tools.PVPipeline;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.List;

/**
 * Core game class.
 */
public class MazeRunnerGame extends Game {
    private AssetManager assets;

    public AssetManager getAssets() {
        return assets;
    }
    private SpriteBatch spriteBatch;
    private Skin skin;
    private AudioManager audioManager;

    private GameManager gameManager;
    private DifficultyConfig difficultyConfig;
    private GameScreen activeGameScreen;

    private PVPipeline storyPipeline;

    /* =========================
       Story / Flow
       ========================= */
    public void setActiveGameScreen(GameScreen gs) {
        this.activeGameScreen = gs;
    }

    public boolean hasRunningGame() {
        return activeGameScreen != null;
    }

    public void resumeGame() {
        if (activeGameScreen != null) {
            setScreen(activeGameScreen);
        }
    }

    public GameManager getGameManager() {
        return  gameManager;
    }

    public void startNewGame(Difficulty difficulty) {
        Logger.debug("Start new game with difficulty = " + difficulty);

        // 重建配置 & GameManager
        this.difficultyConfig = DifficultyConfig.of(difficulty);
        this.gameManager = new GameManager(this.difficultyConfig);

        // ⚠️ 新游戏必须清掉旧的运行态
        this.activeGameScreen = null;

        // 从剧情开头开始（或你想直接进游戏也可以）
        this.stage = StoryStage.STORY_BEGIN;
        setScreen(new StoryLoadingScreen(this));
    }


    public enum PV4Result {
        START,
        EXIT
    }

    public enum StoryStage {
        STORY_BEGIN,
        MAZE_GAME_TUTORIAL,
        PV4,
        MODE_MENU,
        MAZE_GAME,
        MAIN_MENU
    }

    private StoryStage stage = StoryStage.MAIN_MENU;

    /* =========================
       Game lifecycle
       ========================= */

    @Override
    public void create() {
        MazeRunnerGameHolder.init(this); // ⭐ 必须最先
        assets = new AssetManager();   // ⭐ 全局唯一
        difficultyConfig = DifficultyConfig.of(Difficulty.NORMAL);
        gameManager = new GameManager(difficultyConfig);

        spriteBatch = new SpriteBatch();

        TextureAtlas uiAtlas =
                new TextureAtlas(Gdx.files.internal("ui/button.atlas"));

        skin = new Skin(
                Gdx.files.internal("ui/skinbutton.json"),
                uiAtlas
        );

        initializeSoundManager();
        goToMenu();
    }

    /* =========================
       Story Pipeline
       ========================= */

    private void buildStoryPipeline() {
        // ⭐ 预热 PV（一次性）
        PVAnimationCache.get("pv/1/PV_1.atlas", "PV_1");
        PVAnimationCache.get("pv/2/PV_2.atlas", "PV_2");
        PVAnimationCache.get("pv/3/PV_3.atlas", "PV_3");
        storyPipeline = new PVPipeline(this, List.of(
                new PVNode(
                        "pv/1/PV_1.atlas",
                        "PV_1",
                        AudioType.PV_1,
                        IntroScreen.PVExit.NEXT_STAGE
                ),
                new PVNode(
                        "pv/2/PV_2.atlas",
                        "PV_2",
                        AudioType.PV_2,
                        IntroScreen.PVExit.NEXT_STAGE
                ),
                new PVNode(
                        "pv/3/PV_3.atlas",
                        "PV_3",
                        AudioType.PV_3,
                        IntroScreen.PVExit.NEXT_STAGE
                )
        ));

        storyPipeline.onFinished(() -> {
            stage = StoryStage.MAZE_GAME_TUTORIAL;
            setScreen(new MazeGameTutorialScreen(this, difficultyConfig));
        });
    }

    public void startStoryFromBeginning() {
        difficultyConfig = DifficultyConfig.of(Difficulty.NORMAL);
        gameManager = new GameManager(difficultyConfig);

        stage = StoryStage.STORY_BEGIN;
        advanceStory();
    }
    public void startStoryWithLoading() {
        setScreen(new StoryLoadingScreen(this));
    }

    public void advanceStory() {
        Logger.debug("advanceStory: " + stage);

        Screen old = getScreen();

        switch (stage) {

            case STORY_BEGIN -> {
                buildStoryPipeline();
                storyPipeline.start();
            }

            case MAZE_GAME_TUTORIAL -> {
                stage = StoryStage.PV4;

                Animation<TextureRegion> pv4 =
                        PVAnimationCache.get("pv/4/PV_4.atlas", "PV_4");

                setScreen(new IntroScreen(
                        this,
                        pv4,
                        IntroScreen.PVExit.PV4_CHOICE,
                        AudioType.PV_4,
                        null
                ));
            }

            case PV4 -> {
                stage = StoryStage.MODE_MENU;
                setScreen(new ChapterSelectScreen(this));
            }

            case MODE_MENU -> {
                stage = StoryStage.MAZE_GAME;
                setScreen(new GameScreen(this, difficultyConfig));
            }

            default -> {
                Logger.debug("advanceStory ignored at stage = " + stage);
            }
        }

        if (old != null) old.dispose();
    }

    /* =========================
       Tutorial / PV4
       ========================= */

    public void onTutorialFinished(MazeGameTutorialScreen tutorial) {
        if (stage == StoryStage.MAZE_GAME_TUTORIAL) {
            advanceStory();
        }
    }

    public void onTutorialFailed(
            MazeGameTutorialScreen tutorial,
            MazeGameTutorialScreen.MazeGameTutorialResult result
    ) {
        stage = StoryStage.MAIN_MENU;
        setScreen(new MenuScreen(this));
    }

    public void onPV4Choice(PV4Result result) {
        if (stage != StoryStage.PV4) return;

        Screen old = getScreen();

        if (result == PV4Result.START) {
            saveProgress();
            stage = StoryStage.MODE_MENU;
            setScreen(new ChapterSelectScreen(this));
        } else {
            stage = StoryStage.MAIN_MENU;
            setScreen(new MenuScreen(this));
        }

        if (old != null) old.dispose();
    }

    /* =========================
       Navigation
       ========================= */

    public void goToMenu() {
        Screen old = getScreen();
        resetGameState();
        setScreen(new MenuScreen(this));
        if (old != null) old.dispose();
    }
    public void exitGame() {
        // 先做必要清理
        dispose();

        // 通知 LibGDX 退出
        Gdx.app.exit();

        // ⚠️ 桌面端保险（防止某些 IDE 卡住）
        System.exit(0);
    }
    public void goToGame() {
        if (difficultyConfig == null) {
            difficultyConfig = DifficultyConfig.of(Difficulty.NORMAL);
            gameManager = new GameManager(difficultyConfig);
        }

        Screen old = getScreen();
        setScreen(new GameScreen(this, difficultyConfig));
        if (old != null) old.dispose();
    }

    /* =========================
       Audio
       ========================= */

    private void initializeSoundManager() {
        audioManager = AudioManager.getInstance();

        audioManager.setMasterVolume(1.0f);
        audioManager.setMusicVolume(0.6f);
        audioManager.setSfxVolume(0.8f);

        audioManager.setMusicEnabled(true);
        audioManager.setSfxEnabled(true);

        AudioConfig uiConfig = audioManager.getAudioConfig(AudioType.UI_CLICK);
        if (uiConfig != null) uiConfig.setPersistent(true);
    }

    public AudioManager getSoundManager() {
        return audioManager;
    }

    /* =========================
       Utils / Cleanup
       ========================= */

    private void saveProgress() {
        Logger.debug("Progress saved (PV4)");
    }

    private void resetGameState() {
        stage = StoryStage.MAIN_MENU;
        gameManager = null;
        difficultyConfig = null;
        activeGameScreen = null;
    }

    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
    }

    public Skin getSkin() {
        return skin;
    }

    @Override
    public void dispose() {
        if (spriteBatch != null) spriteBatch.dispose();
        if (skin != null) skin.dispose();
        if (audioManager != null) audioManager.dispose();
        TextureManager.getInstance().dispose();
    }
}
