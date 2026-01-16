package de.tum.cit.fop.maze.entities.chapter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

public class Chapter1RelicDialog extends Dialog {

    private static final String BG_PATH = "chapters/relic_bg.png";

    private final Chapter1Relic relic;

    private Runnable onRead;
    private Runnable onDiscard;

    private Label textLabel;
    private String[] lines;
    private int currentLine = 0;
    private boolean finished = false;

    public Chapter1RelicDialog(Skin skin, Chapter1Relic relic) {
        super("", skin);
        this.relic = relic;

        // ===== 文本内容 =====
        lines = new String[] {
                "1111",
                "2222",
                "333"
        };

        // ===== Label =====
        Label.LabelStyle labelStyle = skin.get("default", Label.LabelStyle.class);
        textLabel = new Label(lines[0], labelStyle);
        textLabel.setWrap(true);
        textLabel.setAlignment(Align.center);

        // ===== Dialog 基本属性 =====
        setModal(true);
        setMovable(false);
        setResizable(false);

        // ===== 移除标题栏 =====
        getTitleLabel().setVisible(false);
        getTitleTable().clear();

        // ===== 加载背景图 =====
        Texture bgTex = new Texture(Gdx.files.internal(BG_PATH));
        Drawable bgDrawable = new TextureRegionDrawable(bgTex);

        float texW = bgTex.getWidth();
        float texH = bgTex.getHeight();

        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();

        // ⭐ 高度按屏幕 85% 缩放
        float targetH = screenH * 0.85f;
        float scale = targetH / texH;
        float targetW = texW * scale;

        // ===== 内容表 =====
        Table content = getContentTable();
        content.clear();
        content.setBackground(bgDrawable);

        // 文本布局（居中 + 内边距）
        content.add(textLabel)
                .expand()
                .fill()
                .pad(60f);

        // ===== Dialog 尺寸 =====
        setSize(targetW, targetH);

        // ===== 底部按钮 =====
        button("Read", true);
        button("Discard", false);

        // ===== 输入监听 =====
        addListener(new InputListener() {

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                nextLine();
                return true;
            }

            @Override
            public boolean keyDown(InputEvent event, int keycode) {

                if (keycode == Input.Keys.ESCAPE) {
                    relic.onDiscard();
                    if (onDiscard != null) onDiscard.run();
                    hide();
                    return true;
                }

                nextLine();
                return true;
            }
        });

        // ===== 居中 =====
        setPosition(
                (screenW - getWidth()) / 2f,
                (screenH - getHeight()) / 2f
        );
    }

    private void nextLine() {
        if (finished) return;

        currentLine++;
        if (currentLine >= lines.length) {
            finished = true;
            relic.onRead();
            if (onRead != null) onRead.run();
            hide();
            return;
        }

        textLabel.setText(lines[currentLine]);
    }

    @Override
    protected void result(Object object) {
        if (!(object instanceof Boolean)) {
            hide();
            return;
        }

        boolean read = (Boolean) object;

        if (read) {
            relic.onRead();
            if (onRead != null) onRead.run();
        } else {
            relic.onDiscard();
            if (onDiscard != null) onDiscard.run();
        }

        hide();
    }

    public void setOnRead(Runnable onRead) {
        this.onRead = onRead;
    }

    public void setOnDiscard(Runnable onDiscard) {
        this.onDiscard = onDiscard;
    }
}
