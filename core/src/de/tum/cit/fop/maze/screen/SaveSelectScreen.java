package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.save.GameSaveData;
import de.tum.cit.fop.maze.game.save.StorageManager;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.utils.Logger;

public class SaveSelectScreen implements Screen {
    private final MazeRunnerGame game;
    private final Screen previousScreen;
    private final StorageManager storage;
    private Stage stage;
    private Texture backgroundTexture;

    public SaveSelectScreen(MazeRunnerGame game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
        this.storage = StorageManager.getInstance();
        this.stage = new Stage(new ScreenViewport());
        try {
            if (Gdx.files.internal("imgs/menu_bg/bg_front.png").exists())
                backgroundTexture = new Texture(Gdx.files.internal("imgs/menu_bg/bg_front.png"));
        } catch (Exception e) { Logger.warning("BG not found"); }
        setupUI();
    }

    private void setupUI() {
        stage.clear();
        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // Header
        Label title = new Label("SELECT RECORD", game.getSkin(), "title");
        title.setColor(Color.GOLD);
        root.add(title).padTop(40).padBottom(30).row();

        // List
        Table list = new Table();
        list.top();

        boolean empty = true;
        // 简单遍历显示已有存档
        for (int i = 1; i <= StorageManager.MAX_SAVE_SLOTS; i++) {
            GameSaveData data = storage.loadGameFromSlot(i);
            if (data != null) {
                empty = false;
                list.add(createCard(i, data)).width(900).padBottom(15).row();
            }
        }

        if (empty) {
            Label l = new Label("No records found.", game.getSkin());
            l.setColor(Color.GRAY);
            list.add(l).padTop(50);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFadeScrollBars(false);
        root.add(scroll).expand().fill().padBottom(20).row();

        // Footer
        ButtonFactory bf = new ButtonFactory(game.getSkin());
        root.add(bf.create("BACK", () -> game.setScreen(previousScreen))).width(300).height(60).padBottom(40);
    }

    private Table createCard(int slotId, GameSaveData data) {
        Table card = new Table();
        // Style
        com.badlogic.gdx.graphics.Pixmap p = new com.badlogic.gdx.graphics.Pixmap(1,1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        p.setColor(new Color(0.2f, 0.2f, 0.25f, 0.9f)); p.fill();
        card.setBackground(new TextureRegionDrawable(new com.badlogic.gdx.graphics.g2d.TextureRegion(new Texture(p))));
        card.pad(20);

        // Info
        Table info = new Table();
        Label name = new Label("SLOT " + slotId, game.getSkin());
        name.setColor(Color.WHITE);
        info.add(name).left().row();

        String time = storage.getSlotLastModifiedTime(slotId);
        String desc = String.format("Level %d | %s | %s", data.currentLevel, data.difficulty, time);
        Label detail = new Label(desc, game.getSkin());
        detail.setColor(Color.LIGHT_GRAY);
        info.add(detail).left().padTop(5);
        card.add(info).expandX().left();

        // Buttons
        ButtonFactory bf = new ButtonFactory(game.getSkin());
        card.add(bf.create("LOAD", () -> game.loadGameFromSlot(slotId))).width(110).padRight(10);

        TextButton del = bf.create("DEL", () -> {
            storage.deleteSaveSlot(slotId);
            setupUI();
        });
        del.setColor(Color.SALMON);
        card.add(del).width(110);

        return card;
    }

    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    @Override public void render(float delta) {
        Gdx.gl.glClearColor(0.05f,0.05f,0.08f,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().begin();
        if(backgroundTexture!=null) stage.getBatch().draw(backgroundTexture, 0,0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        stage.getBatch().end();
        stage.act(delta);
        stage.draw();
    }
    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() { stage.dispose(); if(backgroundTexture!=null) backgroundTexture.dispose(); }
}
