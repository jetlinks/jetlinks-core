package org.jetlinks.core.server;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/** Fixed-size session-generation facts, shared by transport-specific multi-connection sessions. */
public final class ConnectionLifecycleRecorder {
    private static final AtomicLongFieldUpdater<ConnectionLifecycleRecorder> OPENED =
        AtomicLongFieldUpdater.newUpdater(ConnectionLifecycleRecorder.class, "opened");
    private static final AtomicLongFieldUpdater<ConnectionLifecycleRecorder> CLOSED =
        AtomicLongFieldUpdater.newUpdater(ConnectionLifecycleRecorder.class, "closed");
    private static final AtomicLongFieldUpdater<ConnectionLifecycleRecorder> LAST_CHANGE =
        AtomicLongFieldUpdater.newUpdater(ConnectionLifecycleRecorder.class, "lastChange");
    private final long observedSince = System.currentTimeMillis();
    private volatile long opened, closed, lastChange;

    public void opened() { OPENED.incrementAndGet(this); LAST_CHANGE.accumulateAndGet(this, System.currentTimeMillis(), Math::max); }
    public void closed() { CLOSED.incrementAndGet(this); LAST_CHANGE.accumulateAndGet(this, System.currentTimeMillis(), Math::max); }
    public ConnectionLifecycle snapshot() {
        return new ConnectionLifecycle(observedSince, opened, closed, lastChange);
    }
}
