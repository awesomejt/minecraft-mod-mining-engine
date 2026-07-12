package media.jlt.minecraft.engine.scan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScanDepthBoundsTest {
    @Test
    void includesConfiguredDepthBoundary() {
        assertTrue(ScanDepthBounds.isWithinDepth(64, 62, 2));
    }

    @Test
    void rejectsLogsBelowConfiguredDepth() {
        assertFalse(ScanDepthBounds.isWithinDepth(64, 61, 2));
    }

    @Test
    void zeroDepthRejectsEveryLogBelowCut() {
        assertTrue(ScanDepthBounds.isWithinDepth(64, 64, 0));
        assertFalse(ScanDepthBounds.isWithinDepth(64, 63, 0));
    }

    @Test
    void handlesWorldHeightExtremesWithoutOverflow() {
        assertTrue(ScanDepthBounds.isWithinDepth(Integer.MIN_VALUE, Integer.MIN_VALUE, 16));
    }
}
