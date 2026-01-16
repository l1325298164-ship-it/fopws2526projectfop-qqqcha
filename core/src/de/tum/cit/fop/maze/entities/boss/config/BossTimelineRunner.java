package de.tum.cit.fop.maze.entities.boss.config;

import de.tum.cit.fop.maze.entities.boss.BossFightScreen;

public class BossTimelineRunner {

    private final BossTimeline timeline;

    public BossTimelineRunner(BossTimeline timeline) {
        this.timeline = timeline;
    }

    public void update(float time, BossFightScreen screen) {
        for (BossTimelineEvent e : timeline.events) {
            if (!e.triggered && time >= e.time) {
                e.triggered = true;
                execute(e, screen);
            }
        }
    }

    private void execute(BossTimelineEvent e, BossFightScreen s) {
        switch (e.type) {

            case "RAGE_CHECK" -> {
                s.enterRageCheck();
            }

            case "LOCK_HP" -> {
                s.handleHpThreshold(e.threshold, null);
            }

            case "GLOBAL_AOE" -> {
                s.startGlobalAoe(
                        e.duration,
                        e.tickInterval,
                        e.damage
                );
            }

            case "LOCK_FINAL_HP" -> {
                s.lockFinalHp(e.threshold);
            }

            case "CUP_SHAKE" -> {
                s.startCupShake(
                        e.duration != null ? e.duration : 0f,
                        e.xAmp != null ? e.xAmp : 0f,
                        e.yAmp != null ? e.yAmp : 0f,
                        e.xFreq != null ? e.xFreq : 1f,
                        e.yFreq != null ? e.yFreq : 1f
                );
            }

            case "DIALOGUE" -> {
                s.playBossDialogue(e.speaker, e.text, e.voice);
            }

            case "TIMELINE_END" -> {
                s.markTimelineFinished();
            }

            default -> {
                // ignore unknown
            }
        }
    }


}
