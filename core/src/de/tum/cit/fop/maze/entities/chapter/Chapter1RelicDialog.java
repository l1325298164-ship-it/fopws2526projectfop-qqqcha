//package de.tum.cit.fop.maze.entities.chapter;
//
//import com.badlogic.gdx.Gdx;
//import com.badlogic.gdx.graphics.Texture;
//import com.badlogic.gdx.scenes.scene2d.InputEvent;
//import com.badlogic.gdx.scenes.scene2d.InputListener;
//import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
//import com.badlogic.gdx.scenes.scene2d.ui.Skin;
//import com.badlogic.gdx.scenes.scene2d.ui.Table;
//import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
//import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
//
//public class Chapter1RelicDialog extends Dialog {
//
//    private final Chapter1Relic relic;
//
//    private Runnable onRead;
//    private Runnable onDiscard;
//
//    private static final String BG_PATH = "chapters/relic_bg.png";
//
//    public Chapter1RelicDialog(Skin skin, Chapter1Relic relic) {
//        super("", skin);
//        this.relic = relic;
//
//        setModal(true);
//        setMovable(false);
//        setResizable(false);
//
//        // ===== 移除标题栏 =====
//        getTitleLabel().setVisible(false);
//        getTitleTable().clear();
//
//        // ===== 背景图 =====
//        Texture bgTex = new Texture(Gdx.files.internal(BG_PATH));
//        Drawable bgDrawable = new TextureRegionDrawable(bgTex);
//
//        float bgW = bgTex.getWidth();
//        float bgH = bgTex.getHeight();
//
//        Table content = getContentTable();
//        content.clear();
//        content.setBackground(bgDrawable);
//        content.setSize(bgW, bgH);
//
//        // ===== 底部按钮 =====
//        // 直接用 Skin 里的 ButtonStyle
//        button("read", true);
//        button("dispose", false);
//
//        // ===== ESC 关闭（等价于 dispose）=====
//        addListener(new InputListener() {
//            @Override
//            public boolean keyDown(InputEvent event, int keycode) {
//                hide();
//                if (onDiscard != null) onDiscard.run();
//                return true;
//            }
//        });
//
//        // ===== Dialog 尺寸与居中 =====
//        setSize(bgW, bgH);
//        setPosition(
//                (Gdx.graphics.getWidth() - getWidth()) / 2f,
//                (Gdx.graphics.getHeight() - getHeight()) / 2f
//        );
//    }
//
//    @Override
//    protected void result(Object object) {
//        if (!(object instanceof Boolean)) {
//            hide();
//            return;
//        }
//
//        boolean read = (Boolean) object;
//
//        if (read) {
//            relic.onRead();
//            if (onRead != null) onRead.run();
//        } else {
//            relic.onDiscard();
//            if (onDiscard != null) onDiscard.run();
//        }
//
//        hide();
//    }
//
//    public void setOnRead(Runnable onRead) {
//        this.onRead = onRead;
//    }
//
//    public void setOnDiscard(Runnable onDiscard) {
//        this.onDiscard = onDiscard;
//    }
//}
