package org.jetlinks.core.monitor;

import org.jetlinks.core.monitor.observation.ObservationData;
import org.jetlinks.core.monitor.observation.ObservationKeys;
import org.jetlinks.core.monitor.observation.ObservationSpec;
import org.jetlinks.core.monitor.observation.Observations;
import org.jetlinks.core.monitor.observation.RecorderProvider;
import org.jetlinks.core.monitor.observation.RecorderSelection;
import org.jetlinks.core.things.ThingId;
import org.junit.Test;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

public class ObservationDataTest {

    @Test
    public void selectedInputsAndVersionsRemainSnapshotsAfterBusinessMutation() {
        var recorder = new RecordingTestSupport.TestRecorder();
        var input = ThingId.of("sampleManifest", "window-1@3");
        var device = ThingId.of("device", "probe-a");
        var spec = ObservationSpec.<ThingId, ThingId>of("metric.calculate")
            .onInput((value, metadata) -> {
                ObservationData.subject(metadata, device);
                ObservationData.used(metadata, List.of(value));
                ObservationData.definition(metadata, ThingId.of("metricRevision", "mean@7"));
                ObservationData.inputsComplete(metadata);
            }).onOutput((result, metadata) -> ObservationData.generated(metadata, result));
        RecorderProvider provider = (action, context) -> RecorderSelection.capture(recorder, "p1");

        ThingId output = ThingId.of("metricResult", "value-1");
        assertSame(output, Observations.observeMono(spec, input, ignored -> Mono.just(output))
            .contextWrite(Context.of(RecorderProvider.class, provider)).block());
        input.setId("changed");
        device.setId("changed");
        output.setId("changed");

        var data = recorder.records.get(0).getAttributes();
        assertEquals(ThingId.of("device", "probe-a"), data.get(ObservationData.SUBJECT.getKey()));
        assertEquals(List.of(ThingId.of("sampleManifest", "window-1@3")), data.get(ObservationData.INPUTS.getKey()));
        assertEquals(List.of(ThingId.of("metricResult", "value-1")), data.get(ObservationData.OUTPUTS.getKey()));
        assertEquals(Boolean.TRUE, data.get(ObservationData.INPUTS_COMPLETE.getKey()));
    }

    @Test
    public void subjectIsAvailableBeforeSelectionAndDoesNotExposeMutableContextValue() {
        var subject = ThingId.of("space", "cold-room");
        Context context = ObservationData.withSubject(Context.empty(), subject);
        subject.setId("other");
        ThingId selected = ObservationData.currentSubject(context).orElseThrow();
        assertEquals("cold-room", selected.getId());
        selected.setId("mutated");
        assertEquals("cold-room", ObservationData.currentSubject(context).orElseThrow().getId());
        assertFalse(ObservationData.currentSubject(Context.empty()).isPresent());
    }

    @Test
    public void disabledCollectionDoesNotBuildAnInputManifest() {
        AtomicInteger extracts = new AtomicInteger();
        var spec = ObservationSpec.<String, String>of("metric.calculate").onInput((value, metadata) -> {
            extracts.incrementAndGet();
            ObservationData.used(metadata, List.of(ThingId.of("manifest", value)));
        });
        assertEquals("result", Observations.observeMono(spec, "window", ignored -> Mono.just("result")).block());
        assertEquals(0, extracts.get());
    }

    @Test
    public void oversizedInputsUseExistingBudgetAndNeverDeclareCompleteness() {
        var recorder = new RecordingTestSupport.TestRecorder();
        var inputs = IntStream.range(0, 100).mapToObj(i -> ThingId.of("sample", String.valueOf(i))).toList();
        var spec = ObservationSpec.<List<ThingId>, String>of("metric.calculate").onInput((value, metadata) -> {
            ObservationData.used(metadata, value);
            ObservationData.inputsComplete(metadata);
        });
        RecorderProvider provider = (action, context) -> RecorderSelection.capture(recorder, "p1");
        assertEquals("value", Observations.observeMono(spec, inputs, ignored -> Mono.just("value"))
            .contextWrite(Context.of(RecorderProvider.class, provider)).block());
        var record = recorder.records.get(0);
        assertEquals(ObservationKeys.TRUNCATED, record.getTags().get(ObservationKeys.METADATA_STATUS.getKey()));
        assertTrue(record.getAttributes() == null || !record.getAttributes().containsKey(ObservationData.INPUTS_COMPLETE.getKey()));
    }
}
