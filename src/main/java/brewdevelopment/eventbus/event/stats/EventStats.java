package brewdevelopment.eventbus.event.stats;

/**
 * Statistics for a specific event type.
 */
@SuppressWarnings("unused")
public class EventStats {
    private long calls;
    private long totalTimeNs;
    private long maxTimeNs;
    private long minTimeNs = Long.MAX_VALUE;

    public synchronized void record(long durationNs) {
        calls++;
        totalTimeNs += durationNs;
        if (durationNs < minTimeNs) minTimeNs = durationNs;
        if (durationNs > maxTimeNs) maxTimeNs = durationNs;
    }

    public long getCalls() {
        return calls;
    }

    public long getTotalTimeNs() {
        return totalTimeNs;
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
