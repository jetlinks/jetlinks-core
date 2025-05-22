package org.jetlinks.core.monitor;

import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.tracer.Tracer;

import java.util.function.Supplier;

/**
 * 通用监控接口定义
 *
 * @author zhouhao
 * @since 1.2.1
 */
public interface Monitor {

    /**
     * 创建一个延迟加载的监控对象,用于解决Monitor延后加载时无法获取等场景.
     *
     * @param lazy Monitor提供商
     * @return Monitor
     */
    static Monitor lazy(Supplier<Monitor> lazy) {
        return new ProxyMonitor(lazy);
    }

    /**
     * @return 什么也不做的Monitor
     */
    static Monitor noop() {
        return NoopMonitor.INSTANCE;
    }

    /**
     * 日志接口,用于打印日志,替代Slf4j,有助于更好的记录很跟踪对应操作的监控
     *
     * @return 日志接口
     */
    Logger logger();

    /**
     * 链路追踪接口,用于进行链路追踪
     *
     * @return 链路追踪接口
     */
    Tracer tracer();

    /**
     * 度量接口,用于记录一些监控数据,如 数据量统计等
     *
     * @return 度量接口
     */
    Metrics metrics();

}
