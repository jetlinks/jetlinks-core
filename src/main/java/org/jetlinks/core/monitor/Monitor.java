package org.jetlinks.core.monitor;

import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.tracer.Tracer;

import java.util.function.Supplier;

public interface Monitor  {


    static Monitor lazy(Supplier<Monitor> lazy) {
        return new ProxyMonitor(lazy);
    }

    Logger logger();

    Tracer tracer();

    Metrics metrics();

}
