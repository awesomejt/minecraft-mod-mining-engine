package media.jlt.minecraft.engine.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryIdentityTest {
    @Test
    void findsExactObjectAfterItMovesSlots() {
        Object boundTool = new Object();
        List<Object> inventory = List.of(new Object(), new Object(), boundTool);

        assertEquals(2, InventoryIdentity.findSlot(inventory.size(), inventory::get, boundTool));
    }

    @Test
    void rejectsEqualButDistinctReplacement() {
        String boundTool = new String("diamond axe");
        List<String> inventory = List.of(new String("diamond axe"));

        assertEquals(-1, InventoryIdentity.findSlot(inventory.size(), inventory::get, boundTool));
    }

    @Test
    void returnsMissingForEmptyInventory() {
        assertEquals(-1, InventoryIdentity.findSlot(0, slot -> new Object(), new Object()));
    }
}
