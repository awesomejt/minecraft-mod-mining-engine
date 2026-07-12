package media.jlt.minecraft.engine.scan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ConnectedBlockScanner {
    private ConnectedBlockScanner() {
    }

    public static List<Pos3i> scan(Pos3i origin, BlockMatcher matcher, ScanLimits limits) {
        Set<Pos3i> found = new HashSet<>();
        Set<Pos3i> visited = new HashSet<>();
        ArrayDeque<Pos3i> queue = new ArrayDeque<>();
        queue.add(origin);

        // AFTER break events usually see the origin as air, so seed nearby candidates.
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    queue.add(origin.offset(dx, dy, dz));
                }
            }
        }

        while (!queue.isEmpty() && found.size() < limits.maxBlocks()) {
            Pos3i current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            if (!isWithinHorizontalRadius(origin, current, limits.maxHorizontalRadius())) {
                continue;
            }
            if (limits.maxDepthBelowOrigin().isPresent()
                && !ScanDepthBounds.isWithinDepth(
                    origin.y(),
                    current.y(),
                    limits.maxDepthBelowOrigin().getAsInt()
                )) {
                continue;
            }
            if (!matcher.matches(current.x(), current.y(), current.z())) {
                continue;
            }
            found.add(current);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Pos3i neighbor = current.offset(dx, dy, dz);
                        if (!visited.contains(neighbor)) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }

        return sortByTopDown(found);
    }

    private static boolean isWithinHorizontalRadius(Pos3i center, Pos3i check, int radius) {
        long dx = (long) check.x() - center.x();
        long dz = (long) check.z() - center.z();
        long squaredDistance = dx * dx + dz * dz;
        long maxSquaredDistance = (long) radius * radius;
        return squaredDistance <= maxSquaredDistance;
    }

    private static List<Pos3i> sortByTopDown(Set<Pos3i> positions) {
        List<Pos3i> sorted = new ArrayList<>(positions);
        sorted.sort(Comparator.comparingInt(Pos3i::y).reversed());
        return sorted;
    }
}
