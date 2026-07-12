package media.jlt.minecraft.engine.balance;

public final class DurabilityBudget {
    private DurabilityBudget() {
    }

    public static boolean canSpend(int remainingDurability, int durabilityCost, int protectionFloor) {
        if (durabilityCost <= 0) {
            return true;
        }
        long remainingAfterCost = (long) remainingDurability - durabilityCost;
        return remainingAfterCost >= Math.max(0, protectionFloor);
    }
}
