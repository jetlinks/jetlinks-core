package org.jetlinks.core.monitor;

import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.observation.ObservationSpec;
import org.jetlinks.core.monitor.observation.RecorderProvider;
import org.jetlinks.core.monitor.recorder.Recorder;
import org.jetlinks.core.monitor.tracer.Tracer;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 保留原 Monitor 对执行入口的覆写；只在没有显式选择器时绑定工厂配置。
 */
final class ConfiguredMonitor implements Monitor {
    private final Monitor delegate;
    private final RecorderProvider provider;

    ConfiguredMonitor(Monitor delegate, RecorderProvider provider) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.provider = Objects.requireNonNull(provider, "provider");
    }

    @Override
    public Logger logger() {
        return delegate.logger();
    }

    @Override
    public Tracer tracer() {
        return delegate.tracer();
    }

    @Override
    public Metrics metrics() {
        return delegate.metrics();
    }

    @Override
    public Recorder recorder() {
        return delegate.recorder();
    }

    @Override
    public RecorderProvider recorderProvider() {
        return provider;
    }

    private Context bind(Context context) {
        return context.hasKey(RecorderProvider.class) ? context : context.put(RecorderProvider.class, provider);
    }

    @Override
    public <T> Mono<T> observeMono(String action, Supplier<? extends Mono<T>> operation) {
        return Mono.defer(() -> delegate.observeMono(action, operation)).contextWrite(this::bind);
    }

    @Override
    public <I, O> Mono<O> observeMono(ObservationSpec<I, O> spec, I input,
                                    Function<? super I, ? extends Mono<O>> operation) {
        return Mono.defer(() -> delegate.observeMono(spec, input, operation)).contextWrite(this::bind);
    }
}
