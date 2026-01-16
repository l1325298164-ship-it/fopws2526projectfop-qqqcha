package de.tum.cit.fop.maze.entities.chapter;

import java.util.List;

public class ChapterDropData {
    public int chapterId;
    public DropRates drops;
    public List<RelicData> relics;
    public List<RelicData> loreItems;

    public static class DropRates {
        public float relicChance;
        public float loreChance;
    }
}
