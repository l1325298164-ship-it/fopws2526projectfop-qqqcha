package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.tools.ButtonFactory;

public class SettingsScreen implements Screen {

    private final MazeRunnerGame game;
    private final SettingsSource source;
    private final Screen previousScreen;

    private Stage stage;

    public enum SettingsSource {
        MAIN_MENU,
        PAUSE_MENU
    }

    private static final int[][] COMMON_RESOLUTIONS = {
            {1280, 720},
            {1366, 768},
            {1600, 900},
            {1920, 1080},
            {2560, 1440}
    };

    // ===== Confirm UI =====
    private Table confirmDialog;
    private Table confirmBlocker;
    private Label confirmText;

    private SelectBox<String> resolutionSelect;

    private int originalWidth;
    private int originalHeight;
    private WindowMode originalMode;

    private int selectedWidth;
    private int selectedHeight;
    private WindowMode selectedMode;

    private boolean waitingForConfirm = false;
    private float confirmTimer = 0f;

    private enum WindowMode {
        WINDOWED,
        BORDERLESS,
        FULLSCREEN
    }

    public SettingsScreen(MazeRunnerGame game, SettingsSource source, Screen previousScreen) {
        this.game = game;
        this.source = source;
        this.previousScreen = previousScreen;
    }

    // ======================================================
    // SHOW
    // ======================================================
    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // ===== 外层 Root（不滚动）=====
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // ===== 可滚动内容 Table =====
        Table content = new Table();
        content.top();
        content.pad(40);
        content.padBottom(80);

        ScrollPane scrollPane = new ScrollPane(content, createScrollPaneStyle());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.setOverscroll(false, false);
        scrollPane.setSmoothScrolling(true);

        root.add(scrollPane).expand().fill();

        // ==================================================
        // SETTINGS CONTENT
        // ==================================================

        content.add(new Label("SETTINGS", game.getSkin(), "title"))
                .padBottom(40)
                .row();

        ButtonFactory bf = new ButtonFactory(game.getSkin());
        AudioManager audio = AudioManager.getInstance();

        // ===== MASTER =====
        content.add(new Label("MASTER VOLUME", game.getSkin())).padBottom(8).row();
        Slider masterSlider = createVolumeSlider();
        masterSlider.setValue(audio.getMasterVolume());
        masterSlider.addListener(e -> {
            audio.setMasterVolume(masterSlider.getValue());
            return false;
        });
        content.add(masterSlider).width(400).padBottom(20).row();

        // ===== MUSIC =====
        content.add(new Label("MUSIC VOLUME", game.getSkin())).padBottom(8).row();
        Slider musicSlider = createVolumeSlider();
        musicSlider.setValue(audio.getMusicVolume());
        musicSlider.addListener(e -> {
            audio.setMusicVolume(musicSlider.getValue());
            return false;
        });
        content.add(musicSlider).width(400).padBottom(20).row();

        // ===== SFX =====
        content.add(new Label("SFX VOLUME", game.getSkin())).padBottom(8).row();
        Slider sfxSlider = createVolumeSlider();
        sfxSlider.setValue(audio.getSfxVolume());
        sfxSlider.addListener(e -> {
            audio.setSfxVolume(sfxSlider.getValue());
            return false;
        });
        content.add(sfxSlider).width(400).padBottom(30).row();

        // ===== TOGGLES =====
        TextButton musicToggle = bf.create(
                "Music: " + (audio.isMusicEnabled() ? "ON" : "OFF"), () -> {}
        );
        musicToggle.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent e, float x, float y, int p, int b) {
                boolean v = !audio.isMusicEnabled();
                audio.setMusicEnabled(v);
                musicToggle.setText("Music: " + (v ? "ON" : "OFF"));
                return true;
            }
        });

        TextButton sfxToggle = bf.create(
                "SFX: " + (audio.isSfxEnabled() ? "ON" : "OFF"), () -> {}
        );
        sfxToggle.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent e, float x, float y, int p, int b) {
                boolean v = !audio.isSfxEnabled();
                audio.setSfxEnabled(v);
                sfxToggle.setText("SFX: " + (v ? "ON" : "OFF"));
                return true;
            }
        });

        content.add(musicToggle).width(400).height(60).padBottom(15).row();
        content.add(sfxToggle).width(400).height(60).padBottom(40).row();

        // ===== DISPLAY =====
        content.add(new Label("DISPLAY SETTINGS", game.getSkin()))
                .padBottom(20)
                .row();

        resolutionSelect = new SelectBox<>(createSelectBoxStyle());
        resolutionSelect.setItems(buildResolutionItems());

        originalWidth = Gdx.graphics.getWidth();
        originalHeight = Gdx.graphics.getHeight();
        originalMode = Gdx.graphics.isFullscreen() ? WindowMode.FULLSCREEN : WindowMode.WINDOWED;

        selectedWidth = originalWidth;
        selectedHeight = originalHeight;
        selectedMode = originalMode;

        resolutionSelect.setSelected(originalWidth + " x " + originalHeight);
        resolutionSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!waitingForConfirm) {
                    originalWidth = Gdx.graphics.getWidth();
                    originalHeight = Gdx.graphics.getHeight();
                    originalMode = Gdx.graphics.isFullscreen()
                            ? WindowMode.FULLSCREEN
                            : WindowMode.WINDOWED;
                }

                String[] p = resolutionSelect.getSelected().split(" x ");
                selectedWidth = Integer.parseInt(p[0]);
                selectedHeight = Integer.parseInt(p[1]);
                selectedMode = WindowMode.WINDOWED;

                previewDisplaySettings();
                openOrRefreshConfirmDialog();
            }
        });

        content.add(new Label("Resolution", game.getSkin())).padBottom(8).row();
        content.add(resolutionSelect).width(500).height(60).padBottom(30).row();

        // ===== TWO PLAYER =====
        TextButton twoPlayerBtn =
                bf.create(game.isTwoPlayerMode() ? "2 PLAYERS" : "1 PLAYER", () -> {});
        twoPlayerBtn.addListener(new InputListener() {
            @Override public boolean touchDown(InputEvent e, float x, float y, int p, int b) {
                boolean v = !game.isTwoPlayerMode();
                game.setTwoPlayerMode(v);
                twoPlayerBtn.setText(v ? "2 PLAYERS" : "1 PLAYER");
                return true;
            }
        });

        content.add(twoPlayerBtn).width(400).height(70).padBottom(40).row();
// ===== CONTROLS =====
        content.add(new Label("CONTROLS", game.getSkin()))
                .padBottom(20)
                .row();

        TextButton controlsBtn = bf.create("KEY BINDINGS", () -> {
            game.setScreen(new KeyMappingScreen(game, this));
        });

        content.add(controlsBtn)
                .width(400)
                .height(70)
                .padBottom(40)
                .row();

        // ===== BACK =====
        content.add(bf.create("BACK", this::goBack))
                .width(400)
                .height(80);
    }

    // ======================================================
    // RENDER / RESIZE
    // ======================================================
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (waitingForConfirm) {
            confirmTimer -= delta;
            updateConfirmText();
            if (confirmTimer <= 0) {
                revertDisplaySettings();
                closeConfirmDialog();
            }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); }

    // ======================================================
    // HELPERS（原逻辑不变）
    // ======================================================

    private Slider createVolumeSlider() {
        var skin = game.getSkin();
        Pixmap bg = new Pixmap(200, 6, Pixmap.Format.RGBA8888);
        bg.setColor(0.3f, 0.3f, 0.3f, 1);
        bg.fill();

        Pixmap knob = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
        knob.setColor(Color.WHITE);
        knob.fillCircle(8, 8, 8);

        skin.add("slider-bg", new Texture(bg));
        skin.add("slider-knob", new Texture(knob));

        bg.dispose();
        knob.dispose();

        Slider.SliderStyle s = new Slider.SliderStyle();
        s.background = skin.newDrawable("slider-bg");
        s.knob = skin.newDrawable("slider-knob");
        return new Slider(0, 1, 0.01f, false, s);
    }

    private String[] buildResolutionItems() {
        String[] items = new String[COMMON_RESOLUTIONS.length];
        for (int i = 0; i < items.length; i++) {
            items[i] = COMMON_RESOLUTIONS[i][0] + " x " + COMMON_RESOLUTIONS[i][1];
        }
        return items;
    }

    private SelectBox.SelectBoxStyle createSelectBoxStyle() {
        Skin skin = game.getSkin();
        BitmapFont font = skin.getFont("default-font");

        Label.LabelStyle label = new Label.LabelStyle(font, Color.WHITE);
        List.ListStyle list = new List.ListStyle(font, Color.LIGHT_GRAY, Color.WHITE,
                skin.newDrawable("white", Color.DARK_GRAY));
        ScrollPane.ScrollPaneStyle scroll = new ScrollPane.ScrollPaneStyle();

        SelectBox.SelectBoxStyle s = new SelectBox.SelectBoxStyle();
        s.font = font;
        s.fontColor = Color.WHITE;
        s.background = skin.newDrawable("white", Color.DARK_GRAY);
        s.listStyle = list;
        s.scrollStyle = scroll;
        return s;
    }

    private void previewDisplaySettings() {
        Gdx.graphics.setWindowedMode(selectedWidth, selectedHeight);
    }

    private void revertDisplaySettings() {
        Gdx.graphics.setWindowedMode(originalWidth, originalHeight);
    }

    private void openOrRefreshConfirmDialog() {
        waitingForConfirm = true;
        confirmTimer = 10f;

        if (confirmDialog != null) {
            updateConfirmText();
            return;
        }

        confirmBlocker = new Table();
        confirmBlocker.setFillParent(true);
        confirmBlocker.setTouchable(Touchable.enabled);

        confirmBlocker.setBackground(createDimBackground(0.65f));
        stage.addActor(confirmBlocker);

        confirmDialog = new Table(game.getSkin());
        confirmDialog.setFillParent(true);
        stage.addActor(confirmDialog);

        confirmText = new Label("", game.getSkin());
        updateConfirmText();

        confirmDialog.add(confirmText).padBottom(30).row();
        ButtonFactory bf = new ButtonFactory(game.getSkin());

        confirmDialog.add(bf.create("CONFIRM", this::closeConfirmDialog))
                .width(300).height(70).padBottom(15).row();

        confirmDialog.add(bf.create("CANCEL", () -> {
            revertDisplaySettings();
            closeConfirmDialog();
        })).width(300).height(70);
    }

    private Drawable createDimBackground(float alpha) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, alpha); // 黑色半透明
        pm.fill();

        Texture tex = new Texture(pm);
        pm.dispose();

        return new Image(tex).getDrawable();
    }


    private void closeConfirmDialog() {
        waitingForConfirm = false;
        if (confirmDialog != null) confirmDialog.remove();
        if (confirmBlocker != null) confirmBlocker.remove();
        confirmDialog = null;
        confirmBlocker = null;
        confirmText = null;
    }

    private void updateConfirmText() {
        if (confirmText != null) {
            confirmText.setText("Keep these settings?\nReverting in " +
                    (int)Math.ceil(confirmTimer) + " seconds...");
        }
    }
    private ScrollPane.ScrollPaneStyle createScrollPaneStyle() {
        Skin skin = game.getSkin();

        ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle();
        style.background = skin.newDrawable("white", new Color(0f, 0f, 0f, 0f)); // 透明背景

        return style;
    }

    private void goBack() {
        if (source == SettingsSource.MAIN_MENU) {
            game.setScreen(new MenuScreen(game));
        } else {
            game.setScreen(previousScreen);
        }
    }
}
