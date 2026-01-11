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
import de.tum.cit.fop.maze.game.GameSaveData;
import de.tum.cit.fop.maze.utils.Logger;
import de.tum.cit.fop.maze.utils.StorageManager;
import de.tum.cit.fop.maze.utils.TextureManager;

import java.util.List;

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
    private boolean twoPlayerMode = false;
    public boolean isTwoPlayerMode() { return twoPlayerMode; }

    private GameManager gameManager;
    private DifficultyConfig difficultyConfig;
    private PVPipeline storyPipeline;

    public boolean hasRunningGame() {
        return getScreen() instanceof GameScreen || getScreen() instanceof EndlessScreen;
    }

    public void resumeGame() {
        if (getScreen() instanceof GameScreen gs) {
            Gdx.input.setInputProcessor(null);
        }
    }

    public GameManager getGameManager() { return gameManager; }

    public void startNewGame(Difficulty difficulty) {
        this.currentDifficulty = difficulty;
        Logger.debug("Start new game with difficulty = " + difficulty);

        this.difficultyConfig = createDifficultyConfig(difficulty);
        this.gameManager = new GameManager(this.difficultyConfig, this.twoPlayerMode);

        if (difficulty == Difficulty.ENDLESS) {
            if (getScreen() != null) getScreen().hide();
            EndlessScreen endlessScreen = new EndlessScreen(this, difficultyConfig);
            setScreen(endlessScreen);
            return;
        }

        this.stage = StoryStage.STORY_BEGIN;
        // 如果这里不需要 loading，可以直接 setScreen(new GameScreen(...))
        // 但目前保留 StoryLoadingScreen 如果它是正常的
        setScreen(new StoryLoadingScreen(this));
    }

    private DifficultyConfig createDifficultyConfig(Difficulty difficulty) {
        DifficultyConfig baseConfig = DifficultyConfig.of(difficulty);
        if (difficulty == Difficulty.ENDLESS) {
            return new DifficultyConfig(
                    Difficulty.ENDLESS, 40, 40, 0,
                    1, 1, 1, 1,
                    10, 5, 3, 2,
                    200,
                    1.4f, 1.3f, 0, 1f, 1f
            );
        }
        return baseConfig;
    }

    public enum PV4Result { START, EXIT }
    public enum StoryStage {
        STORY_BEGIN, MAZE_GAME_TUTORIAL, PV4, MODE_MENU, MAZE_GAME, MAIN_MENU
    }

    private StoryStage stage = StoryStage.MAIN_MENU;

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
        super.setScreen(screen);
    }

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
        gameManager = new GameManager(difficultyConfig, twoPlayerMode);
        stage = StoryStage.STORY_BEGIN;
        advanceStory();
    }

    public void startStoryWithLoading() {
        setScreen(new StoryLoadingScreen(this));
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
                Animation<TextureRegion> pv4 = PVAnimationCache.get("pv/4/PV_4.atlas", "PV_4");
                setScreen(new IntroScreen(this, pv4, IntroScreen.PVExit.PV4_CHOICE, AudioType.PV_4, null));
            }
            case PV4 -> {
                stage = StoryStage.MODE_MENU;
                setScreen(new ChapterSelectScreen(this));
            }
            case MODE_MENU -> {
                stage = StoryStage.MAZE_GAME;
                setScreen(new GameScreen(this, difficultyConfig));
            }
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
        Screen old = getScreen();
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
            gameManager = new GameManager(difficultyConfig, twoPlayerMode);
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
    private void resetGameState() { stage = StoryStage.MAIN_MENU; }
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
        this.currentDifficulty = difficulty;
        this.difficultyConfig = DifficultyConfig.of(difficulty);
        this.gameManager = new GameManager(this.difficultyConfig, this.twoPlayerMode);

        if (difficulty == Difficulty.ENDLESS) {
            setScreen(new EndlessScreen(this, difficultyConfig));
            return;
        }
        setScreen(new GameScreen(this, difficultyConfig));
    }

    public void loadGame() {
        Logger.info("Loading game from save...");
        StorageManager storage = StorageManager.getInstance();
        GameSaveData saveData = storage.loadGame();

        if (saveData == null) {
            startNewGameFromMenu();
            return;
        }

        Difficulty savedDifficulty;
        try {
            savedDifficulty = Difficulty.valueOf(saveData.difficulty);
        } catch (Exception e) {
            savedDifficulty = Difficulty.NORMAL;
        }

        this.currentDifficulty = savedDifficulty;
        this.difficultyConfig = DifficultyConfig.of(savedDifficulty);
        this.setTwoPlayerMode(saveData.twoPlayerMode);
        this.gameManager = new GameManager(this.difficultyConfig, this.twoPlayerMode);
        this.gameManager.restoreFromSaveData(saveData);

        if (savedDifficulty == Difficulty.ENDLESS) {
            setScreen(new EndlessScreen(this, difficultyConfig));
        } else {
            setScreen(new GameScreen(this, difficultyConfig));
        }
    }

    public void startNewGameFromMenu() {
        Logger.info("Starting new game from menu...");
        StorageManager storage = StorageManager.getInstance();
        storage.deleteSave();

        Difficulty difficulty = this.currentDifficulty != null ? this.currentDifficulty : Difficulty.NORMAL;
        // 🔥 [修复] 强制跳过 StoryLoadingScreen，直接开始游戏
        // 原代码:
        // this.difficultyConfig = DifficultyConfig.of(difficulty);
        // this.gameManager = new GameManager(this.difficultyConfig, this.twoPlayerMode);
        // this.stage = StoryStage.STORY_BEGIN;
        // setScreen(new StoryLoadingScreen(this));

        // 新代码 (确保能玩)：
        startNewGame(difficulty);
    }

    public void setGameManager(GameManager gm) { this.gameManager = gm; }

    public void debugEnterTutorial() {
        stage = StoryStage.MAZE_GAME_TUTORIAL;
        storyPipeline = null;
        difficultyConfig = DifficultyConfig.of(Difficulty.NORMAL);
        gameManager = new GameManager(difficultyConfig, twoPlayerMode);
        AssetManager am = getAssets();
        if (!am.isLoaded("pv/4/PV_4.atlas")) {
            am.load("pv/4/PV_4.atlas", TextureAtlas.class);
            am.finishLoadingAsset("pv/4/PV_4.atlas");
        }
        setScreen(new MazeGameTutorialScreen(this, difficultyConfig));
    }

    public void restartCurrentGame() {
        if (!hasRunningGame()) return;
        Difficulty d = getCurrentDifficulty();
        resetMaze(d);
    }

    private boolean twoPlayerModeDirty = false;
    public void setTwoPlayerMode(boolean enabled) {
        if (this.twoPlayerMode != enabled) {
            this.twoPlayerMode = enabled;
            this.twoPlayerModeDirty = true;
        }
    }
    public boolean consumeTwoPlayerModeDirty() {
        boolean dirty = twoPlayerModeDirty;
        twoPlayerModeDirty = false;
        return dirty;
    }
}