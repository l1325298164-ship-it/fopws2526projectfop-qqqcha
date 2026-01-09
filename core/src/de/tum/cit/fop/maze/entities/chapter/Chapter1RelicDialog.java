package de.tum.cit.fop.maze.entities.chapter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
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

    private static final String BG_PATH = "chapters/relic_bg.png";

    public Chapter1RelicDialog(Skin skin, Chapter1Relic relic) {
        // ❗ 不要标题
        super("", skin);
        this.relic = relic;

        setModal(true);
        setMovable(false);
        setResizable(false);

        // ❗ 强制隐藏 title 区域
        getTitleLabel().setVisible(false);
        getTitleTable().clear();

        /* ================== 背景图 ================== */
        Texture bgTex = new Texture(Gdx.files.internal(BG_PATH));
        Drawable bgDrawable = new TextureRegionDrawable(bgTex);

        float bgW = bgTex.getWidth();
        float bgH = bgTex.getHeight();

        Table content = getContentTable();
        content.clear();
        content.setBackground(bgDrawable);

        /* ================== 文本 ================== */
        // ✅ 明确指定字体（必须）
        BitmapFont font = skin.getFont("default-font-BF");

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.BLACK; // ⭐关键：别用白色（背景是亮色）

        textLabel = new Label("", labelStyle);
        textLabel.setWrap(true);
        textLabel.setAlignment(Align.topLeft);

// 🔥 防止父级透明度影响
        textLabel.getColor().a = 1f;

        scrollPane = new ScrollPane(textLabel, skin);
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, true);

// ⭐ ScrollPane 自身也强制不透明
        scrollPane.getColor().a = 1f;

        // ⭐ 用 padding 控制内容区域
        content.pad(60);
        content.add(scrollPane).expand().fill();
        content.invalidateHierarchy();
        this.layout();
        refreshText();
        unlockNextLine();
        /* ================== 按钮 ================== */
        button("read", true);
        button("dispose", false);

        /* ================== 输入：点击 / 滚轮 ================== */
        scrollPane.addListener(new InputListener() {

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
        });s

        // ❗ 不用 pack()
        setSize(bgW, bgH);
        setPosition(
                (Gdx.graphics.getWidth() - getWidth()) / 2f,
                (Gdx.graphics.getHeight() - getHeight()) / 2f
        );

    }

    /* ================== 解锁逻辑 ================== */

    private void unlockNextLine() {
        System.out.println("Unlocked lines = " + unlockedLines);
        if (unlockedLines < lines.size()) {
            unlockedLines++;
            refreshText();
        }

        if (unlockedLines >= lines.size()) {
            fullyUnlocked = true;
            scrollPane.setScrollingDisabled(true, false);
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
        System.out.println("Dialog result = " + read);

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
