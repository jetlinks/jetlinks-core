package org.jetlinks.core.monitor;

import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.observation.ObservationSpec;
import org.jetlinks.core.monitor.observation.RecorderProvider;
import org.jetlinks.core.monitor.recorder.ActionRecord;
import org.jetlinks.core.monitor.recorder.ActionRecorder;
import org.jetlinks.core.monitor.recorder.Recorder;
import org.jetlinks.core.monitor.tracer.Tracer;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

class ProxyMonitor extends DefaultMonitor {

    private final Supplier<Monitor> lazyRef;
    private final Recorder recorder;

    public ProxyMonitor(Supplier<Monitor> lazyRef) {
        super(
            Logger.lazy(() -> lazyRef.get().logger()),
            Tracer.lazy(() -> lazyRef.get().tracer()),
            Metrics.lazy(() -> lazyRef.get().metrics())
        );
        this.lazyRef = Objects.requireNonNull(lazyRef, "lazyRef");
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

    @Override
    public RecorderProvider recorderProvider() {
        return (action, context) -> lazyRef.get().recorderProvider().select(action, context);
    }

    @Override
    public <T> Mono<T> observeMono(String action, Supplier<? extends Mono<T>> operation) {
        return Mono.defer(() -> lazyRef.get().observeMono(action, operation));
    }

    @Override
    public <I, O> Mono<O> observeMono(ObservationSpec<I, O> spec, I input,
                                    Function<? super I, ? extends Mono<O>> operation) {
        // 原 Monitor 可能提供未启动工厂或独有标签，不能用本代理的默认入口绕过。
        return Mono.defer(() -> lazyRef.get().observeMono(spec, input, operation));
    }
}
