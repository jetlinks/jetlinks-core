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
     * 注册一个运行对象，由实现按需读取契约声明的指标，而非冻结注册时的值。
     * 默认不支持，保留既有 count/value/error 实现的兼容性；不解析注解或调用 getter。
     * 支持实现应复用 MetricSet，绑定实例生命周期，读取不清零；注册表与采样调度由实现管理。
     *
     * @param scope 实现所属范围内的稳定实例身份；不自动导出为无限基数标签
     * @param contract public 观测契约，不从 target 的实现类推断额外属性
     * @param target 实现契约的对象，由调用方保证 getter 只读有界内存值
     * @param <T> 契约类型
     * @return 本次注册的句柄；关闭须幂等，只释放本次注册；unsupported 不等于零值
     * @since 1.3.3
     * @see MetricSet
     */
    default <T> MetricRegistration observe(String scope, Class<T> contract, T target) {
        return MetricRegistration.unsupported();
    }

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
