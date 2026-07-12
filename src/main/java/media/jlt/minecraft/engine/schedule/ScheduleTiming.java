package media.jlt.minecraft.engine.schedule;

public final class ScheduleTiming {
    private ScheduleTiming() {
    }

    public static long resumeTick(long currentTick, int delayPerBlockTicks, int queuePosition) {
        if (queuePosition < 1) {
            throw new IllegalArgumentException("queuePosition must be at least 1");
        }
        long delay = Math.max(0, delayPerBlockTicks);
        try {
            return Math.addExact(currentTick, Math.multiplyExact(delay, queuePosition));
        } catch (ArithmeticException ignored) {
            return Long.MAX_VALUE;
        }
    }
}
