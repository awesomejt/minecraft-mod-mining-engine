package media.jlt.minecraft.engine.port;

public record ToolSnapshot(
    boolean damageable,
    boolean enchanted,
    int maxDamage,
    int damage,
    boolean isEmpty
) {
    public int remainingDurability() {
        return maxDamage - damage;
    }
}
