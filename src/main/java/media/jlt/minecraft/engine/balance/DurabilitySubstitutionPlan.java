package media.jlt.minecraft.engine.balance;

public record DurabilitySubstitutionPlan(
    int supportedBlocks,
    int substitutedBlocks,
    int xpCostPerSubstitutedBlock
) {
    public static DurabilitySubstitutionPlan create(
        int requestedBlocks,
        int spendableDurability,
        int durabilityCostPerBlock,
        boolean substitutionEnabled,
        int substitutionWindowDurability,
        int xpPerSubstitutedDurabilityPoint,
        int availableExperience
    ) {
        int requested = Math.max(0, requestedBlocks);
        int spendable = Math.max(0, spendableDurability);
        int costPerBlock = Math.max(1, durabilityCostPerBlock);
        int extraDurabilityPerBlock = costPerBlock - 1;
        int xpPerBlock = safeProduct(extraDurabilityPerBlock, Math.max(0, xpPerSubstitutedDurabilityPoint));

        int maxSubstitutedBlocks = 0;
        if (substitutionEnabled && extraDurabilityPerBlock > 0 && xpPerBlock > 0) {
            int byWindow = Math.max(0, substitutionWindowDurability) / extraDurabilityPerBlock;
            int byExperience = Math.max(0, availableExperience) / xpPerBlock;
            maxSubstitutedBlocks = Math.min(byWindow, byExperience);
        }

        for (int targetBlocks = requested; targetBlocks > 0; targetBlocks--) {
            long fullDurabilityCost = (long) targetBlocks * costPerBlock;
            long requiredSavings = Math.max(0L, fullDurabilityCost - spendable);
            if (requiredSavings == 0) {
                return new DurabilitySubstitutionPlan(targetBlocks, 0, 0);
            }
            if (extraDurabilityPerBlock <= 0) {
                continue;
            }
            int substitutionsNeeded = (int) ((requiredSavings + extraDurabilityPerBlock - 1) / extraDurabilityPerBlock);
            if (substitutionsNeeded <= targetBlocks && substitutionsNeeded <= maxSubstitutedBlocks) {
                return new DurabilitySubstitutionPlan(targetBlocks, substitutionsNeeded, xpPerBlock);
            }
        }
        return new DurabilitySubstitutionPlan(0, 0, 0);
    }

    private static int safeProduct(int left, int right) {
        return (int) Math.min(Integer.MAX_VALUE, (long) left * right);
    }
}
