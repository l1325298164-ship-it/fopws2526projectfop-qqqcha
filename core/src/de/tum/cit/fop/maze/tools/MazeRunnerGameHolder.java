package de.tum.cit.fop.maze.tools;

import de.tum.cit.fop.maze.MazeRunnerGame;

public final class MazeRunnerGameHolder {

    private static MazeRunnerGame game;

    private MazeRunnerGameHolder() {}

    public static void init(MazeRunnerGame g) {
        game = g;
    }

    public static MazeRunnerGame get() {
        if (game == null) {
            throw new IllegalStateException(
                    "MazeRunnerGameHolder not initialized"
            );
        }
        return game;
    }
}
