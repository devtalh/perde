package io.github.devtalh.perde.net;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.function.LongSupplier;

/** Lowest RTT of the last eight replies, using only a local monotonic clock. */
public final class ServerTimeEstimator {
    private record Sample(long receivedNanos, long rttNanos, double serverAtReceiptMs) { }
    private final ArrayDeque<Sample> samples = new ArrayDeque<>();
    private final LongSupplier nanos;
    private Sample best;
    public ServerTimeEstimator(LongSupplier nanos) { this.nanos = nanos; }

    public boolean sample(long sentNanos, long receivedNanos, long serverMs) {
        long rtt = receivedNanos - sentNanos;
        if (rtt < 0 || rtt > 10_000_000_000L || serverMs < 0) return false;
        samples.addLast(new Sample(receivedNanos, rtt, serverMs + rtt / 2_000_000.0));
        if (samples.size() > 8) samples.removeFirst();
        best = samples.stream().min(Comparator.comparingLong(Sample::rttNanos)).orElseThrow();
        return true;
    }
    public boolean ready() { return best != null; }
    public long serverNowMs() {
        if (best == null) throw new IllegalStateException("No server clock sample");
        return Math.round(best.serverAtReceiptMs + (nanos.getAsLong() - best.receivedNanos) / 1_000_000.0);
    }
    public double rttMs() { return best == null ? 0 : best.rttNanos / 1_000_000.0; }
    public double offsetMs() { return best == null ? 0 : best.serverAtReceiptMs - best.receivedNanos / 1_000_000.0; }
    public void reset() { samples.clear(); best = null; }
}
