package de.tum.cit.fop.maze.entities.boss;

import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;

public class BossFoundDialog extends Dialog {

    private Runnable onFight;
    private Runnable onEscape;

    public BossFoundDialog(Skin skin) {
        super("", skin);

        setModal(true);
        setMovable(false);

        // 移除标题
        getTitleLabel().setVisible(false);
        getTitleTable().clear();

        // ===== 内容区 =====
        getContentTable().pad(40); // ⭐ 核心：用 padding 撑气质

        Label label = new Label("Boss finds you.", skin);
        label.setAlignment(Align.center);

        getContentTable().add(label).width(420);

        // ===== 按钮区 =====
        getButtonTable().padTop(20).padBottom(25);

        button("READY TO COMBAT", true);
        button("RUN AWAY", false);

        pack();
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


