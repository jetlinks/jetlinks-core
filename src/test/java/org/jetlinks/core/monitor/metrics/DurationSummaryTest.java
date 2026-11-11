package org.jetlinks.core.monitor.metrics;

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class DurationSummaryTest {
    @Test
    public void emptyAndZeroDurationRemainDistinguishableAndTiesKeepFirstPeak() {
        DurationSummary summary = new DurationSummary();
        assertEquals(0, summary.snapshot().count());
        summary.record(0, 100);
        assertEquals(1, summary.getCount());
        assertEquals(100, summary.snapshot().maxAtMillis());
        summary.record(2_000_000, 200);
        summary.record(1_000_000, 300);
        summary.record(2_000_000, 400);
        assertEquals(4, summary.getCount());
        assertEquals(0.005, summary.getTotalDurationSeconds(), 1e-12);
        assertEquals(0.00125, summary.getMeanDurationSeconds(), 1e-12);
        assertEquals(2_000_000, summary.getLastDurationNanos());
        assertEquals(2_000_000, summary.snapshot().maxDurationNanos());
        assertEquals(200, summary.snapshot().maxAtMillis());
    }

    @Test
    public void concurrentDurationTotalsDoNotLoseUpdatesOrOverflowLongNanoseconds() {
        DurationSummary summary = new DurationSummary();
        IntStream.range(0, 10_000).parallel().forEach(i -> summary.record(2_000_000, i));
        assertEquals(10_000, summary.getCount());
        assertEquals(20, summary.getTotalDurationSeconds(), 1e-8);
        IntStream.range(0, 100).parallel().forEach(i -> summary.record(Long.MAX_VALUE / 10, i));
        assertTrue(Double.isFinite(summary.getTotalDurationSeconds()));
        assertTrue(summary.getTotalDurationSeconds() > Long.MAX_VALUE / 1_000_000_000D);
    }

    @Test
    public void snapshotsKeepPeakValueAndTimePairedDuringConcurrentUpdates() {
        DurationSummary summary = new DurationSummary();
        AtomicBoolean done = new AtomicBoolean();
        CompletableFuture<Void> writer = CompletableFuture.runAsync(() -> {
            try {
                for (int i = 0; i < 20_000; i++) {
                    summary.record(i, i + 100);
                }
            } finally {
                done.set(true);
            }
        });
        do {
            DurationSummary.Snapshot snapshot = summary.snapshot();
            if (snapshot.maxAtMillis() != 0) {
                assertEquals(snapshot.maxDurationNanos() + 100, snapshot.maxAtMillis());
            }
        } while (!done.get());
        writer.join();
        assertEquals(20_000, summary.getCount());
        assertEquals(19_999, summary.getMaxDurationNanos());
    }
}
