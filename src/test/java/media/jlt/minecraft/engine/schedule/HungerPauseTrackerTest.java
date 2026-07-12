package media.jlt.minecraft.engine.schedule;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HungerPauseTrackerTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void preservesOriginalDeadlineWhileWorkRemains() {
        HungerPauseTracker tracker = new HungerPauseTracker();
        tracker.markIfAbsent(PLAYER, 400);
        tracker.markIfAbsent(PLAYER, 800);
        tracker.removeOrphans(Set.of(PLAYER));

        assertTrue(tracker.isPaused(PLAYER));
        assertEquals(400, tracker.deadline(PLAYER));
    }

    @Test
    void removesDeadlineWhenFinalScheduledWorkIsGone() {
        HungerPauseTracker tracker = new HungerPauseTracker();
        tracker.markIfAbsent(PLAYER, 400);

        tracker.removeOrphans(Set.of());

        assertFalse(tracker.isPaused(PLAYER));
        assertThrows(IllegalStateException.class, () -> tracker.deadline(PLAYER));
    }

    @Test
    void newJobReceivesFreshTimeoutAfterClearingStaleState() {
        HungerPauseTracker tracker = new HungerPauseTracker();
        tracker.markIfAbsent(PLAYER, 400);

        tracker.clear(PLAYER);
        tracker.markIfAbsent(PLAYER, 1_200);

        assertEquals(1_200, tracker.deadline(PLAYER));
    }

    @Test
    void clearAllRemovesEveryPlayersDeadline() {
        HungerPauseTracker tracker = new HungerPauseTracker();
        UUID secondPlayer = UUID.fromString("00000000-0000-0000-0000-000000000002");
        tracker.markIfAbsent(PLAYER, 400);
        tracker.markIfAbsent(secondPlayer, 800);

        tracker.clearAll();

        assertFalse(tracker.isPaused(PLAYER));
        assertFalse(tracker.isPaused(secondPlayer));
    }
}
