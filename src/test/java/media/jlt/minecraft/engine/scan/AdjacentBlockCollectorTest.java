package media.jlt.minecraft.engine.scan;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdjacentBlockCollectorTest {
    private static final Pos3i PRIMARY = new Pos3i(0, 64, 0);

    @Test
    void collectsMatchesWithinChebyshevDistanceTwo() {
        Set<Pos3i> matches = Set.of(
            PRIMARY.offset(2, 2, 2),
            PRIMARY.offset(-2, -2, -2),
            PRIMARY.offset(3, 0, 0)
        );

        List<Pos3i> found = collect(List.of(PRIMARY), matches, 10);

        assertEquals(Set.of(PRIMARY.offset(2, 2, 2), PRIMARY.offset(-2, -2, -2)), Set.copyOf(found));
    }

    @Test
    void excludesEveryPrimaryEvenWhenItMatches() {
        Pos3i secondPrimary = PRIMARY.offset(1, 0, 0);
        Pos3i adjacent = PRIMARY.offset(0, 1, 0);
        Set<Pos3i> matches = Set.of(PRIMARY, secondPrimary, adjacent);

        List<Pos3i> found = collect(List.of(PRIMARY, secondPrimary), matches, 10);

        assertEquals(List.of(adjacent), found);
    }

    @Test
    void capsUniqueCollectedBlocks() {
        List<Pos3i> found = AdjacentBlockCollector.collect(
            List.of(PRIMARY),
            (x, y, z) -> true,
            4
        );

        assertEquals(4, found.size());
        assertFalse(found.contains(PRIMARY));
    }

    @Test
    void returnsMatchesTopDown() {
        Set<Pos3i> matches = Set.of(
            PRIMARY.offset(0, -2, 0),
            PRIMARY.offset(0, 2, 0),
            PRIMARY.offset(0, 0, 1)
        );

        List<Pos3i> found = collect(List.of(PRIMARY), matches, 10);

        assertEquals(List.of(66, 64, 62), found.stream().map(Pos3i::y).toList());
    }

    @Test
    void nonPositiveCapReturnsEmptyWithoutCallingMatcher() {
        List<Pos3i> found = AdjacentBlockCollector.collect(
            List.of(PRIMARY),
            (x, y, z) -> {
                throw new AssertionError("matcher should not be called");
            },
            0
        );

        assertTrue(found.isEmpty());
    }

    private static List<Pos3i> collect(List<Pos3i> primaries, Set<Pos3i> matches, int maxBlocks) {
        return AdjacentBlockCollector.collect(
            primaries,
            (x, y, z) -> matches.contains(new Pos3i(x, y, z)),
            maxBlocks
        );
    }
}
