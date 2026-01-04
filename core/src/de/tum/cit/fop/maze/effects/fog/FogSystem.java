package de.tum.cit.fop.maze.effects.fog;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import de.tum.cit.fop.maze.game.GameConstants;
import de.tum.cit.fop.maze.tools.PerlinNoise;
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
    private float renderedAlpha;
    private float renderedRadiusMultiplier = 1.0f;
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

// --- 新增：使用 PerlinNoise 计算动态波动 ---
        // 使用 timer * 速度 作为 noise 的输入
        // 0.5f 是噪声的基础频率，你可以调大让波动变快
        float noiseValue = PerlinNoise.noise(timer * 0.8f, 0f);

        // 1. 让透明度在 [MAX_FOG_ALPHA - 0.1, MAX_FOG_ALPHA] 之间呼吸
        // 这样雾气会有深浅变化
        float dynamicAlpha = currentAlpha * (0.9f + noiseValue * 0.1f);

        // 2. 让视野半径产生 5% - 10% 的随机晃动
        // 模拟火把不稳定的光照
        float radiusFlicker = 0.95f + noiseValue * 0.1f;

        this.renderedAlpha = dynamicAlpha;
        this.renderedRadiusMultiplier = radiusFlicker;






        this.active = currentAlpha > 0;
    }

    public void render(SpriteBatch batch, float camLeft, float camBottom, float camWidth, float camHeight, float catWorldX, float catWorldY) {
        if (shader == null || currentAlpha <= 0 || disposed) return;

        batch.setShader(shader);

        // 1. 基础坐标转换 (保持不变)
        float relativeX = (catWorldX * GameConstants.CELL_SIZE - camLeft) / camWidth;
        float relativeY = (catWorldY * GameConstants.CELL_SIZE - camBottom) / camHeight;
        float screenX = relativeX * Gdx.graphics.getWidth();
        float screenY = relativeY * Gdx.graphics.getHeight();

        // 2. 计算【基础半径】(这就刚才说的基准线)
        float radiusInPixels = 5.0f * GameConstants.CELL_SIZE * (Gdx.graphics.getWidth() / camWidth);

        // 3. 【新增】加入 PerlinNoise 动态波动
        // 我们让 noise 基于时间变化，产生 0.9 到 1.1 之间的随机倍率
        float noiseValue = PerlinNoise.noise(timer * 1.5f, 0f); // 1.5f 控制晃动速度
        float flicker = 0.92f + (noiseValue * 0.16f); // 在 92% 到 108% 之间波动

        // 最终半径 = 基础半径 * 波动倍率
        float finalDynamicRadius = radiusInPixels * flicker;

        // 4. 传参给 Shader
        shader.setUniformf("u_maskCenter", screenX, screenY);
        shader.setUniformf("u_maskRadius", finalDynamicRadius); // 传这个动起来的半径
        shader.setUniformf("u_maxAlpha", currentAlpha);

        // 5. 绘制
        batch.setColor(1, 1, 1, 1);
        batch.draw(fogTexture, camLeft, camBottom, camWidth, camHeight);

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