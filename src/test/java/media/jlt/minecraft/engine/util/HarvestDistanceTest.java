package media.jlt.minecraft.engine.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarvestDistanceTest {
    @Test
    void acceptsPlayerAtBlockCenter() {
        assertTrue(HarvestDistance.isWithinRange(10.5, 64.5, 10.5, 10, 64, 10, 64));
    }

    @Test
    void includesExactDistanceBoundary() {
        assertTrue(HarvestDistance.isWithinRange(64.5, 0.5, 0.5, 0, 0, 0, 64));
    }

    @Test
    void rejectsPlayerBeyondDistanceBoundary() {
        assertFalse(HarvestDistance.isWithinRange(64.51, 0.5, 0.5, 0, 0, 0, 64));
    }

    @Test
    void usesThreeDimensionalDistance() {
        assertFalse(HarvestDistance.isWithinRange(48.5, 48.5, 0.5, 0, 0, 0, 64));
    }
}
