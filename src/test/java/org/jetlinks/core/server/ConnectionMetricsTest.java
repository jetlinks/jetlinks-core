package org.jetlinks.core.server;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConnectionMetricsTest {
    @Test
    public void legacyImplementationsAndSnapshotsKeepOptionalCapabilitiesAbsent() {
        ConnectionMetrics legacy = new LegacyMetrics();
        assertNull(legacy.toInfo().getAckCount());
        assertNull(legacy.toInfo().getReadErrors());
        assertNull(legacy.toInfo().getAckDurationP95());
        assertNull(new ConnectionMetricsInfo(0, 0, 0, 0, 0, 0, 0).getWriteErrors());
    }

    @Test
    public void percentilesUseMergedCountsAndDeclareUnknownWithoutSamples() {
        long[] buckets = new long[ConnectionMetricsSupport.ACK_DURATION_BUCKET_COUNT];
        buckets[ConnectionMetricsSupport.ackDurationBucket(1_000_000)] = 100;
        buckets[ConnectionMetricsSupport.ackDurationBucket(10_000_000_000L)] = 1;
        var info = new ConnectionMetricsInfo();
        info.setAckDurationBuckets(buckets);
        info.setAckMaxDuration(10D);
        assertEquals(0.001, info.getAckDurationP95(), 0.000001);
        buckets[0] = 0;
        assertEquals(10, info.getAckDurationP95(), 0.000001);
        assertNull(ConnectionMetricsSupport.ackDurationPercentile(new long[14], 0.95, 0D));
        assertNull(ConnectionMetricsSupport.ackDurationPercentile(null, 0.95, 1D));
        assertNull(ConnectionMetricsSupport.ackDurationPercentile(buckets, Double.NaN, 1D));
    }

    @Test
    public void sharedHistogramRetainsAckBoundariesAndObservationTrailer() {
        long[] bounds = {1_000_000L, 5_000_000L, 10_000_000L, 25_000_000L, 50_000_000L,
            100_000_000L, 250_000_000L, 500_000_000L, 1_000_000_000L, 2_500_000_000L,
            5_000_000_000L, 10_000_000_000L, 30_000_000_000L};
        for (int i = 0; i < bounds.length; i++) {
            assertEquals(i, ConnectionMetricsSupport.ackDurationBucket(bounds[i] - 1));
            assertEquals(i, ConnectionMetricsSupport.ackDurationBucket(bounds[i]));
            assertEquals(i + 1, ConnectionMetricsSupport.ackDurationBucket(bounds[i] + 1));
        }
        long[] histogram = ConnectionMetricsSupport.createAckDurationHistogram();
        long start = histogram[ConnectionMetricsSupport.ACK_DURATION_BUCKET_COUNT];
        ConnectionMetricsSupport.recordAckDuration(histogram, 100_000_000);
        long[] copy = ConnectionMetricsSupport.ackDurationHistogramSnapshot(histogram);
        assertEquals(14, copy.length);
        assertEquals(start, histogram[14]);
        assertEquals(0.1, ConnectionMetricsSupport.ackDurationPercentile(copy, 0.95, 0.1), 0);
        assertNull(ConnectionMetricsSupport.ackDurationHistogramSnapshot(null));
        ConnectionMetricsSupport.recordAckDuration(null, 100);
    }

    @Test
    public void concurrentDurationAccumulationUsesNoPerCounterObjectsAndAvoidsNanosecondTotalOverflow() {
        var metric = new DurationMetric();
        java.util.stream.IntStream.range(0, 10000).parallel().forEach(i ->
            ConnectionMetricsSupport.addAckDuration(DurationMetric.DURATION, metric, 2_000_000));
        assertEquals(20, Double.longBitsToDouble(metric.duration), 0.000001);
        java.util.stream.IntStream.range(0, 100).parallel().forEach(i ->
            ConnectionMetricsSupport.addAckDuration(DurationMetric.DURATION, metric, Long.MAX_VALUE / 10));
        assertTrue(Double.isFinite(Double.longBitsToDouble(metric.duration)));
        assertTrue(Double.longBitsToDouble(metric.duration) > Long.MAX_VALUE / 1_000_000_000D);
    }

    private static class DurationMetric {
        static final java.util.concurrent.atomic.AtomicLongFieldUpdater<DurationMetric> DURATION =
            java.util.concurrent.atomic.AtomicLongFieldUpdater.newUpdater(DurationMetric.class, "duration");
        volatile long duration;
    }

    private static class LegacyMetrics implements ConnectionMetrics {
        public long getReadBytes() { return 0; }
        public long getWrittenBytes() { return 0; }
        public long getConnectTime() { return 0; }
        public long getLastCommTime() { return 0; }
        public long getTotalMessages() { return 0; }
        public long getPendingMessages() { return 0; }
        public long getDroppedMessages() { return 0; }
    }
}
