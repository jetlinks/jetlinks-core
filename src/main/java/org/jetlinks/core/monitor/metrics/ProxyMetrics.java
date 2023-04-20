package org.jetlinks.core.monitor.metrics;

import lombok.AllArgsConstructor;

import java.util.function.Supplier;

@AllArgsConstructor
public class ProxyMetrics implements Metrics {
    private final Supplier<Metrics> lazyRef;

    @Override
    public void count(String operation, int inc) {
        lazyRef.get().count(operation, inc);
    }

    @Override
    public void value(String operation, double value) {
        lazyRef.get().value(operation, value);
    }

    @Override
    public void error(String operation, Throwable error) {
        lazyRef.get().error(operation, error);
    }
}
