package org.jetlinks.core.server;

import org.jetlinks.core.monitor.metrics.MetricSet;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.TimeoutException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class ConnectionMetricsRecorderTest {
    @Test
    public void publicContractExposesTrafficAndRequestMetadataToObservationReaders() {
        var properties = MetricSet.of(ConnectionMetrics.class).getMetadata().getProperties();
        var read = properties.stream().filter(property -> property.getId().equals("readBytes")).findFirst().orElseThrow();
        assertEquals("接收字节", read.getName());
        assertTrue(read.getExpand("metric").isPresent());
        assertTrue(properties.stream().anyMatch(property -> property.getId().equals("requestErrors")));
        assertTrue(properties.stream().anyMatch(property -> property.getId().equals("ackAverageDuration")));
        assertTrue(properties.stream().noneMatch(property -> property.getId().equals("activeConnections")));
    }

    @Test
    public void trafficSurvivesReconnectAndConcurrentUpdatesWithoutCountingChunksAsMessages() {
        var metrics = new ConnectionMetricsRecorder();
        metrics.connecting();metrics.connectFailed();metrics.connecting();metrics.connected();
        IntStream.range(0, 10000).parallel().forEach(i -> {metrics.received(12);metrics.sent(8);});
        assertEquals(120000, metrics.getReadBytes());assertEquals(80000, metrics.getWrittenBytes());
        assertEquals(0, metrics.getTotalMessages());
        metrics.messageReceived();metrics.disconnected();metrics.connecting();metrics.connected();
        assertEquals(Long.valueOf(1), metrics.getActiveConnections());assertEquals(Long.valueOf(1), metrics.getReconnects());
        assertEquals(Long.valueOf(3), metrics.getConnectAttempts());assertEquals(Long.valueOf(1), metrics.getConnectFailures());
        assertEquals(120000, metrics.getReadBytes());assertEquals(1, metrics.getTotalMessages());
        assertTrue(metrics.getLastCommTime() > 0);
    }

    @Test
    public void requestsPreserveFailureAndCancellationAndDoNotLeavePendingCounts() {
        var metrics = new ConnectionMetricsRecorder();
        assertNull(metrics.getAckLastDuration());assertNull(metrics.getAckDurationP95());
        StepVerifier.create(metrics.recordRequest(Mono.just(42), error -> error instanceof TimeoutException))
            .expectNext(42).verifyComplete();
        assertEquals(0, metrics.getPendingMessages());assertNotNull(metrics.getAckLastDuration());
        StepVerifier.create(metrics.recordRequest(Mono.error(new TimeoutException()), error -> error instanceof TimeoutException))
            .expectError(TimeoutException.class).verify();
        StepVerifier.create(metrics.recordRequest(Mono.never(), error -> false))
            .then(() -> assertEquals(1, metrics.getPendingMessages())).thenCancel().verify();
        assertEquals(0, metrics.getPendingMessages());assertEquals(Long.valueOf(1), metrics.getRequestErrors());
        assertEquals(Long.valueOf(1), metrics.getAckTimeouts());
    }

    @Test
    public void repeatedSubscriptionsRetainContextAndReleaseTheirOwnPendingCount() {
        var metrics = new ConnectionMetricsRecorder();
        var executions = new AtomicInteger();
        Mono<String> request = metrics.recordRequest(Mono.deferContextual(context -> {
            executions.incrementAndGet();
            assertEquals(1, metrics.getPendingMessages());
            return Mono.just(context.get("request"));
        }), error -> false);
        assertEquals(0, executions.get());
        StepVerifier.create(request.contextWrite(context -> context.put("request", "first")))
            .expectNext("first").verifyComplete();
        StepVerifier.create(request.contextWrite(context -> context.put("request", "second")))
            .expectNext("second").verifyComplete();
        assertEquals(2, executions.get());
        assertEquals(0, metrics.getPendingMessages());
        assertEquals(Long.valueOf(2), metrics.toInfo().getAckCount());
    }

    @Test
    public void emptyCompletionIsNotAnAcknowledgementOrARequestFailure() {
        var metrics = new ConnectionMetricsRecorder();
        StepVerifier.create(metrics.recordRequest(Mono.empty(), error -> false)).verifyComplete();
        assertEquals(0, metrics.getPendingMessages());
        assertEquals(Long.valueOf(0), metrics.getRequestErrors());
        assertEquals(Long.valueOf(0), metrics.toInfo().getAckCount());
        assertNull(metrics.toInfo().getAckAverageDuration());
    }

    @Test
    public void snapshotsCarryRawAcknowledgementSamplesForWeightedAggregation() {
        var first = new ConnectionMetricsRecorder();
        var second = new ConnectionMetricsRecorder();
        assertEquals(Long.valueOf(0), first.toInfo().getAckCount());
        assertNull(first.toInfo().getAckLastDuration());
        StepVerifier.create(first.recordRequest(Mono.just(1), error -> false)).expectNext(1).verifyComplete();
        StepVerifier.create(second.recordRequest(Mono.just(2), error -> false)).expectNext(2).verifyComplete();
        StepVerifier.create(second.recordRequest(Mono.just(3), error -> false)).expectNext(3).verifyComplete();
        var one = first.toInfo();
        var two = second.toInfo();
        var combined = ConnectionMetricsInfo.aggregate(List.of(one, two), value -> value);
        assertNotNull(combined);
        assertEquals(Long.valueOf(3), combined.getAckCount());
        assertEquals(one.getAckTotalDuration() + two.getAckTotalDuration(), combined.getAckTotalDuration(), 1e-12);
        assertEquals(combined.getAckTotalDuration() / 3, combined.getAckAverageDuration(), 1e-12);
        assertNull(combined.getAckDurationP95());
        assertEquals("later requests do not mutate an earlier snapshot", Long.valueOf(1), one.getAckCount());
    }
}
