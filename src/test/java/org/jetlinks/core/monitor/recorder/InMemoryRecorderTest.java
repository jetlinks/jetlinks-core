package org.jetlinks.core.monitor.recorder;

import org.jetlinks.core.Key;
import org.jetlinks.core.monitor.observation.ObservationSpec;
import org.jetlinks.core.monitor.observation.Observations;
import org.jetlinks.core.monitor.observation.RecorderProvider;
import org.jetlinks.core.monitor.observation.RecorderSelection;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class InMemoryRecorderTest {

    private static final Key<Integer> INPUT = Key.intKey("input");
    private static final Key<Integer> OUTPUT = Key.intKey("output");

    private static InMemoryRecorder recorder() {
        return InMemoryRecorder.factory(Set.of("device"), Set.of("input", "output")).get();
    }

    @Test
    public void shouldFilterFieldsBeforeEvaluatingSuppliersAndKeepNamespacesSeparate() {
        var recorder = InMemoryRecorder.factory(Set.of("same"), Set.of("same", "input")).get();
        var calls = new AtomicInteger();
        ActionRecorder<Object> action = recorder.action("calculate");
        action.start(Context.empty());
        action.tag("same", "tag").attribute("same", "attribute")
            .tag(Key.intKey("secret"), (Supplier<Integer>) calls::incrementAndGet)
            .attribute(Key.intKey("secret"), (Supplier<Integer>) calls::incrementAndGet)
            .tags(Map.of("ignored", new Object()))
            .attributes(Map.of("input", 2, "ignored", new Object()))
            .value(new Object()).valueConverter(value -> { throw new AssertionError("must not inspect payload"); });
        action.complete();
        var snapshot = recorder.snapshot();
        var entry = snapshot.actions().get(0);
        assertEquals(Map.of("same", "tag"), entry.tags());
        assertEquals(Map.of("same", "attribute", "input", 2), entry.attributes());
        assertEquals(0, calls.get());
        assertFalse(snapshot.truncated());
        assertEquals(InMemoryRecorder.Status.completed, entry.status());
        assertTrue(entry.timestamp() > 0);
        assertTrue(entry.useNanos() >= 0);
    }

    @Test
    public void shouldPreserveTypedKeyMergeAndLocalParentage() {
        Key<List<Integer>> samples = Key.listKey("samples");
        var recorder = InMemoryRecorder.factory(Set.of(), Set.of("samples")).get();
        ActionRecorder<Object> parent = recorder.action("round").start(Context.empty());
        parent.child("window").start(Context.empty()).attribute(samples, List.of(1))
            .attribute(samples, List.of(2)).complete();
        parent.complete();
        var records = recorder.snapshot().actions();
        assertEquals(-1, records.get(0).parentId());
        assertEquals(records.get(0).id(), records.get(1).parentId());
        assertEquals(List.of(1, 2), records.get(1).attributes().get("samples"));
        assertEquals("window", records.get(1).action());
    }

    @Test
    public void shouldShareImmutableValuesAndKeepRecordSnapshotsStable() {
        var recorder = InMemoryRecorder.factory(Set.of(), Set.of("data")).get();
        var source = Map.of("values", List.of(1));
        var action = recorder.action("accept");
        action.attribute("data", source);
        var first = recorder.snapshot();
        action.attribute("data", Map.of("new", "accepted"));
        action.complete();
        var second = recorder.snapshot();
        recorder.close();
        Map<?, ?> accepted = (Map<?, ?>) first.actions().get(0).attributes().get("data");
        assertSame(source, accepted);
        assertEquals(List.of(1), accepted.get("values"));
        assertEquals(Map.of("new", "accepted"), second.actions().get(0).attributes().get("data"));
        assertThrows(UnsupportedOperationException.class, () -> first.actions().clear());
        assertThrows(UnsupportedOperationException.class, () -> first.actions().get(0).attributes().clear());
        assertThrows(UnsupportedOperationException.class, accepted::clear);
        assertThrows(UnsupportedOperationException.class, () -> ((List<?>) accepted.get("values")).clear());
        assertTrue(recorder.snapshot().actions().isEmpty());
    }

    @Test
    public void repeatedReferencesMustStillMergeAndEqualValuesMustKeepLatestReference() {
        var recorder = InMemoryRecorder.factory(Set.of(), Set.of("samples", "value")).get();
        var action = recorder.action("repeat");
        Key<List<Integer>> samples = Key.listKey("samples");
        var sameInput = List.of(1);
        action.attribute(samples, sameInput).attribute(samples, sameInput);
        String first = new String("same");
        String latest = new String("same");
        action.attribute("value", first).attribute("value", first);
        var before = recorder.snapshot();
        action.attribute("value", latest).complete();
        var after = recorder.snapshot();
        assertEquals(List.of(1, 1), after.actions().get(0).attributes().get("samples"));
        assertSame(first, before.actions().get(0).attributes().get("value"));
        assertSame(latest, after.actions().get(0).attributes().get("value"));
        recorder.close();
    }

    @Test
    public void shouldBatchSelectedFieldsAndPreserveEarlierSnapshots() {
        try (var recorder = InMemoryRecorder.factory(Set.of("same"), Set.of("same", "input", "output")).get()) {
            var action = recorder.action("batch");
            action.tags(Map.of("same", "tag")).attributes(Map.of("same", "attribute", "input", 1, "output", 2));
            var before = recorder.snapshot();
            var update = new HashMap<String, Object>();
            update.put("input", 3);
            update.put("output", null);
            update.put("ignored", 9);
            action.attributes(update).attributes(update).tags(null).attributes(null).complete();
            action.attributes(Map.of("input", 99));
            var after = recorder.snapshot();
            assertEquals(Map.of("same", "tag"), after.actions().get(0).tags());
            assertEquals(Map.of("same", "attribute", "input", 1, "output", 2), before.actions().get(0).attributes());
            assertEquals(Map.of("same", "attribute", "input", 3, "output", 2), after.actions().get(0).attributes());
            assertFalse(after.truncated());
        }
    }

    @Test
    public void shouldKeepABatchAtomicAndRetryAgainstConcurrentSingleFieldUpdates() throws Exception {
        var recorder = InMemoryRecorder.factory(Set.of(), Set.of("left", "right", "sum")).get();
        var action = recorder.action("batch");
        action.attributes(Map.of("left", 0, "right", 0, "sum", 0));
        var before = recorder.snapshot();
        var readingSecondField = new CountDownLatch(1);
        var release = new CompletableFuture<Void>();
        var reads = new AtomicInteger();
        Map<String, Object> update = new AbstractMap<>() {
            @Override public Set<Entry<String, Object>> entrySet() { throw new AssertionError("must only read selected keys"); }
            @Override public Object get(Object key) {
                if (!"left".equals(key) && !"right".equals(key)) return null;
                if (reads.incrementAndGet() == 2) {
                    readingSecondField.countDown();
                    release.join();
                }
                return 1;
            }
        };
        var worker = Executors.newSingleThreadExecutor();
        try {
            Future<?> write = worker.submit(() -> action.attributes(update));
            assertTrue(readingSecondField.await(5, TimeUnit.SECONDS));
            // 第二个字段尚未读完时，第一个字段不能提前发布给快照。
            assertEquals(before.actions().get(0).attributes(), recorder.snapshot().actions().get(0).attributes());
            action.attribute("sum", 7);
            release.complete(null);
            write.get(5, TimeUnit.SECONDS);
            assertEquals(Map.of("left", 1, "right", 1, "sum", 7), recorder.snapshot().actions().get(0).attributes());
            assertEquals(Map.of("left", 0, "right", 0, "sum", 0), before.actions().get(0).attributes());
            assertFalse(recorder.snapshot().truncated());
        } finally {
            release.complete(null);
            worker.shutdownNow();
            recorder.close();
        }
    }

    @Test
    public void shouldIsolateBatchExtractionFailuresAndSkipClosedReaders() {
        try (var recorder = recorder()) {
            var reads = new AtomicInteger();
            Map<String, Object> values = new AbstractMap<>() {
                @Override public Set<Entry<String, Object>> entrySet() { throw new AssertionError("must only read selected keys"); }
                @Override public Object get(Object key) {
                    reads.incrementAndGet();
                    if ("input".equals(key)) throw new IllegalArgumentException("unavailable input");
                    assertEquals("output", key);
                    return 4;
                }
            };
            var action = recorder.action("partial");
            action.attributes(values).complete();
            assertEquals(Map.of("output", 4), recorder.snapshot().actions().get(0).attributes());
            assertTrue(recorder.snapshot().truncated());
            assertEquals(2, reads.get());
            action.attributes(values);
            recorder.close();
            action.attributes(values);
            assertEquals(2, reads.get());
            assertTrue(recorder.snapshot().actions().isEmpty());
        }
    }

    @Test
    public void shouldBoundActionsAndStopClosedOrCompletedSuppliers() {
        var recorder = InMemoryRecorder.factory(Set.of(), Set.of("input"), 2).get();
        var parent = recorder.action("parent");
        var child = parent.child("child");
        assertSame(ActionRecorder.noop(), parent.child("overflow"));
        var calls = new AtomicInteger();
        child.complete().attribute(INPUT, (Supplier<Integer>) calls::incrementAndGet);
        assertTrue(recorder.snapshot().truncated());
        assertEquals(2, recorder.snapshot().actions().size());
        recorder.close();
        parent.attribute(INPUT, (Supplier<Integer>) calls::incrementAndGet).complete();
        assertSame(ActionRecorder.noop(), recorder.action("late"));
        assertSame(ActionRecorder.noop(), parent.child("late"));
        assertEquals(0, calls.get());
    }

    @Test
    public void shouldKeepLatestFieldValueAcrossRepeatedWrites() {
        var recorder = InMemoryRecorder.factory(Set.of(), Set.of("input"), 2).get();
        var action = recorder.action("calculate");
        for (int i = 0; i < 300; i++) action.attribute(INPUT, i);
        var before = recorder.snapshot();
        var calls = new AtomicInteger();
        action.attribute(INPUT, (Supplier<Integer>) () -> { calls.incrementAndGet(); return 300; }).complete();
        var after = recorder.snapshot();
        assertEquals(1, calls.get());
        assertEquals(299, before.actions().get(0).attributes().get("input"));
        assertEquals(Map.of("input", 300), after.actions().get(0).attributes());
        assertFalse(after.truncated());
    }

    @Test
    public void shouldRetainImmutableSummaryReferencesWithoutInspectingThem() {
        record Summary(int count) {
            @Override public String toString() { throw new AssertionError("summary inspected"); }
        }
        var summary = new Summary(3);
        var recorder = recorder();
        recorder.action("summary").attribute("input", summary).complete();
        assertSame(summary, recorder.snapshot().actions().get(0).attributes().get("input"));
        assertFalse(recorder.snapshot().truncated());
    }

    @Test
    public void shouldRetainPreciseImmutableNumbers() {
        var recorder = recorder();
        recorder.action("precise").attribute("input", Long.MAX_VALUE)
            .attribute("output", new BigDecimal("0.12345678901234567890123456789")).complete();
        var values = recorder.snapshot().actions().get(0).attributes();
        assertEquals(Long.MAX_VALUE, values.get("input"));
        assertEquals(new BigDecimal("0.12345678901234567890123456789"), values.get("output"));
        assertFalse(recorder.snapshot().truncated());
    }

    @Test
    public void shouldIsolateExtractionFailuresAndBoundErrorsWithoutRetainingThrowable() {
        var recorder = recorder();
        ActionRecorder<Object> action = recorder.action("failed").start(Context.empty());
        action.attribute(INPUT, (Supplier<Integer>) () -> { throw new IllegalStateException("extract"); });
        action.error(new IllegalArgumentException("x".repeat(1000)));
        action.cancel(); action.complete(); action.attribute(INPUT, 9);
        var entry = recorder.snapshot().actions().get(0);
        assertEquals(InMemoryRecorder.Status.failed, entry.status());
        assertEquals(IllegalArgumentException.class.getName(), entry.errorType());
        assertEquals(512, entry.errorMessage().length());
        assertTrue(entry.attributes().isEmpty());
        assertTrue(recorder.snapshot().truncated());
    }

    @Test
    public void shouldCombineWithOriginalRecorderAndObserveNestedStepsOnce() {
        var original = new OriginalRecorder();
        var memory = recorder();
        var calls = new AtomicInteger();
        ObservationSpec<Integer, Integer> root = ObservationSpec.<Integer, Integer>of("round")
            .onInput((value, metadata) -> metadata.tag(Key.stringKey("device"), "probe"))
            .onOutput((value, metadata) -> metadata.attribute(OUTPUT, value));
        ObservationSpec<Integer, Integer> child = ObservationSpec.<Integer, Integer>of("calculate")
            .onInput((value, metadata) -> metadata.attribute(INPUT, value))
            .onOutput((value, metadata) -> metadata.attribute(OUTPUT, value));
        RecorderProvider capture = (action, context) -> RecorderSelection.capture(Recorder.noop(), null);
        StepVerifier.create(Observations.observeMono(root, 2, value ->
                Observations.observeMono(child, value, input -> Mono.fromSupplier(() -> {
                    calls.incrementAndGet(); return input * 3;
                })))
            .contextWrite(context -> context.put(Recorder.class, Recorder.combine(original, memory))
                .put(InMemoryRecorder.class, memory).put(RecorderProvider.class, capture)))
            .expectNext(6).verifyComplete();
        assertEquals(1, calls.get());
        assertEquals(2, original.records.size());
        var records = memory.snapshot().actions();
        assertEquals(2, records.size());
        assertEquals(Map.of("device", "probe"), records.get(0).tags());
        assertEquals(Map.of("input", 2, "output", 6), records.get(1).attributes());
        assertEquals(records.get(0).id(), records.get(1).parentId());
        assertEquals(InMemoryRecorder.Status.completed, records.get(0).status());
    }

    @Test
    public void shouldPreserveBusinessErrorsCancellationAndPartialFluxOutput() {
        var failed = recorder();
        RuntimeException error = new IllegalStateException("business");
        StepVerifier.create(Flux.concat(Flux.just(1), Flux.error(error)).transform(failed.action("stream")))
            .expectNext(1).expectErrorMatches(actual -> actual == error).verify();
        assertEquals(InMemoryRecorder.Status.failed, failed.snapshot().actions().get(0).status());
        var cancelled = recorder();
        var cancellations = new AtomicInteger();
        StepVerifier.create(Mono.never().doOnCancel(cancellations::incrementAndGet)
            .transform(cancelled.action("wait"))).thenCancel().verify();
        assertEquals(1, cancellations.get());
        assertEquals(InMemoryRecorder.Status.cancelled, cancelled.snapshot().actions().get(0).status());
    }

    @Test
    public void shouldCreateIndependentCollectorsForRepeatedSubscriptionsAndReadFromContext() {
        var factory = InMemoryRecorder.factory(Set.of(), Set.of("input"));
        var snapshots = new ArrayList<InMemoryRecorder.Snapshot>();
        var count = new AtomicInteger();
        Mono<Integer> request = Mono.defer(() -> {
            var memory = factory.get();
            return Mono.deferContextual(context -> {
                InMemoryRecorder collector = context.get(InMemoryRecorder.class);
                var action = collector.action("request");
                action.start(context).attribute(INPUT, count.incrementAndGet()).complete();
                snapshots.add(collector.snapshot());
                return Mono.just(count.get());
            }).contextWrite(context -> context.put(InMemoryRecorder.class, memory))
                .doFinally(signal -> memory.close());
        });
        StepVerifier.create(request.repeat(1)).expectNext(1, 2).verifyComplete();
        assertEquals(1, snapshots.get(0).actions().get(0).attributes().get("input"));
        assertEquals(2, snapshots.get(1).actions().get(0).attributes().get("input"));
        assertEquals(0, snapshots.get(1).actions().get(0).id());
    }

    @Test
    public void shouldMergeParallelUpdatesWithoutLosingValuesOrMutatingSnapshots() throws Exception {
        var recorder = InMemoryRecorder.factory(Set.of(), Set.of("sum")).get();
        Key<Integer> sum = new Key<>() {
            @Override public String getKey() { return "sum"; }
            @Override public Integer apply(Integer old, Integer next) { return old + next; }
        };
        var action = recorder.action("parallel");
        var workers = Executors.newFixedThreadPool(8);
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<?>>();
        try {
            for (int worker = 0; worker < 8; worker++) futures.add(workers.submit(() -> {
                start.await();
                for (int i = 0; i < 200; i++) action.attribute(sum, 1);
                return null;
            }));
            start.countDown();
            for (Future<?> future : futures) future.get();
            var before = recorder.snapshot();
            action.attribute(sum, 1).complete();
            assertEquals(1600, before.actions().get(0).attributes().get("sum"));
            assertEquals(1601, recorder.snapshot().actions().get(0).attributes().get("sum"));
            assertFalse(recorder.snapshot().truncated());
        } finally { workers.shutdownNow(); recorder.close(); }
    }

    @Test
    public void shouldReleaseConcurrentAdmissionsAndIgnoreLateWrites() throws Exception {
        var workers = Executors.newFixedThreadPool(4);
        try {
            for (int round = 0; round < 20; round++) {
                var recorder = recorder();
                var start = new CountDownLatch(1);
                var futures = new ArrayList<Future<?>>();
                for (int i = 0; i < 4; i++) futures.add(workers.submit(() -> {
                    start.await();
                    for (int j = 0; j < 32; j++) recorder.action("work").attribute(INPUT, j).complete();
                    return null;
                }));
                start.countDown();
                recorder.close();
                for (Future<?> future : futures) future.get();
                assertTrue(recorder.snapshot().actions().isEmpty());
            }
        } finally { workers.shutdownNow(); }
    }

    @Test
    public void shouldReleaseAnActionPublishedAfterClose() throws Exception {
        var recorder = recorder();
        var worker = Executors.newSingleThreadExecutor();
        var reserved = new CountDownLatch(1);
        var publish = new CompletableFuture<String>();
        CharSequence name = new CharSequence() {
            @Override public int length() { return 4; }
            @Override public char charAt(int index) { return "work".charAt(index); }
            @Override public CharSequence subSequence(int start, int end) { return "work".subSequence(start, end); }
            @Override public String toString() { reserved.countDown(); return publish.join(); }
        };
        try {
            Future<ActionRecorder<Object>> action = worker.submit(() -> recorder.action(name));
            assertTrue(reserved.await(5, TimeUnit.SECONDS));
            // 在槽位预留后、发布前关闭，确保迟到发布者不能留下可达操作。
            recorder.close();
            publish.complete("work");
            assertSame(ActionRecorder.noop(), action.get(5, TimeUnit.SECONDS));
            assertTrue(recorder.snapshot().actions().isEmpty());
        } finally {
            publish.complete("work");
            worker.shutdownNow();
            recorder.close();
        }
    }

    @Test
    public void shouldBoundConcurrentAdmissionsAndPublishDistinctSlots() throws Exception {
        var recorder = recorder();
        var workers = Executors.newFixedThreadPool(4);
        var start = new CountDownLatch(1);
        var futures = new ArrayList<Future<?>>();
        try {
            for (int worker = 0; worker < 4; worker++) futures.add(workers.submit(() -> {
                start.await();
                for (int i = 0; i < 32; i++) recorder.action("work").attribute(INPUT, i).complete();
                return null;
            }));
            start.countDown();
            for (Future<?> future : futures) future.get(5, TimeUnit.SECONDS);
            var snapshot = recorder.snapshot();
            assertTrue(snapshot.truncated());
            assertEquals(32, snapshot.actions().size());
            for (int i = 0; i < snapshot.actions().size(); i++) {
                var action = snapshot.actions().get(i);
                assertEquals(i, action.id());
                assertEquals(InMemoryRecorder.Status.completed, action.status());
                assertTrue(action.attributes().containsKey("input"));
            }
        } finally {
            start.countDown();
            workers.shutdownNow();
            recorder.close();
        }
    }

    @Test
    public void shouldValidateConfigurationAndCopySelectedNames() {
        assertThrows(IllegalArgumentException.class, () -> InMemoryRecorder.factory(Set.of(), Set.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> InMemoryRecorder.factory(Set.of(), Set.of(), -1));
        var tags = new java.util.HashSet<>(Set.of("device"));
        var factory = InMemoryRecorder.factory(tags, Set.of());
        tags.clear();
        var recorder = factory.get();
        recorder.action("work").tag("device", "a").complete();
        assertEquals(Map.of("device", "a"), recorder.snapshot().actions().get(0).tags());
    }

    private static void assertThrows(Class<? extends Throwable> expected, Runnable operation) {
        try {
            operation.run();
        } catch (Throwable error) {
            assertTrue("unexpected error: " + error, expected.isInstance(error));
            return;
        }
        fail("expected " + expected.getName());
    }

    private static final class OriginalRecorder implements Recorder {
        private final ConcurrentLinkedQueue<ActionRecord> records = new ConcurrentLinkedQueue<>();
        @Override public <E> ActionRecorder<E> action(CharSequence action) { return create(action, null); }
        private <E> ActionRecorder<E> create(CharSequence name, String parent) {
            return new AbstractActionRecorder<>(name, parent) {
                @Override protected void handle(ActionRecord record) { records.add(record); }
                @Override public <T> ActionRecorder<T> child(CharSequence action) { return OriginalRecorder.this.create(action, record.getId()); }
            };
        }
    }
}
