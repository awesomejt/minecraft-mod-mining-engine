package media.jlt.minecraft.engine.balance;

import java.util.Map;
import java.util.Objects;

public record BalanceSettings(
    TaxMode taxMode,
    boolean enableHungerTax,
    int hungerTaxFloor,
    int xpTaxFloor,
    int hungerResumeTimeoutTicks,
    DurabilityProtectionMode durabilityProtectionMode,
    int durabilityProtectionFloor,
    int durabilityMultiplier,
    boolean enableDurabilityXpSubstitution,
    int durabilityXpSubstitutionWindow,
    int xpPerSubstitutedDurabilityPoint,
    boolean enableTimePenalty,
    Map<Integer, Integer> delayTicksByEfficiencyLevel,
    int maxInstantBlocksPerTick,
    int maxHarvestDistance
) {
    public BalanceSettings {
        Objects.requireNonNull(taxMode, "taxMode");
        Objects.requireNonNull(durabilityProtectionMode, "durabilityProtectionMode");
        delayTicksByEfficiencyLevel = Map.copyOf(
            Objects.requireNonNull(delayTicksByEfficiencyLevel, "delayTicksByEfficiencyLevel")
        );
    }

    public int delayTicksForEfficiencyLevel(int efficiencyLevel) {
        int normalizedLevel = Math.max(0, Math.min(5, efficiencyLevel));
        Integer configured = delayTicksByEfficiencyLevel.get(normalizedLevel);
        return configured == null ? defaultDelayTicks(normalizedLevel) : configured;
    }

    public boolean isProtectionActive(boolean toolDamageable, boolean toolEnchanted) {
        if (!toolDamageable || durabilityProtectionMode == DurabilityProtectionMode.OFF) {
            return false;
        }
        if (durabilityProtectionMode == DurabilityProtectionMode.ALL) {
            return true;
        }
        return toolEnchanted;
    }

    private static int defaultDelayTicks(int efficiencyLevel) {
        return switch (efficiencyLevel) {
            case 0 -> 16;
            case 1 -> 8;
            case 2 -> 4;
            case 3 -> 2;
            case 4 -> 1;
            case 5 -> 0;
            default -> throw new IllegalArgumentException("efficiencyLevel must be between 0 and 5");
        };
    }
}
