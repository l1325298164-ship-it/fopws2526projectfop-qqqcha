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
        return gameManager;
    }

    public void startNewGame(Difficulty difficulty) {
        Logger.debug("Start new game with difficulty = " + difficulty);

        // 🔥 创建配置 - 根据难度调整生命值
        this.difficultyConfig = createDifficultyConfig(difficulty);
        this.gameManager = new GameManager(this.difficultyConfig);
        this.activeGameScreen = null;

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
                    40, 40, 0,           // 地图（0钥匙）
                    1, 1, 1,            // 敌人
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

    // 🔥 添加：调试版本的 setScreen 方法
    @Override
    public void setScreen(Screen screen) {
        String oldScreen = getScreen() != null ? getScreen().getClass().getSimpleName() : "null";
        String newScreen = screen != null ? screen.getClass().getSimpleName() : "null";

        System.out.println("=== 屏幕切换 ===");
        System.out.println("   从: " + oldScreen);
        System.out.println("   到: " + newScreen);

        // 如果是切换到 GameScreen 且当前是 EndlessScreen，打印调用栈
        if (oldScreen.contains("EndlessScreen") && newScreen.contains("GameScreen")) {
            System.out.println("⚠️ 警告：EndlessScreen 被 GameScreen 替换！");
            System.out.println("   调用栈:");
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (int i = 2; i < Math.min(stackTrace.length, 8); i++) {
                System.out.println("      " + stackTrace[i].getClassName() +
                        "." + stackTrace[i].getMethodName() +
                        ":" + stackTrace[i].getLineNumber());
            }
        }

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
        System.out.println("🔄 goToMenu() 被调用");

        // 如果当前在无尽模式，需要特殊处理
        if (getScreen() instanceof EndlessScreen) {
            System.out.println("   当前在无尽模式，正常返回菜单");
        }

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
        System.out.println("⚠️ goToGame() 被调用！");
        System.out.println("   当前屏幕: " + (getScreen() != null ? getScreen().getClass().getSimpleName() : "null"));

        // 如果当前已经在无尽模式，不要切换到 GameScreen
        if (getScreen() instanceof EndlessScreen) {
            System.out.println("❌ 阻止：当前已在无尽模式，不切换到 GameScreen");
            return;
        }

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
        System.out.println("🗑️ MazeRunnerGame.dispose()");
        if (spriteBatch != null) spriteBatch.dispose();
        if (skin != null) skin.dispose();
        if (audioManager != null) audioManager.dispose();
        TextureManager.getInstance().dispose();
    }
}