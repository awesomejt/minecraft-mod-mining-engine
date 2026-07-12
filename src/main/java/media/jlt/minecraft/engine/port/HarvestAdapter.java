package media.jlt.minecraft.engine.port;

import media.jlt.minecraft.engine.scan.Pos3i;
import media.jlt.minecraft.engine.schedule.HarvestFeedback;

import java.util.UUID;

public interface HarvestAdapter<L, T> {
    /**
     * Resolves an online player in the supplied level, or returns {@code null}
     * when the player is offline or no longer in that level.
     */
    ActivePlayer<T> resolve(UUID playerId, L level);

    boolean blockMatches(L level, Pos3i pos, int matchKind);

    boolean breakBlock(
        ActivePlayer<T> player,
        int toolSlot,
        T boundTool,
        Pos3i pos,
        boolean applyBaseDurability,
        int extraDurability
    );

    void feedback(ActivePlayer<T> player, HarvestFeedback reason, Object... args);
}
