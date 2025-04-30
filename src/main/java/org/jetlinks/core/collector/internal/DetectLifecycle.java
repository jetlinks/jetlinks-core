package org.jetlinks.core.collector.internal;

import org.jetlinks.core.collector.DataCollectorProvider;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.util.function.BiConsumer;

public interface DetectLifecycle extends DataCollectorProvider.Lifecycle {

    DataCollectorProvider.Lifecycle getDetected();

    @Override
    default boolean isWrapperFor(Class<?> type) {
        return getDetected().isWrapperFor(type);
    }

    @Override
    default <T> T unwrap(Class<T> type) {
        return DataCollectorProvider.Lifecycle.super.unwrap(type);
    }

    @Override
    default Disposable onStateChanged(BiConsumer<DataCollectorProvider.State, DataCollectorProvider.State> listener) {
        return getDetected().onStateChanged(listener);
    }

    @Override
    default DataCollectorProvider.State state() {
        return getDetected().state();
    }

    @Override
   default Mono<DataCollectorProvider.State> checkState(){
        return getDetected().checkState();
    }
}
