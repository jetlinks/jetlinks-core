package org.jetlinks.core.monitor;

import org.jetlinks.core.monitor.observation.ObservationReference;
import org.jetlinks.core.monitor.observation.ObservationSpec;
import org.jetlinks.core.monitor.observation.Observations;
import org.jetlinks.core.monitor.observation.RecorderProvider;
import org.jetlinks.core.monitor.observation.RecorderSelection;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.context.Context;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DefaultObservationProviderTest {

    @Test
    public void independentAsyncSubscriptionsSelectWithTheirOwnContext() {
        var recorder = new RecordingTestSupport.TestRecorder();
        AtomicInteger selections = new AtomicInteger();
        var registration = Observations.registerDefaultProvider((action, context) -> {
            selections.incrementAndGet();
            assertTrue(context.hasKey(RecordingTestSupport.SUBJECT));
            return RecorderSelection.capture(recorder, "p1");
        });
        try {
            Mono<Integer> operation = Observations.observeMono("device.report", () -> Mono.just(1));
            assertEquals(20, Flux.range(0, 20)
                .flatMap(i -> operation.subscribeOn(Schedulers.parallel())
                    .contextWrite(Context.of(RecordingTestSupport.SUBJECT, "device-" + i)))
                .count().block().longValue());
            assertEquals(20, selections.get());
            assertEquals(20, recorder.records.size());
            assertEquals(20, recorder.records.stream().map(record -> record.subject).distinct().count());
            assertEquals(20, recorder.records.stream().map(record -> record.getId()).distinct().count());
        } finally {
            registration.dispose();
        }
    }

    @Test
    public void contextProviderAndExplicitDisabledOverrideTheDefault() {
        var recorder = new RecordingTestSupport.TestRecorder();
        var nearer = new RecordingTestSupport.TestRecorder();
        AtomicInteger selections = new AtomicInteger();
        var registration = Observations.registerDefaultProvider((action, context) -> {
            selections.incrementAndGet();
            return RecorderSelection.capture(recorder, "default");
        });
        try {
            var operation = Observations.observeFlux("device.batch", () -> Flux.just(1, 2));
            RecorderProvider explicit = (action, context) -> RecorderSelection.capture(nearer, "explicit");
            assertEquals(2, operation.contextWrite(Context.of(RecorderProvider.class, explicit)).count().block().longValue());
            assertEquals(2, operation.contextWrite(Context.of(RecorderProvider.class, RecorderProvider.disabled()))
                .count().block().longValue());
            assertEquals(0, selections.get());
            assertTrue(recorder.records.isEmpty());
            assertEquals(1, nearer.records.size());
            operation.blockLast();
            assertEquals(1, selections.get());
            assertEquals(1, recorder.records.size());
        } finally {
            registration.dispose();
        }
    }

    @Test
    public void replacementIsVisibleAtSubscriptionAndOldHandleCannotClearIt() {
        var first = new RecordingTestSupport.TestRecorder();
        var second = new RecordingTestSupport.TestRecorder();
        var operation = Observations.observeMono("timer.tick", () -> Mono.just(1));
        var old = Observations.registerDefaultProvider((action, context) -> RecorderSelection.capture(first, "old"));
        var current = Observations.registerDefaultProvider((action, context) -> RecorderSelection.capture(second, "new"));
        try {
            assertTrue(old.isDisposed());
            old.dispose();
            assertFalse(current.isDisposed());
            operation.block();
            assertTrue(first.records.isEmpty());
            assertEquals(1, second.records.size());
            current.dispose();
            operation.block();
            assertEquals(1, second.records.size());
            assertTrue(current.isDisposed());
        } finally {
            old.dispose();
            current.dispose();
        }
    }

    @Test
    public void repeatedRegistrationOfSameFactoryUsesIndependentHandles() {
        AtomicInteger selections = new AtomicInteger();
        RecorderProvider provider = (action, context) -> {
            selections.incrementAndGet();
            return RecorderSelection.disabled();
        };
        var old = Observations.registerDefaultProvider(provider);
        var current = Observations.registerDefaultProvider(provider);
        try {
            old.dispose();
            Observations.observeMono("timer.tick", () -> Mono.just(1)).block();
            assertEquals(1, selections.get());
            current.dispose();
            Observations.observeMono("timer.tick", () -> Mono.just(1)).block();
            assertEquals(1, selections.get());
        } finally {
            old.dispose();
            current.dispose();
        }
    }

    @Test
    public void nestedObservationsKeepNativeParentWithoutDoubleRecording() {
        var recorder = new RecordingTestSupport.TestRecorder();
        var registration = Observations.registerDefaultProvider(
            (action, context) -> RecorderSelection.capture(recorder, "p1"));
        try {
            Observations.observeMono("scene.execute", () ->
                Observations.observeMono("notification.deliver", () -> Mono.just(1))).block();
            assertEquals(2, recorder.records.size());
            assertEquals(recorder.records.get(1).getId(), recorder.records.get(0).getParentId());
            assertEquals(2, recorder.starts.get());
        } finally {
            registration.dispose();
        }
    }

    @Test
    public void disabledDefaultDoesNotExtractOrCreateExecutionContext() {
        AtomicInteger extracts = new AtomicInteger();
        var spec = ObservationSpec.<Integer, Integer>of("metric.calculate")
            .onInput((input, metadata) -> extracts.incrementAndGet())
            .onOutput((output, metadata) -> extracts.incrementAndGet());
        var registration = Observations.registerDefaultProvider(RecorderProvider.disabled());
        try {
            assertEquals(Integer.valueOf(1), Observations.observeMono(spec, 1, input -> Mono.deferContextual(context -> {
                assertFalse(context.hasKey(ObservationReference.class));
                return Mono.just(input);
            })).block());
            assertEquals(0, extracts.get());
        } finally {
            registration.dispose();
        }
    }
}
