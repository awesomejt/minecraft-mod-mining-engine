package media.jlt.minecraft.engine.balance;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalanceSettingsTest {
    @Test
    void looksUpConfiguredEfficiencyDelaysAndClampsLevels() {
        BalanceSettings settings = settings(Map.of(0, 12, 1, 7, 2, 3, 3, 2, 4, 1, 5, 0));

        assertEquals(12, settings.delayTicksForEfficiencyLevel(-1));
        assertEquals(12, settings.delayTicksForEfficiencyLevel(0));
        assertEquals(7, settings.delayTicksForEfficiencyLevel(1));
        assertEquals(3, settings.delayTicksForEfficiencyLevel(2));
        assertEquals(2, settings.delayTicksForEfficiencyLevel(3));
        assertEquals(1, settings.delayTicksForEfficiencyLevel(4));
        assertEquals(0, settings.delayTicksForEfficiencyLevel(5));
        assertEquals(0, settings.delayTicksForEfficiencyLevel(100));
    }

    @Test
    void missingEfficiencyLevelsUseTreesDefaults() {
        BalanceSettings settings = settings(Map.of(2, 9));

        assertEquals(16, settings.delayTicksForEfficiencyLevel(0));
        assertEquals(8, settings.delayTicksForEfficiencyLevel(1));
        assertEquals(9, settings.delayTicksForEfficiencyLevel(2));
        assertEquals(2, settings.delayTicksForEfficiencyLevel(3));
        assertEquals(1, settings.delayTicksForEfficiencyLevel(4));
        assertEquals(0, settings.delayTicksForEfficiencyLevel(5));
    }

    @Test
    void defensivelyCopiesEfficiencyDelays() {
        Map<Integer, Integer> delays = new HashMap<>();
        delays.put(0, 12);
        BalanceSettings settings = settings(delays);

        delays.put(0, 30);

        assertEquals(12, settings.delayTicksForEfficiencyLevel(0));
        assertThrows(UnsupportedOperationException.class, () -> settings.delayTicksByEfficiencyLevel().put(0, 30));
    }

    @Test
    void allModeProtectsEveryDamageableTool() {
        BalanceSettings settings = settings(DurabilityProtectionMode.ALL);

        assertTrue(settings.isProtectionActive(true, false));
        assertTrue(settings.isProtectionActive(true, true));
        assertFalse(settings.isProtectionActive(false, true));
    }

    @Test
    void enchantedOnlyModeRequiresDamageableEnchantedTool() {
        BalanceSettings settings = settings(DurabilityProtectionMode.ENCHANTED_ONLY);

        assertFalse(settings.isProtectionActive(true, false));
        assertTrue(settings.isProtectionActive(true, true));
        assertFalse(settings.isProtectionActive(false, true));
    }

    @Test
    void offModeNeverActivatesProtection() {
        BalanceSettings settings = settings(DurabilityProtectionMode.OFF);

        assertFalse(settings.isProtectionActive(true, false));
        assertFalse(settings.isProtectionActive(true, true));
        assertFalse(settings.isProtectionActive(false, true));
    }

    @Test
    void requiresNonNullModesAndDelayMap() {
        assertThrows(NullPointerException.class, () -> settings(null, DurabilityProtectionMode.ALL, Map.of()));
        assertThrows(NullPointerException.class, () -> settings(TaxMode.HUNGER_ONLY, null, Map.of()));
        assertThrows(NullPointerException.class, () -> settings(TaxMode.HUNGER_ONLY, DurabilityProtectionMode.ALL, null));
    }

    private static BalanceSettings settings(Map<Integer, Integer> delays) {
        return settings(TaxMode.HUNGER_ONLY, DurabilityProtectionMode.ALL, delays);
    }

    private static BalanceSettings settings(DurabilityProtectionMode mode) {
        return settings(TaxMode.HUNGER_ONLY, mode, Map.of());
    }

    private static BalanceSettings settings(
        TaxMode taxMode,
        DurabilityProtectionMode protectionMode,
        Map<Integer, Integer> delays
    ) {
        return new BalanceSettings(
            taxMode,
            true,
            1,
            0,
            400,
            protectionMode,
            1,
            4,
            false,
            10,
            3,
            true,
            delays,
            8,
            64
        );
    }
}
