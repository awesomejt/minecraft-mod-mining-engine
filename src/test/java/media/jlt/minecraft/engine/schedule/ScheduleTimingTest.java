package media.jlt.minecraft.engine.schedule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ScheduleTimingTest {
    @Test
    void restoresFullSpacingForRemainingQueue() {
        long resumeTick = 1_000;

        assertEquals(1_006, ScheduleTiming.resumeTick(resumeTick, 6, 1));
        assertEquals(1_180, ScheduleTiming.resumeTick(resumeTick, 6, 30));
    }

    @Test
    void preservesZeroDelayMode() {
        assertEquals(1_000, ScheduleTiming.resumeTick(1_000, 0, 30));
    }

    @Test
    void rejectsInvalidQueuePositions() {
        assertThrows(IllegalArgumentException.class, () -> ScheduleTiming.resumeTick(1_000, 6, 0));
    }

    @Test
    void saturatesOnTickOverflow() {
        assertEquals(Long.MAX_VALUE, ScheduleTiming.resumeTick(Long.MAX_VALUE - 1, 6, 30));
    }
}
