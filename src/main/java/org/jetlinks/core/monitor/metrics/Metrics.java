package org.jetlinks.core.monitor.metrics;

import java.util.function.Supplier;

public interface Metrics {

    static Metrics noop() {
        return NoopMetrics.instance;
    }

    static Metrics lazy(Supplier<Metrics> lazy) {
        return new ProxyMetrics(lazy);
    }

    void count(String operation, int inc);

    void value(String operation, double value);

    void error(String operation, Throwable error);

}
