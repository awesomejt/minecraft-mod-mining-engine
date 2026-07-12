package media.jlt.minecraft.engine.scan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AdjacentBlockCollector {
    private AdjacentBlockCollector() {
    }

    public static List<Pos3i> collect(Collection<Pos3i> primaries, BlockMatcher matcher, int maxBlocks) {
        Set<Pos3i> primaryPositions = new HashSet<>(primaries);
        Set<Pos3i> found = new HashSet<>();
        if (maxBlocks <= 0) {
            return new ArrayList<>();
        }

        for (Pos3i primary : primaries) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -2; dy <= 2; dy++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        Pos3i candidate = primary.offset(dx, dy, dz);
                        if (primaryPositions.contains(candidate)) {
                            continue;
                        }
                        if (matcher.matches(candidate.x(), candidate.y(), candidate.z())) {
                            found.add(candidate);
                            if (found.size() >= maxBlocks) {
                                return sortByTopDown(found);
                            }
                        }
                    }
                }
            }
        }
        return sortByTopDown(found);
    }

    private static List<Pos3i> sortByTopDown(Set<Pos3i> positions) {
        List<Pos3i> sorted = new ArrayList<>(positions);
        sorted.sort(Comparator.comparingInt(Pos3i::y).reversed());
        return sorted;
    }
}
