package org.jetlinks.core.monitor;

import org.jetlinks.core.Key;
import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.recorder.AbstractActionRecorder;
import org.jetlinks.core.monitor.recorder.ActionRecord;
import org.jetlinks.core.monitor.recorder.ActionRecorder;
import org.jetlinks.core.monitor.recorder.Recorder;
import org.jetlinks.core.monitor.tracer.Tracer;
import reactor.util.context.ContextView;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

final class RecordingTestSupport {

    static final String SUBJECT = "test.subject";

    static Monitor monitor(TestRecorder recorder) {
        return new DefaultMonitor(Logger.noop(), Tracer.noop(), Metrics.noop(), recorder);
    }

    static final class NativeRecord extends ActionRecord {
        String subject;
    }

    static class TestRecorder implements Recorder {
        final List<NativeRecord> records = new CopyOnWriteArrayList<>();
        final AtomicInteger factories = new AtomicInteger();
        final AtomicInteger starts = new AtomicInteger();
        final AtomicInteger converters = new AtomicInteger();
        final String failAt;

        TestRecorder() {
            this(null);
        }

        TestRecorder(String failAt) {
            this.failAt = failAt;
        }

        void fail(String phase) {
            if (phase.equals(failAt)) {
                throw new IllegalStateException("Broken test recorder: " + phase);
            }
        }

        @Override
        public <E> ActionRecorder<E> action(CharSequence name) {
            fail("factory");
            factories.incrementAndGet();
            return new NativeAction<>(name, null);
        }

        private final class NativeAction<E> extends AbstractActionRecorder<E> {
            private NativeAction(CharSequence action, String parent) {
                super(action, parent);
            }

            @Override
            protected ActionRecord newRecord() {
                return new NativeRecord();
            }

            @Override
            public ActionRecorder<E> tag(String key, Object value) {
                fail("tag");
                return super.tag(key, value);
            }

            @Override
            public <V> ActionRecorder<E> tag(Key<V> key, V value) {
                fail("tag");
                return super.tag(key, value);
            }

            @Override
            public <V> ActionRecorder<E> attribute(Key<V> key, V value) {
                fail("attribute");
                return super.attribute(key, value);
            }

            @Override
            public ActionRecorder<E> start(ContextView context) {
                fail("start");
                starts.incrementAndGet();
                ((NativeRecord) record).subject = context.getOrDefault(SUBJECT, "none");
                return super.start(context);
            }

            @Override
            public ActionRecorder<E> value(E value) {
                fail("value");
                return super.value(value);
            }

            @Override
            public ActionRecorder<E> valueConverter(Function<E, Object> converter) {
                fail("converter");
                converters.incrementAndGet();
                return super.valueConverter(converter);
            }

            @Override
            protected void handle(ActionRecord result) {
                fail(result.isCancel() ? "cancel" : result.isHasError() ? "error" : "complete");
                // 故意要求原生子类型：通用 ActionRecord replay 不能替代该生命周期。
                records.add((NativeRecord) result);
            }

            @Override
            public <T> ActionRecorder<T> child(CharSequence action) {
                fail("child");
                return new NativeAction<>(action, record.getId());
            }
        }
    }
}
