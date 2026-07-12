package media.jlt.minecraft.engine.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolTierTest {
    @Test
    void parsesCanonicalNamesIgnoringCaseAndWhitespace() {
        assertEquals(ToolTier.WOOD, ToolTier.fromConfigName(" wood ").orElseThrow());
        assertEquals(ToolTier.COPPER, ToolTier.fromConfigName("CoPpEr").orElseThrow());
        assertEquals(ToolTier.NETHERITE, ToolTier.fromConfigName("NETHERITE").orElseThrow());
    }

    @Test
    void rejectsNullBlankAndUnknownNames() {
        assertTrue(ToolTier.fromConfigName(null).isEmpty());
        assertTrue(ToolTier.fromConfigName("  ").isEmpty());
        assertTrue(ToolTier.fromConfigName("gold").isEmpty());
    }

    @Test
    void parsingIsStableUnderTurkishDefaultLocale() {
        Locale originalLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(ToolTier.DIAMOND, ToolTier.fromConfigName("diamond").orElseThrow());
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    void maxDamageBucketsChangeAtEveryDocumentedBoundary() {
        assertEquals(ToolTier.WOOD, ToolTier.fromMaxDamage(Integer.MIN_VALUE));
        assertEquals(ToolTier.WOOD, ToolTier.fromMaxDamage(94));
        assertEquals(ToolTier.STONE, ToolTier.fromMaxDamage(95));
        assertEquals(ToolTier.STONE, ToolTier.fromMaxDamage(159));
        assertEquals(ToolTier.COPPER, ToolTier.fromMaxDamage(160));
        assertEquals(ToolTier.COPPER, ToolTier.fromMaxDamage(219));
        assertEquals(ToolTier.IRON, ToolTier.fromMaxDamage(220));
        assertEquals(ToolTier.IRON, ToolTier.fromMaxDamage(904));
        assertEquals(ToolTier.DIAMOND, ToolTier.fromMaxDamage(905));
        assertEquals(ToolTier.DIAMOND, ToolTier.fromMaxDamage(1795));
        assertEquals(ToolTier.NETHERITE, ToolTier.fromMaxDamage(1796));
        assertEquals(ToolTier.NETHERITE, ToolTier.fromMaxDamage(Integer.MAX_VALUE));
    }
}
