package de.tum.cit.fop.maze.tools;

import de.tum.cit.fop.maze.audio.AudioType;
import de.tum.cit.fop.maze.screen.IntroScreen;

/**
 * One PV story node definition.
 */
public record PVNode(
        String atlasPath,
        String regionName,
        AudioType audio,
        IntroScreen.PVExit exit
) {
}
