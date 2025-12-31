package de.tum.cit.fop.maze.menu_tile;

public class CorruptionManager {

    public float value = 0f;

    // 可调参数
    private static final float IDLE_RATE = 0.03f;
    private static final float USELESS_CLICK_AMOUNT = 0.06f;

    // 每帧更新（停留）
    public void update(float delta, boolean idle) {
        if (idle) {
            value += delta * IDLE_RATE;
        }
        clamp();
    }

    // ⭐ 无效点击
    public void onUselessClick() {
        value += USELESS_CLICK_AMOUNT;
        clamp();
    }

    // stage 判断
    public int getStage() {
        if (value < 0.3f) return 0;
        if (value < 0.6f) return 1;
        return 2;
    }

    private void clamp() {
        if (value > 1f) value = 1f;
        if (value < 0f) value = 0f;
    }
}
