package org.jetlinks.core.monitor;

import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.tracer.Tracer;

class NoopMonitor implements Monitor {

    static NoopMonitor INSTANCE = new NoopMonitor();

    @Override
    public Logger logger() {
        return Logger.noop();
    }

    @Override
    public Tracer tracer() {
        return Tracer.noop();
    }

    @Override
    public Metrics metrics() {
        return Metrics.noop();
    }
}
