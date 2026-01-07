package de.tum.cit.fop.maze.entities.trap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import de.tum.cit.fop.maze.entities.Player;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.TextureManager;

public class TrapT03_TeaShards extends Trap {

    private enum State {
        IDLE,
        DAMAGING
    }
    private static TextureRegion idleFrame;
    private static Animation<TextureRegion> damageAnimation;

    private float animTime = 0f;
    private State state = State.IDLE;
    private static Animation<TextureRegion> animation;
    /* ===== 参数 ===== */
    private static final int DAMAGE = 5;
    private static final float DAMAGE_INTERVAL = 0.5f; // 1 秒 2 次
    private static final float SLOW_DURATION = 2.0f;
    private TextureManager textureManager;
    private float damageTimer = 0f;

    public TrapT03_TeaShards(int x, int y) {
        super(x, y);
        TextureAtlas atlas = new TextureAtlas("ani/T03/T03.atlas");
        loadAnimation(atlas);


    }
    @Override
    public boolean isPassable() {
        return true;
    }
    @Override
    public void update(float delta) {
        animTime += delta;

        if (state == State.DAMAGING) {
            damageTimer -= delta;
            if (damageTimer <= 0f) {
                damageTimer = 0f;
                state = State.IDLE; // 👈 自动回待机
            }
        }
    }
    private static void loadAnimation(TextureAtlas atlas) {
        if (damageAnimation != null) return;

        Array<TextureAtlas.AtlasRegion> regions =
                atlas.findRegions("T03");

        idleFrame = regions.get(7);   // 👈 这里就是「待机帧」
        damageAnimation = new Animation<>(
                0.15f,
                regions,
                Animation.PlayMode.LOOP
        );
    }
    @Override
    public void onPlayerStep(Player player) {

        // 只有主角有效（Enemy 不触发）
        state = State.DAMAGING;

        // ===== 扣血（按频率）=====
        if (damageTimer <= 0f) {
            player.takeDamage(DAMAGE);
            damageTimer = DAMAGE_INTERVAL;
        }

        // ===== 减速（不可叠加，但刷新时间）=====
        player.applySlow(SLOW_DURATION);
    }

    /* ================= 渲染 ================= */

    @Override
    public void drawShape(ShapeRenderer sr) {
        if (!active) return;

        float size = GameConstants.CELL_SIZE;
        float px = x * size;
        float py = y * size;

        // 地刺：深绿偏黄
        sr.setColor(new Color(0.1f, 0.6f, 0.2f, 1f));
        sr.rect(px, py, size, size);
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        if (!active) return;

        float size = GameConstants.CELL_SIZE;
        float px = x * size;
        float py = y * size;

        TextureRegion frame;

        if (state == State.DAMAGING) {
            frame = damageAnimation.getKeyFrame(animTime);
        } else {
            frame = idleFrame;
        }

        batch.draw(frame, px, py, size, size);
    }

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }
}
