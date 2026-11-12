package org.jetlinks.core.monitor.metrics;

import org.junit.Test;

import java.time.Duration;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class WindowMaxTest {
    private static final long HOUR = 3_600_000;

    @Test
    public void peakExpiresWithoutATimerAndLateOldWindowsCannotOverwriteTheCurrentOne() {
        WindowMax peak = new WindowMax(Duration.ofHours(1));
        assertFalse(peak.snapshot(0).available());
        peak.record(-5, 100);
        peak.record(-7, 200);
        peak.record(-5, 300);
        assertEquals(-5, peak.snapshot(400).maximum());
        assertEquals(100, peak.snapshot(400).occurredAtMillis());
        assertFalse(peak.snapshot(HOUR).available());
        peak.record(0, HOUR + 2);
        peak.record(999, HOUR - 1);
        assertTrue(peak.snapshot(HOUR + 3).available());
        assertEquals(0, peak.snapshot(HOUR + 3).maximum());
        assertEquals(HOUR + 2, peak.snapshot(HOUR + 3).occurredAtMillis());
        assertFalse(peak.snapshot(HOUR - 1).available());
        assertFalse(peak.snapshot(HOUR * 10_000).available());
        peak.record(8, HOUR * 10_000);
        assertEquals(8, peak.snapshot(HOUR * 10_000).maximum());
    }

    @Test
    public void fixedOffsetSupportsLocalMidnightWithoutChangingPeriodLength() {
        WindowMax peak = new WindowMax(Duration.ofDays(1), Duration.ofHours(-8));
        peak.record(42, 0);
        assertEquals(-8 * HOUR, peak.snapshot(0).startMillis());
        assertEquals(16 * HOUR, peak.snapshot(0).endMillis());
        assertFalse(peak.snapshot(16 * HOUR).available());
    }

    @Test
    public void concurrentPeaksKeepTheMatchingOccurrenceTime() {
        WindowMax peak = new WindowMax(Duration.ofHours(1));
        IntStream.range(0, 10_000).parallel().forEach(i -> peak.record(i, i + 1000));
        WindowMax.Snapshot snapshot = peak.snapshot(20_000);
        assertEquals(9999, snapshot.maximum());
        assertEquals(10_999, snapshot.occurredAtMillis());
    }
}
