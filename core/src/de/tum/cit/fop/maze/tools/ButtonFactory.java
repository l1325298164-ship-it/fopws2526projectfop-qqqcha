package de.tum.cit.fop.maze.tools;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import de.tum.cit.fop.maze.audio.AudioManager;
import de.tum.cit.fop.maze.audio.AudioType;

public class ButtonFactory {

    private final Skin skin;
    private final AudioManager audioManager;

    // 动画时间配置
    private float hoverDuration = 0.12f;
    private float clickDownDuration = 0.08f;
    private float clickUpDuration = 0.10f;
    private float hoverScale = 1.05f;
    private float clickScale = 0.95f;
    private float hoverBrightness = 1.08f;

    // 声音配置
    private boolean enableHoverSound = true;
    private boolean enableClickSound = true;
    private AudioType hoverSound = AudioType.UI_HIT_DAZZLE;
    private AudioType clickSound = AudioType.UI_CLICK;
    private AudioType successSound = AudioType.UI_SUCCESS;

    public ButtonFactory(Skin skin) {
        this.skin = skin;
        this.audioManager = AudioManager.getInstance();
    }

    /**
     * 设置声音参数（可选）
     */
    public void setSoundParams(boolean enableHoverSound, boolean enableClickSound,
                               AudioType hoverSound, AudioType clickSound) {
        this.enableHoverSound = enableHoverSound;
        this.enableClickSound = enableClickSound;
        this.hoverSound = hoverSound;
        this.clickSound = clickSound;
    }

    /**
     * 设置动画参数（可选）
     */
    public void setAnimationParams(float hoverDuration, float clickDownDuration,
                                   float clickUpDuration, float hoverScale,
                                   float clickScale, float hoverBrightness) {
        this.hoverDuration = hoverDuration;
        this.clickDownDuration = clickDownDuration;
        this.clickUpDuration = clickUpDuration;
        this.hoverScale = hoverScale;
        this.clickScale = clickScale;
        this.hoverBrightness = hoverBrightness;
    }

    /**
     * 创建带有完整交互效果的按钮
     */
    public TextButton create(String text, Runnable onClick) {
        return create(text, onClick, true, true);
    }

    /**
     * 创建带有完整交互效果的按钮（可自定义声音）
     */
    public TextButton create(String text, Runnable onClick,
                             boolean playClickSound, boolean playSuccessSound) {


        TextButton button = new TextButton(text, skin, "navTextButton");
        button.getLabel().setFontScale(0.7f);//全局更改字体大小
        button.pad(18f, 60f, 18f, 60f);

        // 启用变换，设置原点为中心
        button.setTransform(true);
        button.setOrigin(Align.center);

        // 状态跟踪
        final boolean[] isOver = {false};
        final boolean[] isPressed = {false};

        button.addListener(new InputListener() {

            // ========== HOVER 效果 ==========
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                isOver[0] = true;

                if (enableHoverSound && pointer == -1) {
                    audioManager.playSound(hoverSound.name(), 0.6f);
                }

                if (!isPressed[0]) {
                    resetToBase(button); // ✅ 关键
                    button.setScale(hoverScale);
                    button.setColor(
                            hoverBrightness,
                            hoverBrightness,
                            hoverBrightness,
                            1f
                    );
                }
            }


            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                isOver[0] = false;

                // 只有在未按下状态才恢复正常
                if (!isPressed[0]) {
                    performNormalAnimation(button);
                }
            }

            // ========== CLICK 效果 ==========
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int buttonCode) {
                isPressed[0] = true;

                if (enableClickSound && playClickSound) {
                    audioManager.playUIClick();
                }

                // ✅【核心修复】按下前，强制回到 BASE
                resetToBase(button);

                // ✅ 再进入 PRESSED（绝对不会叠加）
                button.setScale(clickScale);
                button.setColor(0.9f, 0.9f, 0.9f, 1f);

                return true;
            }


            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int buttonCode) {
                boolean wasPressed = isPressed[0];
                isPressed[0] = false;

                boolean isValidClick = wasPressed &&
                        x >= 0 && x <= button.getWidth() &&
                        y >= 0 && y <= button.getHeight();

                resetToBase(button); // ✅ 先回 BASE

                if (isOver[0]) {
                    // 松开后还在按钮上 → HOVER
                    button.setScale(hoverScale);
                    button.setColor(
                            hoverBrightness,
                            hoverBrightness,
                            hoverBrightness,
                            1f
                    );
                }
                // else：已经是 NORMAL（BASE），不用再设

                if (onClick != null && isValidClick) {
                    if (playSuccessSound) {
                        audioManager.playSound(successSound.name(), 0.8f);
                    }
                    onClick.run();
                }
            }

        });

        return button;
    }
//强制回基准状态
    private void resetToBase(TextButton button) {
        button.clearActions();
        button.setScale(1f);
        button.setColor(Color.WHITE);
    }









    // ========== 动画方法 ==========

    private void performHoverAnimation(TextButton button) {
        button.clearActions();
        button.addAction(
                Actions.parallel(
                        Actions.scaleTo(hoverScale, hoverScale, hoverDuration),
                        Actions.color(new Color(hoverBrightness, hoverBrightness, hoverBrightness, 1f), hoverDuration)
                )
        );
    }

    private void performNormalAnimation(TextButton button) {
        button.clearActions();
        button.addAction(
                Actions.parallel(
                        Actions.scaleTo(1f, 1f, hoverDuration),
                        Actions.color(Color.WHITE, hoverDuration)
                )
        );
    }

    private void performClickDownAnimation(TextButton button) {
        button.clearActions();
        button.addAction(
                Actions.parallel(
                        Actions.scaleTo(clickScale, clickScale, clickDownDuration),
                        Actions.color(new Color(0.9f, 0.9f, 0.9f, 1f), clickDownDuration)
                )
        );
    }

    private void performClickUpToHoverAnimation(TextButton button) {
        button.clearActions();
        button.addAction(
                Actions.parallel(
                        Actions.scaleTo(hoverScale, hoverScale, clickUpDuration),
                        Actions.color(new Color(hoverBrightness, hoverBrightness, hoverBrightness, 1f), clickUpDuration)
                )
        );
    }

    private void performClickUpToNormalAnimation(TextButton button) {
        button.clearActions();
        button.addAction(
                Actions.parallel(
                        Actions.scaleTo(1f, 1f, clickUpDuration),
                        Actions.color(Color.WHITE, clickUpDuration)
                )
        );
    }

    /**
     * 创建带有强烈点击反馈的按钮（推荐用于页面切换）
     */
    public TextButton createWithStrongFeedback(String text, Runnable onClick) {
        // 保存原来的参数
        float originalClickScale = this.clickScale;
        float originalClickDownDuration = this.clickDownDuration;

        // 设置更强的反馈
        this.clickScale = 0.92f;
        this.clickDownDuration = 0.06f;

        // 使用更强的点击声音
        AudioType originalClickSound = this.clickSound;
        this.clickSound = AudioType.UI_THROW_ATTACK;

        TextButton button = create(text, onClick);

        // 恢复原来的参数
        this.clickScale = originalClickScale;
        this.clickDownDuration = originalClickDownDuration;
        this.clickSound = originalClickSound;

        return button;
    }

    /**
     * 创建带有震动效果的按钮
     */
    public TextButton createWithShakeEffect(String text, Runnable onClick) {
        TextButton button = create(text, onClick, true, true);

        // 在点击时添加轻微震动
        button.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int buttonCode) {
                // 添加轻微震动效果
                button.clearActions();
                button.addAction(
                        Actions.sequence(
                                Actions.parallel(
                                        Actions.scaleTo(clickScale, clickScale, clickDownDuration),
                                        Actions.color(new Color(0.9f, 0.9f, 0.9f, 1f), clickDownDuration),
                                        Actions.sequence(
                                                Actions.moveBy(-2, 0, 0.02f),
                                                Actions.moveBy(4, 0, 0.04f),
                                                Actions.moveBy(-2, 0, 0.02f)
                                        )
                                )
                        )
                );
                return true;
            }
        });

        return button;
    }

    /**
     * 创建静音按钮（没有声音反馈）
     */
    public TextButton createSilent(String text, Runnable onClick) {
        return create(text, onClick, false, false);
    }

    /**
     * 创建导航按钮（用于菜单切换，有特殊的音效）
     */
    public TextButton createNavigationButton(String text, Runnable onClick) {
        // 保存原来的声音设置
        AudioType originalHoverSound = this.hoverSound;
        AudioType originalClickSound = this.clickSound;

        // 使用特殊的导航音效
        this.hoverSound = AudioType.UI_HIT_DAZZLE;
        this.clickSound = AudioType.UI_THROW_ATTACK;

        TextButton button = createWithStrongFeedback(text, onClick);

        // 恢复原来的声音设置
        this.hoverSound = originalHoverSound;
        this.clickSound = originalClickSound;

        return button;
    }

    /**
     * 创建重要操作按钮（如开始游戏、保存等）
     */
    public TextButton createImportantButton(String text, Runnable onClick) {
        // 增强的声音反馈
        audioManager.setSfxVolume(audioManager.getSfxVolume() * 1.2f); // 临时提高音量

        TextButton button = create(text, onClick, true, true);

        // 添加额外的视觉反馈
        button.addListener(new InputListener() {
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int buttonCode) {
                // 有效点击时添加闪光效果
                if (x >= 0 && x <= button.getWidth() && y >= 0 && y <= button.getHeight()) {
                    button.addAction(Actions.sequence(
                            Actions.color(new Color(1.5f, 1.5f, 1.5f, 1f), 0.05f),
                            Actions.color(Color.WHITE, 0.1f)
                    ));
                }
            }
        });

        return button;
    }
}