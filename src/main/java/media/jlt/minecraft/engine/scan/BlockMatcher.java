package media.jlt.minecraft.engine.scan;

@FunctionalInterface
public interface BlockMatcher {
    boolean matches(int x, int y, int z);
}
