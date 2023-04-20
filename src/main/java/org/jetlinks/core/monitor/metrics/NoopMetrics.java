package org.jetlinks.core.monitor.metrics;

final class NoopMetrics implements Metrics {

    static final NoopMetrics instance = new NoopMetrics();

    @Override
    public void count(String operation, int inc) {

    }

    @Override
    public void value(String operation, double value) {

    }

    @Override
    public void error(String operation, Throwable error) {

    }
}
