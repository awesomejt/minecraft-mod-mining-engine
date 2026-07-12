package media.jlt.minecraft.engine.balance;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurabilityProtectionModeTest {
    @Test
    void roundTripsConfigNamesIgnoringCaseAndWhitespace() {
        for (DurabilityProtectionMode mode : DurabilityProtectionMode.values()) {
            assertEquals(
                mode,
                DurabilityProtectionMode.fromConfigName(
                    "  " + mode.configName().toUpperCase(Locale.ROOT) + "  "
                ).orElseThrow()
            );
        }
    }

    @Test
    void rejectsMissingAndUnknownConfigNames() {
        assertTrue(DurabilityProtectionMode.fromConfigName(null).isEmpty());
        assertTrue(DurabilityProtectionMode.fromConfigName("  ").isEmpty());
        assertTrue(DurabilityProtectionMode.fromConfigName("unbreaking_only").isEmpty());
    }

    @Test
    void parsingIsStableUnderTurkishDefaultLocale() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(
                DurabilityProtectionMode.ENCHANTED_ONLY,
                DurabilityProtectionMode.fromConfigName("enchanted_only").orElseThrow()
            );
        } finally {
            Locale.setDefault(originalLocale);
        }
    }
}
