package media.jlt.minecraft.engine.schedule;

import media.jlt.minecraft.engine.balance.BalanceSettings;
import media.jlt.minecraft.engine.balance.DurabilityBudget;
import media.jlt.minecraft.engine.balance.TaxMode;
import media.jlt.minecraft.engine.port.ActivePlayer;
import media.jlt.minecraft.engine.port.HarvestAdapter;
import media.jlt.minecraft.engine.port.ToolSnapshot;
import media.jlt.minecraft.engine.scan.Pos3i;
import media.jlt.minecraft.engine.util.HarvestDistance;
import media.jlt.minecraft.engine.util.ToolTier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class HarvestScheduler<L, T> {
    private final HarvestAdapter<L, T> adapter;
    private final Supplier<BalanceSettings> settingsSupplier;
    private final List<ScheduledBreak<L, T>> scheduledBreaks = new ArrayList<>();
    private final HungerPauseTracker hungerPauses = new HungerPauseTracker();
    private long currentTick;

    public HarvestScheduler(HarvestAdapter<L, T> adapter, Supplier<BalanceSettings> settingsSupplier) {
        this.adapter = Objects.requireNonNull(adapter, "adapter");
        this.settingsSupplier = Objects.requireNonNull(settingsSupplier, "settingsSupplier");
    }

    public void schedule(
        UUID playerId,
        L level,
        List<Pos3i> positions,
        int extraDurability,
        int delayPerBlockTicks,
        int matchKind,
        float exhaustionPerBlock,
        int xpCostPerBlock,
        int durabilityChargedBlocks,
        int substitutionXpCostPerBlock,
        boolean applyBaseDurability,
        ToolTier requiredTier,
        T boundTool
    ) {
        Objects.requireNonNull(positions, "positions");
        long executeAtTick = currentTick;
        int processed = 0;
        for (Pos3i pos : positions) {
            int effectiveExtraDurability = processed < durabilityChargedBlocks ? extraDurability : 0;
            int effectiveSubstitutionXp = processed < durabilityChargedBlocks ? 0 : substitutionXpCostPerBlock;
            boolean instantBatch = delayPerBlockTicks <= 0;
            if (!instantBatch) {
                executeAtTick += delayPerBlockTicks;
            }
            scheduledBreaks.add(new ScheduledBreak<>(
                playerId,
                level,
                pos,
                executeAtTick,
                instantBatch,
                effectiveExtraDurability,
                effectiveSubstitutionXp,
                applyBaseDurability,
                requiredTier,
                boundTool,
                matchKind,
                exhaustionPerBlock,
                xpCostPerBlock
            ));
            processed++;
        }
    }

    public boolean hasScheduledWork(UUID playerId) {
        for (ScheduledBreak<L, T> scheduled : scheduledBreaks) {
            if (scheduled.playerId().equals(playerId)) {
                return true;
            }
        }
        return false;
    }

    public void tick(long currentTick) {
        this.currentTick = currentTick;
        if (scheduledBreaks.isEmpty()) {
            hungerPauses.removeOrphans(Set.of());
            return;
        }

        BalanceSettings settings = Objects.requireNonNull(settingsSupplier.get(), "settingsSupplier result");
        Set<UUID> timedOutPlayers = new HashSet<>();
        Set<UUID> noXpPlayers = new HashSet<>();
        Set<UUID> invalidToolPlayers = new HashSet<>();
        Set<UUID> durabilityFloorPlayers = new HashSet<>();
        Set<UUID> inactivePlayers = new HashSet<>();
        Set<UUID> resumedPlayers = new HashSet<>();
        Map<UUID, Integer> instantBreakCounts = new HashMap<>();
        Iterator<ScheduledBreak<L, T>> iterator = scheduledBreaks.iterator();
        while (iterator.hasNext()) {
            ScheduledBreak<L, T> scheduled = iterator.next();
            if (invalidToolPlayers.contains(scheduled.playerId())
                || durabilityFloorPlayers.contains(scheduled.playerId())
                || inactivePlayers.contains(scheduled.playerId())) {
                iterator.remove();
                continue;
            }
            if (resumedPlayers.contains(scheduled.playerId())) {
                continue;
            }
            if (scheduled.executeAtTick() > currentTick) {
                continue;
            }

            ActivePlayer<T> player = adapter.resolve(scheduled.playerId(), scheduled.level());
            if (player == null) {
                hungerPauses.clear(scheduled.playerId());
                iterator.remove();
                continue;
            }
            if (player.isDeadOrDying()) {
                inactivePlayers.add(player.id());
                adapter.feedback(player, HarvestFeedback.CANCELLED_DEATH);
                iterator.remove();
                continue;
            }
            if (!HarvestDistance.isWithinRange(
                player.x(),
                player.y(),
                player.z(),
                scheduled.pos().x(),
                scheduled.pos().y(),
                scheduled.pos().z(),
                settings.maxHarvestDistance()
            )) {
                inactivePlayers.add(player.id());
                adapter.feedback(player, HarvestFeedback.CANCELLED_DISTANCE);
                iterator.remove();
                continue;
            }
            if (scheduled.instantBatch() && !InstantBreakBatch.hasCapacity(
                instantBreakCounts.getOrDefault(scheduled.playerId(), 0),
                settings.maxInstantBlocksPerTick()
            )) {
                scheduled.executeAtTick(currentTick + 1);
                continue;
            }

            int boundToolSlot = player.findToolSlot(scheduled.boundTool());
            if (boundToolSlot < 0 || !player.isValidTool(scheduled.boundTool(), scheduled.requiredTier())) {
                invalidToolPlayers.add(player.id());
                adapter.feedback(player, HarvestFeedback.CANCELLED_TOOL_MISSING);
                iterator.remove();
                continue;
            }

            if (settings.taxMode() == TaxMode.HUNGER_ONLY && hungerPauses.isPaused(player.id())) {
                if (canContinue(player)) {
                    hungerPauses.clear(player.id());
                    int delayPerBlockTicks = settings.enableTimePenalty()
                        ? settings.delayTicksForEfficiencyLevel(player.efficiencyLevel(scheduled.boundTool()))
                        : 0;
                    restaggerScheduledBreaks(player.id(), currentTick, delayPerBlockTicks);
                    resumedPlayers.add(player.id());
                    adapter.feedback(player, HarvestFeedback.HUNGER_RESUMED);
                    continue;
                } else if (currentTick > hungerPauses.deadline(player.id())) {
                    timedOutPlayers.add(player.id());
                    hungerPauses.clear(player.id());
                    adapter.feedback(player, HarvestFeedback.CANCELLED_HUNGER_TIMEOUT);
                    iterator.remove();
                    continue;
                } else {
                    scheduled.executeAtTick(Math.max(scheduled.executeAtTick(), currentTick + 20));
                    continue;
                }
            }

            if (settings.taxMode() == TaxMode.HUNGER_ONLY
                && settings.enableHungerTax()
                && !player.isCreative()
                && !canContinue(player)) {
                markHungerPause(player.id(), currentTick, settings.hungerResumeTimeoutTicks());
                adapter.feedback(player, HarvestFeedback.HUNGER_PAUSED);
                scheduled.executeAtTick(Math.max(scheduled.executeAtTick(), currentTick + 20));
                continue;
            }

            if (!adapter.blockMatches(scheduled.level(), scheduled.pos(), scheduled.matchKind())) {
                iterator.remove();
                continue;
            }

            ToolSnapshot snapshot = player.toolSnapshot(scheduled.boundTool());
            if (!canAffordAutomatedDurability(
                player,
                snapshot,
                scheduled.applyBaseDurability(),
                scheduled.extraDurability(),
                settings
            )) {
                durabilityFloorPlayers.add(player.id());
                adapter.feedback(player, HarvestFeedback.DURABILITY_FLOOR, settings.durabilityProtectionFloor());
                iterator.remove();
                continue;
            }
            if (!canAffordXp(player, scheduled.substitutionXpCost(), settings.xpTaxFloor())) {
                noXpPlayers.add(player.id());
                adapter.feedback(player, HarvestFeedback.NO_SUBSTITUTION_XP);
                iterator.remove();
                continue;
            }

            if (!adapter.breakBlock(
                player,
                boundToolSlot,
                scheduled.boundTool(),
                scheduled.pos(),
                scheduled.applyBaseDurability(),
                scheduled.extraDurability()
            )) {
                iterator.remove();
                continue;
            }
            if (scheduled.instantBatch()) {
                instantBreakCounts.merge(player.id(), 1, Integer::sum);
            }
            if (scheduled.substitutionXpCost() > 0 && !player.isCreative()) {
                player.giveExperiencePoints(-scheduled.substitutionXpCost());
            }

            TaxOutcome taxOutcome = applyResourceTax(
                player,
                scheduled.exhaustionPerBlock(),
                scheduled.xpCostPerBlock(),
                settings
            );
            if (taxOutcome == TaxOutcome.PAUSE_HUNGER) {
                markHungerPause(player.id(), currentTick, settings.hungerResumeTimeoutTicks());
                adapter.feedback(player, HarvestFeedback.HUNGER_PAUSED);
            } else if (taxOutcome == TaxOutcome.STOP_NO_XP) {
                adapter.feedback(player, HarvestFeedback.NO_XP);
                noXpPlayers.add(player.id());
            }
            if (player.toolSnapshot(scheduled.boundTool()).isEmpty()) {
                adapter.feedback(player, HarvestFeedback.TOOL_BROKE);
                invalidToolPlayers.add(player.id());
            }
            iterator.remove();
        }

        if (!timedOutPlayers.isEmpty()) {
            scheduledBreaks.removeIf(scheduled -> timedOutPlayers.contains(scheduled.playerId()));
        }
        if (!noXpPlayers.isEmpty()) {
            scheduledBreaks.removeIf(scheduled -> noXpPlayers.contains(scheduled.playerId()));
        }
        if (!invalidToolPlayers.isEmpty()) {
            scheduledBreaks.removeIf(scheduled -> invalidToolPlayers.contains(scheduled.playerId()));
        }
        if (!durabilityFloorPlayers.isEmpty()) {
            scheduledBreaks.removeIf(scheduled -> durabilityFloorPlayers.contains(scheduled.playerId()));
        }
        if (!inactivePlayers.isEmpty()) {
            scheduledBreaks.removeIf(scheduled -> inactivePlayers.contains(scheduled.playerId()));
        }
        Set<UUID> playersWithScheduledWork = new HashSet<>();
        for (ScheduledBreak<L, T> scheduled : scheduledBreaks) {
            playersWithScheduledWork.add(scheduled.playerId());
        }
        hungerPauses.removeOrphans(playersWithScheduledWork);
    }

    public void clearAll() {
        scheduledBreaks.clear();
        hungerPauses.clearAll();
        currentTick = 0;
    }

    public void clearHungerPause(UUID playerId) {
        hungerPauses.clear(playerId);
    }

    private void restaggerScheduledBreaks(UUID playerId, long resumeTick, int delayPerBlockTicks) {
        int queuePosition = 1;
        for (ScheduledBreak<L, T> scheduled : scheduledBreaks) {
            if (scheduled.playerId().equals(playerId)) {
                scheduled.executeAtTick(ScheduleTiming.resumeTick(resumeTick, delayPerBlockTicks, queuePosition));
                queuePosition++;
            }
        }
    }

    private void markHungerPause(UUID playerId, long tick, int timeoutTicks) {
        hungerPauses.markIfAbsent(playerId, tick + timeoutTicks);
    }

    private boolean canAffordAutomatedDurability(
        ActivePlayer<T> player,
        ToolSnapshot tool,
        boolean applyBaseDurability,
        int extraDurability,
        BalanceSettings settings
    ) {
        if (player.isCreative() || !settings.isProtectionActive(tool.damageable(), tool.enchanted())) {
            return true;
        }
        int cost = (applyBaseDurability ? 1 : 0) + Math.max(0, extraDurability);
        return DurabilityBudget.canSpend(
            tool.remainingDurability(),
            cost,
            settings.durabilityProtectionFloor()
        );
    }

    private static boolean canAffordXp(ActivePlayer<?> player, int cost, int xpFloor) {
        if (cost <= 0 || player.isCreative()) {
            return true;
        }
        return player.totalExperience() - cost >= xpFloor;
    }

    private static TaxOutcome applyResourceTax(
        ActivePlayer<?> player,
        float exhaustionPerBlock,
        int xpCostPerBlock,
        BalanceSettings settings
    ) {
        if (settings.taxMode() == TaxMode.XP_ONLY) {
            return applyXpTax(player, xpCostPerBlock, settings.xpTaxFloor())
                ? TaxOutcome.CONTINUE
                : TaxOutcome.STOP_NO_XP;
        }
        if (settings.taxMode() == TaxMode.XP_AFTER_HUNGER_DEPLETED) {
            if (settings.enableHungerTax() && !player.isCreative()) {
                if (applyHungerTax(player, exhaustionPerBlock, settings)) {
                    return TaxOutcome.CONTINUE;
                }
            }
            return applyXpTax(player, xpCostPerBlock, settings.xpTaxFloor())
                ? TaxOutcome.CONTINUE
                : TaxOutcome.STOP_NO_XP;
        }
        return applyHungerTax(player, exhaustionPerBlock, settings)
            ? TaxOutcome.CONTINUE
            : TaxOutcome.PAUSE_HUNGER;
    }

    private static boolean applyHungerTax(
        ActivePlayer<?> player,
        float exhaustionPerBlock,
        BalanceSettings settings
    ) {
        if (!settings.enableHungerTax() || player.isCreative() || exhaustionPerBlock <= 0f) {
            return true;
        }
        if (settings.hungerTaxFloor() > 0
            && player.foodLevel() <= settings.hungerTaxFloor()
            && player.saturation() <= 0f) {
            return false;
        }
        player.addExhaustion(exhaustionPerBlock);
        return canContinue(player);
    }

    private static boolean applyXpTax(ActivePlayer<?> player, int cost, int xpFloor) {
        if (player.isCreative() || cost <= 0) {
            return true;
        }
        if (player.totalExperience() - cost < xpFloor) {
            return false;
        }
        player.giveExperiencePoints(-cost);
        return true;
    }

    private static boolean canContinue(ActivePlayer<?> player) {
        return player.foodLevel() > 0 || player.saturation() > 0f;
    }

    private enum TaxOutcome {
        CONTINUE,
        PAUSE_HUNGER,
        STOP_NO_XP
    }
}
