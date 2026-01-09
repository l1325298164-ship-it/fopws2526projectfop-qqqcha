package de.tum.cit.fop.maze.entities.chapter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import de.tum.cit.fop.maze.game.GameConstants;

public class LoreDialog extends Group {

    private final Stage stage;
    private final Skin skin;
    private final LoreDialogCallback callback;

    public LoreDialog(
            Stage stage,
            Skin skin,
            String title,
            String contentText,
            String backgroundImagePath,
            LoreDialogCallback callback
    ) {
        this.stage = stage;
        this.skin = skin;
        this.callback = callback;

        setSize(stage.getWidth(), stage.getHeight());
        setPosition(0, 0);

        createBackgroundMask();
        createDialog(title, contentText, backgroundImagePath);

        stage.addActor(this);
    }

    /* ================= 半透明遮罩 ================= */
    private void createBackgroundMask() {
        Image mask = new Image(skin.newDrawable("white", new Color(0, 0, 0, 0.65f)));
        mask.setSize(getWidth(), getHeight());
        addActor(mask);
    }

    /* ================= 主体 Dialog ================= */
    private void createDialog(String titleText, String content, String bgPath) {

        Table root = new Table();
        root.setSize(900, 600);
        root.setPosition(
                (getWidth() - root.getWidth()) / 2f,
                (getHeight() - root.getHeight()) / 2f
        );

        // ===== 背景图 =====
        if (bgPath != null && Gdx.files.internal(bgPath).exists()) {
            Texture bg = new Texture(Gdx.files.internal(bgPath));
            root.setBackground(new TextureRegionDrawable(bg));
        } else {
            root.setBackground(skin.newDrawable("white", Color.DARK_GRAY));
        }

        root.pad(30);
        addActor(root);

        /* ===== 标题 ===== */
        Label title = new Label(titleText, skin, "title");
        title.setAlignment(Align.CENTER);
        root.add(title).expandX().fillX().padBottom(20).row();

        /* ===== 文本 ScrollPane ===== */
        Label contentLabel = new Label(content, skin);
        contentLabel.setWrap(true);
        contentLabel.setAlignment(Align.TOP_LEFT);

        ScrollPane scrollPane = new ScrollPane(contentLabel, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);

        root.add(scrollPane)
                .expand()
                .fill()
                .padBottom(20)
                .row();

        /* ===== 按钮 ===== */
        Table btnTable = new Table();

        TextButton readBtn = new TextButton("阅读", skin);
        TextButton discardBtn = new TextButton("丢弃", skin);

        readBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                remove();
                callback.onRead();
            }
        });

        discardBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                remove();
                callback.onDiscard();
            }
        });

        btnTable.add(readBtn).width(180).height(55).padRight(40);
        btnTable.add(discardBtn).width(180).height(55);

        root.add(btnTable).center();
    }

    public static String buildLoreText(List<Section> sections) {
        StringBuilder sb = new StringBuilder();

        for (Section s : sections) {
            if ("divider".equals(s.type)) {
                sb.append("\n\n──────────\n\n");
            } else {
                sb.append(s.value).append("\n\n");
            }
        }
        return sb.toString();
    }

}
