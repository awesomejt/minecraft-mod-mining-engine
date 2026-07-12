package media.jlt.minecraft.engine.balance;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarvestPlannerTest {
    @Test
    void creativePlayerBypassesDurabilityPlanning() {
        HarvestPlanner.PlanResult result = plan(10, true, true, 0, true, 4, settings(false), 0);

        assertEquals(HarvestPlanner.Outcome.PROCEED, result.outcome());
        assertEquals(10, result.blocksSupported());
        assertEquals(Integer.MAX_VALUE, result.predictedRemainingDurability());
        assertEquals(0, result.requiredDurability());
        assertFalse(result.trimmed());
    }

    @Test
    void undamageableToolBypassesDurabilityPlanning() {
        HarvestPlanner.PlanResult result = plan(10, false, false, 0, true, 4, settings(false), 0);

        assertEquals(HarvestPlanner.Outcome.PROCEED, result.outcome());
        assertEquals(10, result.blocksSupported());
        assertEquals(Integer.MAX_VALUE, result.predictedRemainingDurability());
    }

    @Test
    void protectionBlocksWorkAtDurabilityFloor() {
        HarvestPlanner.PlanResult result = plan(1, false, true, 1, true, 4, settings(false), 0);

        assertEquals(HarvestPlanner.Outcome.BLOCKED_DURABILITY_FLOOR, result.outcome());
        assertEquals(0, result.blocksSupported());
        assertEquals(1, result.predictedRemainingDurability());
        assertFalse(result.trimmed());
    }

    @Test
    void protectionTrimsWorkToDurabilityAboveFloor() {
        HarvestPlanner.PlanResult result = plan(10, false, true, 11, true, 4, settings(false), 0);

        assertEquals(HarvestPlanner.Outcome.PROCEED, result.outcome());
        assertEquals(2, result.blocksSupported());
        assertTrue(result.trimmed());
        assertEquals(0, result.substitutedBlocks());
        assertEquals(3, result.predictedRemainingDurability());
    }

    @Test
    void protectionPlansSubstitutionsAndPredictsBaseDurabilityCost() {
        HarvestPlanner.PlanResult result = plan(10, false, true, 11, true, 4, settings(true), 90);

        assertEquals(HarvestPlanner.Outcome.PROCEED, result.outcome());
        assertEquals(10, result.blocksSupported());
        assertFalse(result.trimmed());
        assertEquals(10, result.substitutedBlocks());
        assertEquals(9, result.xpCostPerSubstitutedBlock());
        assertEquals(1, result.predictedRemainingDurability());
    }

    @Test
    void protectionCanMixNormalAndSubstitutedBlocks() {
        HarvestPlanner.PlanResult result = plan(10, false, true, 26, true, 4, settings(true), 45);

        assertEquals(HarvestPlanner.Outcome.PROCEED, result.outcome());
        assertEquals(10, result.blocksSupported());
        assertEquals(5, result.substitutedBlocks());
        assertEquals(9, result.xpCostPerSubstitutedBlock());
        assertEquals(1, result.predictedRemainingDurability());
    }

    @Test
    void unprotectedWorkIsBlockedWhenTotalDurabilityIsInsufficient() {
        HarvestPlanner.PlanResult result = plan(10, false, true, 39, false, 4, settings(false), 0);

        assertEquals(HarvestPlanner.Outcome.BLOCKED_INSUFFICIENT_DURABILITY, result.outcome());
        assertEquals(0, result.blocksSupported());
        assertEquals(40, result.requiredDurability());
        assertEquals(39, result.predictedRemainingDurability());
    }

    @Test
    void unprotectedWorkPredictsRemainingDurability() {
        HarvestPlanner.PlanResult result = plan(10, false, true, 50, false, 4, settings(false), 0);

        assertEquals(HarvestPlanner.Outcome.PROCEED, result.outcome());
        assertEquals(10, result.blocksSupported());
        assertEquals(40, result.requiredDurability());
        assertEquals(10, result.predictedRemainingDurability());
    }

    @Test
    void defensivelyNormalizesRequestedBlocksAndPerBlockCost() {
        HarvestPlanner.PlanResult noBlocks = plan(-1, false, true, 10, false, 4, settings(false), 0);
        HarvestPlanner.PlanResult minimumCost = plan(2, false, true, 2, false, 0, settings(false), 0);

        assertEquals(0, noBlocks.blocksSupported());
        assertEquals(10, noBlocks.predictedRemainingDurability());
        assertEquals(2, minimumCost.requiredDurability());
        assertEquals(0, minimumCost.predictedRemainingDurability());
    }

    private static HarvestPlanner.PlanResult plan(
        int requestedBlocks,
        boolean creative,
        boolean toolDamageable,
        int remainingDurability,
        boolean protectionActive,
        int costPerBlock,
        BalanceSettings settings,
        int availableXp
    ) {
        return HarvestPlanner.plan(
            requestedBlocks,
            creative,
            toolDamageable,
            remainingDurability,
            protectionActive,
            costPerBlock,
            settings,
            availableXp
        );
    }

    private static BalanceSettings settings(boolean substitutionEnabled) {
        return new BalanceSettings(
            TaxMode.HUNGER_ONLY,
            true,
            1,
            0,
            400,
            DurabilityProtectionMode.ALL,
            1,
            4,
            substitutionEnabled,
            30,
            3,
            true,
            Map.of(),
            8,
            64
        );
    }
}
