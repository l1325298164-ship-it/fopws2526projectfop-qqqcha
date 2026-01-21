package de.tum.cit.fop.maze.game.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.tools.ButtonFactory;
import de.tum.cit.fop.maze.utils.Logger;

/**
 * 存档选择面板 (Save List Panel) - 全屏覆盖版
 * <p>
 * 这种设计不需要修改 MenuScreen 的跳转逻辑，直接作为一个全屏 Actor 覆盖在菜单上。
 * 包含：自动存档(AutoSave) + 手动存档(Slot 1-3)
 */
public class SaveListPanel extends Table {

    private final MazeRunnerGame game;
    private final Skin skin;
    private final StorageManager storage;

    // 背景纹理
    private Texture backgroundTexture;

    public SaveListPanel(MazeRunnerGame game, Skin skin) {
        super(skin);
        this.game = game;
        this.skin = skin;
        this.storage = StorageManager.getInstance();

        // 1. 关键：设置为全屏大小，覆盖主菜单
        this.setFillParent(true);

        // 2. 关键：拦截所有点击事件，防止穿透到后面的菜单按钮
        this.setTouchable(Touchable.enabled);
        this.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // 吞掉点击事件，不做任何事，仅为了阻挡穿透
            }
        });

        // 3. 尝试加载背景图 (如果失败则使用深色背景)
        loadBackground();

        // 4. 构建界面
        rebuild();
    }

    private void loadBackground() {
        try {
            if (Gdx.files.internal("imgs/menu_bg/bg_front.png").exists()) {
                backgroundTexture = new Texture(Gdx.files.internal("imgs/menu_bg/bg_front.png"));
            }
        } catch (Exception e) {
            Logger.warning("SaveListPanel background load failed: " + e.getMessage());
        }

        // 如果没有图片，用深色半透明背景兜底
        if (backgroundTexture == null) {
            setBackground(createColorDrawable(new Color(0.05f, 0.05f, 0.08f, 0.95f)));
        }
    }

    /**
     * 重写 draw 方法以绘制图片背景
     */
    @Override
    public void draw(Batch batch, float parentAlpha) {
        if (backgroundTexture != null) {
            batch.setColor(0.5f, 0.5f, 0.5f, parentAlpha); // 稍微压暗一点背景
            batch.draw(backgroundTexture, getX(), getY(), getWidth(), getHeight());
            batch.setColor(Color.WHITE);
        }
        super.draw(batch, parentAlpha);
    }

    private void rebuild() {
        clearChildren();

        // ================= HEADER =================
        Table headerTable = new Table();
        headerTable.pad(30);

        Label title = new Label("SELECT SAVE FILE", skin, "title");
        title.setColor(Color.GOLD);
        title.setFontScale(1.2f);
        headerTable.add(title).padBottom(10).row();

        Label hint = new Label("Choose a record to continue your journey", skin);
        hint.setColor(Color.LIGHT_GRAY);
        headerTable.add(hint).row();

        add(headerTable).top().padTop(40).row();

        // ================= CONTENT (SCROLL LIST) =================
        Table listContent = new Table();
        listContent.top().pad(20);

        // --- 1. 自动存档 (Auto Save) ---
        GameSaveData autoData = storage.loadAutoSave();
        // 总是显示 Auto 槽位，方便查看
        listContent.add(createSaveCard("AUTO SAVE", -1, autoData, true))
                .width(1000).padBottom(20).row();

        // --- 2. 普通存档 (Slots 1-3) ---
        for (int i = 1; i <= StorageManager.MAX_SAVE_SLOTS; i++) {
            GameSaveData data = storage.loadGameFromSlot(i);
            listContent.add(createSaveCard("SAVE SLOT " + i, i, data, false))
                    .width(1000).padBottom(20).row();
        }

        // 底部留白
        listContent.add(new Label("", skin)).height(80).row();

        ScrollPane scrollPane = new ScrollPane(listContent, createScrollPaneStyle());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false); // 禁止水平滚动

        add(scrollPane).expand().fill().padBottom(20).row();

        // ================= FOOTER =================
        Table footer = new Table();
        ButtonFactory bf = new ButtonFactory(skin);

        // 点击 BACK 实际上是把自己从 Stage 移除，露出下面的 Menu
        footer.add(bf.create("BACK", this::remove))
                .width(300).height(60);

        add(footer).bottom().padBottom(40);
    }

    /**
     * 创建单个存档卡片
     */
    private Table createSaveCard(String title, int slotId, GameSaveData data, boolean isAuto) {
        Table card = new Table();
        boolean exists = (data != null);

        // 背景颜色
        Color bgColor = exists ? new Color(0.15f, 0.15f, 0.18f, 0.9f) : new Color(0.1f, 0.1f, 0.1f, 0.5f);
        Color borderColor = isAuto ? new Color(1f, 0.8f, 0.2f, 1f) : (exists ? Color.GRAY : Color.DARK_GRAY);

        card.setBackground(createBorderedDrawable(bgColor, borderColor));
        card.pad(20);

        // --- 左侧信息 ---
        Table infoTable = new Table();

        Label titleLabel = new Label(title, skin);
        titleLabel.setFontScale(1.1f);
        titleLabel.setColor(isAuto ? Color.ORANGE : Color.LIGHT_GRAY);
        infoTable.add(titleLabel).left().padBottom(10).row();

        if (exists) {
            String scoreStr = String.format("%,d", data.score);
            String info = String.format("Floor: %d   |   %s   |   Score: %s",
                    data.currentLevel, data.difficulty, scoreStr);

            Label details = new Label(info, skin);
            details.setColor(Color.WHITE);
            details.setWrap(true);
            infoTable.add(details).left().width(600);
        } else {
            Label empty = new Label("- Empty Slot -", skin);
            empty.setColor(new Color(1, 1, 1, 0.3f));
            infoTable.add(empty).left();
        }

        card.add(infoTable).expandX().fillX().left();

        // --- 右侧按钮 ---
        Table btnTable = new Table();
        ButtonFactory bf = new ButtonFactory(skin);

        if (exists) {
            // LOAD
            btnTable.add(bf.create("LOAD", () -> showLoadDialog(slotId, title)))
                    .width(130).height(50).padRight(15);

            // DELETE (AutoSave 不显示删除)
            if (!isAuto) {
                TextButton delBtn = bf.create("DEL", () -> showDeleteDialog(slotId));
                delBtn.setColor(1f, 0.4f, 0.4f, 1f);
                btnTable.add(delBtn).width(130).height(50);
            } else {
                Label tag = new Label("AUTO", skin);
                tag.setColor(Color.YELLOW);
                tag.setAlignment(Align.center);
                btnTable.add(tag).width(130);
            }
        } else {
            Label unused = new Label("UNUSED", skin);
            unused.setColor(new Color(1,1,1,0.1f));
            unused.setAlignment(Align.center);
            btnTable.add(unused).width(275);
        }

        card.add(btnTable).right();

        return card;
    }

    // ================= DIALOGS =================

    private void showLoadDialog(int slotId, String title) {
        // 使用内部类规避 protected 访问限制
        class LoadDialog extends Dialog {
            public LoadDialog() { super("", skin); }
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    if (slotId == -1) {
                        // 加载 AutoSave
                        GameSaveData auto = storage.loadAutoSave();
                        if (auto != null) {
                            storage.saveAuto(auto); // 确保它是最新的
                            game.loadGame();
                        }
                    } else {
                        game.loadGameFromSlot(slotId);
                    }
                    // 加载后游戏会切换 Screen，所以这里不需要手动 remove
                } else {
                    hide();
                }
            }
        }

        LoadDialog d = new LoadDialog();
        styleDialog(d);

        Label text = new Label("Load " + title + "?\nCurrent progress will be lost.", skin);
        text.setAlignment(Align.center);
        d.text(text);

        ButtonFactory bf = new ButtonFactory(skin);
        d.getButtonTable().add(bf.create("YES", () -> d.result(true))).width(120).padRight(20);
        d.getButtonTable().add(bf.create("NO", () -> d.result(false))).width(120);

        d.show(getStage());
    }

    private void showDeleteDialog(int slotId) {
        class DeleteDialog extends Dialog {
            public DeleteDialog() { super("", skin); }
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    storage.deleteSaveSlot(slotId);
                    rebuild(); // 刷新列表
                } else {
                    hide();
                }
            }
        }

        DeleteDialog d = new DeleteDialog();
        styleDialog(d);

        Label text = new Label("Delete Slot " + slotId + "?\nThis cannot be undone!", skin);
        text.setColor(Color.SALMON);
        text.setAlignment(Align.center);
        d.text(text);

        ButtonFactory bf = new ButtonFactory(skin);
        d.getButtonTable().add(bf.create("DELETE", () -> d.result(true))).width(120).padRight(20);
        d.getButtonTable().add(bf.create("CANCEL", () -> d.result(false))).width(120);

        d.show(getStage());
    }

    private void styleDialog(Dialog d) {
        d.setBackground(createBorderedDrawable(new Color(0.1f,0.1f,0.1f,0.95f), Color.WHITE));
        d.getContentTable().pad(30);
        d.getButtonTable().pad(20);
    }

    // ================= HELPERS =================

    private ScrollPane.ScrollPaneStyle createScrollPaneStyle() {
        ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle();
        style.vScrollKnob = createColorDrawable(new Color(1f, 1f, 1f, 0.2f));
        return style;
    }

    private TextureRegionDrawable createColorDrawable(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture t = new Texture(pixmap);
        // 注意：pixmap disposed后 texture 仍可用，但在 UI 销毁时最好管理 texture
        // 这里为了简便让 GC 处理 texture，生产环境可优化
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(t));
    }

    private TextureRegionDrawable createBorderedDrawable(Color bg, Color border) {
        int w = 64, h = 64, b = 2;
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(border);
        p.fill();
        p.setColor(bg);
        p.fillRectangle(b, b, w - 2*b, h - 2*b);
        Texture t = new Texture(p);
        p.dispose();
        return new TextureRegionDrawable(new TextureRegion(t));
    }
}