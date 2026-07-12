package media.jlt.minecraft.engine.schedule;

import media.jlt.minecraft.engine.balance.BalanceSettings;
import media.jlt.minecraft.engine.balance.DurabilityProtectionMode;
import media.jlt.minecraft.engine.balance.TaxMode;
import media.jlt.minecraft.engine.port.ActivePlayer;
import media.jlt.minecraft.engine.port.HarvestAdapter;
import media.jlt.minecraft.engine.port.ToolSnapshot;
import media.jlt.minecraft.engine.scan.Pos3i;
import media.jlt.minecraft.engine.util.ToolTier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarvestSchedulerSmokeTest {
    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String LEVEL = "overworld";
    private static final Object TOOL = new Object();

    @Test
    void pacedWorkRunsAtTicksAnchoredToTheLatestSchedulerTick() {
        Fixture fixture = new Fixture();
        fixture.scheduler.tick(100);
        fixture.schedule(List.of(new Pos3i(0, 64, 0), new Pos3i(1, 64, 0)), 2, 2, 0);

        fixture.scheduler.tick(101);
        assertTrue(fixture.adapter.breaks.isEmpty());

        fixture.scheduler.tick(102);
        assertEquals(List.of(new Pos3i(0, 64, 0)), fixture.adapter.positions());
        assertTrue(fixture.scheduler.hasScheduledWork(PLAYER_ID));

        fixture.scheduler.tick(103);
        assertEquals(1, fixture.adapter.breaks.size());

        fixture.scheduler.tick(104);
        assertEquals(List.of(new Pos3i(0, 64, 0), new Pos3i(1, 64, 0)), fixture.adapter.positions());
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));
    }

    @Test
    void scheduleDistributesNormalAndSubstitutedDurabilityEntries() {
        Fixture fixture = new Fixture();
        fixture.schedule(
            List.of(new Pos3i(0, 64, 0), new Pos3i(1, 64, 0), new Pos3i(2, 64, 0)),
            0,
            3,
            2
        );

        fixture.scheduler.tick(0);

        assertEquals(List.of(3, 3, 0), fixture.adapter.breaks.stream().map(BreakCall::extraDurability).toList());
        assertEquals(List.of(true, true, true), fixture.adapter.breaks.stream().map(BreakCall::applyBase).toList());
        assertEquals(91, fixture.player.totalExperience);
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));
    }

    @Test
    void clearAllDropsWorkAndResetsTheSchedulingTick() {
        Fixture fixture = new Fixture();
        fixture.scheduler.tick(100);
        fixture.schedule(List.of(new Pos3i(0, 64, 0)), 5, 0, 1);

        fixture.scheduler.clearAll();
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));

        fixture.schedule(List.of(new Pos3i(0, 64, 0)), 5, 0, 1);
        fixture.scheduler.tick(4);
        assertTrue(fixture.adapter.breaks.isEmpty());
        fixture.scheduler.tick(5);
        assertEquals(1, fixture.adapter.breaks.size());
    }

    private static final class Fixture {
        private final FakePlayer player = new FakePlayer();
        private final FakeAdapter adapter = new FakeAdapter(player);
        private final HarvestScheduler<String, Object> scheduler = new HarvestScheduler<>(adapter, Fixture::settings);

        private void schedule(List<Pos3i> positions, int delay, int extraDurability, int chargedBlocks) {
            scheduler.schedule(
                PLAYER_ID,
                LEVEL,
                positions,
                extraDurability,
                delay,
                7,
                0.4f,
                1,
                chargedBlocks,
                9,
                true,
                ToolTier.STONE,
                TOOL
            );
        }

        private static BalanceSettings settings() {
            return new BalanceSettings(
                TaxMode.HUNGER_ONLY,
                false,
                1,
                0,
                400,
                DurabilityProtectionMode.OFF,
                1,
                4,
                false,
                10,
                3,
                true,
                Map.of(0, 16, 1, 8, 2, 4, 3, 2, 4, 1, 5, 0),
                8,
                64
            );
        }
    }

    private static final class FakeAdapter implements HarvestAdapter<String, Object> {
        private final FakePlayer player;
        private final List<BreakCall> breaks = new ArrayList<>();

        private FakeAdapter(FakePlayer player) {
            this.player = player;
        }

        @Override
        public ActivePlayer<Object> resolve(UUID playerId, String level) {
            return PLAYER_ID.equals(playerId) && LEVEL.equals(level) ? player : null;
        }

        @Override
        public boolean blockMatches(String level, Pos3i pos, int matchKind) {
            return true;
        }

        @Override
        public boolean breakBlock(
            ActivePlayer<Object> activePlayer,
            int toolSlot,
            Object boundTool,
            Pos3i pos,
            boolean applyBaseDurability,
            int extraDurability
        ) {
            breaks.add(new BreakCall(pos, applyBaseDurability, extraDurability));
            return true;
        }

        @Override
        public void feedback(ActivePlayer<Object> activePlayer, HarvestFeedback reason, Object... args) {
        }

        private List<Pos3i> positions() {
            return breaks.stream().map(BreakCall::pos).toList();
        }
    }

    private static final class FakePlayer implements ActivePlayer<Object> {
        private int totalExperience = 100;

        @Override
        public UUID id() {
            return PLAYER_ID;
        }

        @Override
        public boolean isDeadOrDying() {
            return false;
        }

        @Override
        public double x() {
            return 0.5;
        }

        @Override
        public double y() {
            return 64.5;
        }

        @Override
        public double z() {
            return 0.5;
        }

        @Override
        public boolean isCreative() {
            return false;
        }

        @Override
        public int foodLevel() {
            return 20;
        }

        @Override
        public float saturation() {
            return 5f;
        }

        @Override
        public void addExhaustion(float amount) {
        }

        @Override
        public int totalExperience() {
            return totalExperience;
        }

        @Override
        public void giveExperiencePoints(int delta) {
            totalExperience += delta;
        }

        @Override
        public int findToolSlot(Object boundTool) {
            return boundTool == TOOL ? 2 : -1;
        }

        @Override
        public boolean isValidTool(Object tool, ToolTier requiredTier) {
            return tool == TOOL;
        }

        @Override
        public ToolSnapshot toolSnapshot(Object tool) {
            return new ToolSnapshot(true, false, 250, 0, false);
        }

        @Override
        public int efficiencyLevel(Object tool) {
            return 0;
        }
    }

    private record BreakCall(Pos3i pos, boolean applyBase, int extraDurability) {
    }
}
