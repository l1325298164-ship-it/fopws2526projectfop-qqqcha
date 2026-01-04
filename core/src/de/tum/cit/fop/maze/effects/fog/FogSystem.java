package de.tum.cit.fop.maze.effects.fog;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.utils.Logger;

public class FogSystem {
    private ShaderProgram shader;
    private final Texture fogTexture;

    private boolean active = false;
    private boolean debugEnabled = false;
    private float currentAlpha = 0f; // 当前雾气的实时透明度
    private static final float FADE_SPEED = 1.5f; // 出现和消失的速度（数值越大越快）
    private static final float MAX_FOG_ALPHA = 0.85f; // 雾气的最大浓度
    private float timer = 0f;
    private static final float CYCLE = 60f;
    private static final float FOG_DURATION = 30f;

    private boolean disposed = false;

    public FogSystem() {
        ShaderProgram.pedantic = false; // 增强兼容性
        fogTexture = new Texture("effects/fog.png");

        String vertexShader = Gdx.files.internal("shaders/vertex.glsl").readString();
        String fragmentShader = Gdx.files.internal("shaders/fragment.glsl").readString();
        shader = new ShaderProgram(vertexShader, fragmentShader);

        if (!shader.isCompiled()) {
            System.err.println("Shader Error: " + shader.getLog());
        }
    }

    public void update(float delta) {
        if (disposed) return;

        // F7 调试逻辑
        if (Gdx.input.isKeyJustPressed(Input.Keys.F7)) {
            debugEnabled = !debugEnabled;
        }

        timer += delta;
        if (timer >= CYCLE) timer -= CYCLE;

        // 判断当前是否应该激活雾气
        boolean targetActive = (timer <= FOG_DURATION) || debugEnabled;

        // 【关键】平滑过渡透明度
        if (targetActive) {
            // 慢慢增加到 0.85
            currentAlpha = Math.min(MAX_FOG_ALPHA, currentAlpha + delta * FADE_SPEED);
        } else {
            // 慢慢减少到 0
            currentAlpha = Math.max(0f, currentAlpha - delta * FADE_SPEED);
        }

        this.active = currentAlpha > 0;
    }

    public void render(SpriteBatch batch, float camLeft, float camBottom, float camWidth, float camHeight, float catWorldX, float catWorldY) {
        // 检查点：如果透明度为0，直接跳过，节省GPU开销
        if (shader == null || currentAlpha <= 0 || disposed) return;

        // 1. 设置着色器
        batch.setShader(shader);

        // 2. 坐标与半径转换
        float relativeX = (catWorldX * GameConstants.CELL_SIZE - camLeft) / camWidth;
        float relativeY = (catWorldY * GameConstants.CELL_SIZE - camBottom) / camHeight;
        float screenX = relativeX * Gdx.graphics.getWidth();
        float screenY = relativeY * Gdx.graphics.getHeight();
        float radiusInPixels = 5.0f * GameConstants.CELL_SIZE * (Gdx.graphics.getWidth() / camWidth);

        // 3. 传参 (不需要手动 shader.bind()，SpriteBatch 处理好了)
        shader.setUniformf("u_maskCenter", screenX, screenY);
        shader.setUniformf("u_maskRadius", radiusInPixels);
        shader.setUniformf("u_maxAlpha", currentAlpha);

        // 4. 执行绘制
        // 注意：这里的颜色设为白色，因为我们在片段着色器里已经硬编码了 (0,0,0) 的 RGB
        batch.setColor(Color.WHITE);
        batch.draw(fogTexture, camLeft, camBottom, camWidth, camHeight);

        // 5. 必须刷新当前批次，否则 setShader(null) 会提前生效
        batch.flush();
        batch.setShader(null);
    }


    public void dispose() {
        if (disposed) return;

        fogTexture.dispose();
        disposed = true;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public float getTimer() {
        return timer;
    }
}