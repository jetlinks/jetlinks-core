package org.jetlinks.core.collector.internal;

import org.jetlinks.core.collector.CollectorConstants;
import org.jetlinks.core.collector.DataCollectorProvider;
import org.jetlinks.core.command.AbstractCommandSupport;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public abstract class AbstractLifecycle extends AbstractCommandSupport implements DataCollectorProvider.Lifecycle {

    private static final AtomicReferenceFieldUpdater<AbstractLifecycle, DataCollectorProvider.State>
            STATE = AtomicReferenceFieldUpdater.newUpdater(AbstractLifecycle.class, DataCollectorProvider.State.class, "state");

    private volatile DataCollectorProvider.State state;

    private final Disposable.Composite disposable = Disposables.composite();

    protected abstract void start0();

    protected abstract void stop0();

    protected Mono<DataCollectorProvider.State> checkState0() {
        return Mono.just(state());
    }

    @Override
    public final Mono<DataCollectorProvider.State> checkState() {
        return checkState0();
    }

    @Override
    public final DataCollectorProvider.State state() {
        return STATE.get(this);
    }

    protected final boolean changeState(DataCollectorProvider.State state) {
        return STATE.getAndSet(this, state) != state;
    }

    @Override
    public final void start() {
        if (disposable.isDisposed()) {
            return;
        }
        if (changeState(CollectorConstants.States.starting)) {
            start0();
            changeState(CollectorConstants.States.running);
        }
    }

    protected final void doOnStop(Disposable listener) {
        this.disposable.add(listener);
    }


    @Override
    public final void dispose() {
        if (disposable.isDisposed()) {
            return;
        }
        disposable.dispose();
        stop0();
    }
}
