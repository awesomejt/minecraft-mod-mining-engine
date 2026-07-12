package media.jlt.minecraft.engine.balance;

import java.util.Locale;
import java.util.Optional;

public enum DurabilityProtectionMode {
    OFF,
    ENCHANTED_ONLY,
    ALL;

    public String configName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<DurabilityProtectionMode> fromConfigName(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return Optional.of(DurabilityProtectionMode.valueOf(normalized));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
