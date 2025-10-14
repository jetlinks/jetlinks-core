package org.jetlinks.core.monitor;

import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.recorder.Recorder;
import org.jetlinks.core.monitor.tracer.Tracer;

public class DefaultMonitor implements Monitor {

    private final Logger logger;
    private final Tracer tracer;
    private final Metrics metrics;
    private final Recorder recorder;

    public DefaultMonitor(Logger logger, Tracer tracer, Metrics metrics) {
        this(logger, tracer, metrics, Recorder.noop());
    }

    public DefaultMonitor(Logger logger, Tracer tracer, Metrics metrics, Recorder recorder) {
        this.logger = logger;
        this.tracer = tracer;
        this.metrics = metrics;
        this.recorder = recorder;
    }

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

    @Override
    public Recorder recorder() {
        return recorder;
    }
}
