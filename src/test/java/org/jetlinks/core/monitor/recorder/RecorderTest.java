package org.jetlinks.core.monitor.recorder;

import org.junit.Test;
import reactor.util.context.Context;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

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
}
