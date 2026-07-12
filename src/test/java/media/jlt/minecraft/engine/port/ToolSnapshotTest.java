package media.jlt.minecraft.engine.port;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ToolSnapshotTest {
    @Test
    void calculatesRemainingDurabilityFromCapturedValues() {
        ToolSnapshot snapshot = new ToolSnapshot(true, true, 1_561, 61, false);

        assertEquals(1_500, snapshot.remainingDurability());
    }

    @Test
    void preservesSourceArithmeticForBrokenToolBoundary() {
        ToolSnapshot snapshot = new ToolSnapshot(true, false, 250, 250, true);

        assertEquals(0, snapshot.remainingDurability());
    }
}
