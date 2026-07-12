package media.jlt.minecraft.engine.balance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExperienceMathTest {
    @Test
    void totalExperienceMatchesFormulaBoundaries() {
        assertEquals(0, ExperienceMath.totalForLevel(0));
        assertEquals(352, ExperienceMath.totalForLevel(16));
        assertEquals(394, ExperienceMath.totalForLevel(17));
        assertEquals(1_507, ExperienceMath.totalForLevel(31));
        assertEquals(1_628, ExperienceMath.totalForLevel(32));
    }

    @Test
    void rejectsNegativeLevels() {
        assertThrows(IllegalArgumentException.class, () -> ExperienceMath.totalForLevel(-1));
    }
}
