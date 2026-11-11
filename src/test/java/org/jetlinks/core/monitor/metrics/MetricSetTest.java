package org.jetlinks.core.monitor.metrics;

import io.swagger.v3.oas.annotations.media.Schema;
import org.jetlinks.core.annotation.metric.Counter;
import org.jetlinks.core.annotation.metric.Duration;
import org.jetlinks.core.annotation.metric.Gauge;
import org.jetlinks.core.annotation.metric.Timestamp;
import org.jetlinks.core.metadata.UnitSupported;
import org.jetlinks.core.metadata.types.ObjectType;
import org.junit.Test;
import org.springframework.core.annotation.Order;

import java.util.Locale;

import static org.junit.Assert.*;

public class MetricSetTest {
    @Test
    public void discoveryIsSharedAndReadersUseTheCurrentTargetState() throws Throwable {
        MetricSet set = MetricSet.of(QueueView.class);
        assertSame(set, MetricSet.of(QueueView.class));
        Queue first = new Queue();
        Queue second = new Queue();
        assertEquals(0, first.reads);
        Metric pending = metric(set, "pending");
        first.pending = 8;
        second.pending = 3;
        assertEquals(8L, pending.read(first));
        assertEquals(3L, pending.read(second));
        first.pending = 0;
        assertEquals(0L, pending.read(first));
        assertEquals(2, first.reads);
        assertEquals(MetricKind.counter, metric(set, "received").getKind());
        // 实现类的额外 Counter 注解不改变 QueueView 的 Gauge 契约。
        assertEquals(MetricKind.gauge, pending.getKind());
        assertEquals("pending", set.getMetrics().get(0).getId());
    }

    @Test
    public void unitsAreNormalizedToTheExistingDataTypeAndViewsDoNotMutateTemplates() {
        MetricSet set = MetricSet.of(QueueView.class);
        ObjectType metadata = set.getMetadata(Locale.ENGLISH);
        assertEquals("count", ((UnitSupported) metadata.getProperty("received").orElseThrow().getValueType()).getUnit().getId());
        assertEquals("nanoseconds", ((UnitSupported) metadata.getProperty("elapsed").orElseThrow().getValueType()).getUnit().getId());
        assertEquals("milliseconds", ((UnitSupported) metadata.getProperty("lastSuccess").orElseThrow().getValueType()).getUnit().getId());
        assertFalse(metadata.getProperty("received").orElseThrow().getExpand("metric")
            .map(java.util.Map.class::cast).orElseThrow().containsKey("unit"));
        metadata.getProperties().clear();
        assertEquals(4, set.getMetadata(Locale.ENGLISH).getProperties().size());
    }

    @Test
    public void invalidDefinitionsAndInheritedConflictsAreRejectedBeforeReading() {
        for (Class<?> type : new Class<?>[]{InvalidCounter.class, InvalidUnit.class, InvalidDuration.class,
            Conflicting.class, ConflictingUnit.class, DuplicateId.class}) {
            try {
                MetricSet.of(type);
                fail(type.getName());
            } catch (org.hswebframework.web.exception.I18nSupportException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }

    @Test
    public void getterErrorsRetainTheirOriginalIdentity() throws Throwable {
        Metric metric = metric(MetricSet.of(Broken.class), "value");
        IllegalStateException error = new IllegalStateException("failed read");
        try {
            metric.read((Broken) () -> { throw error; });
            fail("getter error");
        } catch (IllegalStateException expected) {
            assertSame(error, expected);
        }
    }

    @Test
    public void symbolicUnitsKeepTheExistingValueUnitsBehavior() {
        ObjectType metadata = MetricSet.of(SymbolCounter.class).getMetadata(Locale.ENGLISH);
        assertEquals("sample", ((UnitSupported) metadata.getProperty("value").orElseThrow().getValueType()).getUnit().getSymbol());
    }

    @Test
    public void childInterfacesCanOverrideLabelsWhileInheritingTheMetricContract() {
        var field = MetricSet.of(Renamed.class).getMetadata(Locale.ROOT).getProperty("value").orElseThrow();
        assertEquals("当前累计", field.getName());
        assertEquals(MetricKind.counter, metric(MetricSet.of(Renamed.class), "value").getKind());
    }

    private static Metric metric(MetricSet set, String id) {
        return set.getMetrics().stream().filter(metric -> id.equals(metric.getId())).findFirst().orElseThrow();
    }

    public interface QueueView {
        @Order(0) @Gauge @Schema(title = "待执行") long getPending();
        @Counter long getReceived();
        @Duration(unit = "nanoseconds") long getElapsed();
        @Timestamp long getLastSuccess();
        @Schema(hidden = true) @Counter long getHidden();
        long getSecret();
    }

    public static class Queue implements QueueView {
        long pending;
        int reads;
        @Override @Counter public long getPending() { reads++; return pending; }
        @Override public long getReceived() { return 20; }
        @Override public long getElapsed() { return 30; }
        @Override public long getLastSuccess() { return 40; }
        @Override public long getHidden() { return 0; }
        @Override public long getSecret() { throw new AssertionError("secret"); }
    }

    public interface InvalidCounter { @Counter String getValue(); }
    public interface InvalidDuration { @Duration String getValue(); }
    public interface InvalidUnit { @Gauge(unit = "milliseconds") String getValue(); }
    public interface SymbolCounter { @Counter(unit = "sample") long getValue(); }
    public interface Counted { @Counter long getValue(); }
    public interface Gauged { @Gauge long getValue(); }
    public interface Conflicting extends Counted, Gauged { }
    public interface ConflictingUnit extends Counted { @Override @Counter(unit = "seconds") long getValue(); }
    public interface Renamed extends Counted { @Override @Schema(title = "当前累计") long getValue(); }
    public interface DuplicateId {
        @Schema(name = "value") @Gauge long getFirst();
        @Schema(name = "value") @Gauge long getSecond();
    }
    public interface Broken { @Gauge long getValue(); }
}
