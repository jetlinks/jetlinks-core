package org.jetlinks.core.monitor;

import org.jetlinks.core.Key;
import org.jetlinks.core.monitor.observation.ObservationKeys;
import org.jetlinks.core.monitor.observation.ObservationReference;
import org.jetlinks.core.monitor.observation.ObservationSource;
import org.jetlinks.core.monitor.observation.ObservationSpec;
import org.jetlinks.core.monitor.observation.RecorderProvider;
import org.jetlinks.core.monitor.observation.RecorderSelection;
import org.jetlinks.core.monitor.recorder.ActionRecord;
import org.jetlinks.core.monitor.recorder.Recorder;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.concurrent.atomic.AtomicInteger;

import static org.jetlinks.core.monitor.observation.Observations.observeFlux;
import static org.jetlinks.core.monitor.observation.Observations.observeMono;
import static org.junit.Assert.*;

public class FluxObservationTest {

    @Test
    public void shouldRespectDemandAndCreateOneActionPerSubscription() {
        var recorder = new RecordingTestSupport.TestRecorder();
        var produced = new AtomicInteger();
        var selections = new AtomicInteger();
        RecorderProvider provider = (name, ctx) -> {
            selections.incrementAndGet();
            return RecorderSelection.disabled();
        };
        Flux<Integer> reports = observeFlux("device.history", () -> Flux.range(1, 5).doOnNext(v -> produced.incrementAndGet()))
            .contextWrite(Context.of(Recorder.class, recorder, RecorderProvider.class, provider));
        assertEquals(0, recorder.starts.get());
        StepVerifier.create(reports, 0)
            .then(() -> assertEquals(0, produced.get()))
            .thenRequest(2).expectNext(1, 2)
            .then(() -> assertEquals(2, produced.get()))
            .thenRequest(3).expectNext(3, 4, 5).verifyComplete();
        StepVerifier.create(reports).expectNextCount(5).verifyComplete();
        assertEquals(2, selections.get());
        assertEquals(2, recorder.records.size());
        assertEquals(5L, recorder.records.get(0).getValueCount());
        assertNotEquals(tag(recorder.records.get(0), ObservationKeys.EXECUTION_ID),
                        tag(recorder.records.get(1), ObservationKeys.EXECUTION_ID));
    }

    @Test
    public void shouldKeepNativeParentsForParallelAlarmActionsInEachReceiver() {
        var original = new RecordingTestSupport.TestRecorder();
        var extra = new RecordingTestSupport.TestRecorder();
        var source = new ObservationSource("alarmOccurrence", "alarm-once");
        RecorderProvider provider = (name, ctx) -> RecorderSelection.capture(extra, "v1");
        Mono<Void> closure = observeMono("alarm.trigger", () -> Flux.just("device.control", "notification.send")
            .flatMap(name -> observeMono(name, () -> Mono.just(name).subscribeOn(Schedulers.parallel())))
            .then());
        StepVerifier.create(closure.contextWrite(Context.of(Recorder.class, original,
            RecorderProvider.class, provider, ObservationSource.class, source))).verifyComplete();
        for (var receiver : new RecordingTestSupport.TestRecorder[]{original, extra}) {
            ActionRecord root = receiver.records.stream().filter(r -> "alarm.trigger".equals(r.getAction())).findFirst().orElseThrow();
            assertEquals(3, receiver.records.size());
            for (ActionRecord child : receiver.records) {
                assertEquals("alarm-once", tag(child, ObservationKeys.SOURCE_ID));
                if (child != root) {
                    assertEquals(root.getId(), child.getParentId());
                    assertEquals(tag(root, ObservationKeys.EXECUTION_ID), tag(child, ObservationKeys.PARENT_EXECUTION_ID));
                }
            }
        }
        assertEquals(tag(original.records.get(2), ObservationKeys.EXECUTION_ID),
                     tag(extra.records.get(2), ObservationKeys.EXECUTION_ID));
    }

    @Test
    public void shouldNotInheritAnAdditionalReceiverDisabledForChild() {
        var original = new RecordingTestSupport.TestRecorder();
        var extra = new RecordingTestSupport.TestRecorder();
        RecorderProvider provider = (name, ctx) -> "root".equals(name)
            ? RecorderSelection.capture(extra, "v1") : RecorderSelection.disabled();
        StepVerifier.create(observeMono("root", () -> observeMono("child", () -> Mono.just(1)))
            .contextWrite(Context.of(Recorder.class, original, RecorderProvider.class, provider)))
            .expectNext(1).verifyComplete();
        assertEquals(2, original.records.size());
        assertEquals(1, extra.records.size());
        assertEquals(original.records.get(1).getId(), original.records.get(0).getParentId());
    }

    @Test
    public void shouldKeepPartialOutputsErrorsCancellationAndEmptyCompletion() {
        var recorder = new RecordingTestSupport.TestRecorder();
        var error = new IllegalStateException("provider unavailable");
        StepVerifier.create(observeFlux(recorder, "partial", () -> Flux.just(1, 2).concatWith(Mono.error(error))))
            .expectNext(1, 2).expectErrorMatches(e -> e == error).verify();
        StepVerifier.create(observeFlux(recorder, "cancel", () -> Flux.range(1, 10)), 1)
            .expectNext(1).thenCancel().verify();
        StepVerifier.create(observeFlux(recorder, "empty", Flux::empty)).verifyComplete();
        StepVerifier.create(observeFlux(recorder, "throw", () -> { throw error; }))
            .expectErrorMatches(e -> e == error).verify();
        assertTrue(recorder.records.get(0).isHasError());
        assertEquals(2L, recorder.records.get(0).getValueCount());
        assertTrue(recorder.records.get(1).isCancel());
        assertEquals(0L, recorder.records.get(2).getValueCount());
        assertTrue(recorder.records.get(3).isHasError());
    }

    @Test
    public void shouldCreateNewExecutionsOnRetryAndKeepRemoteParentReference() {
        var recorder = new RecordingTestSupport.TestRecorder();
        var attempts = new AtomicInteger();
        Flux<Integer> remote = observeFlux("remote.send", () -> attempts.incrementAndGet() == 1
            ? Flux.error(new IllegalStateException("retry")) : Flux.just(1));
        StepVerifier.create(remote.retry(1).contextWrite(Context.of(Recorder.class, recorder,
            ObservationReference.class, new ObservationReference("upstream")))).expectNext(1).verifyComplete();
        assertEquals(2, recorder.records.size());
        for (var record : recorder.records) {
            assertNull(record.getParentId());
            assertEquals("upstream", tag(record, ObservationKeys.PARENT_EXECUTION_ID));
        }
        assertNotEquals(tag(recorder.records.get(0), ObservationKeys.EXECUTION_ID),
                        tag(recorder.records.get(1), ObservationKeys.EXECUTION_ID));
    }

    @Test
    public void shouldStopExtractionAtBudgetWithoutLosingOutputs() {
        var recorder = new RecordingTestSupport.TestRecorder();
        var extracted = new AtomicInteger();
        var spec = ObservationSpec.<Void, Integer>of("history.export").onOutput((value, metadata) -> {
            extracted.incrementAndGet();
            metadata.attribute(Key.stringKey("output"), value.toString());
        });
        StepVerifier.create(observeFlux(recorder, spec, null, ignored -> Flux.range(1, 1000))
            .contextWrite(Context.of(RecorderProvider.class, (RecorderProvider) (name, ctx) -> RecorderSelection.capture(Recorder.noop(), "v1"))))
            .expectNextCount(1000).verifyComplete();
        assertTrue(extracted.get() <= 65);
        assertEquals(ObservationKeys.TRUNCATED, tag(recorder.records.get(0), ObservationKeys.METADATA_STATUS));
    }

    @Test
    public void shouldStopFailedOutputExtractionAndLeaveNoopUnchanged() {
        var recorder = new RecordingTestSupport.TestRecorder();
        var extracted = new AtomicInteger();
        var spec = ObservationSpec.<Void, Integer>of("history.export").onOutput((value, metadata) -> {
            extracted.incrementAndGet();
            throw new IllegalStateException("bad extractor");
        });
        StepVerifier.create(observeFlux(recorder, spec, null, ignored -> Flux.range(1, 10))
            .contextWrite(Context.of(RecorderProvider.class, (RecorderProvider) (name, ctx) -> RecorderSelection.capture(Recorder.noop(), "v1"))))
            .expectNextCount(10).verifyComplete();
        assertEquals(1, extracted.get());
        assertEquals(ObservationKeys.FAILED, tag(recorder.records.get(0), ObservationKeys.METADATA_STATUS));
        StepVerifier.create(observeFlux(spec, null, ignored -> Flux.deferContextual(ctx -> {
            assertFalse(ctx.hasKey(ObservationReference.class));
            return Flux.range(1, 10);
        }))).expectNextCount(10).verifyComplete();
        assertEquals(1, extracted.get());
    }

    private static Object tag(ActionRecord record, Key<String> key) {
        return record.getTags().get(key.getKey());
    }
}
