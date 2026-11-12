package org.jetlinks.core.monitor.metrics;

import org.junit.Test;

import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class FixedHistogramTest {
    @Test
    public void boundsAreSharedImmutableAndIncludeTheUpperBoundary() {
        long[] bounds = {1, 5, 10};
        FixedHistogram histogram = new FixedHistogram(bounds);
        bounds[0] = 99;
        assertEquals(0, histogram.bucketIndex(1));
        assertEquals(1, histogram.bucketIndex(2));
        assertEquals(1, histogram.bucketIndex(5));
        assertEquals(2, histogram.bucketIndex(10));
        assertEquals(3, histogram.bucketIndex(Long.MAX_VALUE));
    }

    @Test
    public void concurrentRecordAndSnapshotKeepOwnerTrailerOutsideTheDistribution() {
        FixedHistogram histogram = new FixedHistogram(1, 5, 10);
        long[] counts = new long[5];
        counts[4] = 123;
        long[] values = {1, 3, 7, 11};
        IntStream.range(0, 10_000).parallel().forEach(i -> histogram.record(counts, values[i % 4]));
        long[] copy = histogram.snapshot(counts);
        assertArrayEquals(new long[]{2500, 2500, 2500, 2500}, copy);
        histogram.record(counts, 0);
        assertEquals(2500, copy[0]);
        assertEquals(123, counts[4]);
    }

    @Test
    public void percentilesUseMergedCountsAndCapTheOverflowBucketAtTheRealMaximum() {
        FixedHistogram histogram = new FixedHistogram(1, 5, 10);
        long[] a = {99, 0, 0, 1};
        long[] b = {1, 0, 0, 99};
        long[] combined = {100, 0, 0, 100};
        assertEquals(1, histogram.percentile(a, 0.95, 50), 0);
        assertEquals(50, histogram.percentile(b, 0.95, 50), 0);
        assertEquals(50, histogram.percentile(combined, 0.95, 50), 0);
        assertTrue(Double.isNaN(histogram.percentile(histogram.createCounts(), 0.95, 0)));
        assertTrue(Double.isNaN(histogram.percentile(combined, Double.NaN, 50)));
        assertTrue(Double.isNaN(histogram.percentile(new long[]{-1, 0, 0, 0}, 0.95, 50)));
        assertTrue(Double.isNaN(histogram.percentile(new long[]{Long.MAX_VALUE, 1, 0, 0}, 0.95, 50)));
    }
}
