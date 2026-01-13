package de.tum.cit.fop.maze.game.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import de.tum.cit.fop.maze.MazeRunnerGame;

public class SaveListPanel extends Window {

    private static final float PANEL_WIDTH = 1900f;

    private final MazeRunnerGame game;
    private final StorageManager storage;

    public SaveListPanel(MazeRunnerGame game, Skin skin) {
        super(" SELECT SAVE ", skin);
        this.game = game;
        this.storage = StorageManager.getInstance();

        setModal(true);
        setMovable(false);
        setResizable(false);
        pad(105);

        rebuild();

        setSize(PANEL_WIDTH, getPrefHeight());

        setPosition(
                (Gdx.graphics.getWidth() - getWidth()) / 2f,
                (Gdx.graphics.getHeight() - getHeight()) / 2f
        );
    }

    // ==================================================
    // UI rebuild
    // ==================================================

    private void rebuild() {
        clearChildren();

        for (int slot = 1; slot <= StorageManager.MAX_SAVE_SLOTS; slot++) {
            GameSaveData data = storage.loadGameFromSlot(slot);
            add(createSlotRow(slot, data))
                    .expandX()
                    .fillX()
                    .padBottom(20)
                    .row();
        }

        TextButton cancel = new TextButton("CANCEL", getSkin());
        cancel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                remove();
            }
        });

        add(cancel).width(360).height(65).padTop(10);
    }

    // ==================================================
    // Slot row
    // ==================================================

    private Table createSlotRow(int slot, GameSaveData data) {
        boolean exists = data != null;

        Table row = new Table(getSkin());
        row.setBackground(getSkin().newDrawable("white", 0f, 0f, 0f, 0.35f));
        row.pad(20);

        // ---------- 左侧信息 ----------
        Table info = new Table(getSkin());

        Label title = new Label("SAVE SLOT " + slot, getSkin(), "title");

        String detailText;
        if (exists) {
            String difficulty = data.difficulty;
            String mode = data.twoPlayerMode ? "2P" : "1P";
            int level = data.currentLevel;

            detailText =
                    "  |  " + difficulty +
                            "   |   Mode: " + mode +
                            "   |   Floor: " + level;
        } else {
            detailText = "Empty Slot";
        }

        Label detail = new Label(detailText, getSkin());
        detail.setWrap(true);

        info.add(title).left().row();
        info.add(detail)
                .expandX()
                .fillX()
                .left()
                .padTop(8);

        // ---------- 右侧按钮 ----------
        TextButton loadBtn = new TextButton("LOAD", getSkin());
        TextButton deleteBtn = new TextButton("DELETE", getSkin());

        loadBtn.setDisabled(!exists);
        deleteBtn.setDisabled(!exists);

        if (exists) {
            loadBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    showLoadConfirm(slot);
                }
            });

            deleteBtn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    showDeleteConfirm(slot);
                }
            });
        }

        Table buttons = new Table(getSkin());
        buttons.add(loadBtn).width(300).height(60).row();
        buttons.add(deleteBtn).width(300).height(60).padTop(12);

        // ---------- 布局 ----------
        row.add(info).expandX().fillX().left();
        row.add(buttons).right().padLeft(30);

        row.setWidth(PANEL_WIDTH - 200);
        row.setHeight(110);

        return row;
    }


    // ==================================================
    // Dialogs
    // ==================================================

    private void showLoadConfirm(int slot) {
        Dialog dialog = new Dialog(" LOAD GAME ", getSkin()) {
            @Override
            protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) {
                    game.loadGameFromSlot(slot);
                    remove();
                }
            }
        };

        dialog.text(
                "\n  Load save slot " + slot + "?\n" +
                        "  Unsaved progress will be lost.\n"
        );

        dialog.button(" LOAD ", true);
        dialog.button(" CANCEL ", false);
        dialog.show(getStage());
    }

    private void showDeleteConfirm(int slot) {
        Dialog dialog = new Dialog(" DELETE SAVE ", getSkin()) {
            @Override
            protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) {
                    storage.deleteSaveSlot(slot);
                    rebuild();
                    setSize(PANEL_WIDTH, getPrefHeight());
                }
            }
        };

        dialog.text(
                "\n  Delete save slot " + slot + "?\n" +
                        "  This action cannot be undone.\n"
        );

        dialog.button(" DELETE ", true);
        dialog.button(" CANCEL ", false);
        dialog.show(getStage());
    }
}
