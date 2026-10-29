package org.jetlinks.core.monitor;

import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.recorder.ActionRecord;
import org.jetlinks.core.monitor.recorder.ActionRecorder;
import org.jetlinks.core.monitor.recorder.Recorder;
import org.jetlinks.core.monitor.tracer.Tracer;

import java.util.Objects;
import java.util.function.Supplier;

class ProxyMonitor extends DefaultMonitor {

    private final Recorder recorder;

    public ProxyMonitor(Supplier<Monitor> lazyRef) {
        super(
            Logger.lazy(() -> lazyRef.get().logger()),
            Tracer.lazy(() -> lazyRef.get().tracer()),
            Metrics.lazy(() -> lazyRef.get().metrics())
        );
        Objects.requireNonNull(lazyRef, "lazyRef");
        this.recorder = new Recorder() {
            @Override
            public <E> ActionRecorder<E> action(CharSequence action) {
                return lazyRef.get().recorder().action(action);
            }

            @Override
            public void record(ActionRecord record) {
                lazyRef.get().recorder().record(record);
            }
        };
    }

    @Override
    public Recorder recorder() {
        return recorder;
    }
}
