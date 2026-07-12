package media.jlt.minecraft.engine.util;

public final class HarvestDistance {
    private HarvestDistance() {
    }

    public static boolean isWithinRange(
        double playerX,
        double playerY,
        double playerZ,
        int blockX,
        int blockY,
        int blockZ,
        int maxDistance
    ) {
        double dx = playerX - (blockX + 0.5);
        double dy = playerY - (blockY + 0.5);
        double dz = playerZ - (blockZ + 0.5);
        double distance = Math.max(0, maxDistance);
        return dx * dx + dy * dy + dz * dz <= distance * distance;
    }
}
