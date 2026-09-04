package brewdevelopment.eventbus.event.stats;

import lombok.Getter;

/**
 * Statistics for a specific event listener.
 */
@SuppressWarnings("unused")
public class HandlerStats {
    private static final HandlerStats NO_OP = new HandlerStats() {
        @Override
        public synchronized void record(long durationNs) {
        }
    };

    @Getter
    private long calls;
    @Getter
    private long totalTimeNs;
    @Getter
    private long maxTimeNs;
    private long minTimeNs = Long.MAX_VALUE;

    public static HandlerStats noOp() {
        return NO_OP;
    }

    public synchronized void record(long durationNs) {
        calls++;
        totalTimeNs += durationNs;
        if (durationNs < minTimeNs) minTimeNs = durationNs;
        if (durationNs > maxTimeNs) maxTimeNs = durationNs;
    }

    public long getMinTimeNs() {
        return calls == 0 ? 0 : minTimeNs;
    }

    public double getAverageMs() {
        if (calls == 0) return 0;
        return (totalTimeNs / (double) calls) / 1_000_000.0;
    }

    public double getMaxMs() {
        return maxTimeNs / 1_000_000.0;
    }
}
