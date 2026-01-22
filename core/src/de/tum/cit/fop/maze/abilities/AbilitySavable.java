package de.tum.cit.fop.maze.abilities;

import java.util.Map;

public interface AbilitySavable {
    Map<String, Object> saveState();
    void loadState(Map<String, Object> data);
}
