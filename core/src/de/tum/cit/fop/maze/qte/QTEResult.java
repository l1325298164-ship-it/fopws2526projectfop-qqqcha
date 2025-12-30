package de.tum.cit.fop.maze.qte;

/**
 * QTE结果枚举 - 供所有QTE屏幕共用
 */
public enum QTEResult {
    SUCCESS,
    FAIL
}
//
//package de.tum.cit.fop.maze.screen;
//
//// 其他import保持不变...
//// 删除原来内部的 QTEResult 枚举定义
//
//public class QTEScreen implements Screen {
//
//    // 使用公共的 QTEResult
//    private QTEResult result = null;
//
//    // 删除原来的内部枚举定义：
//    // public enum QTEResult {
//    //     SUCCESS,
//    //     FAIL
//    // }
//
//    // ... 其他代码保持不变 ...
//
//    private void finishQTE(QTEResult result) {  // 参数类型改为 QTEResult
//        if (qteState == QTEState.DONE) return;
//
//        qteState = QTEState.DONE;
//        Logger.debug("QTE -> DONE, 最终位置: (" + playerGridX + ", " + playerGridY + ")");
//        this.result = result;
//
//        Gdx.app.postRunnable(() -> {
//            game.onQTEFinished(result);
//        });
//    }
//
//    // 在 updateQTE 方法中调用时：
//    private void updateQTE(float delta) {
//        if (qteState != QTEState.ACTIVE) return;
//
//        qteTimer += delta;
//        if (qteTimer >= QTE_TIME_LIMIT) {
//            finishQTE(QTEResult.FAIL);  // 直接使用 QTEResult.FAIL
//            return;
//        }
//        // ... 其他代码
//    }
//
//    // 在 checkSuccess 或其他调用 finishQTE 的地方也要修改
//    private void enterSuccessStart() {
//        // ... 成功逻辑
//        // 最终会调用：
//        // finishQTE(QTEResult.SUCCESS);
//    }
//}
//
//
//package de.tum.cit.fop.maze.screen;
//
//import com.badlogic.gdx.Gdx;
//// ... 其他 import
//import de.tum.cit.fop.maze.screen.QTEResult; // 导入公共的 QTEResult
//
//public class QTEScreen2 implements Screen {
//
//    // 使用公共的 QTEResult
//    private QTEResult result = null;
//
//    // 删除原来的内部枚举定义（如果有的话）
//
//    // ... 其他代码保持不变 ...
//
//    private void finishQTE(QTEResult result) {
//        if (qteState == QTEState.DONE) return;
//
//        qteState = QTEState.DONE;
//        Logger.debug("双人QTE -> DONE, 结果: " + result);
//        this.result = result;
//
//        Gdx.app.postRunnable(() -> {
//            game.onQTEFinished(result);
//        });
//    }
//
//    private void checkFailCondition() {
//        int totalMash = mashCountA + mashCountB;
//
//        if (totalMash < MASH_REQUIRED_TOTAL ||
//                mashCountA < MASH_MINIMUM_PER_PLAYER ||
//                mashCountB < MASH_MINIMUM_PER_PLAYER) {
//            finishQTE(QTEResult.FAIL);  // 使用 QTEResult
//        } else {
//            enterSuccessStart();
//        }
//    }
//
//    private void updateSuccess(float delta) {
//        // ...
//        if (successStayTimer >= 0.8f) {
//            finishQTE(QTEResult.SUCCESS);  // 使用 QTEResult
//        }
//    }
//}
//
//package de.tum.cit.fop.maze;
//
//import de.tum.cit.fop.maze.screen.QTEResult;
//
//public class MazeRunnerGame extends Game {
//
//    // ... 其他代码 ...
//
//    public void onQTEFinished(QTEResult result) {
//        // 确保这个方法接受 QTEResult 参数
//        switch (result) {
//            case SUCCESS:
//                // 成功逻辑
//                Logger.debug("QTE 成功");
//                // 返回游戏主屏幕或其他逻辑
//                break;
//            case FAIL:
//                // 失败逻辑
//                Logger.debug("QTE 失败");
//                // 返回游戏主屏幕或其他逻辑
//                break;
//        }
//    }
//
//    // ... 其他代码 ...
//}


