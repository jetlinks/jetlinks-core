package org.jetlinks.core.collector.internal;

import org.jetlinks.core.collector.CollectorConstants;
import org.jetlinks.core.collector.DataCollectorProvider;
import org.jetlinks.core.command.AbstractCommandSupport;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Mono;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.function.BiConsumer;

public abstract class AbstractLifecycle extends AbstractCommandSupport implements DataCollectorProvider.Lifecycle {

    private static final AtomicReferenceFieldUpdater<AbstractLifecycle, DataCollectorProvider.State>
        STATE = AtomicReferenceFieldUpdater.newUpdater(AbstractLifecycle.class, DataCollectorProvider.State.class, "state");

    private List<BiConsumer<DataCollectorProvider.State, DataCollectorProvider.State>> stateListener;

    private volatile DataCollectorProvider.State state = CollectorConstants.States.initializing;

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

    protected final boolean changeState(DataCollectorProvider.State expect,
                                        DataCollectorProvider.State state) {

        if (STATE.compareAndSet(this, expect, state)) {
            fireListener(expect, state);
            return true;
        }
        return false;
    }

    protected final boolean changeState(DataCollectorProvider.State state) {

        DataCollectorProvider.State before = STATE.getAndSet(this, state);

        if (before != state) {
            fireListener(before, state);
            return true;
        }
        return false;
    }

    private void fireListener(DataCollectorProvider.State before,
                              DataCollectorProvider.State after) {

        List<BiConsumer<DataCollectorProvider.State, DataCollectorProvider.State>> stateListener = this.stateListener;

        if (stateListener != null) {
            for (BiConsumer<DataCollectorProvider.State, DataCollectorProvider.State> consumer : stateListener) {
                consumer.accept(before, after);
            }
        }

    }

    @Override
    public final void start() {
        if (disposable.isDisposed()) {
            return;
        }
        if (changeState(CollectorConstants.States.starting)) {
            start0();
            changeState(CollectorConstants.States.starting,
                        CollectorConstants.States.running);
        }
    }

    @Override
    public void pause() {
        if (disposable.isDisposed()) {
            return;
        }
        changeState(CollectorConstants.States.paused);
    }

    protected final void doOnStop(Disposable listener) {
        this.disposable.add(listener);
    }

    @Override
    public final Disposable onStateChanged(BiConsumer<DataCollectorProvider.State, DataCollectorProvider.State> listener) {
        synchronized (this) {
            List<BiConsumer<DataCollectorProvider.State, DataCollectorProvider.State>> listeners = this.stateListener;
            if (listeners == null) {
                listeners = new LinkedList<>();
                this.stateListener = listeners;
            }
            listeners.add(listener);

            return () -> {
                synchronized (this) {
                    List<BiConsumer<DataCollectorProvider.State, DataCollectorProvider.State>> _listeners = this.stateListener;
                    _listeners.remove(listener);
                    if (_listeners.isEmpty()) {
                        this.stateListener = null;
                    }
                }
            };
        }
    }

    @Override
    public final void dispose() {
        synchronized (this) {
            if (disposable.isDisposed()) {
                return;
            }
            disposable.dispose();
        }
        stop0();
        changeState(CollectorConstants.States.stopped);
    }
}
