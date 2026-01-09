package de.tum.cit.fop.maze.entities.chapter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;

import javax.swing.text.AbstractDocument;

public class ChapterRelicData {
    public String chapterId;
    public String relicId;
    public String title;
    public String backgroundImage;
    public AbstractDocument.Content content;



    Json json = new Json();
    ChapterRelicData data =
            json.fromJson(ChapterRelicData.class,
                    Gdx.files.internal("chapters/chapter1_relic.json"));
}