package org.jetlinks.core.monitor;

import org.jetlinks.core.Key;
import org.jetlinks.core.monitor.recorder.ActionRecord;
import org.jetlinks.core.monitor.recorder.ActionRecorder;
import org.jetlinks.core.monitor.recorder.Recorder;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.Assert.*;

public class CompositeRecorderTest {

    @Test
    public void shouldFlattenAndDeduplicateSinksWithoutEvaluatingNoopMetadata() {
        var first = new RecordingTestSupport.TestRecorder();
        var second = new RecordingTestSupport.TestRecorder();
        Recorder combined = Recorder.combine(first, Recorder.combine(second, first), Recorder.noop(), second);
        StepVerifier.create(Mono.just("accepted").transform(combined.action("control")))
                    .expectNext("accepted").verifyComplete();

        assertEquals(1, first.factories.get());
        assertEquals(1, second.factories.get());
        assertEquals(1, first.records.size());
        assertEquals(1, second.records.size());
        assertSame(Recorder.noop(), Recorder.combine());

        var extracts = new AtomicInteger();
        Recorder.combine(Recorder.noop()).action("disabled")
            .tag(Key.intKey("tag"), (Supplier<Integer>) extracts::incrementAndGet)
            .attribute(Key.intKey("attribute"), (Supplier<Integer>) extracts::incrementAndGet)
            .complete();
        assertEquals(0, extracts.get());
    }

    @Test
    public void shouldEvaluateEachSupplierAndSubscribeBusinessOnlyOnce() {
        var first = new RecordingTestSupport.TestRecorder();
        var second = new RecordingTestSupport.TestRecorder();
        var tags = new AtomicInteger();
        var attributes = new AtomicInteger();
        var subscriptions = new AtomicInteger();
        ActionRecorder<String> action = Recorder.combine(first, second).action("notify");
        action.tag(Key.intKey("delivery.attempt"), (Supplier<Integer>) tags::incrementAndGet)
              .attribute(Key.intKey("receipt"), (Supplier<Integer>) attributes::incrementAndGet)
              .tags(Map.of("channel", "sms"))
              .attributes(Map.of("recipient", "operator"))
              .valueConverter(value -> "recorded: " + value);

        StepVerifier.create(Mono.fromSupplier(() -> {
            subscriptions.incrementAndGet();
            return "accepted";
        }).transform(action)).expectNext("accepted").verifyComplete();

        assertEquals(1, tags.get());
        assertEquals(1, attributes.get());
        assertEquals(1, subscriptions.get());
        for (var recorder : List.of(first, second)) {
            assertEquals(1, recorder.starts.get());
            assertEquals(1, recorder.converters.get());
            var record = recorder.records.get(0);
            assertEquals(Map.of("delivery.attempt", 1, "channel", "sms"), record.getTags());
            assertEquals(Map.of("receipt", 1, "recipient", "operator"), record.getAttributes());
            assertEquals(1, record.getValueCount());
        }
    }

    @Test
    public void shouldKeepNativeChildParentsAndTerminateOnlyOnce() {
        var first = new RecordingTestSupport.TestRecorder();
        var second = new RecordingTestSupport.TestRecorder();
        ActionRecorder<Void> parent = Recorder.combine(first, second).action("alarm.dispatch");
        parent.start(Context.of(RecordingTestSupport.SUBJECT, "cold-room"));
        parent.start(Context.empty());
        ActionRecorder<String> child = parent.child("control");

        StepVerifier.create(Mono.just("acknowledged").transform(child))
                    .expectNext("acknowledged").verifyComplete();
        child.error(new IllegalStateException("late error"));
        child.cancel();
        parent.complete();
        parent.cancel();
        parent.error(new IllegalStateException("late error"));

        for (var recorder : List.of(first, second)) {
            assertEquals(2, recorder.starts.get());
            assertEquals(2, recorder.records.size());
            var childRecord = recorder.records.get(0);
            var parentRecord = recorder.records.get(1);
            assertEquals(parentRecord.getId(), childRecord.getParentId());
            assertEquals("cold-room", parentRecord.subject);
            assertFalse(childRecord.isHasError());
            assertFalse(childRecord.isCancel());
        }
        assertNotEquals(first.records.get(1).getId(), second.records.get(1).getId());
    }

    @Test
    public void shouldIsolateBrokenSinksAtEveryLifecycleBoundary() {
        for (String phase : List.of("factory", "tag", "attribute", "converter", "start", "value", "complete")) {
            var broken = new RecordingTestSupport.TestRecorder(phase);
            var healthy = new RecordingTestSupport.TestRecorder();
            ActionRecorder<String> action = Recorder.combine(broken, healthy).action("control");
            action.tag(Key.stringKey("device"), "compressor")
                  .attribute(Key.stringKey("command"), "cool")
                  .valueConverter(value -> value);

            StepVerifier.create(Mono.just("acknowledged").transform(action))
                        .expectNext("acknowledged").verifyComplete();

            assertEquals(phase, 1, healthy.records.size());
            assertEquals(phase, 1, healthy.starts.get());
            assertEquals(phase, 1, healthy.records.get(0).getValueCount());
            assertTrue(phase, broken.records.isEmpty());
        }
    }

    @Test
    public void shouldPreserveOriginalErrorAndCancellationWhenSinkTerminationThrows() {
        var healthy = new RecordingTestSupport.TestRecorder();
        RuntimeException failure = new IllegalStateException("device rejected command");
        var errorAction = Recorder.combine(new RecordingTestSupport.TestRecorder("error"), healthy)
                                  .<String>action("control");
        StepVerifier.create(Mono.<String>error(failure).transform(errorAction))
                    .expectErrorMatches(error -> error == failure).verify();
        errorAction.complete();
        errorAction.cancel();
        assertEquals(1, healthy.records.size());
        assertTrue(healthy.records.get(0).isHasError());

        var cancellations = new AtomicInteger();
        var cancelAction = Recorder.combine(new RecordingTestSupport.TestRecorder("cancel"), healthy)
                                   .<String>action("pending-control");
        StepVerifier.create(Mono.<String>never().doOnCancel(cancellations::incrementAndGet).transform(cancelAction))
                    .expectSubscription().thenCancel().verify();
        cancelAction.complete();
        cancelAction.error(failure);
        assertEquals(1, cancellations.get());
        assertEquals(2, healthy.records.size());
        assertTrue(healthy.records.get(1).isCancel());
    }

    @Test
    public void shouldIsolateChildFactoryFailureAndSkipSuppliersAfterAllSinksFail() {
        var broken = new RecordingTestSupport.TestRecorder("child");
        var healthy = new RecordingTestSupport.TestRecorder();
        ActionRecorder<Void> parent = Recorder.combine(broken, healthy).action("alarm.dispatch");
        parent.start(Context.empty());
        StepVerifier.create(Mono.just("accepted").transform(parent.<String>child("notify")))
                    .expectNext("accepted").verifyComplete();
        parent.complete();
        assertTrue(broken.records.isEmpty());
        assertEquals(2, healthy.records.size());
        assertEquals(healthy.records.get(1).getId(), healthy.records.get(0).getParentId());

        var extracts = new AtomicInteger();
        Recorder.combine(new RecordingTestSupport.TestRecorder("tag")).action("broken")
            .tag("initial", "failure")
            .attribute(Key.intKey("unused"), (Supplier<Integer>) extracts::incrementAndGet)
            .complete();
        assertEquals(0, extracts.get());
    }

    @Test
    public void shouldForwardExistingRecordsThroughNativeRecorderApi() {
        var firstCalls = new AtomicInteger();
        var secondCalls = new AtomicInteger();
        ActionRecord existing = new ActionRecord();
        existing.setAction("existing");
        Recorder first = forwardingRecorder(existing, firstCalls, true);
        Recorder second = forwardingRecorder(existing, secondCalls, false);

        Recorder combined = Recorder.combine(first, second);
        combined.record(null);
        combined.record(new ActionRecord());
        combined.record(existing);
        assertEquals(1, firstCalls.get());
        assertEquals(1, secondCalls.get());
    }

    @Test(expected = LinkageError.class)
    public void shouldNotSuppressReactorFatalErrors() {
        Recorder.combine(new RecordingTestSupport.TestRecorder() {
            @Override
            public <E> ActionRecorder<E> action(CharSequence action) {
                throw new LinkageError("incompatible recorder runtime");
            }
        }).action("control");
    }

    private static Recorder forwardingRecorder(ActionRecord expected, AtomicInteger calls, boolean fail) {
        return new Recorder() {
            @Override
            public <E> ActionRecorder<E> action(CharSequence action) {
                throw new AssertionError("Existing records must use the native record API");
            }

            @Override
            public void record(ActionRecord record) {
                assertSame(expected, record);
                calls.incrementAndGet();
                if (fail) {
                    throw new IllegalStateException("unavailable sink");
                }
            }
        };
    }
}
