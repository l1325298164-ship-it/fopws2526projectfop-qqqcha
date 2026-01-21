package de.tum.cit.fop.maze.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.game.save.GameSaveData;
import de.tum.cit.fop.maze.game.save.StorageManager;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.utils.Logger;

/**
 * 存档选择界面 - 最终修正版
 * 特点：
 * 1. 宽度动态计算 (80% 屏幕宽度)，保证舒展。
 * 2. 视觉轻量化 (半透明 + 细边框)。
 * 3. 按钮竖排且尺寸加大，防止文字溢出。
 */
public class SaveSelectScreen implements Screen {

    private final MazeRunnerGame game;
    private final Screen previousScreen;
    private final StorageManager storage;
    private Stage stage;
    private Texture backgroundTexture;

    // 缓存纹理样式
    private NinePatchDrawable cardBackground;

    public SaveSelectScreen(MazeRunnerGame game, Screen previousScreen) {
        this.game = game;
        this.previousScreen = previousScreen;
        this.storage = StorageManager.getInstance();
        this.stage = new Stage(new ScreenViewport());

        try {
            if (Gdx.files.internal("imgs/menu_bg/bg_front.png").exists()) {
                backgroundTexture = new Texture(Gdx.files.internal("imgs/menu_bg/bg_front.png"));
            }
        } catch (Exception e) {
            Logger.warning("Background not found: " + e.getMessage());
        }

        // 创建高透背景 + 细边框样式
        this.cardBackground = createBorderedBackground(
                new Color(0.05f, 0.05f, 0.1f, 0.4f), // 背景：深蓝黑，透明度 40%
                new Color(1f, 1f, 1f, 0.25f)         // 边框：灰白，透明度 25%
        );

        setupUI();
    }

    private void setupUI() {
        stage.clear();
        Gdx.input.setInputProcessor(stage);

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        // ================= HEADER =================
        Table headerTable = new Table();
        Label title = new Label("SELECT RECORD", game.getSkin(), "title");
        title.setColor(Color.GOLD);
        title.setFontScale(1.3f); // 标题加大
        headerTable.add(title).padBottom(15).row();

        Label hint = new Label("Choose a record to resume your journey", game.getSkin());
        hint.setColor(Color.LIGHT_GRAY);
        headerTable.add(hint).row();

        root.add(headerTable).padTop(60).padBottom(30).row();

        // ================= LIST CONTENT =================
        Table listContent = new Table();
        listContent.top().pad(20);

        // 🔥 关键修正：宽度动态计算，占屏幕 80%，保证宽敞
        float cardWidth = Gdx.graphics.getWidth() * 0.8f;

        boolean hasRecords = false;

        for (int i = 1; i <= StorageManager.MAX_SAVE_SLOTS; i++) {
            GameSaveData data = storage.loadGameFromSlot(i);
            if (data != null) {
                Table card = createSaveCard(i, data);
                listContent.add(card).width(cardWidth).padBottom(25).row(); // 间距 25
                hasRecords = true;
            }
        }

        if (!hasRecords) {
            Label empty = new Label("No records found.", game.getSkin());
            empty.setColor(Color.GRAY);
            empty.setFontScale(1.2f);
            listContent.add(empty).padTop(100);
        }

        ScrollPane scrollPane = new ScrollPane(listContent, createInvisibleScrollPaneStyle());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        root.add(scrollPane).expand().fill().padBottom(20).row();

        // ================= FOOTER =================
        Table footer = new Table();
        ButtonFactory bf = new ButtonFactory(game.getSkin());
        // 底部返回按钮
        footer.add(bf.create("BACK", () -> game.setScreen(previousScreen)))
                .width(300).height(70);

        root.add(footer).padBottom(50);
    }

    /**
     * 创建宽版卡片
     */
    private Table createSaveCard(int slotId, GameSaveData data) {
        Table card = new Table();
        card.setBackground(cardBackground);

        // 内部 Padding 加大，让内容不拥挤
        card.pad(30);

        // --- 左侧：信息区 (自适应宽度) ---
        Table infoTable = new Table();

        // Slot ID
        Label nameLabel = new Label("SLOT " + slotId, game.getSkin());
        nameLabel.setColor(Color.GOLD);
        nameLabel.setFontScale(1.4f); // 再次加大
        infoTable.add(nameLabel).left().padBottom(15).row();

        // 核心信息
        String mode = data.twoPlayerMode ? "2-Player" : "Solo";
        String infoText = String.format("Level %d   •   %s   •   %s",
                data.currentLevel, data.difficulty, mode);

        Label detailLabel = new Label(infoText, game.getSkin());
        detailLabel.setColor(Color.WHITE);
        detailLabel.setFontScale(1.1f);
        infoTable.add(detailLabel).left().padBottom(10).row();

        // 时间
        String timeStr = storage.getSlotLastModifiedTime(slotId);
        Label timeLabel = new Label("Saved: " + timeStr, game.getSkin());
        timeLabel.setColor(new Color(0.7f, 0.7f, 0.7f, 1f));
        timeLabel.setFontScale(0.9f);
        infoTable.add(timeLabel).left();

        card.add(infoTable).expandX().left().padLeft(10);

        // --- 右侧：按钮区 (竖排，固定宽度) ---
        Table btnTable = new Table();
        ButtonFactory bf = new ButtonFactory(game.getSkin());

        // LOAD 按钮 (加大尺寸 180x60，防止 overflow)
        TextButton loadBtn = bf.create("LOAD", () -> {
            game.getGameManager().setCurrentSaveTarget(StorageManager.SaveTarget.fromSlot(slotId));
            game.loadGameFromSlot(slotId);
        });
        btnTable.add(loadBtn).width(180).height(60).padBottom(15).row();

        // DELETE 按钮
        TextButton delBtn = bf.create("DEL", () -> showDeleteConfirm(slotId));
        delBtn.setColor(new Color(0.8f, 0.3f, 0.3f, 1f));
        btnTable.add(delBtn).width(180).height(60);

        // 右侧留一点 padding
        card.add(btnTable).right().padRight(10);

        return card;
    }

    /**
     * 创建带 1px 边框的 NinePatch 背景
     */
    private NinePatchDrawable createBorderedBackground(Color fillColor, Color borderColor) {
        int size = 9;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);

        p.setColor(fillColor);
        p.fill();

        p.setColor(borderColor);
        p.drawRectangle(0, 0, size, size);

        Texture t = new Texture(p);
        p.dispose();

        // 9-patch 切割：上下左右保留 1px
        return new NinePatchDrawable(new NinePatch(t, 1, 1, 1, 1));
    }

    private void showDeleteConfirm(int slotId) {
        Dialog d = new Dialog("", game.getSkin()) {
            @Override protected void result(Object object) {
                if ((Boolean)object) {
                    storage.deleteSaveSlot(slotId);
                    setupUI();
                }
            }
        };
        // 弹窗背景稍微加深一点
        d.setBackground(createBorderedBackground(
                new Color(0.1f, 0.1f, 0.15f, 0.9f),
                Color.GRAY)
        );

        Label l = new Label("\nDelete this record?\n", game.getSkin());
        l.setAlignment(Align.center);
        d.getContentTable().add(l).pad(40);

        d.button("DELETE", true).button("CANCEL", false);
        d.getButtonTable().getCells().forEach(c -> c.width(140).height(55).pad(15));
        d.show(stage);
    }

    private ScrollPane.ScrollPaneStyle createInvisibleScrollPaneStyle() {
        return new ScrollPane.ScrollPaneStyle();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.getBatch().begin();
        if (backgroundTexture != null) {
            // 背景压暗系数 0.5f，突出前景卡片
            stage.getBatch().setColor(0.5f, 0.5f, 0.5f, 1f);
            stage.getBatch().draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            stage.getBatch().setColor(Color.WHITE);
        }
        stage.getBatch().end();

        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { stage.getViewport().update(w, h, true); }
    @Override public void show() { Gdx.input.setInputProcessor(stage); }
    @Override public void hide() {}
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void dispose() {
        stage.dispose();
        if(backgroundTexture != null) backgroundTexture.dispose();
    }
}