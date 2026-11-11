package org.jetlinks.core.monitor.metrics;

import org.jetlinks.core.annotation.metric.Gauge;
import org.jetlinks.core.monitor.Monitor;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class MetricsObservationTest {
    @Test
    public void oldImplementationsAndNoopRemainUnsupportedWithoutParsingOrReading() {
        LegacyMetrics legacy = new LegacyMetrics();
        legacy.count("received", 1);
        assertEquals(1, legacy.count);
        for (Metrics metrics : new Metrics[]{legacy, Metrics.noop(), Monitor.noop().metrics()}) {
            MetricRegistration registration = metrics.observe("queue", View.class,
                () -> { throw new AssertionError("discovery must not read"); });
            assertFalse(registration.isSupported());
            assertTrue(registration.isDisposed());
            registration.dispose();
            registration.dispose();
        }
    }

    @Test
    public void lazyRegistrationAndDisposalStayBoundToTheOriginalOwner() {
        Owner first = new Owner();
        Owner second = new Owner();
        AtomicReference<Owner> owner = new AtomicReference<>(first);
        AtomicInteger lookups = new AtomicInteger();
        Metrics lazy = Metrics.lazy(() -> { lookups.incrementAndGet(); return owner.get(); });
        View target = () -> 1;
        MetricRegistration registration = lazy.observe("queue", View.class, target);
        assertTrue(registration.isSupported());
        assertSame(target, first.target);
        owner.set(second);
        registration.dispose();
        registration.dispose();
        assertTrue(first.registration.isDisposed());
        assertFalse(second.registration.isDisposed());
        assertEquals(1, lookups.get());
        lazy.observe("queue", View.class, target);
        assertSame(target, second.target);
        assertEquals(2, lookups.get());
    }

    public interface View { @Gauge long getPending(); }

    private static class LegacyMetrics implements Metrics {
        int count;
        @Override public void count(String operation, int inc) { count += inc; }
        @Override public void value(String operation, double value) { }
        @Override public void error(String operation, Throwable error) { }
    }

    private static class Owner extends LegacyMetrics {
        Object target;
        final Registration registration = new Registration();
        @Override
        public <T> MetricRegistration observe(String scope, Class<T> contract, T target) {
            this.target = target;
            return registration;
        }
    }

    private static class Registration implements MetricRegistration {
        boolean closed;
        @Override public boolean isSupported() { return true; }
        @Override public boolean isDisposed() { return closed; }
        @Override public void dispose() { closed = true; }
    }
}
