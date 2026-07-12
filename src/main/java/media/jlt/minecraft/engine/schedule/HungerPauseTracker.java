package media.jlt.minecraft.engine.schedule;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class HungerPauseTracker {
    private final Map<UUID, Long> deadlines = new HashMap<>();

    public void markIfAbsent(UUID playerUuid, long deadline) {
        deadlines.putIfAbsent(playerUuid, deadline);
    }

    public void clear(UUID playerUuid) {
        deadlines.remove(playerUuid);
    }

    public void clearAll() {
        deadlines.clear();
    }

    public boolean isPaused(UUID playerUuid) {
        return deadlines.containsKey(playerUuid);
    }

    public long deadline(UUID playerUuid) {
        Long deadline = deadlines.get(playerUuid);
        if (deadline == null) {
            throw new IllegalStateException("Player is not hunger-paused");
        }
        return deadline;
    }

    public void removeOrphans(Set<UUID> playersWithScheduledWork) {
        deadlines.keySet().removeIf(playerUuid -> !playersWithScheduledWork.contains(playerUuid));
    }
}
