package media.jlt.minecraft.engine.scan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Pos3iTest {
    @Test
    void offsetsEveryCoordinate() {
        assertEquals(new Pos3i(8, 67, -4), new Pos3i(10, 64, 2).offset(-2, 3, -6));
    }
}
