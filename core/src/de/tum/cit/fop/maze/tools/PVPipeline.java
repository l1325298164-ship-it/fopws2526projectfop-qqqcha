package de.tum.cit.fop.maze.tools;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import de.tum.cit.fop.maze.MazeRunnerGame;
import de.tum.cit.fop.maze.screen.IntroScreen;
import de.tum.cit.fop.maze.tools.PVAnimationCache;

import java.util.List;

/**
 * Unified PV playing pipeline.
 */
public class PVPipeline {

    private final MazeRunnerGame game;
    private final List<PVNode> nodes;

    private int index = 0;
    private Runnable onFinished;

    public PVPipeline(MazeRunnerGame game, List<PVNode> nodes) {
        this.game = game;
        this.nodes = nodes;
    }

    public void onFinished(Runnable onFinished) {
        this.onFinished = onFinished;
    }

    public void start() {
        index = 0;
        playCurrent();
    }

    public void next() {
        index++;
        if (index >= nodes.size()) {
            if (onFinished != null) onFinished.run();
            return;
        }
        playCurrent();
    }

    private void playCurrent() {
        PVNode node = nodes.get(index);

        Animation<TextureRegion> anim =
                PVAnimationCache.get(node.atlasPath(), node.regionName());

        Screen old = game.getScreen();
        game.setScreen(new IntroScreen(
                game,
                anim,
                node.exit(),
                node.audio(),
                this::next   // ⭐ PV 播完自动播放下一个
        ));

        if (old != null) old.dispose();
    }
}
