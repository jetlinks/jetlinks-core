package org.jetlinks.core.monitor.metrics;

import java.util.function.Supplier;

/**
 * 监控度量指标
 *
 * @author zhouhao
 * @since 1.2.1
 */
public interface Metrics {

    /**
     * @return 什么也不做的Metrics
     */
    static Metrics noop() {
        return NoopMetrics.instance;
    }

    /**
     * 创建一个延迟加载的监控对象,用于解决Metrics延后加载时无法获取等场景.
     *
     * @param lazy Metrics提供商
     * @return Metrics
     */
    static Metrics lazy(Supplier<Metrics> lazy) {
        return new ProxyMetrics(lazy);
    }

    /**
     * 对指定操作计数,如:
     * <pre>{@code
     *   //请求数量 +1
     *   metrics().count("requests",1);
     * }</pre>
     *
     * @param operation 操作
     * @param inc       数量
     */
    void count(String operation, int inc);

    /**
     * 设置指定操作的数值,如: 请求并发数
     * <pre>{@code
     *   metrics().value("conn",100);
     * }</pre>
     *
     * @param operation 操作
     * @param value     数量
     */
    void value(String operation, double value);

    /**
     * 记录错误信息
     *
     * @param operation 操作
     * @param error     异常
     */
    void error(String operation, Throwable error);

}
