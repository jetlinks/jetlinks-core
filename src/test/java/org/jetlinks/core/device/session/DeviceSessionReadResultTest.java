package org.jetlinks.core.device.session;

import org.junit.Test;
import org.jetlinks.core.server.session.DeviceSession;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.Assert.*;

public class DeviceSessionReadResultTest {
    @Test
    public void unknownAndFailedCoverageCannotBecomeCompleteEmptyResults() {
        assertFalse(DeviceSessionReadResult.unknown(List.of()).isComplete());
        assertFalse(new DeviceSessionReadResult(List.of(), Set.of(), Set.of(), true).isComplete());
        assertFalse(new DeviceSessionReadResult(List.of(), Set.of("one", "two"), Set.of("two"), true).isComplete());
        assertTrue(new DeviceSessionReadResult(List.of(), Set.of("one", "two"), Set.of(), true).isComplete());
    }

    @Test
    public void coverageCannotBeChangedByMutatingConstructorCollections() {
        var sessions = new ArrayList<DeviceSessionInfo>();
        var requested = new HashSet<>(Set.of("one"));
        var failed = new HashSet<String>();
        var result = new DeviceSessionReadResult(sessions, requested, failed, true);
        sessions.add(new DeviceSessionInfo());
        requested.add("two");
        failed.add("one");
        assertTrue(result.isComplete());
        assertTrue(result.getSessions().isEmpty());
        assertEquals(Set.of("one"), result.getRequestedServerIds());
    }

    @Test
    public void oldManagerDefaultReportsUnknownCoverageAndPropagatesCancellation() {
        var reads = new HashMap<String, Flux<DeviceSessionInfo>>();
        var manager = new LegacyManager(reads);
        reads.put("one", Flux.just(new DeviceSessionInfo()));
        var snapshot = manager.getDeviceSessionSnapshot("one").block();
        assertFalse(snapshot.isComplete());
        assertFalse(snapshot.isCoverageKnown());
        assertEquals(1, snapshot.getSessions().size());
        var statistics = manager.getDeviceSessionStatistics("one").block();
        assertFalse(statistics.isComplete());
        assertFalse(statistics.isCoverageKnown());
        assertEquals(1, statistics.getSessionCount());
        var cancelled = new AtomicBoolean();
        reads.put("pending", Flux.<DeviceSessionInfo>never().doOnCancel(() -> cancelled.set(true)));
        StepVerifier.create(manager.getDeviceSessionStatistics("pending")).thenCancel().verify();
        assertTrue(cancelled.get());
        reads.put("failed", Flux.error(new IllegalStateException("unavailable")));
        StepVerifier.create(manager.getDeviceSessionSnapshot("failed")).expectError(IllegalStateException.class).verify();
    }

    /** An external manager which implements only the pre-existing contract. */
    private static final class LegacyManager implements DeviceSessionManager {
        private final Map<String, Flux<DeviceSessionInfo>> reads;

        LegacyManager(Map<String, Flux<DeviceSessionInfo>> reads) { this.reads = reads; }
        public Flux<DeviceSessionInfo> getDeviceSessionInfo(String id) { return reads.get(id); }
        public String getCurrentServerId() { throw new UnsupportedOperationException(); }
        public Mono<DeviceSession> compute(String id, Function<Mono<DeviceSession>, Mono<DeviceSession>> fn) { throw new UnsupportedOperationException(); }
        public Mono<DeviceSession> compute(String id, Mono<DeviceSession> creator, Function<DeviceSession, Mono<DeviceSession>> updater) { throw new UnsupportedOperationException(); }
        public Mono<DeviceSession> getSession(String id) { throw new UnsupportedOperationException(); }
        public Mono<DeviceSession> getSession(String id, boolean unregister) { throw new UnsupportedOperationException(); }
        public Flux<DeviceSession> getSessions() { throw new UnsupportedOperationException(); }
        public Mono<Long> remove(String id, boolean local) { throw new UnsupportedOperationException(); }
        public Mono<Long> remove(String id, Predicate<DeviceSession> filter) { throw new UnsupportedOperationException(); }
        public Mono<Boolean> isAlive(String id, boolean local) { throw new UnsupportedOperationException(); }
        public Mono<Boolean> checkAlive(String id, boolean local) { throw new UnsupportedOperationException(); }
        public Mono<Long> totalSessions(boolean local) { throw new UnsupportedOperationException(); }
        public Flux<DeviceSessionInfo> getSessionInfo() { throw new UnsupportedOperationException(); }
        public Flux<DeviceSessionInfo> getSessionInfo(String server) { throw new UnsupportedOperationException(); }
        public Flux<DeviceSessionInfo> getLocalSessionInfo() { throw new UnsupportedOperationException(); }
        public Disposable listenEvent(Function<DeviceSessionEvent, Mono<Void>> handler) { throw new UnsupportedOperationException(); }
    }
}
