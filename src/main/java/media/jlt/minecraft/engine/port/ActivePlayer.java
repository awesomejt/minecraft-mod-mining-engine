package media.jlt.minecraft.engine.port;

import media.jlt.minecraft.engine.util.ToolTier;

import java.util.UUID;

public interface ActivePlayer<T> {
    UUID id();

    boolean isDeadOrDying();

    double x();

    double y();

    double z();

    boolean isCreative();

    int foodLevel();

    float saturation();

    void addExhaustion(float amount);

    int totalExperience();

    void giveExperiencePoints(int delta);

    int findToolSlot(T boundTool);

    boolean isValidTool(T tool, ToolTier requiredTier);

    ToolSnapshot toolSnapshot(T tool);

    int efficiencyLevel(T tool);
}
