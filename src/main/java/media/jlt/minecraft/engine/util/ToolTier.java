package media.jlt.minecraft.engine.util;

import java.util.Locale;
import java.util.Optional;

public enum ToolTier {
    WOOD,
    STONE,
    COPPER,
    IRON,
    DIAMOND,
    NETHERITE;

    public boolean isLowerThan(ToolTier other) {
        return this.ordinal() < other.ordinal();
    }

    public String configName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<ToolTier> fromConfigName(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        try {
            return Optional.of(ToolTier.valueOf(normalized));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    // Thresholds sit at the midpoints between vanilla ToolMaterial durabilities
    // (wood 59, stone 131, copper 190, iron 250, diamond 1561, netherite 2031;
    // gold's 32 falls below wood). Modded axes are bucketed by their real max
    // durability instead of by registry name, so unfamiliar naming still lands right.
    public static ToolTier fromMaxDamage(int maxDamage) {
        if (maxDamage >= 1796) {
            return NETHERITE;
        }
        if (maxDamage >= 905) {
            return DIAMOND;
        }
        if (maxDamage >= 220) {
            return IRON;
        }
        if (maxDamage >= 160) {
            return COPPER;
        }
        if (maxDamage >= 95) {
            return STONE;
        }
        return WOOD;
    }
}
