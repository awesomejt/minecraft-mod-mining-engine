package media.jlt.minecraft.engine.schedule;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HarvestFeedbackTest {
    @Test
    void exposesEverySchedulerFeedbackReasonInContractOrder() {
        assertEquals(
            List.of(
                "CANCELLED_DEATH",
                "CANCELLED_DISTANCE",
                "CANCELLED_TOOL_MISSING",
                "CANCELLED_HUNGER_TIMEOUT",
                "HUNGER_PAUSED",
                "HUNGER_RESUMED",
                "NO_SUBSTITUTION_XP",
                "NO_XP",
                "TOOL_BROKE",
                "DURABILITY_FLOOR"
            ),
            List.of(HarvestFeedback.values()).stream().map(Enum::name).toList()
        );
    }
}
