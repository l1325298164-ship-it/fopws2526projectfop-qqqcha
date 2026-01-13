package de.tum.cit.fop.maze.entities.boss;

import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class BossFoundDialog extends Dialog {

    private Runnable onFight;
    private Runnable onEscape;

    public BossFoundDialog(Skin skin) {
        super("", skin);

        setModal(true);
        setMovable(false);
        getTitleLabel().setVisible(false);
        getTitleTable().clear();

        text("Boss finds you……");

        button("ready to combat", true);
        button("run away", false);
    }

    @Override
    protected void result(Object object) {
        boolean fight = (Boolean) object;
        hide();

        if (fight) {
            if (onFight != null) onFight.run();
        } else {
            if (onEscape != null) onEscape.run();
        }
    }

    public void setOnFight(Runnable onFight) {
        this.onFight = onFight;
    }

    public void setOnEscape(Runnable onEscape) {
        this.onEscape = onEscape;
    }
}
