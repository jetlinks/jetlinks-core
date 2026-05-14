package org.jetlinks.core.monitor.recorder;

import org.junit.Test;
import reactor.util.context.Context;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RecorderTest {

    @Test
    public void should_forward_existing_record_by_public_api() {
        ActionRecord record = new ActionRecord();
        record.setId("id-1");
        record.setParentId("parent-1");
        record.setAction("open-detail");
        record.withTag("tag", "value");
        record.withAttributes("target", "skill-1");
        record.setTraceId("trace-1");
        record.setSpanId("span-1");
        record.setTimestamp(100);
        record.setUseNanos(200);

        RecordingRecorder recorder = new RecordingRecorder();
        recorder.record(record);

        assertSame(record, recorder.lastRecord);
    }

    @Test
    public void should_ignore_empty_record() {
        RecordingRecorder recorder = new RecordingRecorder();

        recorder.record(null);

        assertNull(recorder.lastRecord);
    }

    @Test
    public void should_fallback_to_public_api_when_recorder_does_not_support_replay() {
        ActionRecord record = new ActionRecord();
        record.setAction("open-detail");
        record.withTag("tag", "value");
        record.withAttributes("target", "skill-1");
        record.setHasError(true);
        record.setErrorType("java.lang.IllegalStateException");
        record.setErrorDetail("boom");

        FallbackRecorder recorder = new FallbackRecorder();
        recorder.record(record);

        assertEquals(record.getTags(), recorder.tags);
        assertEquals(record.getAttributes(), recorder.attributes);
        assertTrue(recorder.error instanceof RecordedActionException);
        assertEquals(record.getErrorType(), recorder.error.getErrorType());
        assertEquals(record.getErrorDetail(), recorder.error.getErrorDetail());
    }

    private static class RecordingRecorder implements Recorder {

        private ActionRecord lastRecord;

        @Override
        public <E> ActionRecorder<E> action(CharSequence action) {
            RecordingActionRecorder<E> recorder = new RecordingActionRecorder<>();
            recorder.start(Context.empty());
            return recorder;
        }

        private class RecordingActionRecorder<E> extends AbstractActionRecorder<E> {

            private RecordingActionRecorder() {
                super("ignored");
            }

            @Override
            protected void handle(ActionRecord record) {
                lastRecord = record;
            }

            @Override
            public <T> ActionRecorder<T> child(CharSequence action) {
                return ActionRecorder.noop();
            }
        }
    }

    private static class FallbackRecorder implements Recorder {

        private final Map<String, Object> tags = new LinkedHashMap<>();
        private final Map<String, Object> attributes = new LinkedHashMap<>();
        private RecordedActionException error;

        @Override
        public <E> ActionRecorder<E> action(CharSequence action) {
            return new ActionRecorder<E>() {
                @Override
                public ActionRecorder<E> tag(String tag, Object value) {
                    tags.put(tag, value);
                    return this;
                }

                @Override
                public <V> ActionRecorder<E> tag(org.jetlinks.core.Key<V> key, V value) {
                    tags.put(key.getKey(), value);
                    return this;
                }

                @Override
                public <V> ActionRecorder<E> tag(org.jetlinks.core.Key<V> key, java.util.function.Supplier<V> value) {
                    return tag(key, value.get());
                }

                @Override
                public ActionRecorder<E> tags(Map<String, Object> tags) {
                    FallbackRecorder.this.tags.putAll(tags);
                    return this;
                }

                @Override
                public ActionRecorder<E> attribute(String key, Object value) {
                    attributes.put(key, value);
                    return this;
                }

                @Override
                public <V> ActionRecorder<E> attribute(org.jetlinks.core.Key<V> key, V value) {
                    attributes.put(key.getKey(), value);
                    return this;
                }

                @Override
                public <V> ActionRecorder<E> attribute(org.jetlinks.core.Key<V> key, java.util.function.Supplier<V> value) {
                    return attribute(key, value.get());
                }

                @Override
                public ActionRecorder<E> attributes(Map<String, Object> data) {
                    attributes.putAll(data);
                    return this;
                }

                @Override
                public ActionRecorder<E> error(Throwable error) {
                    FallbackRecorder.this.error = (RecordedActionException) error;
                    return this;
                }

                @Override
                public ActionRecorder<E> cancel() {
                    return this;
                }

                @Override
                public ActionRecorder<E> complete() {
                    return this;
                }

                @Override
                public ActionRecorder<E> value(E value) {
                    return this;
                }

                @Override
                public ActionRecorder<E> valueConverter(Function<E, Object> converter) {
                    return this;
                }

                @Override
                public ActionRecorder<E> start(reactor.util.context.ContextView context) {
                    return this;
                }

                @Override
                public <T> ActionRecorder<T> child(CharSequence action) {
                    return ActionRecorder.noop();
                }
            };
        }
    }
}
