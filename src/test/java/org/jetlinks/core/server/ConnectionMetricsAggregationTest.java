package org.jetlinks.core.server;

import com.alibaba.fastjson.JSON;
import org.jetlinks.core.device.session.DeviceConnectionInfo;
import org.jetlinks.core.device.session.DeviceSessionInfo;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.session.MultiConnectionDeviceSession;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.Assert.*;

public class ConnectionMetricsAggregationTest {

    @Test
    public void timeBoundariesAndCountersRemainCorrectAcrossSessionAggregation() {
        var first = new ConnectionMetricsInfo(10, 20, 100, 800, 3, 4, 5);
        var second = new ConnectionMetricsInfo(30, 40, 300, 900, 6, 7, 8);
        var third = new ConnectionMetricsInfo(50, 60, 200, 700, 9, 10, 11);
        first.setWriteErrors(1L);
        second.setWriteErrors(2L);
        third.setWriteErrors(3L);

        var result = aggregate(aggregate(first, second), aggregate(third));
        assertEquals(90, result.getReadBytes());
        assertEquals(120, result.getWriteBytes());
        assertEquals(18, result.getTotalMessages());
        assertEquals(21, result.getPendingMessages());
        assertEquals(24, result.getDroppedMessages());
        assertEquals(Long.valueOf(6), result.getWriteErrors());
        assertEquals(100, result.getConnectTime());
        assertEquals(300, result.getLatestConnectTime());
        assertEquals(700, result.getEarliestLastCommTime());
        assertEquals(900, result.getLastCommTime());
        assertEquals(100, first.getLatestConnectTime());
        assertEquals(800, first.getEarliestLastCommTime());
    }

    @Test
    public void confirmationsAreWeightedAndDistributionsMergedWithoutMutatingSources() {
        var fast = confirmation(100, 1_000_000L);
        var slow = confirmation(1, 10_000_000_000L);
        fast.setAckDurationObservationStart(100L);
        slow.setAckDurationObservationStart(200L);
        var waiting = new ConnectionMetricsInfo();
        waiting.setAckCount(0L);
        waiting.setAckTimeouts(2L);
        var result = aggregate(aggregate(fast, waiting), aggregate(slow, new ConnectionMetricsInfo()));

        assertEquals(Long.valueOf(101), result.getAckCount());
        assertEquals(Long.valueOf(2), result.getAckTimeouts());
        assertEquals(10.1 / 101, result.getAckAverageDuration(), 0.000001);
        assertEquals(10, result.getAckMaxDuration(), 0.000001);
        assertNull(result.getAckLastDuration());
        assertEquals(0.001, aggregate(fast).getAckLastDuration(), 0.000001);
        assertEquals(0.001, result.getAckDurationP95(), 0.000001);
        assertEquals(Long.valueOf(101), result.getAckDurationSampleCount());
        assertNull(result.getAckDurationObservationStart());
        assertNotSame(fast.getAckDurationBuckets(), result.getAckDurationBuckets());
        assertEquals(100, fast.getAckDurationBuckets()[0]);

        slow.setAckDurationObservationStart(100L);
        assertEquals(Long.valueOf(100), aggregate(fast, slow).getAckDurationObservationStart());

        slow.setAckDurationBuckets(null);
        result = aggregate(fast, slow);
        assertNull(result.getAckDurationP95());
        assertNull(result.getAckDurationBuckets());
        assertNull(result.getAckDurationObservationStart());
        assertEquals(10.1 / 101, result.getAckAverageDuration(), 0.000001);
    }

    @Test
    public void missingSnapshotsAndCapabilitiesNeverBecomeSuccessfulZeroValues() {
        assertNull(ConnectionMetricsInfo.aggregate(Collections.<ConnectionMetricsInfo>emptyList(), Function.identity()));
        assertNull(aggregate(new ConnectionMetricsInfo(), null));
        var measured = confirmation(1, 25_000_000);
        measured.setWriteErrors(0L);
        var unsupported = new ConnectionMetricsInfo();
        var result = aggregate(measured, unsupported);
        assertNull(result.getWriteErrors());
        assertEquals(0.025, result.getAckAverageDuration(), 0.000001);

        var incomplete = confirmation(1, 100_000_000);
        incomplete.setAckTotalDuration(null);
        incomplete.setAckMaxDuration(null);
        result = aggregate(measured, incomplete);
        assertNull(result.getAckAverageDuration());
        assertNull(result.getAckMaxDuration());
        assertNull(result.getAckDurationP95());
        var waiting = new ConnectionMetricsInfo();
        waiting.setAckCount(0L);
        waiting.setAckTimeouts(2L);
        result = aggregate(waiting);
        assertEquals(Long.valueOf(2), result.getAckTimeouts());
        assertNull(result.getAckAverageDuration());
    }

    @Test
    public void invalidOrOverflowingDistributionsCannotProducePercentiles() {
        var valid = confirmation(1, 1_000_000);
        var invalid = confirmation(1, 1_000_000);
        invalid.getAckDurationBuckets()[0] = -1;
        assertNull(aggregate(valid, invalid).getAckDurationP95());
        invalid.setAckDurationBuckets(new long[1]);
        assertNull(aggregate(valid, invalid).getAckDurationP95());
        invalid.setAckDurationBuckets(new long[ConnectionMetricsSupport.ACK_DURATION_BUCKET_COUNT]);
        invalid.getAckDurationBuckets()[0] = Long.MAX_VALUE;
        assertNull(aggregate(valid, invalid).getAckDurationP95());
        assertEquals(1, valid.getAckDurationBuckets()[0]);
    }

    @Test
    public void publishedConstructorsRemainAvailable() {
        var original = new ConnectionMetricsInfo(1, 2, 3, 4, 5, 6, 7,
            8L, 9L, 10L, 11L, 12L, 13L, 14D, 15D, 16D, 17D, 18D, 19L, null);
        assertEquals(1, original.getReadBytes());
        assertEquals(Long.valueOf(8), original.getReadErrors());
        assertEquals(Double.valueOf(18), original.getAckLastDuration());
        assertEquals(Long.valueOf(19), original.getAckDurationObservationStart());
        assertEquals(3, original.getLatestConnectTime());
        assertEquals(4, original.getEarliestLastCommTime());
    }

    @Test
    public void sessionReadsEachConnectionSnapshotOnlyOnce() {
        var session = new MultiConnectionDeviceSession<ClientConnection>("device", null, null) {
            public long connectTime() { return 1; }
            public long lastPingTime() { return 2; }
            public Transport getTransport() { return null; }
            public void ping() { }
            public void setKeepAliveTimeout(Duration timeout) { }
        };
        var first = new SnapshotConnection(new ConnectionMetricsInfo(10, 20, 100, 800, 1, 0, 0));
        var second = new SnapshotConnection(new ConnectionMetricsInfo(30, 40, 300, 900, 1, 0, 0));
        session.registerConnection(first);
        session.registerConnection(second);
        var snapshot = DeviceSessionInfo.of("server", session);
        assertEquals(2, snapshot.getConnections().size());
        assertEquals(40, snapshot.getMetrics().getReadBytes());
        assertEquals(300, snapshot.getMetrics().getLatestConnectTime());
        assertEquals(1, first.snapshots);
        assertEquals(1, second.snapshots);
        assertNotNull(snapshot.getConnectionLifecycle());
        session.close();
        assertEquals(40, snapshot.getMetrics().getReadBytes());
        assertNull(DeviceSessionInfo.of("server", session).getMetrics());
    }

    @Test
    public void largeCollectionsNeedOneVisitPerSourceAndNoDefaultHistogram() {
        var metric = new ConnectionMetricsInfo(1, 2, 100, 200, 3, 0, 0);
        int[] visits = {0};
        var result = ConnectionMetricsInfo.aggregate(Collections.nCopies(10_000, metric), current -> {
            visits[0]++;
            return current;
        });
        assertEquals(10_000, visits[0]);
        assertEquals(10_000, result.getReadBytes());
        assertEquals(30_000, result.getTotalMessages());
        assertNull(result.getAckDurationBuckets());
    }

    @Test
    public void sessionAggregateAndTimeBoundariesSurviveJsonRoundTrip() {
        var first = new DeviceConnectionInfo();
        first.setMetrics(new ConnectionMetricsInfo(10, 20, 100, 800, 1, 0, 0));
        var second = new DeviceConnectionInfo();
        second.setMetrics(new ConnectionMetricsInfo(30, 40, 300, 900, 1, 0, 0));
        var session = new DeviceSessionInfo();
        session.setConnections(List.of(first, second));
        session.setMetrics(ConnectionMetricsInfo.aggregate(session.getConnections(), DeviceConnectionInfo::getMetrics));
        var copy = JSON.parseObject(JSON.toJSONString(session), DeviceSessionInfo.class);
        assertEquals(2, copy.getConnections().size());
        assertEquals(40, copy.getMetrics().getReadBytes());
        assertEquals(100, copy.getMetrics().getConnectTime());
        assertEquals(300, copy.getMetrics().getLatestConnectTime());
        assertEquals(800, copy.getMetrics().getEarliestLastCommTime());
        assertEquals(900, copy.getMetrics().getLastCommTime());
        assertNull(copy.getMetrics().getAckAverageDuration());
    }

    private static ConnectionMetricsInfo aggregate(ConnectionMetricsInfo... snapshots) {
        return ConnectionMetricsInfo.aggregate(Arrays.asList(snapshots), Function.identity());
    }

    private static ConnectionMetricsInfo confirmation(long count, long nanos) {
        var result = new ConnectionMetricsInfo();
        result.setAckCount(count);
        result.setAckTimeouts(0L);
        result.setAckTotalDuration(count * nanos / 1_000_000_000D);
        result.setAckMaxDuration(nanos / 1_000_000_000D);
        result.setAckLastDuration(nanos / 1_000_000_000D);
        var buckets = new long[ConnectionMetricsSupport.ACK_DURATION_BUCKET_COUNT];
        buckets[ConnectionMetricsSupport.ackDurationBucket(nanos)] = count;
        result.setAckDurationBuckets(buckets);
        return result;
    }

    private static class SnapshotConnection implements ClientConnection {
        private final ConnectionMetricsInfo snapshot;
        private int snapshots;

        private SnapshotConnection(ConnectionMetricsInfo snapshot) {
            this.snapshot = snapshot;
        }

        public InetSocketAddress address() { return null; }
        public Mono<Void> sendMessage(EncodedMessage message) { return Mono.empty(); }
        public Flux<EncodedMessage> receiveMessage() { return Flux.empty(); }
        public void disconnect() { }
        public boolean isAlive() { return true; }

        public ConnectionMetrics metrics() {
            return new ConnectionMetrics() {
                public long getReadBytes() { return snapshot.getReadBytes(); }
                public long getWrittenBytes() { return snapshot.getWriteBytes(); }
                public long getConnectTime() { return snapshot.getConnectTime(); }
                public long getLastCommTime() { return snapshot.getLastCommTime(); }
                public long getTotalMessages() { return snapshot.getTotalMessages(); }
                public long getPendingMessages() { return snapshot.getPendingMessages(); }
                public long getDroppedMessages() { return snapshot.getDroppedMessages(); }
                public ConnectionMetricsInfo toInfo() { snapshots++; return snapshot; }
            };
        }
    }
}
