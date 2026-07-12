package media.jlt.minecraft.engine.scan;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConnectedBlockScannerTest {
    private static final Pos3i ORIGIN = new Pos3i(0, 64, 0);

    @Test
    void capsTheNumberOfFoundBlocks() {
        List<Pos3i> found = ConnectedBlockScanner.scan(
            ORIGIN,
            (x, y, z) -> true,
            limits(5, 10, OptionalInt.empty())
        );

        assertEquals(5, found.size());
    }

    @Test
    void appliesEuclideanHorizontalRadiusFromOrigin() {
        Set<Pos3i> blocks = Set.of(
            ORIGIN,
            new Pos3i(1, 64, 1),
            new Pos3i(2, 64, 0),
            new Pos3i(2, 64, 1)
        );

        List<Pos3i> found = scan(blocks, limits(10, 2, OptionalInt.empty()));

        assertTrue(found.contains(new Pos3i(2, 64, 0)));
        assertFalse(found.contains(new Pos3i(2, 64, 1)));
    }

    @Test
    void enforcesPresentDepthBound() {
        Set<Pos3i> blocks = verticalColumn(60, 64);

        List<Pos3i> found = scan(blocks, limits(10, 2, OptionalInt.of(2)));

        assertEquals(Set.of(
            new Pos3i(0, 64, 0),
            new Pos3i(0, 63, 0),
            new Pos3i(0, 62, 0)
        ), Set.copyOf(found));
    }

    @Test
    void emptyDepthBoundAllowsConnectedBlocksBelowOrigin() {
        Set<Pos3i> blocks = verticalColumn(60, 64);

        List<Pos3i> found = scan(blocks, limits(10, 2, OptionalInt.empty()));

        assertEquals(blocks, Set.copyOf(found));
    }

    @Test
    void seedsOriginAndAllTwentySixNeighbors() {
        Pos3i firstDetachedNeighbor = ORIGIN.offset(-1, -1, -1);
        Pos3i oppositeDetachedNeighbor = ORIGIN.offset(1, 1, 1);
        Set<Pos3i> blocks = Set.of(ORIGIN, firstDetachedNeighbor, oppositeDetachedNeighbor);

        List<Pos3i> found = scan(blocks, limits(10, 2, OptionalInt.empty()));

        assertEquals(blocks, Set.copyOf(found));
    }

    @Test
    void neighborSeedsStillWorkWhenBrokenOriginDoesNotMatch() {
        Pos3i west = ORIGIN.offset(-1, 0, 0);
        Pos3i east = ORIGIN.offset(1, 0, 0);
        Set<Pos3i> blocks = Set.of(west, east);

        List<Pos3i> found = scan(blocks, limits(10, 2, OptionalInt.empty()));

        assertEquals(blocks, Set.copyOf(found));
    }

    @Test
    void returnsMatchesTopDown() {
        Set<Pos3i> blocks = Set.of(
            ORIGIN,
            ORIGIN.offset(0, 1, 0),
            ORIGIN.offset(0, 2, 0),
            ORIGIN.offset(0, -1, 0)
        );

        List<Pos3i> found = scan(blocks, limits(10, 2, OptionalInt.empty()));

        assertEquals(List.of(66, 65, 64, 63), found.stream().map(Pos3i::y).toList());
    }

    @Test
    void nonPositiveCapFindsNothingWithoutCallingMatcher() {
        List<Pos3i> found = ConnectedBlockScanner.scan(
            ORIGIN,
            (x, y, z) -> {
                throw new AssertionError("matcher should not be called");
            },
            limits(0, 2, OptionalInt.empty())
        );

        assertTrue(found.isEmpty());
    }

    private static List<Pos3i> scan(Set<Pos3i> blocks, ScanLimits limits) {
        return ConnectedBlockScanner.scan(
            ORIGIN,
            (x, y, z) -> blocks.contains(new Pos3i(x, y, z)),
            limits
        );
    }

    private static ScanLimits limits(int maxBlocks, int radius, OptionalInt depth) {
        return new ScanLimits(maxBlocks, radius, depth);
    }

    private static Set<Pos3i> verticalColumn(int minY, int maxY) {
        Set<Pos3i> blocks = new HashSet<>();
        for (int y = minY; y <= maxY; y++) {
            blocks.add(new Pos3i(0, y, 0));
        }
        return blocks;
    }
}
