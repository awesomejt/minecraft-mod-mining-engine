package media.jlt.minecraft.engine.util;

import java.util.function.IntFunction;

public final class InventoryIdentity {
    private InventoryIdentity() {
    }

    public static <T> int findSlot(int slotCount, IntFunction<T> itemAt, T target) {
        for (int slot = 0; slot < Math.max(0, slotCount); slot++) {
            if (itemAt.apply(slot) == target) {
                return slot;
            }
        }
        return -1;
    }
}
