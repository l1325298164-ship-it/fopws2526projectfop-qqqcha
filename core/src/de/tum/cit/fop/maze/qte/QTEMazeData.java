// QTEMazeData.java
package de.tum.cit.fop.maze.qte;

public class QTEMazeData {

    /**
     * 0 = 墙
     * 1 = 地
     *
     * Y 从下往上（和你现在 GameManager 一致）
     */
    public static final int[][] MAZE1 = {
            {0,0,0,0,0,0,0},
            {0,1,1,1,1,1,0},
            {0,1,0,0,0,1,0},
            {0,1,0,1,0,1,0},
            {0,1,0,1,1,1,0},
            {0,1,1,1,0,1,0},
            {0,0,0,0,0,0,0},
    };

    public static final int[][] MAZE2 = {
            {0,0,0,0,0,0,0},
            {0,1,1,1,1,1,0},
            {0,1,1,1,1,1,0},
            {0,0,0,1,1,1,0},
            {0,0,0,1,1,1,0},
            {0,0,0,1,0,1,0},
            {0,0,0,0,0,0,0},
    };

    private QTEMazeData() {}
}
