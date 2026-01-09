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
    // MazeRunnerGame.java
    private boolean twoPlayerMode = true;
    public boolean isTwoPlayerMode() {
        return twoPlayerMode;
    }



    private GameManager gameManager;
    private DifficultyConfig difficultyConfig;

    private PVPipeline storyPipeline;

    /* =========================
       Story / Flow
       ========================= */

    public boolean hasRunningGame() {
        return getScreen() instanceof GameScreen
                || getScreen() instanceof EndlessScreen;
    }

    public void resumeGame() {
        if (getScreen() instanceof GameScreen gs) {
            // 恢复输入
            Gdx.input.setInputProcessor(null);
        }
    }


    public GameManager getGameManager() {
        return gameManager;
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

            // 清理可能存在的旧屏幕
            if (getScreen() != null) {
                System.out.println("清理旧屏幕: " + getScreen().getClass().getSimpleName());
                getScreen().hide();
            }

            // 创建新的无尽模式屏幕
            EndlessScreen endlessScreen = new EndlessScreen(this, difficultyConfig);
            setScreen(endlessScreen);

            // 立即验证
            System.out.println("✅ 当前屏幕: " +
                    (getScreen() != null ? getScreen().getClass().getSimpleName() : "null"));
            return;
        }

        // 否则，从剧情开头开始
        this.stage = StoryStage.STORY_BEGIN;
        setScreen(new StoryLoadingScreen(this));
    }

    // 🔥 新增：创建配置的方法
    private DifficultyConfig createDifficultyConfig(Difficulty difficulty) {
        // 先获取基础配置
        DifficultyConfig baseConfig = DifficultyConfig.of(difficulty);

        // 🔥 对于无尽模式，我们需要重新创建配置对象
        if (difficulty == Difficulty.ENDLESS) {
            // 创建一个新的配置对象，继承无尽模式的设置但生命值为200
            return new DifficultyConfig(
                    difficulty.ENDLESS,40, 40, 0,           // 地图（0钥匙）
                    1, 1, 1, 1,           // 敌人
                    10, 5, 3, 2,        // 陷阱
                    200,                // 🔥 生命值改为200
                    1.4f, 1.3f, 0       // 其他参数
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
        System.out.println("🎮 MazeRunnerGame.create() 开始");
        System.out.println("   Gdx版本: " + Gdx.app.getVersion());
        System.out.println("   图形尺寸: " + Gdx.graphics.getWidth() + "x" + Gdx.graphics.getHeight());

        MazeRunnerGameHolder.init(this); // ⭐ 必须最先
        assets = new AssetManager();   // ⭐ 全局唯一
        currentDifficulty = Difficulty.NORMAL;
        difficultyConfig = DifficultyConfig.of(currentDifficulty);
        gameManager = new GameManager(difficultyConfig, twoPlayerMode);

        spriteBatch = new SpriteBatch();

        TextureAtlas uiAtlas = new TextureAtlas(Gdx.files.internal("ui/button.atlas"));
        skin = new Skin(Gdx.files.internal("ui/skinbutton.json"), uiAtlas);

        // ✨ 新增：动态创建一个纯白色像素并放入 Skin
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(com.badlogic.gdx.graphics.Color.WHITE);
        pixmap.fill();
        com.badlogic.gdx.graphics.Texture whiteTexture = new com.badlogic.gdx.graphics.Texture(pixmap);
        skin.add("white", new com.badlogic.gdx.graphics.g2d.TextureRegion(whiteTexture));
        pixmap.dispose(); // 用完 Pixmap 记得销毁
        initializeSoundManager();
        goToMenu();
    }

    // 🔥 添加：调试版本的 setScreen 方法
    @Override
    public void setScreen(Screen screen) {
        String oldScreen = getScreen() != null ? getScreen().getClass().getSimpleName() : "null";
        String newScreen = screen != null ? screen.getClass().getSimpleName() : "null";

        System.out.println("=== 屏幕切换 ===");
        System.out.println("   从: " + oldScreen);
        System.out.println("   到: " + newScreen);



        super.setScreen(screen);
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

    }

    /* =========================
       Tutorial / PV4
       ========================= */

    public void onTutorialFinished(MazeGameTutorialScreen tutorial) {
        if (stage == StoryStage.MAZE_GAME_TUTORIAL) {
            Gdx.app.postRunnable(() -> {
                advanceStory();
            });
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


        if (result == PV4Result.START) {
            saveProgress();
            stage = StoryStage.MODE_MENU;
            setScreen(new ChapterSelectScreen(this));
        } else {
            stage = StoryStage.MAIN_MENU;
            setScreen(new MenuScreen(this));
        }

    }

    /* =========================
       Navigation
       ========================= */

    public void goToMenu() {
        System.out.println("🔄 goToMenu() 被调用");

        // 如果当前在无尽模式，需要特殊处理
        if (getScreen() instanceof EndlessScreen) {
            System.out.println("   当前在无尽模式，正常返回菜单");
        }

        Screen old = getScreen();
        resetGameState();
        setScreen(new MenuScreen(this));
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
        System.out.println("⚠️ goToGame() 被调用！");
        System.out.println("   当前屏幕: " + (getScreen() != null ? getScreen().getClass().getSimpleName() : "null"));

        // 如果当前已经在无尽模式，不要切换到 GameScreen
        if (getScreen() instanceof EndlessScreen) {
            System.out.println("❌ 阻止：当前已在无尽模式，不切换到 GameScreen");
            return;
        }

        if (difficultyConfig == null) {
            difficultyConfig = DifficultyConfig.of(Difficulty.NORMAL);
            gameManager = new GameManager(difficultyConfig, twoPlayerMode);
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
    }

    public SpriteBatch getSpriteBatch() {
        return spriteBatch;
    }

    public Skin getSkin() {
        return skin;
    }

    @Override
    public void dispose() {
        System.out.println("🗑️ MazeRunnerGame.dispose()");
        if (spriteBatch != null) spriteBatch.dispose();
        if (skin != null) skin.dispose();
        if (audioManager != null) audioManager.dispose();
        TextureManager.getInstance().dispose();
    }


/* =========================
   Game Logic / Reset
   ========================= */

    /**
     * 专门用于在游戏过程中快速重置当前关卡，不跑剧情，不显示 StoryLoading
     */
    public void resetMaze(Difficulty difficulty) {
        Logger.debug("Resetting maze without story flow, difficulty: " + difficulty);

        this.currentDifficulty = difficulty; // ✅ 记录

        this.difficultyConfig = DifficultyConfig.of(difficulty);
        this.gameManager = new GameManager(
                this.difficultyConfig,
                this.twoPlayerMode
        );

        // ENDLESS 单独处理（否则你会被强行送去 GameScreen）
        if (difficulty == Difficulty.ENDLESS) {
            setScreen(new EndlessScreen(this, difficultyConfig));
            return;
        }

        setScreen(new GameScreen(this, difficultyConfig));
    }


    public void debugEnterTutorial() {
        Logger.debug("DEBUG: Enter Tutorial (standalone)");

        stage = StoryStage.MAZE_GAME_TUTORIAL;
        storyPipeline = null;

        difficultyConfig = DifficultyConfig.of(Difficulty.NORMAL);
        gameManager = new GameManager(difficultyConfig, twoPlayerMode);

        // ✅ 正确加载 PV4
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
        resetMaze(d); // ✅ 直接重开当前模式
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
