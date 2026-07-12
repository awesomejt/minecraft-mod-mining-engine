package media.jlt.minecraft.engine.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class MessageRateLimiter {
    private final long cooldownTicks;
    private final Map<UUID, Map<String, Long>> lastShownTicks = new HashMap<>();

    public MessageRateLimiter(long cooldownTicks) {
        if (cooldownTicks < 0) {
            throw new IllegalArgumentException("cooldownTicks must not be negative");
        }
        this.cooldownTicks = cooldownTicks;
    }

    public boolean shouldShow(UUID playerUuid, Object messageKey, long currentTick, Object... messageArguments) {
        Map<String, Long> playerMessages = lastShownTicks.computeIfAbsent(playerUuid, ignored -> new HashMap<>());
        String messageIdentity = messageKey + Arrays.deepToString(messageArguments);
        Long lastShownTick = playerMessages.get(messageIdentity);
        if (lastShownTick != null && currentTick - lastShownTick < cooldownTicks) {
            return false;
        }
        playerMessages.put(messageIdentity, currentTick);
        return true;
    }

    public void clearAll() {
        lastShownTicks.clear();
    }
}
