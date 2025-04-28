package org.jetlinks.core.collector.subscribe;

import org.jetlinks.core.collector.DataCollectorProvider;
import org.jetlinks.core.collector.PointData;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

public interface PointSubscriber extends Disposable {

    Mono<Void> next(PointData data);

    void setState(DataCollectorProvider.State state);

    @Override
    boolean isDisposed();

    @Override
    void dispose();

}
