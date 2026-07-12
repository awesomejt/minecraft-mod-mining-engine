package media.jlt.minecraft.engine.scan;

public final class ScanDepthBounds {
    private ScanDepthBounds() {
    }

    public static boolean isWithinDepth(int cutY, int candidateY, int maxDepthBelowCut) {
        long minimumY = (long) cutY - Math.max(0, maxDepthBelowCut);
        return candidateY >= minimumY;
    }
}
