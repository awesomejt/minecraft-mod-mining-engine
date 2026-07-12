package media.jlt.minecraft.engine.balance;

public final class ExperienceMath {
    private ExperienceMath() {
    }

    public static int totalForLevel(int level) {
        if (level < 0) {
            throw new IllegalArgumentException("level must not be negative");
        }
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) Math.floor(2.5 * level * level - 40.5 * level + 360);
        }
        return (int) Math.floor(4.5 * level * level - 162.5 * level + 2220);
    }
}
