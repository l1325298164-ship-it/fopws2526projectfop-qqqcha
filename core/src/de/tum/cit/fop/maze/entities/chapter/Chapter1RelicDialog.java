package de.tum.cit.fop.maze.entities.chapter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import java.util.List;

public class Chapter1RelicDialog extends Dialog {

    private final Chapter1Relic relic;

    private Runnable onRead;
    private Runnable onDiscard;

    /* ================== 文本解锁状态 ================== */
    private int unlockedLines = 0;
    private boolean fullyUnlocked = false;

    private Label textLabel;
    private ScrollPane scrollPane;

    /* ================== 文本内容（逐行） ================== */
    private final List<String> lines = List.of(
            "[ Recipe ] Rock Salt Cheese Green Tea",
            "(Codename: Disguise)",
            "",
            "Mixing Instructions:",
            "1. Select morning-picked Jasmine Green Tea",
            "   from an altitude of 1,200 meters",
            "   as the tea base.",
            "",
            "2. Layer with a thick cheese foam made",
            "   from heavy cream and rock salt,",
            "   whipped to 50% stiffness.",
            "",
            "3. Key Point:",
            "   Do not stir.",
            "   Taste the salty bitterness first,",
            "   then reach the crisp sweetness beneath.",
            "",
            "Hidden Truth:",
            "On the Dessert Planet, within every layer",
            "of rich milk cap, slumbers a",
            "\"Milk Cap Cat\" — the physical incarnation",
            "of the planet’s will.",
            "",
            "They are guardian deities of flavor.",
            "Every breath they take carries a",
            "dense, milky aroma.",
            "",
            "However, Momota is invading from a",
            "parallel universe with",
            "\"Pretentious High-End Labeling\".",
            "",
            "Once a Milk Cap Cat loses its spirituality,",
            "the very soul of the Dessert Planet",
            "will cease to exist."
    );

    private static final String BG_PATH = "chapter/relic_bg.png";

    public Chapter1RelicDialog(Skin skin, Chapter1Relic relic) {
        super("", skin);
        this.relic = relic;

        setModal(true);
        setMovable(false);
        setResizable(false);

        /* ================== 半透明遮罩 ================== */
        Drawable dim = skin.newDrawable("white", new Color(0, 0, 0, 0.65f));
        getContentTable().setBackground(dim);

        /* ================== 背景图 ================== */
        Texture bgTex = new Texture(Gdx.files.internal(BG_PATH));
        Drawable bgDrawable = new TextureRegionDrawable(bgTex);

        Table content = getContentTable();
        content.pad(40);
        content.setBackground(bgDrawable);

        /* ================== 文本 Label ================== */
        textLabel = new Label("", skin);
        textLabel.setWrap(true);
        textLabel.setAlignment(Align.left);

        scrollPane = new ScrollPane(textLabel);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, true); // 初始不可滚动

        content.add(scrollPane).width(620).height(420);

        refreshText();

        /* ================== 按钮 ================== */
        button("📖 阅读", true);
        button("🗑 丢弃", false);

        /* ================== 输入：点击 / 滚轮 ================== */
        addListener(new InputListener() {

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (!fullyUnlocked) {
                    unlockNextLine();
                    return true;
                }
                return false;
            }

            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
                if (!fullyUnlocked && amountY > 0) {
                    unlockNextLine();
                    return true;
                }
                return false;
            }
        });

        pack();
        setPosition(
                (Gdx.graphics.getWidth() - getWidth()) / 2f,
                (Gdx.graphics.getHeight() - getHeight()) / 2f
        );
    }

    /* ================== 解锁逻辑 ================== */

    private void unlockNextLine() {
        if (unlockedLines < lines.size()) {
            unlockedLines++;
            refreshText();
        }

        if (unlockedLines >= lines.size()) {
            fullyUnlocked = true;
            scrollPane.setScrollingDisabled(true, false); // 解锁滚动
        }
    }

    private void refreshText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < unlockedLines; i++) {
            sb.append(lines.get(i)).append("\n");
        }
        textLabel.setText(sb.toString());
    }

    @Override
    protected void result(Object object) {
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
