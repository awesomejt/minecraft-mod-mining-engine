package media.jlt.minecraft.engine.util;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageRateLimiterTest {
    private enum Hint {
        DURABILITY
    }

    @Test
    void suppressesSameMessageUntilCooldownExpires() {
        MessageRateLimiter limiter = new MessageRateLimiter(100);
        UUID player = UUID.randomUUID();

        assertTrue(limiter.shouldShow(player, "hint.one", 20));
        assertFalse(limiter.shouldShow(player, "hint.one", 119));
        assertTrue(limiter.shouldShow(player, "hint.one", 120));
    }

    @Test
    void tracksPlayersAndMessageKeysIndependently() {
        MessageRateLimiter limiter = new MessageRateLimiter(100);
        UUID firstPlayer = UUID.randomUUID();
        UUID secondPlayer = UUID.randomUUID();

        assertTrue(limiter.shouldShow(firstPlayer, "hint.one", 20));
        assertTrue(limiter.shouldShow(firstPlayer, "hint.two", 21));
        assertTrue(limiter.shouldShow(secondPlayer, "hint.one", 21));
    }

    @Test
    void permitsSameKeyImmediatelyWhenMessageArgumentsChange() {
        MessageRateLimiter limiter = new MessageRateLimiter(100);
        UUID player = UUID.randomUUID();

        assertTrue(limiter.shouldShow(player, "hint.durability", 20, 40, 10));
        assertFalse(limiter.shouldShow(player, "hint.durability", 21, 40, 10));
        assertTrue(limiter.shouldShow(player, "hint.durability", 21, 44, 10));
    }

    @Test
    void clearAllRemovesCooldownHistory() {
        MessageRateLimiter limiter = new MessageRateLimiter(100);
        UUID player = UUID.randomUUID();

        assertTrue(limiter.shouldShow(player, "hint.one", 20));
        limiter.clearAll();
        assertTrue(limiter.shouldShow(player, "hint.one", 21));
    }

    @Test
    void acceptsNonStringMessageKeys() {
        MessageRateLimiter limiter = new MessageRateLimiter(100);
        UUID player = UUID.randomUUID();

        assertTrue(limiter.shouldShow(player, Hint.DURABILITY, 20));
        assertFalse(limiter.shouldShow(player, Hint.DURABILITY, 21));
    }
}
