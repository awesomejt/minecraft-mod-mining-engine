package media.jlt.minecraft.engine.balance;

import java.util.Locale;
import java.util.Optional;

public enum TaxMode {
    HUNGER_ONLY,
    XP_ONLY,
    XP_AFTER_HUNGER_DEPLETED;

    public String configName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<TaxMode> fromConfigName(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return Optional.of(TaxMode.valueOf(normalized));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
