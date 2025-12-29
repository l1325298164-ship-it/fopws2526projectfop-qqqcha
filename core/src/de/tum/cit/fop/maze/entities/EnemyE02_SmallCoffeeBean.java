package de.tum.cit.fop.maze.entities;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import de.tum.cit.fop.maze.game.GameManager;

/**
 * 敌人 E02：小包咖啡豆
 * 行为：只会乱窜，不会攻击
 */
public class EnemyE02_SmallCoffeeBean extends Enemy {

    public EnemyE02_SmallCoffeeBean(int x, int y) {
        super(x, y);

        hp = 3;
        collisionDamage = 5;

        moveSpeed = 6.0f;          // 连续移动速度（格/秒）
        moveInterval = 0.2f;      // 走得频繁
        changeDirInterval = 0.2f;  // 疯狂换方向

        updateTexture();
    }

    /* ================== 渲染 ================== */

    @Override
    public RenderType getRenderType() {
        return RenderType.SPRITE;
    }

    @Override
    public void drawSprite(SpriteBatch batch) {
        super.drawSprite(batch); // 直接复用 Enemy 的渲染
    }

    @Override
    protected void updateTexture() {
        // 先复用 enemy1 的贴图
        // 以后你可以在 TextureManager 里加 ENEMY2
        texture = textureManager.getEnemy1Texture();
        needsTextureUpdate = false;
    }

    /* ================== 行为 ================== */

    @Override
    public void update(float delta, GameManager gm) {
        if (!active) return;

        // ⭐ 受击闪烁
        updateHitFlash(delta);

        // ⭐ 纯随机移动（乱窜）
        tryMoveRandom(delta, gm);
        moveContinuously(delta);
    }
}
