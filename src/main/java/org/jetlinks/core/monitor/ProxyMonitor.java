package org.jetlinks.core.monitor;

import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.tracer.Tracer;

import java.util.function.Supplier;

class ProxyMonitor extends DefaultMonitor {


    public ProxyMonitor(Supplier<Monitor> lazyRef) {
        super(
                Logger.lazy(() -> lazyRef.get().logger()),
                Tracer.lazy(() -> lazyRef.get().tracer()),
                Metrics.lazy(() -> lazyRef.get().metrics())
        );
    }

}
