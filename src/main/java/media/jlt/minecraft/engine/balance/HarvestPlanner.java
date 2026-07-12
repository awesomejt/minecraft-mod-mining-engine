package media.jlt.minecraft.engine.balance;

import java.util.Objects;

public final class HarvestPlanner {
    private HarvestPlanner() {
    }

    public static PlanResult plan(
        int requestedBlocks,
        boolean creative,
        boolean toolDamageable,
        int remainingDurability,
        boolean protectionActive,
        int durabilityCostPerBlock,
        BalanceSettings settings,
        int availableXpAboveFloor
    ) {
        Objects.requireNonNull(settings, "settings");
        int requested = Math.max(0, requestedBlocks);
        int costPerBlock = Math.max(1, durabilityCostPerBlock);

        if (creative || !toolDamageable) {
            return new PlanResult(
                Outcome.PROCEED,
                requested,
                false,
                0,
                0,
                Integer.MAX_VALUE,
                0
            );
        }

        if (protectionActive) {
            int spendable = Math.max(0, remainingDurability - settings.durabilityProtectionFloor());
            DurabilitySubstitutionPlan substitution = DurabilitySubstitutionPlan.create(
                requested,
                spendable,
                costPerBlock,
                settings.enableDurabilityXpSubstitution(),
                settings.durabilityXpSubstitutionWindow(),
                settings.xpPerSubstitutedDurabilityPoint(),
                availableXpAboveFloor
            );
            int supported = substitution.supportedBlocks();
            if (supported <= 0) {
                return new PlanResult(
                    Outcome.BLOCKED_DURABILITY_FLOOR,
                    0,
                    false,
                    0,
                    0,
                    remainingDurability,
                    0
                );
            }

            int substituted = substitution.substitutedBlocks();
            int nonSubstituted = Math.max(0, supported - substituted);
            int predicted = remainingDurability
                - (nonSubstituted * costPerBlock)
                - substituted;
            return new PlanResult(
                Outcome.PROCEED,
                supported,
                supported < requested,
                substituted,
                substitution.xpCostPerSubstitutedBlock(),
                predicted,
                0
            );
        }

        int required = requested * costPerBlock;
        if (remainingDurability < required) {
            return new PlanResult(
                Outcome.BLOCKED_INSUFFICIENT_DURABILITY,
                0,
                false,
                0,
                0,
                remainingDurability,
                required
            );
        }
        return new PlanResult(
            Outcome.PROCEED,
            requested,
            false,
            0,
            0,
            remainingDurability - required,
            required
        );
    }

    public enum Outcome {
        PROCEED,
        BLOCKED_DURABILITY_FLOOR,
        BLOCKED_INSUFFICIENT_DURABILITY
    }

    public record PlanResult(
        Outcome outcome,
        int blocksSupported,
        boolean trimmed,
        int substitutedBlocks,
        int xpCostPerSubstitutedBlock,
        int predictedRemainingDurability,
        int requiredDurability
    ) {
        public PlanResult {
            Objects.requireNonNull(outcome, "outcome");
        }
    }
}
