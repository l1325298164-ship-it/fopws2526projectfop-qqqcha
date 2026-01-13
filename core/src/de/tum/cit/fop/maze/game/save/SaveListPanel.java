package de.tum.cit.fop.maze.game.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import de.tum.cit.fop.maze.MazeRunnerGame;

public class SaveListPanel extends Window {
    private final MazeRunnerGame game;
    private final StorageManager storage;

    public SaveListPanel(MazeRunnerGame game, Skin skin) {
        super(" SELECT SAVE ", skin);
        this.game = game;
        this.storage = StorageManager.getInstance();

        setModal(true);
        setMovable(false);
        setResizable(false);
        pad(30);

        rebuild();

        pack();
        setPosition(
                (Gdx.graphics.getWidth() - getWidth()) / 2f,
                (Gdx.graphics.getHeight() - getHeight()) / 2f
        );
    }

    private void rebuild() {
        clearChildren();

        // === Slot 1 ===
        if (storage.hasSaveFile()) {
            add(createSlotButton(1, true)).width(520).height(70).padBottom(15).row();
        } else {
            add(createSlotButton(1, false)).width(520).height(70).padBottom(15).row();
        }

        // === Slot 2 / 3 预留 ===
        add(createSlotButton(2, false)).width(520).height(70).padBottom(15).row();
        add(createSlotButton(3, false)).width(520).height(70).padBottom(30).row();

        add(new TextButton("CANCEL", getSkin()))
                .width(300).height(60);
    }

    private TextButton createSlotButton(int slot, boolean exists) {
        String text = exists
                ? "SAVE SLOT " + slot + "  |  Level ?  |  Continue"
                : "SAVE SLOT " + slot + "  |  Empty";

        TextButton btn = new TextButton(text, getSkin());
        btn.setDisabled(!exists);

        if (exists) {
            btn.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    showConfirmDialog(slot);
                }
            });
        }

        return btn;
    }

    private void showConfirmDialog(int slot) {
        Dialog dialog = new Dialog(" LOAD GAME ", getSkin()) {
            @Override
            protected void result(Object object) {
                if ((Boolean) object) {
                    game.loadGame(); // 现在只有一个 slot
                    remove();
                }
            }
        };

        dialog.text(
                "\n  Load save slot " + slot + " ?\n" +
                        "  Unsaved progress will be lost.\n"
        );

        dialog.button(" LOAD ", true);
        dialog.button(" CANCEL ", false);
        dialog.show(getStage());
    }
}