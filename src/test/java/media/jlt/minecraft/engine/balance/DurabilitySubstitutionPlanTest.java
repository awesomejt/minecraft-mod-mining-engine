package media.jlt.minecraft.engine.balance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurabilitySubstitutionPlanTest {
    @Test
    void needsNoSubstitutionWhenDurabilitySupportsEveryBlock() {
        DurabilitySubstitutionPlan plan = DurabilitySubstitutionPlan.create(10, 40, 4, true, 30, 3, 100);

        assertEquals(new DurabilitySubstitutionPlan(10, 0, 0), plan);
    }

    @Test
    void substitutesOnlyExtraDurabilityAndRetainsBaseCost() {
        DurabilitySubstitutionPlan plan = DurabilitySubstitutionPlan.create(10, 10, 4, true, 30, 3, 90);

        assertEquals(new DurabilitySubstitutionPlan(10, 10, 9), plan);
    }

    @Test
    void limitsSupportedBlocksByAvailableExperience() {
        DurabilitySubstitutionPlan plan = DurabilitySubstitutionPlan.create(10, 10, 4, true, 30, 3, 45);

        assertEquals(new DurabilitySubstitutionPlan(6, 5, 9), plan);
    }

    @Test
    void limitsSupportedBlocksBySubstitutionWindow() {
        DurabilitySubstitutionPlan plan = DurabilitySubstitutionPlan.create(10, 10, 4, true, 6, 3, 100);

        assertEquals(new DurabilitySubstitutionPlan(4, 2, 9), plan);
    }

    @Test
    void fallsBackToNormalDurabilityWhenSubstitutionDisabled() {
        DurabilitySubstitutionPlan plan = DurabilitySubstitutionPlan.create(10, 10, 4, false, 30, 3, 100);

        assertEquals(new DurabilitySubstitutionPlan(2, 0, 0), plan);
    }

    @Test
    void zeroXpPriceDoesNotCreateFreeSubstitution() {
        DurabilitySubstitutionPlan plan = DurabilitySubstitutionPlan.create(10, 10, 4, true, 30, 0, 100);

        assertEquals(new DurabilitySubstitutionPlan(2, 0, 0), plan);
    }

    @Test
    void cannotSubstituteVanillaBaseDurability() {
        DurabilitySubstitutionPlan plan = DurabilitySubstitutionPlan.create(10, 0, 4, true, 30, 3, 100);

        assertEquals(new DurabilitySubstitutionPlan(0, 0, 0), plan);
    }

    @Test
    void substitutionStartsAtExactWindowAndExperienceBoundaries() {
        DurabilitySubstitutionPlan belowWindow = DurabilitySubstitutionPlan.create(1, 1, 4, true, 2, 3, 9);
        DurabilitySubstitutionPlan belowExperience = DurabilitySubstitutionPlan.create(1, 1, 4, true, 3, 3, 8);
        DurabilitySubstitutionPlan exactBoundary = DurabilitySubstitutionPlan.create(1, 1, 4, true, 3, 3, 9);

        assertEquals(new DurabilitySubstitutionPlan(0, 0, 0), belowWindow);
        assertEquals(new DurabilitySubstitutionPlan(0, 0, 0), belowExperience);
        assertEquals(new DurabilitySubstitutionPlan(1, 1, 9), exactBoundary);
    }

    @Test
    void xpCostSaturatesInsteadOfOverflowing() {
        DurabilitySubstitutionPlan plan = DurabilitySubstitutionPlan.create(
            1,
            1,
            Integer.MAX_VALUE,
            true,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE,
            Integer.MAX_VALUE
        );

        assertEquals(new DurabilitySubstitutionPlan(1, 1, Integer.MAX_VALUE), plan);
    }

    @Test
    void negativeInputsCannotCreateBlocksOrResources() {
        DurabilitySubstitutionPlan plan = DurabilitySubstitutionPlan.create(-1, -1, -1, true, -1, -1, -1);

        assertEquals(new DurabilitySubstitutionPlan(0, 0, 0), plan);
    }
}
