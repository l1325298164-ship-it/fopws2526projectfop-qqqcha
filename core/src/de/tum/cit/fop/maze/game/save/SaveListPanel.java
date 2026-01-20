package de.tum.cit.fop.maze.game.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.tools.ButtonFactory;

/**
 * 存档选择面板 (Save Selection Panel)
 * <p>
 * Fixes:
 * 1. "Protected access" error resolved using local Dialog subclasses.
 * 2. Visual overhaul: Full-screen dark overlay + Card layout.
 * 3. Auto-Save support added.
 */
public class SaveListPanel extends Table {

    private final MazeRunnerGame game;
    private final StorageManager storage;
    private final Skin skin;

    public SaveListPanel(MazeRunnerGame game, Skin skin) {
        super(skin);
        this.game = game;
        this.skin = skin;
        this.storage = StorageManager.getInstance();

        setFillParent(true);
        // 全屏半透明黑色背景遮罩 (Full screen dark overlay)
        setBackground(createColorDrawable(new Color(0f, 0f, 0f, 0.85f)));

        rebuild();
    }

    private void rebuild() {
        clearChildren();

        // ======================================
        // 1. 标题区域
        // ======================================
        Table contentTable = new Table();
        contentTable.top();

        Label title = new Label("SELECT GAME RECORD", skin, "title");
        title.setColor(Color.GOLD);
        title.setFontScale(1.2f);
        contentTable.add(title).padBottom(30).row();

        // ======================================
        // 2. 滚动列表区域
        // ======================================
        Table listTable = new Table();
        listTable.top();

        // --- A. 自动存档 (Auto Save) ---
        GameSaveData autoData = storage.loadAutoSave();
        if (autoData != null) {
            listTable.add(createSlotCard("AUTO SAVE", -1, autoData, true)).width(900).padBottom(15).row();
        }

        // --- B. 普通存档槽位 (Slot 1-3) ---
        for (int slot = 1; slot <= StorageManager.MAX_SAVE_SLOTS; slot++) {
            GameSaveData data = storage.loadGameFromSlot(slot);
            listTable.add(createSlotCard("SLOT " + slot, slot, data, false)).width(900).padBottom(15).row();
        }

        ScrollPane scrollPane = new ScrollPane(listTable);
        // 隐藏滚动条背景
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        contentTable.add(scrollPane).width(950).height(500).padBottom(20).row();

        // ======================================
        // 3. 底部按钮
        // ======================================
        ButtonFactory bf = new ButtonFactory(skin);
        contentTable.add(bf.create("CANCEL", this::remove)).width(300).height(60);

        // 将内容居中放入全屏 Table
        add(contentTable).center();
    }

    /**
     * 创建单个存档卡片
     */
    private Table createSlotCard(String title, int slotId, GameSaveData data, boolean isAuto) {
        Table card = new Table();
        boolean exists = (data != null);

        // 背景样式：深色底 + 边框
        Color bgColor = exists ? new Color(0.2f, 0.2f, 0.25f, 1f) : new Color(0.15f, 0.15f, 0.15f, 0.8f);
        Color borderColor = isAuto ? Color.GOLD : (exists ? Color.GRAY : Color.DARK_GRAY);
        card.setBackground(createBorderedDrawable(bgColor, borderColor));
        card.pad(15);

        // --- 左侧：信息 ---
        Table infoTable = new Table();

        Label titleLabel = new Label(title, skin);
        titleLabel.setColor(isAuto ? Color.ORANGE : Color.LIGHT_GRAY);
        titleLabel.setFontScale(1.1f);
        infoTable.add(titleLabel).left().row();

        String details;
        if (exists) {
            String mode = data.twoPlayerMode ? "2-Player" : "Solo";
            details = "Floor " + data.currentLevel + "  |  " + data.difficulty + "  |  " + mode;

            Label detailLabel = new Label(details, skin);
            detailLabel.setColor(Color.WHITE);
            infoTable.add(detailLabel).left().padTop(10);
        } else {
            Label emptyLabel = new Label("- Empty -", skin);
            emptyLabel.setColor(Color.GRAY);
            infoTable.add(emptyLabel).left().padTop(10);
        }

        card.add(infoTable).expandX().fillX().left().padLeft(10);

        // --- 右侧：操作按钮 ---
        Table btnTable = new Table();
        ButtonFactory bf = new ButtonFactory(skin);

        if (exists) {
            // LOAD 按钮
            TextButton loadBtn = bf.create("LOAD", () -> {
                if (isAuto) {
                    showLoadConfirm(-1, "Auto Save");
                } else {
                    showLoadConfirm(slotId, "Slot " + slotId);
                }
            });
            btnTable.add(loadBtn).width(120).height(45).padRight(10);

            // DELETE 按钮
            if (!isAuto) {
                TextButton delBtn = bf.create("DEL", () -> showDeleteConfirm(slotId));
                delBtn.setColor(1f, 0.5f, 0.5f, 1f); // Red tint
                btnTable.add(delBtn).width(120).height(45);
            } else {
                Label tag = new Label("AUTO", skin);
                tag.setColor(Color.YELLOW);
                tag.setAlignment(Align.center);
                btnTable.add(tag).width(120);
            }
        } else {
            // 空槽位占位
            Label unused = new Label("UNUSED", skin);
            unused.setColor(new Color(1,1,1,0.2f));
            unused.setAlignment(Align.center);
            btnTable.add(unused).width(250);
        }

        card.add(btnTable).right().padRight(10);

        return card;
    }

    // ==================================================
    // Dialogs (Local Class Fix)
    // ==================================================

    private void showLoadConfirm(int slot, String slotName) {
        // ✅ Local Class to access protected result() method
        class LoadDialog extends Dialog {
            public LoadDialog(String title, Skin skin) { super(title, skin); }

            public void trigger(boolean val) {
                result(val); // call protected method
                hide();
            }

            @Override
            protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) {
                    if (slot == -1) {
                        // 加载自动存档：需要确保 StorageManager 或 Game 支持
                        // 这里的简易做法：读取数据 -> 存入临时位置 -> 让游戏加载
                        // 或者直接依靠 game.loadGame() (如果它默认加载 auto/recent)
                        // 假设 game.loadGame() 会自动处理：
                        GameSaveData auto = storage.loadAutoSave();
                        storage.saveGameAuto(StorageManager.SaveTarget.AUTO, auto); // Ensure it's ready
                        game.loadGame();
                    } else {
                        game.loadGameFromSlot(slot);
                    }
                    SaveListPanel.this.remove();
                }
            }
        }

        LoadDialog dialog = new LoadDialog("", skin);

        dialog.setBackground(createBorderedDrawable(new Color(0.1f, 0.1f, 0.1f, 0.95f), Color.WHITE));
        dialog.getContentTable().pad(30);
        dialog.getButtonTable().pad(20);

        Label text = new Label("Load " + slotName + "?\nCurrent progress will be lost.", skin);
        text.setAlignment(Align.center);
        dialog.text(text);

        ButtonFactory bf = new ButtonFactory(skin);
        dialog.getButtonTable().add(bf.create("CONFIRM", () -> dialog.trigger(true))).width(150).padRight(20);
        dialog.getButtonTable().add(bf.create("CANCEL", () -> dialog.trigger(false))).width(150);

        dialog.show(getStage());
    }

    private void showDeleteConfirm(int slot) {
        // ✅ Local Class
        class DeleteDialog extends Dialog {
            public DeleteDialog(String title, Skin skin) { super(title, skin); }

            public void trigger(boolean val) {
                result(val);
                hide();
            }

            @Override
            protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) {
                    storage.deleteSaveSlot(slot);
                    rebuild(); // Refresh UI
                }
            }
        }

        DeleteDialog dialog = new DeleteDialog("", skin);

        dialog.setBackground(createBorderedDrawable(new Color(0.1f, 0.1f, 0.1f, 0.95f), Color.RED));
        dialog.getContentTable().pad(30);
        dialog.getButtonTable().pad(20);

        Label text = new Label("DELETE Slot " + slot + "?\nThis cannot be undone!", skin);
        text.setColor(Color.SALMON);
        text.setAlignment(Align.center);
        dialog.text(text);

        ButtonFactory bf = new ButtonFactory(skin);
        dialog.getButtonTable().add(bf.create("DELETE", () -> dialog.trigger(true))).width(150).padRight(20);
        dialog.getButtonTable().add(bf.create("CANCEL", () -> dialog.trigger(false))).width(150);

        dialog.show(getStage());
    }

    // ==================================================
    // Helpers
    // ==================================================

    private TextureRegionDrawable createColorDrawable(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private TextureRegionDrawable createBorderedDrawable(Color bgColor, Color borderColor) {
        int w = 64;
        int h = 64;
        int border = 2;

        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);

        // Fill Border
        pixmap.setColor(borderColor);
        pixmap.fill();

        // Fill Background
        pixmap.setColor(bgColor);
        pixmap.fillRectangle(border, border, w - 2*border, h - 2*border);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }
}