package org.jetlinks.core.device.session;

import com.alibaba.fastjson.JSON;
import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.server.ClientConnection;
import org.jetlinks.core.server.ConnectionMetrics;
import org.jetlinks.core.server.ConnectionMetricsInfo;
import org.jetlinks.core.server.ConnectionMetricsSupport;
import org.jetlinks.core.server.session.MultiConnectionDeviceSession;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class DeviceSessionStatisticsTest {
    @Test
    public void externalFormPreservesAllMetricsAndCoverage() throws Exception {
        var m = metrics(100, 1_000_000);
        m.setWriteBytes(9); m.setTotalMessages(8); m.setPendingMessages(7); m.setDroppedMessages(6);
        m.setLatestConnectTime(400L); m.setEarliestLastCommTime(600L);
        m.setReadErrors(0L); m.setTransportErrors(1L); m.setWriteSuccesses(2L); m.setWriteErrors(3L);
        var source = statistics(m).withCoverage(Set.of("one", "two"), Set.of("two"), true);
        var copy = copy(source);
        assertEquals(source.getSessionCount(), copy.getSessionCount());
        assertEquals(source.getConnectionCount(), copy.getConnectionCount());
        assertEquals(source.getSessionConnectTime(), copy.getSessionConnectTime());
        assertEquals(source.getLastCommunicationTime(), copy.getLastCommunicationTime());
        assertEquals(JSON.toJSONString(m), JSON.toJSONString(copy.getMetrics()));
        assertEquals(source.getRequestedServerIds(), copy.getRequestedServerIds());
        assertEquals(source.getFailedServerIds(), copy.getFailedServerIds());
        assertTrue(copy.isCoverageKnown());
        assertFalse(copy.isComplete());
        assertNotSame(m.getAckDurationBuckets(), copy.getMetrics().getAckDurationBuckets());
    }

    @Test
    public void zeroUnknownAndLegacyDerivedMetricsRemainDistinct() throws Exception {
        var empty = copy(new DeviceSessionStatistics().withCoverage(Set.of("one"), Set.of(), true));
        assertTrue(empty.isComplete());
        assertEquals(Long.valueOf(0), empty.getConnectionCount());
        assertNull(empty.getMetrics());
        var unknown = new DeviceSessionStatistics();
        unknown.setConnectionCount(null);
        unknown.setMetrics(new ConnectionMetricsInfo());
        unknown.getMetrics().setAckAverageDuration(1.2);
        unknown.getMetrics().setAckDurationP95(2.3);
        var copy = copy(unknown);
        assertFalse(copy.isComplete());
        assertNull(copy.getConnectionCount());
        assertNull(copy.getMetrics().getReadErrors());
        assertEquals(1.2, copy.getMetrics().getAckAverageDuration(), 0);
        assertEquals(2.3, copy.getMetrics().getAckDurationP95(), 0);
        assertEquals(0, copy.getMetrics().getReadBytes());
    }

    @Test
    public void nodeAggregationAfterEncodingMatchesFlatConnectionAggregation() throws Exception {
        var first = statistics(ConnectionMetricsInfo.aggregate(List.of(metrics(100, 1_000_000), metrics(50, 1_000_000)), value -> value));
        first.setConnectionCount(2L);
        var second = statistics(metrics(1, 10_000_000_000L));
        second.setSessionConnectTime(500); second.setLastCommunicationTime(900);
        var result = DeviceSessionStatistics.aggregate(List.of(copy(first), copy(second), copy(new DeviceSessionStatistics())));
        assertEquals(2, result.getSessionCount());
        assertEquals(Long.valueOf(3), result.getConnectionCount());
        assertEquals(10.15 / 151, result.getMetrics().getAckAverageDuration(), 0.000001);
        assertEquals(0.001, result.getMetrics().getAckDurationP95(), 0.000001);
        assertNull(result.getMetrics().getAckLastDuration());
        assertEquals(100, result.getSessionConnectTime());
        assertEquals(900, result.getLastCommunicationTime());
        var missing = new DeviceSessionStatistics(); missing.setSessionCount(1); missing.setConnectionCount(null);
        var incomplete = DeviceSessionStatistics.aggregate(List.of(first, missing));
        assertNull(incomplete.getConnectionCount());
        assertNull(incomplete.getMetrics());
    }

    @Test
    public void directReadNeverBuildsConnectionIdentityAndReadsMetricsOnce() {
        int[] snapshots = {0};
        var session = new MultiConnectionDeviceSession<ClientConnection>("device", null, null) {
            public long connectTime() { return 100; }
            public long lastPingTime() { return 800; }
            public Transport getTransport() { return null; }
            public void ping() { }
            public void setKeepAliveTimeout(Duration timeout) { }
        };
        session.add(new ClientConnection() {
            public String id() { throw new AssertionError("connection identity must not be read"); }
            public InetSocketAddress address() { throw new AssertionError("address must not be read"); }
            public Mono<Void> sendMessage(EncodedMessage message) { return Mono.empty(); }
            public Flux<EncodedMessage> receiveMessage() { return Flux.empty(); }
            public void disconnect() { }
            public boolean isAlive() { return true; }
            public ConnectionMetrics metrics() {
                return new ConnectionMetrics() {
                    public long getReadBytes() { return 10; }
                    public long getWrittenBytes() { return 0; }
                    public long getConnectTime() { return 200; }
                    public long getLastCommTime() { return 700; }
                    public long getTotalMessages() { return 0; }
                    public long getPendingMessages() { return 0; }
                    public long getDroppedMessages() { return 0; }
                    public ConnectionMetricsInfo toInfo() { snapshots[0]++; return DeviceSessionStatisticsTest.metrics(1, 1_000_000); }
                };
            }
        });
        var result = DeviceSessionStatistics.of(session);
        assertEquals(1, snapshots[0]);
        assertEquals(Long.valueOf(1), result.getConnectionCount());
        assertEquals(100, result.getSessionConnectTime());
        assertEquals(800, result.getLastCommunicationTime());
        assertNotNull(result.getMetrics());
        session.clear();
        assertEquals(Long.valueOf(1), result.getConnectionCount());
        assertEquals(Long.valueOf(0), DeviceSessionStatistics.of(session).getConnectionCount());
    }

    @Test
    public void malformedAndTruncatedExternalFormsFailInsteadOfReturningEmptyStatistics() throws Exception {
        byte[] encoded = encode(statistics(metrics(1, 1_000_000)));
        for (int length : new int[]{4, encoded.length / 2, encoded.length - 1}) {
            try (var in = new ObjectInputStream(new ByteArrayInputStream(Arrays.copyOf(encoded, length)))) {
                assertReadFails(in);
            }
        }
        var out = new ByteArrayOutputStream();
        try (var objects = new ObjectOutputStream(out)) { objects.writeByte(99); }
        try (var in = new ObjectInputStream(new ByteArrayInputStream(out.toByteArray()))) {
            assertReadFails(in);
        }
    }

    private static DeviceSessionStatistics statistics(ConnectionMetricsInfo metrics) {
        var result = new DeviceSessionStatistics();
        result.setSessionCount(1); result.setConnectionCount(1L);
        result.setSessionConnectTime(100); result.setLastCommunicationTime(800); result.setMetrics(metrics);
        return result;
    }

    private static ConnectionMetricsInfo metrics(long count, long nanos) {
        var result = new ConnectionMetricsInfo(10, 0, 200, 700, 1, 0, 0);
        result.setAckCount(count); result.setAckTimeouts(0L);
        result.setAckTotalDuration(count * nanos / 1_000_000_000D);
        result.setAckMaxDuration(nanos / 1_000_000_000D); result.setAckLastDuration(nanos / 1_000_000_000D);
        result.setAckDurationObservationStart(1000L);
        long[] buckets = new long[ConnectionMetricsSupport.ACK_DURATION_BUCKET_COUNT];
        buckets[ConnectionMetricsSupport.ackDurationBucket(nanos)] = count;
        result.setAckDurationBuckets(buckets);
        return result;
    }

    private static byte[] encode(DeviceSessionStatistics value) throws IOException {
        var output = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(output)) { value.writeExternal(out); }
        return output.toByteArray();
    }

    private static void assertReadFails(ObjectInputStream in) {
        try {
            new DeviceSessionStatistics().readExternal(in);
        } catch (IOException expected) {
            return;
        }
        fail("invalid external form must not decode as a successful empty result");
    }

    private static DeviceSessionStatistics copy(DeviceSessionStatistics value) throws IOException {
        try (var in = new ObjectInputStream(new ByteArrayInputStream(encode(value)))) {
            var result = new DeviceSessionStatistics(); result.readExternal(in); return result;
        }
    }
}
