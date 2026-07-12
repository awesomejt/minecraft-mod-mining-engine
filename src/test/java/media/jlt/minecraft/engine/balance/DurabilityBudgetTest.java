package media.jlt.minecraft.engine.balance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurabilityBudgetTest {
    @Test
    void allowsCostThatLandsExactlyOnFloor() {
        assertTrue(DurabilityBudget.canSpend(20, 10, 10));
    }

    @Test
    void rejectsCostThatWouldCrossFloor() {
        assertFalse(DurabilityBudget.canSpend(19, 10, 10));
    }

    @Test
    void reevaluatesAgainstCurrentDurability() {
        assertTrue(DurabilityBudget.canSpend(14, 4, 10));
        assertFalse(DurabilityBudget.canSpend(13, 4, 10));
    }

    @Test
    void permitsDurabilityExemptWorkEvenBelowFloor() {
        assertTrue(DurabilityBudget.canSpend(5, 0, 10));
    }

    @Test
    void usesLongArithmeticForExtremeInputs() {
        assertFalse(DurabilityBudget.canSpend(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));
    }
}
