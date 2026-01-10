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
import de.tum.cit.fop.maze.game.GameSaveData;
import de.tum.cit.fop.maze.screen.*;
import de.tum.cit.fop.maze.tools.MazeRunnerGameHolder;
import de.tum.cit.fop.maze.tools.PVAnimationCache;
import de.tum.cit.fop.maze.tools.PVNode;
import de.tum.cit.fop.maze.tools.PVPipeline;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.StorageManager;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.List;

/**
 * Core game class.
 */
public class MazeRunnerGame extends Game {

    private AssetManager assets;
    private Difficulty currentDifficulty = Difficulty.NORMAL;

    public Difficulty getCurrentDifficulty() {
        return currentDifficulty != null ? currentDifficulty : Difficulty.NORMAL;
    }

    public AssetManager getAssets() {
        return assets;
    }

    private SpriteBatch spriteBatch;
    private Skin skin;
    private AudioManager audioManager;

    private boolean twoPlayerMode = true;
    public boolean isTwoPlayerMode() {
        return twoPlayerMode;
    }

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

    // 🔧 FIX: 原来有两个 return（编译错误）
    public boolean hasRunningGame() {
        return activeGameScreen != null
                || getScreen() instanceof GameScreen
                || getScreen() instanceof EndlessScreen;
    }

    // 🔧 FIX: 大括号缺失
    public void resumeGame() {
        if (activeGameScreen != null) {
            setScreen(activeGameScreen);
            if (getScreen() instanceof GameScreen) {
                Gdx.input.setInputProcessor(null);
            }
        }
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    /**
     * ✨ [新增] 设置 GameManager（用于 GameScreen 同步）
     */
    public void setGameManager(GameManager gm) {
        this.gameManager = gm;
    }

    public void startNewGame(Difficulty difficulty) {
        this.currentDifficulty = difficulty;
        Logger.debug("Start new game with difficulty = " + difficulty);

        this.difficultyConfig = createDifficultyConfig(difficulty);
        this.gameManager = new GameManager(
                this.difficultyConfig,
                this.twoPlayerMode
        );

        if (difficulty == Difficulty.ENDLESS) {
            System.out.println("🎮 直接进入无尽模式");

            if (getScreen() != null) {
                getScreen().hide();
            }

            setScreen(new EndlessScreen(this, difficultyConfig));
            return;
        }

        this.stage = StoryStage.STORY_BEGIN;
        setScreen(new StoryLoadingScreen(this));
    }

    public void loadGame() {
        StorageManager storage = StorageManager.getInstance();
        GameSaveData saveData = storage.loadGame();

        if (saveData == null) {
            Logger.error("Load failed: No save data found.");
            startNewGameFromMenu();
            return;
        }

        Difficulty savedDifficulty = Difficulty.NORMAL;
        try {
            if (saveData.difficulty != null && !saveData.difficulty.isEmpty()) {
                savedDifficulty = Difficulty.valueOf(saveData.difficulty);
            }
        } catch (IllegalArgumentException e) {
            Logger.warning("Invalid difficulty in save data: " + saveData.difficulty);
        }

        difficultyConfig = DifficultyConfig.of(savedDifficulty);
        gameManager = new GameManager(difficultyConfig);
        gameManager.restoreState(saveData);

        setScreen(new GameScreen(this, difficultyConfig));
    }

    public void startNewGameFromMenu() {
        StorageManager.getInstance().deleteSave();
        difficultyConfig = DifficultyConfig.of(Difficulty.NORMAL);
        gameManager = new GameManager(difficultyConfig);
        setScreen(new GameScreen(this, difficultyConfig));
    }

    private DifficultyConfig createDifficultyConfig(Difficulty difficulty) {
        DifficultyConfig baseConfig = DifficultyConfig.of(difficulty);

        if (difficulty == Difficulty.ENDLESS) { // 🔧 FIX: typo
            return new DifficultyConfig(
                    Difficulty.ENDLESS,
                    40, 40, 0,
                    1, 1, 1, 1,
                    10, 5, 3, 2,
                    200,
                    1.4f, 1.3f, 0
            );
        }
        return baseConfig;
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
        MazeRunnerGameHolder.init(this);
        assets = new AssetManager();

        currentDifficulty = Difficulty.NORMAL;
        difficultyConfig = DifficultyConfig.of(currentDifficulty);
        gameManager = new GameManager(difficultyConfig, twoPlayerMode);

        spriteBatch = new SpriteBatch();

        TextureAtlas uiAtlas = new TextureAtlas(Gdx.files.internal("ui/button.atlas"));
        skin = new Skin(Gdx.files.internal("ui/skinbutton.json"), uiAtlas);

        initializeSoundManager();
        goToMenu();
    }

    @Override
    public void setScreen(Screen screen) {
        System.out.println("=== 屏幕切换 ===");
        System.out.println("   从: " + (getScreen() == null ? "null" : getScreen().getClass().getSimpleName()));
        System.out.println("   到: " + (screen == null ? "null" : screen.getClass().getSimpleName()));
        super.setScreen(screen);
    }

    /* =========================
       Story Pipeline
       ========================= */

    private void buildStoryPipeline() {
        PVAnimationCache.get("pv/1/PV_1.atlas", "PV_1");
        PVAnimationCache.get("pv/2/PV_2.atlas", "PV_2");
        PVAnimationCache.get("pv/3/PV_3.atlas", "PV_3");

        storyPipeline = new PVPipeline(this, List.of(
                new PVNode("pv/1/PV_1.atlas", "PV_1", AudioType.PV_1, IntroScreen.PVExit.NEXT_STAGE),
                new PVNode("pv/2/PV_2.atlas", "PV_2", AudioType.PV_2, IntroScreen.PVExit.NEXT_STAGE),
                new PVNode("pv/3/PV_3.atlas", "PV_3", AudioType.PV_3, IntroScreen.PVExit.NEXT_STAGE)
        ));

        storyPipeline.onFinished(() -> {
            stage = StoryStage.MAZE_GAME_TUTORIAL;
            setScreen(new MazeGameTutorialScreen(this, difficultyConfig));
        });
    }

    public void advanceStory() {
        Logger.debug("advanceStory ENTER, stage = " + stage);

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
            default -> Logger.debug("advanceStory ignored");
        }
    }

    /* =========================
       Navigation / Audio / Dispose
       ========================= */

    public void goToMenu() {
        stage = StoryStage.MAIN_MENU;
        setScreen(new MenuScreen(this));
    }

    private void initializeSoundManager() {
        audioManager = AudioManager.getInstance();
        audioManager.setMasterVolume(1f);
        audioManager.setMusicVolume(0.6f);
        audioManager.setSfxVolume(0.8f);
        audioManager.setMusicEnabled(true);
        audioManager.setSfxEnabled(true);

        AudioConfig ui = audioManager.getAudioConfig(AudioType.UI_CLICK);
        if (ui != null) ui.setPersistent(true);
    }

    @Override
    public void dispose() {
        if (spriteBatch != null) spriteBatch.dispose();
        if (skin != null) skin.dispose();
        if (audioManager != null) audioManager.dispose();
        TextureManager.getInstance().dispose();
    }
}
