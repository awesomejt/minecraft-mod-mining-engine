package media.jlt.minecraft.engine.schedule;

import media.jlt.minecraft.engine.scan.Pos3i;
import media.jlt.minecraft.engine.util.ToolTier;

import java.util.Objects;
import java.util.UUID;

public final class ScheduledBreak<L, T> {
    private final UUID playerId;
    private final L level;
    private final Pos3i pos;
    private long executeAtTick;
    private final boolean instantBatch;
    private final int extraDurability;
    private final int substitutionXpCost;
    private final boolean applyBaseDurability;
    private final ToolTier requiredTier;
    private final T boundTool;
    private final int matchKind;
    private final float exhaustionPerBlock;
    private final int xpCostPerBlock;

    ScheduledBreak(
        UUID playerId,
        L level,
        Pos3i pos,
        long executeAtTick,
        boolean instantBatch,
        int extraDurability,
        int substitutionXpCost,
        boolean applyBaseDurability,
        ToolTier requiredTier,
        T boundTool,
        int matchKind,
        float exhaustionPerBlock,
        int xpCostPerBlock
    ) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.level = Objects.requireNonNull(level, "level");
        this.pos = Objects.requireNonNull(pos, "pos");
        this.executeAtTick = executeAtTick;
        this.instantBatch = instantBatch;
        this.extraDurability = extraDurability;
        this.substitutionXpCost = substitutionXpCost;
        this.applyBaseDurability = applyBaseDurability;
        this.requiredTier = Objects.requireNonNull(requiredTier, "requiredTier");
        this.boundTool = Objects.requireNonNull(boundTool, "boundTool");
        this.matchKind = matchKind;
        this.exhaustionPerBlock = exhaustionPerBlock;
        this.xpCostPerBlock = xpCostPerBlock;
    }

    public UUID playerId() {
        return playerId;
    }

    public L level() {
        return level;
    }

    public Pos3i pos() {
        return pos;
    }

    public long executeAtTick() {
        return executeAtTick;
    }

    void executeAtTick(long executeAtTick) {
        this.executeAtTick = executeAtTick;
    }

    public boolean instantBatch() {
        return instantBatch;
    }

    public int extraDurability() {
        return extraDurability;
    }

    public int substitutionXpCost() {
        return substitutionXpCost;
    }

    public boolean applyBaseDurability() {
        return applyBaseDurability;
    }

    public ToolTier requiredTier() {
        return requiredTier;
    }

    public T boundTool() {
        return boundTool;
    }

    public int matchKind() {
        return matchKind;
    }

    public float exhaustionPerBlock() {
        return exhaustionPerBlock;
    }

    public int xpCostPerBlock() {
        return xpCostPerBlock;
    }
}
