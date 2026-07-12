package media.jlt.minecraft.engine.scan;

public record Pos3i(int x, int y, int z) {
    public Pos3i offset(int dx, int dy, int dz) {
        return new Pos3i(x + dx, y + dy, z + dz);
    }
}
