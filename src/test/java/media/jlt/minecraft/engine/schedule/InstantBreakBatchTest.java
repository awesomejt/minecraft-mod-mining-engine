package media.jlt.minecraft.engine.schedule;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstantBreakBatchTest {
    @Test
    void allowsWorkBelowConfiguredLimit() {
        assertTrue(InstantBreakBatch.hasCapacity(7, 8));
    }

    @Test
    void stopsWorkAtConfiguredLimit() {
        assertFalse(InstantBreakBatch.hasCapacity(8, 8));
    }

    @Test
    void defensivelyEnforcesMinimumLimit() {
        assertTrue(InstantBreakBatch.hasCapacity(0, 0));
        assertFalse(InstantBreakBatch.hasCapacity(1, 0));
    }
}
