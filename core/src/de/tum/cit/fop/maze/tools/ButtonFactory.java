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
    //使用说明书：
//    【ButtonFactory 功能列表】
//
//            ===== 1. 基础按钮创建 =====
//            1.1 create(String text, Runnable onClick)
//    - 创建标准按钮
//    - 包含：悬停动画 + 点击动画 + 声音反馈
//
//===== 2. 声音系统 =====
//            2.1 悬停声音 (hoverSound)
//    - 类型：AudioType.UI_HIT_DAZZLE
//    - 触发：鼠标进入按钮时
//    - 条件：pointer == -1（鼠标设备）
//
//            2.2 点击声音 (clickSound)
//    - 类型：AudioType.UI_CLICK
//    - 触发：touchDown按下瞬间
//    - 使用：audioManager.playUIClick()（带冷却时间）
//
//            2.3 成功音效 (successSound)
//    - 类型：AudioType.UI_SUCCESS
//    - 触发：有效点击完成时
//    - 条件：按下和松开都在按钮区域内
//
//2.4 声音配置方法
//    - setSoundParams()：自定义声音开关和类型
//    - 默认：悬停音效开启，点击音效开启
//
//===== 3. 动画系统 =====
//            3.1 动画参数配置
//    - hoverDuration：悬停动画时长 (0.12f)
//    - clickDownDuration：按下动画时长 (0.08f)
//    - clickUpDuration：松开动画时长 (0.10f)
//    - hoverScale：悬停缩放比例 (1.05f)
//    - clickScale：点击缩放比例 (0.95f)
//    - hoverBrightness：悬停亮度 (1.08f)
//
//3.2 动画状态管理
//    - isOver：鼠标是否在按钮上
//    - isPressed：按钮是否被按下
//    - 状态互斥：按下时不执行悬停动画
//
//3.3 动画方法
//    - performHoverAnimation()：执行悬停动画（放大+变亮）
//            - performNormalAnimation()：恢复普通状态（还原）
//            - performClickDownAnimation()：点击按下动画（缩小+变暗）
//            - performClickUpToHoverAnimation()：松开后到悬停状态
//    - performClickUpToNormalAnimation()：松开后到普通状态
//
//===== 4. 特殊按钮类型 =====
//            4.1 createWithStrongFeedback()
//    - 强烈反馈按钮
//    - 特点：更明显的缩放(0.92f) + 更快的动画(0.06f) + 特殊音效
//
//4.2 createWithShakeEffect()
//    - 震动效果按钮
//    - 特点：点击时左右轻微震动（-2px → +4px → -2px）
//
//            4.3 createSilent()
//    - 静音按钮
//    - 特点：关闭所有声音反馈，只有视觉动画
//
//4.4 createNavigationButton()
//    - 导航专用按钮
//    - 特点：使用特殊音效（UI_HIT_DAZZLE + UI_THROW_ATTACK）
//
//            4.5 createImportantButton()
//    - 重要操作按钮
//    - 特点：临时提高音量 + 点击闪光效果
//
//===== 5. 事件处理逻辑 =====
//            5.1 有效点击判断
//    - 条件：touchDown和touchUp都在按钮区域内
//    - 实现：x,y坐标在按钮宽高范围内
//
//5.2 回调执行时机
//    - 默认：延迟执行（clickUpDuration * 0.3f）
//            - 目的：让用户看到动画效果
//    - 使用：Actions.sequence(Actions.delay(), Actions.run())
//
//            ===== 6. 技术要点 =====
//            6.1 必须设置的属性
//    - button.setTransform(true)：启用变换
//    - button.setOrigin(Align.center)：设置缩放原点为中心
//
//6.2 状态跟踪技巧
//    - 使用final boolean[]数组（绕过final限制）
//            - isOver[0]：跟踪鼠标悬停状态
//    - isPressed[0]：跟踪按下状态
//
//6.3 动画清除策略
//    - 每次动画前执行button.clearActions()
//            - 避免动画冲突和叠加
//
//===== 7. 使用方法示例 =====
//            7.1 基本使用
//    ButtonFactory bf = new ButtonFactory(skin);
//    TextButton btn = bf.create("文本", () -> { 执行代码 });
//
//7.2 自定义配置
//    // 设置动画参数
//    bf.setAnimationParams(0.1f, 0.05f, 0.08f, 1.08f, 0.92f, 1.1f);
//
//    // 设置声音参数
//    bf.setSoundParams(true, false, AudioType.UI_HIT_DAZZLE, AudioType.UI_CLICK);
//
//7.3 特殊按钮
//    bf.createNavigationButton("开始游戏", game::start);
//    bf.createSilent("设置", this::openSettings);
//
//===== 8. 与其他系统集成 =====
//            8.1 与AudioManager集成
//    - 自动获取AudioManager.getInstance()
//            - 使用预定义的AudioType枚举
//    - 遵循音频管理器的冷却时间限制
//
//8.2 与Stage和Table兼容
//    - 返回标准TextButton对象
//    - 可直接添加到Table中使用
//
//===== 9. 注意事项 =====
//            9.1 确保皮肤样式存在
//    - 使用"navTextButton"样式
//    - 确保skin.json中有对应定义
//
//9.2 性能考虑
//    - 动画使用Actions，由Stage统一管理
//    - 声音使用轻量级音效文件
//    - 避免创建过多监听器实例
//
//===== 10. 扩展建议 =====
//            10.1 可添加的功能
//    - 禁用状态样式
//    - 按钮按下长按效果
//    - 自定义颜色主题
//    - 动态加载音效
//
//10.2 优化建议
//    - 添加按钮池重用
//    - 支持键盘导航
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

                // 播放悬停声音
                if (enableHoverSound && pointer == -1) { // pointer == -1 表示鼠标
                    audioManager.playSound(hoverSound.name(), 0.6f);
                }

                // 只有在未按下状态才执行 hover 动画
                if (!isPressed[0]) {
                    performHoverAnimation(button);
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

                // 播放点击声音（按下瞬间）
                if (enableClickSound && playClickSound) {
                    audioManager.playUIClick(); // 使用专用的UI点击方法
                }

                // 立即执行点击动画（按下瞬间）
                performClickDownAnimation(button);

                return true; // 返回 true 表示处理了这个事件
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int buttonCode) {
                boolean wasPressed = isPressed[0];
                isPressed[0] = false;

                // 检查是否是有效点击
                boolean isValidClick = wasPressed &&
                        x >= 0 && x <= button.getWidth() &&
                        y >= 0 && y <= button.getHeight();

                // 立即执行释放动画
                if (isOver[0]) {
                    // 鼠标还在按钮上，执行弹起动画到hover状态
                    performClickUpToHoverAnimation(button);
                } else {
                    // 鼠标已离开，执行弹起动画到普通状态
                    performClickUpToNormalAnimation(button);
                }

                // 执行点击回调
                if (onClick != null && isValidClick) {
                    // 播放成功音效（如果需要）
                    if (playSuccessSound) {
                        audioManager.playSound(successSound.name(), 0.8f);
                    }

                    // ❌ 移除延迟执行，改为立即执行
                    // button.addAction(Actions.sequence(
                    //     Actions.delay(clickUpDuration * 0.3f),
                    //     Actions.run(onClick)
                    // ));

                    // ✅ 改为立即执行回调
                    onClick.run();
                }
            }
        });

        return button;
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