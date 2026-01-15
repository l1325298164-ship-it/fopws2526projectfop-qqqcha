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
            case "RAGE_CHECK":
                s.enterRageCheck();
                break;

            case "CUP_SHAKE":
                s.setCupShakeViolent(true);
                break;

            case "LOCK_HP":
                s.handleHpThreshold(e.threshold,  e.onFail);
                break;

            case "GLOBAL_AOE":
                s.startGlobalAoe(e.duration, e.tickInterval, e.damage);
                break;

            case "LOCK_FINAL_HP":
                s.lockFinalHp(e.threshold);
                break;

            case "TIMELINE_END":
                s.markTimelineFinished();
                break;

            case "DIALOGUE":
                s.playBossDialogue(e.speaker, e.text, e.voice);
                break;

            default:
                // unknown event type, ignore
                break;
        }
    }

}
