package media.jlt.minecraft.engine.schedule;

public final class InstantBreakBatch {
    private InstantBreakBatch() {
    }

    public static boolean hasCapacity(int processedBlocks, int maxBlocksPerTick) {
        return Math.max(0, processedBlocks) < Math.max(1, maxBlocksPerTick);
    }
}
