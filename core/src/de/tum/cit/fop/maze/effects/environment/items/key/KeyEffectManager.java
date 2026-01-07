package de.tum.cit.fop.maze.effects.environment.items.key;


import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class KeyEffectManager {
    private List<KeyCollectEffect> keyEffects;

    public KeyEffectManager() {
        keyEffects = new ArrayList<>();
    }

    /**
     * 生成一个钥匙收集特效
     * @param x 像素坐标 X
     * @param y 像素坐标 Y
     * @param texture 钥匙的纹理
     */
    public void spawnKeyEffect(float x, float y, Texture texture) {
        keyEffects.add(new KeyCollectEffect(x, y, texture));
    }

    public void update(float delta) {
        Iterator<KeyCollectEffect> it = keyEffects.iterator();
        while (it.hasNext()) {
            KeyCollectEffect effect = it.next();
            effect.update(delta);
            if (effect.isFinished()) {
                it.remove();
            }
        }
    }

    public void render(SpriteBatch batch) {
        for (KeyCollectEffect effect : keyEffects) {
            effect.render(batch);
        }
    }

    public void dispose() {
        keyEffects.clear();
    }
}