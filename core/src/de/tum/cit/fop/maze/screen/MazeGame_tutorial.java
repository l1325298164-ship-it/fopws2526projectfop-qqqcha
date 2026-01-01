package de.tum.cit.fop.maze.screen;

public class MazeGame_tutorial {
    public enum GameResult{
        SUCCESS,
        FAILURE_DEAD,
        FAILURE_LOST,
        RUNNING
    }
    private GameResult result=GameResult.RUNNING;
    //TODO 判断是否再限时 逃出去了，如果死了就是pv6，没逃出去就是pv7，成功就进入做梦pv，解锁下一章节



}
