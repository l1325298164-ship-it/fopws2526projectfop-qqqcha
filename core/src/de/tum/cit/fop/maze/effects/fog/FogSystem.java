package de.tum.cit.fop.maze.effects.fog;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.GL20;
import de.tum.cit.fop.maze.game.GameConstants;

public class FogSystem {

    private final Texture fogTexture;
    private final Texture maskTexture;

    private boolean active = false;
    private boolean debugEnabled = false;

    private float timer = 0f;
    private static final float CYCLE = 60f;
    private static final float FOG_DURATION = 30f;

    public FogSystem() {
        fogTexture = new Texture("effects/fog.png");
        fogTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        maskTexture = new Texture("effects/fog_mask.png");
    }

    public void update(float delta) {
        // F7 Debug 切换
        if (Gdx.input.isKeyJustPressed(Input.Keys.F7)) {
            debugEnabled = !debugEnabled;
        }

        timer += delta;
        if (timer >= CYCLE) timer -= CYCLE;

        active = timer <= FOG_DURATION;

        if (debugEnabled) {
            active = true;
        }
    }

    public void render(
            SpriteBatch batch,
            float camLeft,
            float camBottom,
            float camWidth,
            float camHeight,
            float catWorldX,
            float catWorldY
    ) {
        if (!active) return;

        // ===== 1️⃣ 先画整屏雾 =====
        batch.setColor(1f, 1f, 1f, 0.85f);

        batch.draw(
                fogTexture,
                camLeft,
                camBottom,
                camWidth,
                camHeight,
                0, 0,
                camWidth / 64f,
                camHeight / 64f
        );

        // ===== 2️⃣ 用遮罩「擦掉雾」 =====
        batch.flush();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_ZERO, GL20.GL_ONE_MINUS_SRC_ALPHA);

        float maskSize = 6f * GameConstants.CELL_SIZE;

        float maskX = catWorldX * GameConstants.CELL_SIZE - maskSize / 2f;
        float maskY = catWorldY * GameConstants.CELL_SIZE - maskSize / 2f;

        batch.setColor(Color.WHITE);
        batch.draw(maskTexture, maskX, maskY, maskSize, maskSize);

        batch.flush();
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void dispose() {
        fogTexture.dispose();
        maskTexture.dispose();
    }
}

