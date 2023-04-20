package org.jetlinks.core.monitor;

import lombok.AllArgsConstructor;
import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.tracer.Tracer;

@AllArgsConstructor
public class DefaultMonitor implements Monitor {

    private final Logger logger;
    private final Tracer tracer;
    private final Metrics metrics;

    @Override
    public Logger logger() {
        return logger;
    }

    @Override
    public Tracer tracer() {
        return tracer;
    }

    @Override
    public Metrics metrics() {
        return metrics;
    }
}
