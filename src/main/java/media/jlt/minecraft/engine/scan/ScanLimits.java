package media.jlt.minecraft.engine.scan;

import java.util.OptionalInt;

public record ScanLimits(
    int maxBlocks,
    int maxHorizontalRadius,
    OptionalInt maxDepthBelowOrigin
) {
}
