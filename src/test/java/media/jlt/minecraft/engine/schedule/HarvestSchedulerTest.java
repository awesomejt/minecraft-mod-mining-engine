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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarvestSchedulerTest {
    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String LEVEL = "overworld";

    @Test
    void instantBatchSpillsExcessWorkAcrossTicks() {
        Fixture fixture = new Fixture();
        fixture.settings.maxInstantBlocksPerTick = 2;
        fixture.schedule(5, 0);

        fixture.scheduler.tick(0);
        assertEquals(2, fixture.adapter.breaks.size());
        assertTrue(fixture.scheduler.hasScheduledWork(PLAYER_ID));

        fixture.scheduler.tick(1);
        assertEquals(4, fixture.adapter.breaks.size());

        fixture.scheduler.tick(2);
        assertEquals(5, fixture.adapter.breaks.size());
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));
    }

    @Test
    void hungerPauseResumesWithAllEntriesRestaggeredInQueueOrder() {
        Fixture fixture = new Fixture();
        fixture.settings.taxMode = TaxMode.HUNGER_ONLY;
        fixture.settings.hungerResumeTimeoutTicks = 100;
        fixture.player.foodLevel = 0;
        fixture.player.saturation = 0f;
        fixture.player.efficiencyLevel = 2;
        fixture.schedule(3, 0);

        fixture.scheduler.tick(0);
        assertTrue(fixture.adapter.breaks.isEmpty());
        assertEquals(1, fixture.adapter.feedbackCount(HarvestFeedback.HUNGER_PAUSED));

        fixture.player.foodLevel = 20;
        fixture.scheduler.tick(20);
        assertTrue(fixture.adapter.breaks.isEmpty());
        assertEquals(1, fixture.adapter.feedbackCount(HarvestFeedback.HUNGER_RESUMED));

        fixture.scheduler.tick(24);
        fixture.scheduler.tick(28);
        fixture.scheduler.tick(32);

        assertEquals(positions(3), fixture.adapter.breakPositions());
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));
    }

    @Test
    void hungerPauseTimesOutOnlyWhenDeferredEntryBecomesDue() {
        Fixture fixture = new Fixture();
        fixture.settings.taxMode = TaxMode.HUNGER_ONLY;
        fixture.settings.hungerResumeTimeoutTicks = 20;
        fixture.player.foodLevel = 0;
        fixture.player.saturation = 0f;
        fixture.schedule(1, 0);

        fixture.scheduler.tick(0);
        fixture.scheduler.tick(20);
        fixture.scheduler.tick(21);
        assertTrue(fixture.scheduler.hasScheduledWork(PLAYER_ID));
        assertEquals(0, fixture.adapter.feedbackCount(HarvestFeedback.CANCELLED_HUNGER_TIMEOUT));

        fixture.scheduler.tick(40);
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));
        assertEquals(1, fixture.adapter.feedbackCount(HarvestFeedback.CANCELLED_HUNGER_TIMEOUT));
    }

    @Test
    void durabilityFloorFailureCascadesAfterAnEarlierBreak() {
        Fixture fixture = new Fixture();
        fixture.settings.durabilityProtectionMode = DurabilityProtectionMode.ALL;
        fixture.settings.durabilityProtectionFloor = 1;
        fixture.adapter.afterBreak = () -> fixture.tool.damage = 249;
        fixture.schedule(3, 0);

        fixture.scheduler.tick(0);

        assertEquals(1, fixture.adapter.breaks.size());
        assertEquals(1, fixture.adapter.feedbackCount(HarvestFeedback.DURABILITY_FLOOR));
        assertEquals(List.of(1), fixture.adapter.feedbackArgs(HarvestFeedback.DURABILITY_FLOOR));
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));
    }

    @Test
    void missingToolCancelsTheWholePlayerQueue() {
        Fixture fixture = new Fixture();
        fixture.player.toolSlot = -1;
        fixture.schedule(3, 0);

        fixture.scheduler.tick(0);

        assertTrue(fixture.adapter.breaks.isEmpty());
        assertEquals(1, fixture.adapter.feedbackCount(HarvestFeedback.CANCELLED_TOOL_MISSING));
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));
    }

    @Test
    void invalidToolCancelsTheWholePlayerQueue() {
        Fixture fixture = new Fixture();
        fixture.player.toolValid = false;
        fixture.schedule(3, 0);

        fixture.scheduler.tick(0);

        assertTrue(fixture.adapter.breaks.isEmpty());
        assertEquals(1, fixture.adapter.feedbackCount(HarvestFeedback.CANCELLED_TOOL_MISSING));
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));
    }

    @Test
    void brokenToolCancelsEntriesAfterTheSuccessfulBreak() {
        Fixture fixture = new Fixture();
        fixture.adapter.afterBreak = () -> fixture.tool.empty = true;
        fixture.schedule(3, 0);

        fixture.scheduler.tick(0);

        assertEquals(1, fixture.adapter.breaks.size());
        assertEquals(1, fixture.adapter.feedbackCount(HarvestFeedback.TOOL_BROKE));
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));
    }

    @Test
    void deathCancelsTheWholePlayerQueueBeforeBreaking() {
        Fixture fixture = new Fixture();
        fixture.player.dead = true;
        fixture.schedule(3, 0);

        fixture.scheduler.tick(0);

        assertTrue(fixture.adapter.breaks.isEmpty());
        assertEquals(1, fixture.adapter.feedbackCount(HarvestFeedback.CANCELLED_DEATH));
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));
    }

    @Test
    void distanceCancelsTheWholePlayerQueueBeforeBreaking() {
        Fixture fixture = new Fixture();
        fixture.settings.maxHarvestDistance = 8;
        fixture.player.x = 100.5;
        fixture.schedule(3, 0);

        fixture.scheduler.tick(0);

        assertTrue(fixture.adapter.breaks.isEmpty());
        assertEquals(1, fixture.adapter.feedbackCount(HarvestFeedback.CANCELLED_DISTANCE));
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));
    }

    @Test
    void offlinePlayerWorkAndPauseStateAreCleanedUp() {
        Fixture fixture = new Fixture();
        fixture.settings.taxMode = TaxMode.HUNGER_ONLY;
        fixture.player.foodLevel = 0;
        fixture.player.saturation = 0f;
        fixture.schedule(1, 0);
        fixture.scheduler.tick(0);

        fixture.adapter.online = false;
        fixture.scheduler.tick(20);
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));

        fixture.adapter.online = true;
        fixture.player.foodLevel = 20;
        fixture.schedule(1, 0);
        fixture.scheduler.tick(20);

        assertEquals(1, fixture.adapter.breaks.size());
        assertEquals(0, fixture.adapter.feedbackCount(HarvestFeedback.HUNGER_RESUMED));
    }

    @Test
    void pauseBecomesOrphanedAndIsRemovedAfterFinalEntry() {
        Fixture fixture = new Fixture();
        fixture.settings.taxMode = TaxMode.HUNGER_ONLY;
        fixture.settings.hungerTaxFloor = 0;
        fixture.player.depleteOnExhaustion = true;
        fixture.schedule(1, 0);

        fixture.scheduler.tick(0);
        assertEquals(1, fixture.adapter.breaks.size());
        assertEquals(1, fixture.adapter.feedbackCount(HarvestFeedback.HUNGER_PAUSED));

        fixture.player.depleteOnExhaustion = false;
        fixture.player.foodLevel = 20;
        fixture.schedule(1, 0);
        fixture.scheduler.tick(0);

        assertEquals(2, fixture.adapter.breaks.size());
        assertEquals(0, fixture.adapter.feedbackCount(HarvestFeedback.HUNGER_RESUMED));
    }

    @Test
    void explicitPauseClearLetsANewlyFedJobContinueWithoutResumeRestagger() {
        Fixture fixture = new Fixture();
        fixture.settings.taxMode = TaxMode.HUNGER_ONLY;
        fixture.player.foodLevel = 0;
        fixture.player.saturation = 0f;
        fixture.schedule(1, 0);
        fixture.scheduler.tick(0);

        fixture.scheduler.clearHungerPause(PLAYER_ID);
        fixture.player.foodLevel = 20;
        fixture.scheduler.tick(20);

        assertEquals(1, fixture.adapter.breaks.size());
        assertEquals(0, fixture.adapter.feedbackCount(HarvestFeedback.HUNGER_RESUMED));
    }

    @Test
    void xpOnlyModeChargesXpWithoutApplyingHungerTax() {
        Fixture fixture = new Fixture();
        fixture.settings.taxMode = TaxMode.XP_ONLY;
        fixture.schedule(1, 0, 0.4f, 7, 0);

        fixture.scheduler.tick(0);

        assertEquals(93, fixture.player.totalExperience);
        assertEquals(0f, fixture.player.exhaustionAdded);
    }

    @Test
    void xpOnlyFailureStopsRemainingWorkAfterTheSuccessfulBreak() {
        Fixture fixture = new Fixture();
        fixture.settings.taxMode = TaxMode.XP_ONLY;
        fixture.settings.xpTaxFloor = 95;
        fixture.schedule(3, 1, 0.4f, 7, 0);

        fixture.scheduler.tick(1);

        assertEquals(1, fixture.adapter.breaks.size());
        assertEquals(100, fixture.player.totalExperience);
        assertEquals(1, fixture.adapter.feedbackCount(HarvestFeedback.NO_XP));
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));
    }

    @Test
    void xpAfterHungerModeUsesHungerWhileItCanContinue() {
        Fixture fixture = new Fixture();
        fixture.settings.taxMode = TaxMode.XP_AFTER_HUNGER_DEPLETED;
        fixture.schedule(1, 0, 0.4f, 7, 0);

        fixture.scheduler.tick(0);

        assertEquals(0.4f, fixture.player.exhaustionAdded);
        assertEquals(100, fixture.player.totalExperience);
    }

    @Test
    void xpAfterHungerModeFallsBackToXpWhenHungerCannotContinue() {
        Fixture fixture = new Fixture();
        fixture.settings.taxMode = TaxMode.XP_AFTER_HUNGER_DEPLETED;
        fixture.player.foodLevel = 0;
        fixture.player.saturation = 0f;
        fixture.schedule(1, 0, 0.4f, 7, 0);

        fixture.scheduler.tick(0);

        assertEquals(0f, fixture.player.exhaustionAdded);
        assertEquals(93, fixture.player.totalExperience);
        assertEquals(1, fixture.adapter.breaks.size());
    }

    @Test
    void hungerOnlyModePausesRemainingWorkWhenTaxDepletesHunger() {
        Fixture fixture = new Fixture();
        fixture.settings.taxMode = TaxMode.HUNGER_ONLY;
        fixture.settings.hungerTaxFloor = 0;
        fixture.player.depleteOnExhaustion = true;
        fixture.schedule(2, 0);

        fixture.scheduler.tick(0);

        assertEquals(1, fixture.adapter.breaks.size());
        assertTrue(fixture.scheduler.hasScheduledWork(PLAYER_ID));
        assertEquals(1, fixture.adapter.feedbackCount(HarvestFeedback.HUNGER_PAUSED));
    }

    @Test
    void hungerFloorPausesWithoutAddingExhaustion() {
        Fixture fixture = new Fixture();
        fixture.settings.taxMode = TaxMode.HUNGER_ONLY;
        fixture.settings.hungerTaxFloor = 1;
        fixture.player.foodLevel = 1;
        fixture.player.saturation = 0f;
        fixture.schedule(2, 0);

        fixture.scheduler.tick(0);

        assertEquals(1, fixture.adapter.breaks.size());
        assertEquals(0f, fixture.player.exhaustionAdded);
        assertTrue(fixture.scheduler.hasScheduledWork(PLAYER_ID));
        assertEquals(1, fixture.adapter.feedbackCount(HarvestFeedback.HUNGER_PAUSED));
    }

    @Test
    void substitutionXpFailurePreventsBreakAndRemovesQueuedWork() {
        Fixture fixture = new Fixture();
        fixture.settings.xpTaxFloor = 100;
        fixture.schedule(2, 0, 0f, 0, 9);

        fixture.scheduler.tick(0);

        assertTrue(fixture.adapter.breaks.isEmpty());
        assertEquals(2, fixture.adapter.feedbackCount(HarvestFeedback.NO_SUBSTITUTION_XP));
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));
    }

    @Test
    void staleBlockIsRemovedSilentlyWithoutCallingBreak() {
        Fixture fixture = new Fixture();
        fixture.adapter.blockMatches = false;
        fixture.schedule(1, 0);

        fixture.scheduler.tick(0);

        assertTrue(fixture.adapter.breaks.isEmpty());
        assertTrue(fixture.adapter.feedback.isEmpty());
        assertFalse(fixture.scheduler.hasScheduledWork(PLAYER_ID));
    }

    @Test
    void failedBreakIsRemovedWithoutApplyingTaxes() {
        Fixture fixture = new Fixture();
        fixture.adapter.breakSucceeds = false;
        fixture.settings.taxMode = TaxMode.XP_ONLY;
        fixture.schedule(1, 0, 0.4f, 7, 0);

        fixture.scheduler.tick(0);

        assertEquals(1, fixture.adapter.breakAttempts);
        assertTrue(fixture.adapter.breaks.isEmpty());
        assertEquals(100, fixture.player.totalExperience);
        assertEquals(0f, fixture.player.exhaustionAdded);
    }

    private static List<Pos3i> positions(int count) {
        List<Pos3i> positions = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            positions.add(new Pos3i(index, 64, 0));
        }
        return positions;
    }

    private static final class Fixture {
        private final SettingsSpec settings = new SettingsSpec();
        private final ToolState tool = new ToolState();
        private final FakePlayer player = new FakePlayer(tool);
        private final FakeAdapter adapter = new FakeAdapter(player);
        private final HarvestScheduler<String, ToolState> scheduler = new HarvestScheduler<>(adapter, settings::build);

        private void schedule(int count, int delay) {
            schedule(count, delay, 0.4f, 1, 0);
        }

        private void schedule(int count, int delay, float exhaustion, int xpCost, int substitutionXpCost) {
            List<Pos3i> positions = positions(count);
            scheduler.schedule(
                PLAYER_ID,
                LEVEL,
                positions,
                0,
                delay,
                7,
                exhaustion,
                xpCost,
                substitutionXpCost > 0 ? 0 : positions.size(),
                substitutionXpCost,
                true,
                ToolTier.STONE,
                tool
            );
        }
    }

    private static final class SettingsSpec {
        private TaxMode taxMode = TaxMode.HUNGER_ONLY;
        private boolean enableHungerTax = true;
        private int hungerTaxFloor = 1;
        private int xpTaxFloor;
        private int hungerResumeTimeoutTicks = 400;
        private DurabilityProtectionMode durabilityProtectionMode = DurabilityProtectionMode.OFF;
        private int durabilityProtectionFloor = 1;
        private boolean enableTimePenalty = true;
        private int maxInstantBlocksPerTick = 8;
        private int maxHarvestDistance = 64;

        private BalanceSettings build() {
            return new BalanceSettings(
                taxMode,
                enableHungerTax,
                hungerTaxFloor,
                xpTaxFloor,
                hungerResumeTimeoutTicks,
                durabilityProtectionMode,
                durabilityProtectionFloor,
                4,
                false,
                10,
                3,
                enableTimePenalty,
                Map.of(0, 16, 1, 8, 2, 4, 3, 2, 4, 1, 5, 0),
                maxInstantBlocksPerTick,
                maxHarvestDistance
            );
        }
    }

    private static final class ToolState {
        private boolean damageable = true;
        private boolean enchanted;
        private int maxDamage = 250;
        private int damage;
        private boolean empty;
    }

    private static final class FakePlayer implements ActivePlayer<ToolState> {
        private final ToolState tool;
        private boolean dead;
        private boolean creative;
        private double x = 0.5;
        private double y = 64.5;
        private double z = 0.5;
        private int foodLevel = 20;
        private float saturation = 5f;
        private float exhaustionAdded;
        private boolean depleteOnExhaustion;
        private int totalExperience = 100;
        private int toolSlot = 2;
        private boolean toolValid = true;
        private int efficiencyLevel;

        private FakePlayer(ToolState tool) {
            this.tool = tool;
        }

        @Override
        public UUID id() {
            return PLAYER_ID;
        }

        @Override
        public boolean isDeadOrDying() {
            return dead;
        }

        @Override
        public double x() {
            return x;
        }

        @Override
        public double y() {
            return y;
        }

        @Override
        public double z() {
            return z;
        }

        @Override
        public boolean isCreative() {
            return creative;
        }

        @Override
        public int foodLevel() {
            return foodLevel;
        }

        @Override
        public float saturation() {
            return saturation;
        }

        @Override
        public void addExhaustion(float amount) {
            exhaustionAdded += amount;
            if (depleteOnExhaustion) {
                foodLevel = 0;
                saturation = 0f;
            }
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
        public int findToolSlot(ToolState boundTool) {
            return boundTool == tool ? toolSlot : -1;
        }

        @Override
        public boolean isValidTool(ToolState candidate, ToolTier requiredTier) {
            return candidate == tool && toolValid;
        }

        @Override
        public ToolSnapshot toolSnapshot(ToolState candidate) {
            return new ToolSnapshot(
                candidate.damageable,
                candidate.enchanted,
                candidate.maxDamage,
                candidate.damage,
                candidate.empty
            );
        }

        @Override
        public int efficiencyLevel(ToolState candidate) {
            return efficiencyLevel;
        }
    }

    private static final class FakeAdapter implements HarvestAdapter<String, ToolState> {
        private final FakePlayer player;
        private boolean online = true;
        private boolean blockMatches = true;
        private boolean breakSucceeds = true;
        private int breakAttempts;
        private Runnable afterBreak = () -> { };
        private final List<BreakCall> breaks = new ArrayList<>();
        private final List<FeedbackCall> feedback = new ArrayList<>();

        private FakeAdapter(FakePlayer player) {
            this.player = player;
        }

        @Override
        public ActivePlayer<ToolState> resolve(UUID playerId, String level) {
            return online && PLAYER_ID.equals(playerId) && LEVEL.equals(level) ? player : null;
        }

        @Override
        public boolean blockMatches(String level, Pos3i pos, int matchKind) {
            return blockMatches;
        }

        @Override
        public boolean breakBlock(
            ActivePlayer<ToolState> activePlayer,
            int toolSlot,
            ToolState boundTool,
            Pos3i pos,
            boolean applyBaseDurability,
            int extraDurability
        ) {
            breakAttempts++;
            if (!breakSucceeds) {
                return false;
            }
            breaks.add(new BreakCall(pos, toolSlot, applyBaseDurability, extraDurability));
            afterBreak.run();
            return true;
        }

        @Override
        public void feedback(ActivePlayer<ToolState> activePlayer, HarvestFeedback reason, Object... args) {
            feedback.add(new FeedbackCall(reason, List.copyOf(Arrays.asList(args))));
        }

        private int feedbackCount(HarvestFeedback reason) {
            return (int) feedback.stream().filter(call -> call.reason() == reason).count();
        }

        private List<Object> feedbackArgs(HarvestFeedback reason) {
            return feedback.stream()
                .filter(call -> call.reason() == reason)
                .findFirst()
                .orElseThrow()
                .args();
        }

        private List<Pos3i> breakPositions() {
            return breaks.stream().map(BreakCall::pos).toList();
        }
    }

    private record BreakCall(Pos3i pos, int toolSlot, boolean applyBaseDurability, int extraDurability) {
    }

    private record FeedbackCall(HarvestFeedback reason, List<Object> args) {
    }
}
