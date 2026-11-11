package org.jetlinks.core.monitor.metrics;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class RateMeterTest {
    @Test
    public void ratesUseTheSameUnitAsBatchedCountsAndCloseThePreviousInterval() {
        AtomicLong clock = new AtomicLong();
        RateMeter meter = new RateMeter(clock::get);
        assertEquals(0, meter.getMeanRate(), 0);
        meter.mark(10);
        assertEquals(0, meter.getOneMinuteRate(), 0);
        clock.set(TimeUnit.SECONDS.toNanos(5));
        meter.mark(20);
        assertEquals(2, meter.getOneMinuteRate(), 1e-12);
        assertEquals(2, meter.getFiveMinuteRate(), 1e-12);
        assertEquals(2, meter.getFifteenMinuteRate(), 1e-12);
        assertEquals(6, meter.getMeanRate(), 1e-12);
        clock.set(TimeUnit.SECONDS.toNanos(10));
        assertEquals(2 + (1 - Math.exp(-5D / 60)) * 2, meter.getOneMinuteRate(), 1e-12);
        assertEquals(30, meter.getCount());
    }

    @Test
    public void idleReadDecaysAllRatesWithoutARecurringTimerOrIdleLengthLoop() {
        AtomicLong clock = new AtomicLong();
        RateMeter meter = new RateMeter(clock::get);
        meter.mark(10);
        clock.set(TimeUnit.SECONDS.toNanos(5));
        assertEquals(2, meter.getOneMinuteRate(), 0);
        clock.addAndGet(TimeUnit.MINUTES.toNanos(1));
        assertEquals(2 * Math.exp(-1), meter.getOneMinuteRate(), 1e-12);
        assertEquals(2 * Math.exp(-0.2), meter.getFiveMinuteRate(), 1e-12);
        assertEquals(2 * Math.exp(-1D / 15), meter.getFifteenMinuteRate(), 1e-12);
        clock.addAndGet(TimeUnit.DAYS.toNanos(365));
        assertEquals(0, meter.getOneMinuteRate(), 0);
        assertEquals(0, meter.getFifteenMinuteRate(), 0);
        meter.mark();
        clock.addAndGet(TimeUnit.SECONDS.toNanos(5));
        assertTrue(meter.getOneMinuteRate() > 0);
        assertEquals(11, meter.getCount());
    }

    @Test
    public void concurrentMarksKeepTheTotalAndCrossTickBoundaries() {
        AtomicLong clock = new AtomicLong();
        RateMeter meter = new RateMeter(clock::get);
        IntStream.range(0, 20_000).parallel().forEach(i -> {
            clock.addAndGet(TimeUnit.MILLISECONDS.toNanos(1));
            meter.mark(3);
        });
        assertEquals(60_000, meter.getCount());
        clock.addAndGet(TimeUnit.SECONDS.toNanos(5));
        assertTrue(Double.isFinite(meter.getOneMinuteRate()));
        assertTrue(meter.getOneMinuteRate() > 0);
        assertEquals(2400, meter.getMeanRate(), 1e-9);
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeDeltaIsRejected() {
        RateMeter meter = new RateMeter(() -> 0);
        meter.mark(-1);
    }
}
