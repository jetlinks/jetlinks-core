package org.jetlinks.core.monitor;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import org.jetlinks.core.Key;
import org.jetlinks.core.monitor.observation.ObservationKeys;
import org.jetlinks.core.monitor.observation.ObservationSpec;
import org.jetlinks.core.monitor.observation.RecorderProvider;
import org.jetlinks.core.monitor.observation.RecorderSelection;
import org.jetlinks.core.monitor.recorder.Recorder;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.jetlinks.core.monitor.RecordingTestSupport.SUBJECT;
import static org.jetlinks.core.monitor.RecordingTestSupport.monitor;
import static org.jetlinks.core.monitor.observation.Observations.observeMono;
import static org.junit.Assert.*;

public class MonitorObservationTest {

    private static final Key<List<String>> INPUTS = Key.listKey("metric.inputs");
    private static final Key<String> DEFINITION = Key.stringKey("metric.definition");
    private static final Key<String> OUTPUT = Key.stringKey("metric.output");
    private static final Context CAPTURE_CONTEXT = Context.of(RecorderProvider.class,
        (RecorderProvider) (action, context) -> RecorderSelection.capture(Recorder.noop(), "policy@1"));
    private static final Window COLD_ROOM = new Window("cold-room", List.of(
        new Sample("probe-a:1@1", -17, true),
        new Sample("probe-b:1@1", -13, true),
        new Sample("probe-a:2@1", 100, false)
    ));

    private static final ObservationSpec<Window, Metric> CALCULATE =
        ObservationSpec.<Window, Metric>of("metric.calculate")
            .onInput((input, metadata) -> {
                metadata.attribute(INPUTS, input.selected().stream().map(Sample::reference).toList());
                metadata.attribute(DEFINITION, "mean-temperature@7");
            })
            .onOutput((output, metadata) -> metadata.attribute(OUTPUT, output.reference()));

    @Test
    public void shouldCalculateRealWindowOnceAndRetainNativeRecordsAndTrace() {
        var original = new RecordingTestSupport.TestRecorder();
        var lineage = new RecordingTestSupport.TestRecorder();
        var calls = new AtomicInteger();
        RecorderProvider provider = (action, context) -> RecorderSelection.capture(lineage, "policy@3");
        String traceId = "0123456789abcdef0123456789abcdef";
        SpanContext span = SpanContext.create(
            traceId, "0123456789abcdef", TraceFlags.getSampled(), TraceState.getDefault()
        );
        var traceContext = io.opentelemetry.context.Context.root().with(Span.wrap(span));

        Mono<Metric> result = observeMono(monitor(original).recorder(), CALCULATE, COLD_ROOM, window -> {
            calls.incrementAndGet();
            return calculate(window);
        }).contextWrite(Context.of(RecorderProvider.class, provider, SUBJECT, "cold-room",
                                  io.opentelemetry.context.Context.class, traceContext));

        assertEquals(0, calls.get());
        StepVerifier.create(result).assertNext(metric -> assertEquals(-15, metric.temperature(), 0)).verifyComplete();
        assertEquals(1, calls.get());
        assertEquals(1, original.records.size());
        assertEquals(1, lineage.records.size());
        var proof = lineage.records.get(0);
        assertEquals(List.of("probe-a:1@1", "probe-b:1@1"), proof.getAttributes().get(INPUTS.getKey()));
        assertEquals("mean-temperature@7", proof.getAttributes().get(DEFINITION.getKey()));
        assertEquals("metric:cold-room@1", proof.getAttributes().get(OUTPUT.getKey()));
        assertEquals(traceId, proof.getTraceId());
        assertEquals("cold-room", proof.subject);
        assertEquals(ObservationKeys.CAPTURED, proof.getTags().get(ObservationKeys.METADATA_STATUS.getKey()));
        assertNotEquals(proof.getId(), original.records.get(0).getId());
        assertEquals(1, original.starts.get());
        assertEquals(1, lineage.starts.get());
    }

    @Test
    public void shouldSkipAllDetailedExtractorsWhileKeepingOriginalRecorder() {
        var original = new RecordingTestSupport.TestRecorder();
        var extracts = new AtomicInteger();
        ObservationSpec<Window, Metric> spec = CALCULATE
            .onInput((value, metadata) -> extracts.incrementAndGet())
            .onOutput((value, metadata) -> extracts.incrementAndGet());

        StepVerifier.create(observeMono(original, spec, COLD_ROOM, MonitorObservationTest::calculate))
                    .expectNextMatches(value -> value.temperature() == -15)
                    .verifyComplete();

        assertEquals(0, extracts.get());
        assertEquals(1, original.records.size());
        assertEquals(ObservationKeys.NOT_REQUESTED,
                     original.records.get(0).getTags().get(ObservationKeys.METADATA_STATUS.getKey()));
        assertNull(original.records.get(0).getAttributes());
    }

    @Test
    public void shouldRefreshLocalPolicyForEachSubscriptionWithoutReplacingRecorder() {
        var original = new RecordingTestSupport.TestRecorder();
        var enabled = new AtomicBoolean();
        var selections = new AtomicInteger();
        RecorderProvider provider = (action, context) -> {
            selections.incrementAndGet();
            return enabled.get()
                ? RecorderSelection.capture(Recorder.noop(), "policy@2")
                : RecorderSelection.disabled();
        };
        Mono<Metric> result = observeMono(original, CALCULATE, COLD_ROOM, MonitorObservationTest::calculate)
            .contextWrite(Context.of(RecorderProvider.class, provider));

        StepVerifier.create(result).expectNextCount(1).verifyComplete();
        enabled.set(true);
        StepVerifier.create(result).expectNextCount(1).verifyComplete();

        assertEquals(2, selections.get());
        assertNull(original.records.get(0).getAttributes());
        assertEquals("policy@2", original.records.get(1).getTags().get(ObservationKeys.POLICY_VERSION.getKey()));
        assertNotEquals(original.records.get(0).getId(), original.records.get(1).getId());
    }

    @Test
    public void shouldJoinAsyncMetricAndDetectionButRecordOnlyInputsActuallyUsed() {
        var records = new RecordingTestSupport.TestRecorder();
        Sinks.One<Metric> metric = Sinks.one();
        Sinks.One<Detection> detection = Sinks.one();
        ObservationSpec<AlarmInput, Alarm> evaluate = ObservationSpec.<AlarmInput, Alarm>of("alarm.evaluate")
            .onInput((input, metadata) -> metadata.attribute(INPUTS, input.usedReferences()))
            .onOutput((output, metadata) -> metadata.attribute(OUTPUT, output.reference()));
        Mono<Alarm> result = Mono
            .zip(metric.asMono(), detection.asMono())
            .map(pair -> new AlarmInput(pair.getT1(), pair.getT2()))
            .flatMap(input -> observeMono(records, evaluate, input, MonitorObservationTest::evaluate))
            .contextWrite(CAPTURE_CONTEXT.put(SUBJECT, "cold-room"));

        StepVerifier.create(result)
                    .then(() -> assertEquals(Sinks.EmitResult.OK,
                                            detection.tryEmitValue(new Detection("ai:frame-1", 0.2))))
                    .then(() -> assertTrue(records.records.isEmpty()))
                    .then(() -> assertEquals(Sinks.EmitResult.OK,
                                            metric.tryEmitValue(new Metric("metric:cold-room@1", -15))))
                    .expectNext(new Alarm("alarm:occurrence-1", true))
                    .verifyComplete();

        assertEquals(List.of("metric:cold-room@1"), records.records.get(0).getAttributes().get(INPUTS.getKey()));
        assertEquals("cold-room", records.records.get(0).subject);
    }

    @Test
    public void shouldKeepConcurrentDeviceContextsSeparate() {
        var records = new RecordingTestSupport.TestRecorder();
        Window freezer = new Window("freezer", List.of(new Sample("probe-c:1", -21, true)));
        // 输入是明确的两项批次；collectList 不作用于持续设备流。
        Mono<List<Metric>> batch = Flux.just(COLD_ROOM, freezer)
            .flatMap(window -> observeMono(records, CALCULATE, window, MonitorObservationTest::calculate)
                .subscribeOn(Schedulers.parallel())
                .contextWrite(context -> context.put(SUBJECT, window.id())), 2)
            .collectList()
            .contextWrite(CAPTURE_CONTEXT);

        StepVerifier.create(batch)
                    .assertNext(values -> assertEquals(Set.of(-15.0, -21.0),
                        values.stream().map(Metric::temperature).collect(Collectors.toSet())))
                    .verifyComplete();

        Map<String, String> outputs = records.records.stream().collect(Collectors.toMap(
            record -> record.subject, record -> (String) record.getAttributes().get(OUTPUT.getKey())
        ));
        assertEquals(Map.of("cold-room", "metric:cold-room@1", "freezer", "metric:freezer@1"), outputs);
    }

    @Test
    public void shouldRecordEachRetryWithoutIntroducingRetries() {
        var records = new RecordingTestSupport.TestRecorder();
        var attempts = new AtomicInteger();
        RuntimeException offline = new IllegalStateException("temporarily offline");
        Mono<Metric> result = observeMono(records, CALCULATE, COLD_ROOM, window ->
            attempts.incrementAndGet() == 1 ? Mono.error(offline) : calculate(window))
            .contextWrite(CAPTURE_CONTEXT);

        StepVerifier.create(result.retry(1)).expectNextCount(1).verifyComplete();
        assertEquals(2, attempts.get());
        assertEquals(2, records.records.size());
        assertTrue(records.records.get(0).isHasError());
        assertFalse(records.records.get(1).isHasError());
        assertNotEquals(records.records.get(0).getId(), records.records.get(1).getId());
    }

    @Test
    public void shouldPreserveOriginalErrorEmptyAndCancellationSignals() {
        var records = new RecordingTestSupport.TestRecorder();
        RuntimeException failure = new IllegalStateException("control rejected");
        StepVerifier.create(observeMono(records, "control", () -> Mono.error(failure)))
                    .expectErrorMatches(error -> error == failure).verify();
        StepVerifier.create(observeMono(records, "empty", Mono::empty)).verifyComplete();
        var cancels = new AtomicInteger();
        StepVerifier.create(observeMono(records, "pending", () -> Mono.never().doOnCancel(cancels::incrementAndGet)))
                    .expectSubscription().thenCancel().verify();
        StepVerifier.create(observeMono(records, "supplier", () -> { throw failure; }))
                    .expectErrorMatches(error -> error == failure).verify();

        assertEquals(4, records.records.size());
        assertTrue(records.records.get(0).isHasError());
        assertEquals(0, records.records.get(1).getValueCount());
        assertTrue(records.records.get(2).isCancel());
        assertEquals(1, cancels.get());
        assertEquals(4, records.starts.get());
    }

    @Test
    public void shouldNotRollBackCompletedControlWhenParallelNotificationFails() {
        var records = new RecordingTestSupport.TestRecorder();
        var controlled = new AtomicBoolean();
        Sinks.Empty<Void> notification = Sinks.empty();
        RuntimeException rejected = new IllegalStateException("notification channel rejected");
        Mono<Void> operations = Mono.when(
            observeMono(records, "notify", notification::asMono),
            observeMono(records, "control", () -> Mono.fromRunnable(() -> controlled.set(true)))
        );
        StepVerifier.create(operations)
                    .then(() -> assertTrue(controlled.get()))
                    .then(() -> notification.tryEmitError(rejected))
                    .expectErrorMatches(error -> error == rejected).verify();

        assertTrue(controlled.get());
        var control = records.records.stream().filter(record -> record.getAction().equals("control")).findFirst().orElseThrow();
        assertFalse(control.isHasError());
        assertFalse(control.isCancel());
        assertEquals(2, records.records.size());
    }

    @Test
    public void shouldTraceColdRoomCalculationAlarmAndPartialDispatchFailure() {
        var records = new RecordingTestSupport.TestRecorder();
        var deviceCommands = new AtomicInteger();
        RuntimeException notificationFailure = new IllegalStateException("sms provider unavailable");
        var alarmSpec = ObservationSpec.<AlarmInput, Alarm>of("alarm.evaluate")
            .onInput((input, metadata) -> metadata.attribute(INPUTS, input.usedReferences()))
            .onOutput((output, metadata) -> metadata.attribute(OUTPUT, output.reference()));
        var controlSpec = ObservationSpec.<Alarm, String>of("device.control")
            .onInput((input, metadata) -> metadata.attribute(INPUTS, List.of(input.reference())))
            .onOutput((receipt, metadata) -> metadata.attribute(OUTPUT, receipt));
        var notifySpec = ObservationSpec.<Alarm, Void>of("alarm.notify")
            .onInput((input, metadata) -> metadata.attribute(INPUTS, List.of(input.reference())));

        Mono<Void> flow = observeMono(records, CALCULATE, COLD_ROOM, MonitorObservationTest::calculate)
            .zipWith(Mono.just(new Detection("ai:frame-1", 0.2)), AlarmInput::new)
            .flatMap(input -> observeMono(records, alarmSpec, input, MonitorObservationTest::evaluate))
            .filter(Alarm::triggered)
            .flatMap(alarm -> Mono.when(
                observeMono(records, controlSpec, alarm, ignored -> Mono.fromSupplier(() -> {
                    deviceCommands.incrementAndGet();
                    return "control:compressor-ack@1";
                })),
                observeMono(records, notifySpec, alarm, ignored -> Mono.error(notificationFailure))
            ))
            .contextWrite(CAPTURE_CONTEXT.put(SUBJECT, "cold-room"));

        StepVerifier.create(flow).expectErrorMatches(error -> error == notificationFailure).verify();

        Map<String, RecordingTestSupport.NativeRecord> stages = records.records.stream()
            .collect(Collectors.toMap(record -> record.getAction().toString(), record -> record));
        assertEquals(Set.of("metric.calculate", "alarm.evaluate", "device.control", "alarm.notify"), stages.keySet());
        assertEquals(1, deviceCommands.get());
        assertEquals(stages.get("metric.calculate").getAttributes().get(OUTPUT.getKey()),
                     ((List<?>) stages.get("alarm.evaluate").getAttributes().get(INPUTS.getKey())).get(0));
        for (String action : List.of("device.control", "alarm.notify")) {
            assertEquals(List.of(stages.get("alarm.evaluate").getAttributes().get(OUTPUT.getKey())),
                         stages.get(action).getAttributes().get(INPUTS.getKey()));
        }
        assertEquals("control:compressor-ack@1", stages.get("device.control").getAttributes().get(OUTPUT.getKey()));
        assertFalse(stages.get("device.control").isHasError());
        assertTrue(stages.get("alarm.notify").isHasError());
        assertEquals(Set.of("cold-room"), records.records.stream().map(record -> record.subject).collect(Collectors.toSet()));
    }

    @Test
    public void shouldIncludeAiEvidenceWhenTemperatureAloneDoesNotTriggerAlarm() {
        var records = new RecordingTestSupport.TestRecorder();
        var input = new AlarmInput(new Metric("metric:cold-room@2", -20), new Detection("ai:frame-2", 0.95));
        var spec = ObservationSpec.<AlarmInput, Alarm>of("alarm.evaluate")
            .onInput((selected, metadata) -> metadata.attribute(INPUTS, selected.usedReferences()));

        StepVerifier.create(observeMono(records, spec, input, MonitorObservationTest::evaluate)
                               .contextWrite(CAPTURE_CONTEXT))
                    .expectNext(new Alarm("alarm:occurrence-1", true)).verifyComplete();

        assertEquals(List.of("metric:cold-room@2", "ai:frame-2"),
                     records.records.get(0).getAttributes().get(INPUTS.getKey()));
    }

    @Test
    public void shouldKeepCachedProductionSeparateFromTwoConsumers() {
        var producerRecords = new RecordingTestSupport.TestRecorder();
        var consumerRecords = new RecordingTestSupport.TestRecorder();
        var reads = new AtomicInteger();
        Mono<Metric> cached = observeMono(producerRecords, CALCULATE, COLD_ROOM, window -> {
            reads.incrementAndGet();
            return calculate(window);
        })
            .contextWrite(CAPTURE_CONTEXT.put(SUBJECT, "cold-room"))
            .cache();
        ObservationSpec<Metric, Metric> use = ObservationSpec.<Metric, Metric>of("metric.use")
            .onInput((metric, metadata) -> metadata.attribute(INPUTS, List.of(metric.reference())));

        for (String subject : List.of("operator-a", "operator-b")) {
            StepVerifier.create(cached.flatMap(metric -> observeMono(consumerRecords, use, metric, Mono::just))
                                      .contextWrite(CAPTURE_CONTEXT.put(SUBJECT, subject)))
                        .expectNextCount(1).verifyComplete();
        }
        assertEquals(1, reads.get());
        assertEquals("cold-room", producerRecords.records.get(0).subject);
        assertEquals(Set.of("operator-a", "operator-b"),
                     consumerRecords.records.stream().map(record -> record.subject).collect(Collectors.toSet()));
    }

    @Test
    public void shouldUseExplicitRecorderAndKeepProviderScopedToEachSubscription() {
        var local = new RecordingTestSupport.TestRecorder();
        var upstream = new RecordingTestSupport.TestRecorder();
        Monitor other = monitor(upstream);
        Mono<Metric> result = observeMono(local, CALCULATE, COLD_ROOM, MonitorObservationTest::calculate);

        StepVerifier.create(result.contextWrite(Context.of(
            RecorderProvider.class, RecorderProvider.disabled(), Monitor.class, other
        ))).expectNextCount(1).verifyComplete();
        StepVerifier.create(result.contextWrite(CAPTURE_CONTEXT)).expectNextCount(1).verifyComplete();

        assertEquals(2, local.records.size());
        assertTrue(upstream.records.isEmpty());
        assertNull(local.records.get(0).getAttributes());
        assertEquals(ObservationKeys.NOT_REQUESTED,
                     local.records.get(0).getTags().get(ObservationKeys.METADATA_STATUS.getKey()));
        assertEquals("metric:cold-room@1", local.records.get(1).getAttributes().get(OUTPUT.getKey()));
    }

    @Test
    public void shouldDiscardFailedMetadataCallbackWithoutChangingResult() {
        var records = new RecordingTestSupport.TestRecorder();
        ObservationSpec<Window, Metric> broken = CALCULATE.onInput((input, metadata) -> {
            metadata.attribute(DEFINITION, "must-not-leak");
            throw new IllegalStateException("missing immutable manifest");
        });
        StepVerifier.create(observeMono(records, broken, COLD_ROOM, MonitorObservationTest::calculate)
                               .contextWrite(CAPTURE_CONTEXT))
                    .expectNextCount(1).verifyComplete();

        var result = records.records.get(0);
        assertFalse(result.getAttributes().containsKey(DEFINITION.getKey()));
        assertEquals("metric:cold-room@1", result.getAttributes().get(OUTPUT.getKey()));
        assertEquals(ObservationKeys.FAILED, result.getTags().get(ObservationKeys.METADATA_STATUS.getKey()));
    }

    @Test
    public void shouldKeepInputEvidenceAndBusinessValueWhenOutputExtractionFails() {
        var records = new RecordingTestSupport.TestRecorder();
        var spec = CALCULATE.onOutput((output, metadata) -> {
            metadata.attribute(OUTPUT, "must-not-leak");
            throw new IllegalStateException("missing output reference");
        });

        StepVerifier.create(observeMono(records, spec, COLD_ROOM, MonitorObservationTest::calculate)
                               .contextWrite(CAPTURE_CONTEXT))
                    .expectNext(new Metric("metric:cold-room@1", -15)).verifyComplete();

        var result = records.records.get(0);
        assertEquals(List.of("probe-a:1@1", "probe-b:1@1"), result.getAttributes().get(INPUTS.getKey()));
        assertFalse(result.getAttributes().containsKey(OUTPUT.getKey()));
        assertFalse(result.isHasError());
        assertEquals(ObservationKeys.FAILED, result.getTags().get(ObservationKeys.METADATA_STATUS.getKey()));
    }

    @Test
    public void shouldRejectReservedFieldsAndCumulativeMetadataOverflow() {
        var records = new RecordingTestSupport.TestRecorder();
        var reserved = CALCULATE.onInput((input, metadata) ->
            metadata.tag(ObservationKeys.METADATA_STATUS, "forged"));
        StepVerifier.create(observeMono(records, reserved, COLD_ROOM, MonitorObservationTest::calculate)
                               .contextWrite(CAPTURE_CONTEXT))
                    .expectNextCount(1).verifyComplete();
        assertEquals(ObservationKeys.FAILED,
                     records.records.get(0).getTags().get(ObservationKeys.METADATA_STATUS.getKey()));

        var overflow = CALCULATE
            .onInput((input, metadata) -> IntStream.range(0, 40).forEach(i ->
                metadata.attribute(Key.intKey("input." + i), i)))
            .onOutput((output, metadata) -> IntStream.range(0, 25).forEach(i ->
                metadata.attribute(Key.intKey("output." + i), i)));
        StepVerifier.create(observeMono(records, overflow, COLD_ROOM, MonitorObservationTest::calculate)
                               .contextWrite(CAPTURE_CONTEXT))
                    .expectNextCount(1).verifyComplete();
        assertEquals(40, records.records.get(1).getAttributes().size());
        assertEquals(ObservationKeys.TRUNCATED,
                     records.records.get(1).getTags().get(ObservationKeys.METADATA_STATUS.getKey()));
    }

    @Test
    public void shouldRetainTypedListMergingAcrossInputAndOutput() {
        var records = new RecordingTestSupport.TestRecorder();
        ObservationSpec<Window, Metric> spec = CALCULATE
            .onInput((input, metadata) -> metadata.attribute(INPUTS, List.of("input")))
            .onOutput((output, metadata) -> metadata.attribute(INPUTS, List.of("output")));
        StepVerifier.create(observeMono(records, spec, COLD_ROOM, MonitorObservationTest::calculate)
                               .contextWrite(CAPTURE_CONTEXT))
                    .expectNextCount(1).verifyComplete();
        assertEquals(List.of("input", "output"), records.records.get(0).getAttributes().get(INPUTS.getKey()));
    }

    @Test
    public void shouldBoundCollectionMetadataAcrossBothCallbacks() {
        var records = new RecordingTestSupport.TestRecorder();
        var first = IntStream.range(0, 40).mapToObj(Integer::toString).toList();
        var second = IntStream.range(40, 65).mapToObj(Integer::toString).toList();
        var spec = CALCULATE
            .onInput((input, metadata) -> metadata.attribute(INPUTS, first))
            .onOutput((output, metadata) -> metadata.attribute(INPUTS, second));

        StepVerifier.create(observeMono(records, spec, COLD_ROOM, MonitorObservationTest::calculate)
                               .contextWrite(CAPTURE_CONTEXT))
                    .expectNextMatches(metric -> metric.temperature() == -15).verifyComplete();
        assertEquals(first, records.records.get(0).getAttributes().get(INPUTS.getKey()));
        assertEquals(ObservationKeys.TRUNCATED,
                     records.records.get(0).getTags().get(ObservationKeys.METADATA_STATUS.getKey()));

        var oversized = CALCULATE.onInput((input, metadata) ->
            metadata.attribute(INPUTS, IntStream.range(0, 65).mapToObj(Integer::toString).toList()));
        StepVerifier.create(observeMono(records, oversized, COLD_ROOM, MonitorObservationTest::calculate)
                               .contextWrite(CAPTURE_CONTEXT))
                    .expectNextCount(1).verifyComplete();
        assertFalse(records.records.get(1).getAttributes().containsKey(INPUTS.getKey()));
        assertEquals(ObservationKeys.TRUNCATED,
                     records.records.get(1).getTags().get(ObservationKeys.METADATA_STATUS.getKey()));
    }

    @Test
    public void shouldPreserveBusinessWhenProviderFailsOrNoRecorderExists() {
        var records = new RecordingTestSupport.TestRecorder();
        RecorderProvider broken = (action, context) -> {
            throw new IllegalStateException("policy unavailable");
        };
        StepVerifier.create(observeMono(records, CALCULATE, COLD_ROOM, MonitorObservationTest::calculate)
                               .contextWrite(Context.of(RecorderProvider.class, broken)))
                    .expectNextCount(1).verifyComplete();
        assertEquals(ObservationKeys.FAILED,
                     records.records.get(0).getTags().get(ObservationKeys.METADATA_STATUS.getKey()));

        var extracts = new AtomicInteger();
        var spec = CALCULATE.onInput((input, metadata) -> extracts.incrementAndGet());
        StepVerifier.create(observeMono(Recorder.noop(), spec, COLD_ROOM, MonitorObservationTest::calculate)
                               .contextWrite(CAPTURE_CONTEXT))
                    .expectNextCount(1).verifyComplete();
        assertEquals(0, extracts.get());
    }

    @Test
    public void shouldResolveLazyRecorderAtSubscriptionAndKeepNativeFactoryAndReplay() {
        var records = new RecordingTestSupport.TestRecorder();
        Monitor original = monitor(records);
        var resolutions = new AtomicInteger();
        Monitor lazy = Monitor.lazy(() -> {
            resolutions.incrementAndGet();
            return original;
        });
        Mono<Metric> result = observeMono(lazy.recorder(), CALCULATE, COLD_ROOM, MonitorObservationTest::calculate)
            .contextWrite(CAPTURE_CONTEXT);
        assertEquals(0, resolutions.get());
        StepVerifier.create(result).expectNextCount(1).verifyComplete();
        assertEquals(1, resolutions.get());
        assertEquals(1, records.factories.get());
        assertEquals(1, records.starts.get());
        assertNotNull(records.records.get(0).getAttributes());

        var direct = lazy.recorder().<Integer>action("direct");
        StepVerifier.create(Mono.just(1).transform(direct)).expectNext(1).verifyComplete();
        assertEquals(2, records.records.size());

        var existing = records.records.get(0);
        lazy.recorder().record(existing);
        assertEquals(3, records.records.size());
        assertSame(existing, records.records.get(2));
    }

    private static Mono<Metric> calculate(Window window) {
        return Mono.fromSupplier(() -> new Metric("metric:" + window.id() + "@1",
            window.selected().stream().mapToDouble(Sample::temperature).average().orElseThrow()));
    }

    private static Mono<Alarm> evaluate(AlarmInput input) {
        return Mono.fromSupplier(() -> new Alarm("alarm:occurrence-1",
            input.metric().temperature() > -16 || input.detection().confidence() >= 0.8));
    }

    private record Sample(String reference, double temperature, boolean valid) {
    }

    private record Window(String id, List<Sample> samples) {
        List<Sample> selected() {
            return samples.stream().filter(Sample::valid).toList();
        }
    }

    private record Metric(String reference, double temperature) {
    }

    private record Detection(String reference, double confidence) {
    }

    private record AlarmInput(Metric metric, Detection detection) {
        List<String> usedReferences() {
            // 与短路判断一致：温度已触发时，AI 结果不参与本次决策。
            return metric.temperature() > -16
                ? List.of(metric.reference()) : List.of(metric.reference(), detection.reference());
        }
    }

    private record Alarm(String reference, boolean triggered) {
    }
}
