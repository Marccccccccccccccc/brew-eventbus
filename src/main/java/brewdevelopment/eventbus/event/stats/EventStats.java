package brewdevelopment.eventbus.event.stats;

import lombok.Getter;

/**
 * Stats for a specific event type.
 */
@SuppressWarnings("unused")
public class EventStats {
    private static final EventStats NO_OP = new EventStats() {};

    @Getter
    private long calls;
    @Getter
    private long totalTimeNs;
    private long maxTimeNs;
    private long minTimeNs = Long.MAX_VALUE;

    public static EventStats noOp() {
        return NO_OP;
    }

    public synchronized void record(long durationNs) {
        calls++;
        totalTimeNs += durationNs;
        if (durationNs < minTimeNs) minTimeNs = durationNs;
        if (durationNs > maxTimeNs) maxTimeNs = durationNs;
    }

    public double getAverageMs() {
        if (calls == 0) return 0;
        return (totalTimeNs / (double) calls) / 1_000_000.0;
    }

    public double getMaxMs() {
        return maxTimeNs / 1_000_000.0;
    }

    public double getMinTimeMs() {
        if (calls == 0) return 0;
        return minTimeNs / 1_000_000.0;
    }
}
