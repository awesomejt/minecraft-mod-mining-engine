package media.jlt.minecraft.engine.balance;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaxModeTest {
    @Test
    void roundTripsConfigNamesIgnoringCaseAndWhitespace() {
        for (TaxMode mode : TaxMode.values()) {
            assertEquals(mode, TaxMode.fromConfigName("  " + mode.configName().toUpperCase(Locale.ROOT) + "  ").orElseThrow());
        }
    }

    @Test
    void rejectsMissingAndUnknownConfigNames() {
        assertTrue(TaxMode.fromConfigName(null).isEmpty());
        assertTrue(TaxMode.fromConfigName("  ").isEmpty());
        assertTrue(TaxMode.fromConfigName("levels_only").isEmpty());
    }

    @Test
    void parsingIsStableUnderTurkishDefaultLocale() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(TaxMode.XP_ONLY, TaxMode.fromConfigName("xp_only").orElseThrow());
        } finally {
            Locale.setDefault(originalLocale);
        }
    }
}
