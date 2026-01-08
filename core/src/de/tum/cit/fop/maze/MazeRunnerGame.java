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
import de.tum.cit.fop.maze.game.GameSaveData; // 引入存档数据
import de.tum.cit.fop.maze.screen.*;
import de.tum.cit.fop.maze.tools.MazeRunnerGameHolder;
import de.tum.cit.fop.maze.tools.PVAnimationCache;
import de.tum.cit.fop.maze.tools.PVNode;
import de.tum.cit.fop.maze.tools.PVPipeline;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.StorageManager; // 引入存储管理器
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
        return gameManager;
    }

    public void startNewGame(Difficulty difficulty) {
        Logger.debug("Start new game with difficulty = " + difficulty);

        this.difficultyConfig = createDifficultyConfig(difficulty);
        this.gameManager = new GameManager(this.difficultyConfig);
        this.activeGameScreen = null;

        if (difficulty == Difficulty.ENDLESS) {
            System.out.println("🎮 直接进入无尽模式");
            if (getScreen() != null) {
                getScreen().hide();
            }
            EndlessScreen endlessScreen = new EndlessScreen(this, difficultyConfig);
            setScreen(endlessScreen);
            return;
        }

        // 否则，从剧情开头开始
        this.stage = StoryStage.STORY_BEGIN;
        setScreen(new StoryLoadingScreen(this));
    }

    // 🔥 [新增] 从存档加载游戏
    public void loadGame() {
        StorageManager storage = new StorageManager();
        GameSaveData saveData = storage.loadGame();

        if (saveData == null) {
            Logger.error("Load failed: No save data found.");
            startNewGameFromMenu(); // 降级处理
            return;
        }

        Logger.info("Loading game... Level: " + saveData.currentLevel);

        // 初始化默认配置 (如果存档里没有存难度，只能用默认)
        if (difficultyConfig == null) {
            difficultyConfig = DifficultyConfig.of(Difficulty.NORMAL);
        }

        // 创建新的 GameManager
        gameManager = new GameManager(difficultyConfig);

        // 恢复状态
        gameManager.restoreState(saveData);

        // 切换屏幕
        setScreen(new GameScreen(this, difficultyConfig));
    }

    // 🔥 [新增] 强制开始新游戏 (带清理)
    public void startNewGameFromMenu() {
        // 1. 清理存档
        new StorageManager().deleteSave();

        // 2. 初始化配置
        difficultyConfig = DifficultyConfig.of(Difficulty.NORMAL);
        gameManager = new GameManager(difficultyConfig);

        // 3. 进入游戏
        setScreen(new GameScreen(this, difficultyConfig));
    }

    private DifficultyConfig createDifficultyConfig(Difficulty difficulty) {
        DifficultyConfig baseConfig = DifficultyConfig.of(difficulty);
        if (difficulty == Difficulty.ENDLESS) {
            return new DifficultyConfig(
                    difficulty.ENDLESS,40, 40, 0,
                    1, 1, 1, 1,
                    10, 5, 3, 2,
                    200,
                    1.4f, 1.3f, 0
            );
        }
        return baseConfig;
    }

    public enum PV4Result { START, EXIT }

    public enum StoryStage {
        STORY_BEGIN, MAZE_GAME_TUTORIAL, PV4, MODE_MENU, MAZE_GAME, MAIN_MENU
    }

    private StoryStage stage = StoryStage.MAIN_MENU;

    /* =========================
       Game lifecycle
       ========================= */

    @Override
    public void create() {
        System.out.println("🎮 MazeRunnerGame.create() 开始");
        MazeRunnerGameHolder.init(this);
        assets = new AssetManager();
        difficultyConfig = DifficultyConfig.of(Difficulty.NORMAL);
        gameManager = new GameManager(difficultyConfig);

        spriteBatch = new SpriteBatch();

        TextureAtlas uiAtlas = new TextureAtlas(Gdx.files.internal("ui/button.atlas"));
        skin = new Skin(Gdx.files.internal("ui/skinbutton.json"), uiAtlas);

        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        pixmap.fill();
        com.badlogic.gdx.graphics.Texture whiteTexture = new com.badlogic.gdx.graphics.Texture(pixmap);
        skin.add("white", new com.badlogic.gdx.graphics.g2d.TextureRegion(whiteTexture));
        pixmap.dispose();

        initializeSoundManager();
        goToMenu();
    }

    @Override
    public void setScreen(Screen screen) {
        String oldScreen = getScreen() != null ? getScreen().getClass().getSimpleName() : "null";
        String newScreen = screen != null ? screen.getClass().getSimpleName() : "null";
        System.out.println("=== 屏幕切换: " + oldScreen + " -> " + newScreen + " ===");
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
        Logger.debug("advanceStory ENTER, stage = " + stage);
        switch (stage) {
            case STORY_BEGIN -> { buildStoryPipeline(); storyPipeline.start(); }
            case MAZE_GAME_TUTORIAL -> {
                stage = StoryStage.PV4;
                Animation<TextureRegion> pv4 = PVAnimationCache.get("pv/4/PV_4.atlas", "PV_4");
                setScreen(new IntroScreen(this, pv4, IntroScreen.PVExit.PV4_CHOICE, AudioType.PV_4, null));
            }
            case PV4 -> { stage = StoryStage.MODE_MENU; setScreen(new ChapterSelectScreen(this)); }
            case MODE_MENU -> { stage = StoryStage.MAZE_GAME; setScreen(new GameScreen(this, difficultyConfig)); }
            default -> Logger.debug("advanceStory ignored at stage = " + stage);
        }
    }

    public void onTutorialFinished(MazeGameTutorialScreen tutorial) {
        if (stage == StoryStage.MAZE_GAME_TUTORIAL) {
            Gdx.app.postRunnable(this::advanceStory);
        }
    }

    public void onTutorialFailed(MazeGameTutorialScreen tutorial, MazeGameTutorialScreen.MazeGameTutorialResult result) {
        stage = StoryStage.MAIN_MENU;
        setScreen(new MenuScreen(this));
    }

    public void onPV4Choice(PV4Result result) {
        if (stage != StoryStage.PV4) return;
        if (result == PV4Result.START) {
            saveProgress();
            stage = StoryStage.MODE_MENU;
            setScreen(new ChapterSelectScreen(this));
        } else {
            stage = StoryStage.MAIN_MENU;
            setScreen(new MenuScreen(this));
        }
    }

    public void goToMenu() {
        if (getScreen() instanceof EndlessScreen) return;
        resetGameState();
        setScreen(new MenuScreen(this));
    }

    public void exitGame() {
        dispose();
        Gdx.app.exit();
        System.exit(0);
    }

    public void goToGame() {
        if (getScreen() instanceof EndlessScreen) return;
        if (difficultyConfig == null) {
            difficultyConfig = DifficultyConfig.of(Difficulty.NORMAL);
            gameManager = new GameManager(difficultyConfig);
        }
        Screen old = getScreen();
        setScreen(new GameScreen(this, difficultyConfig));
        if (old != null) old.dispose();
    }

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

    public AudioManager getSoundManager() { return audioManager; }
    private void saveProgress() { Logger.debug("Progress saved (PV4)"); }
    private void resetGameState() {
        stage = StoryStage.MAIN_MENU;
        gameManager = null;
        difficultyConfig = null;
        activeGameScreen = null;
    }

    public SpriteBatch getSpriteBatch() { return spriteBatch; }
    public Skin getSkin() { return skin; }

    @Override
    public void dispose() {
        if (spriteBatch != null) spriteBatch.dispose();
        if (skin != null) skin.dispose();
        if (audioManager != null) audioManager.dispose();
        TextureManager.getInstance().dispose();
    }

    public void resetMaze(Difficulty difficulty) {
        this.difficultyConfig = DifficultyConfig.of(difficulty);
        this.gameManager = new GameManager(this.difficultyConfig);
        this.activeGameScreen = null;
        setScreen(new GameScreen(this, difficultyConfig));
    }

    public void debugEnterTutorial() {
        stage = StoryStage.MAZE_GAME_TUTORIAL;
        storyPipeline = null;
        difficultyConfig = DifficultyConfig.of(Difficulty.NORMAL);
        gameManager = new GameManager(difficultyConfig);
        AssetManager am = getAssets();
        if (!am.isLoaded("pv/4/PV_4.atlas")) {
            am.load("pv/4/PV_4.atlas", TextureAtlas.class);
            am.finishLoadingAsset("pv/4/PV_4.atlas");
        }
        setScreen(new MazeGameTutorialScreen(this, difficultyConfig));
    }
}