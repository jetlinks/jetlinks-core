package org.jetlinks.core.monitor;

import org.jetlinks.core.monitor.logger.Logger;
import org.jetlinks.core.monitor.metrics.Metrics;
import org.jetlinks.core.monitor.observation.ObservationSpec;
import org.jetlinks.core.monitor.observation.ObservationSupport;
import org.jetlinks.core.monitor.observation.RecorderProvider;
import org.jetlinks.core.monitor.recorder.Recorder;
import org.jetlinks.core.monitor.tracer.Tracer;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.Objects;
import java.util.function.Function;
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
     * 读取当前本地作用域，不发现远端 Monitor，也不据此授予业务权限。
     *
     * @since 1.3.3
     */
    static Monitor current(ContextView context, Monitor fallback) {
        return context.getOrDefault(Monitor.class, Objects.requireNonNull(fallback, "fallback"));
    }

    /**
     * @return 默认只保留原记录器；Context 内显式绑定的选择器优先
     * @since 1.3.3
     */
    default RecorderProvider recorderProvider() {
        return RecorderProvider.disabled();
    }

    /**
     * 返回不可变配置包装，保留原四类观测和原生执行入口。
     *
     * @since 1.3.3
     */
    default Monitor withRecorderProvider(RecorderProvider provider) {
        return new ConfiguredMonitor(this, provider);
    }

    /**
     * 按订阅观察原操作生命周期，不额外订阅、重试、建 span 或改变业务信号。
     *
     * @since 1.3.3
     */
    default <T> Mono<T> observeMono(String action, Supplier<? extends Mono<T>> operation) {
        Objects.requireNonNull(operation, "operation");
        return observeMono(ObservationSpec.<Void, T>of(action), null, ignored -> operation.get());
    }

    /**
     * 一次业务包装。每次订阅独立选择记录器并创建 action，详细提取仅在启用时执行。
     * 原 recorder 工厂须返回未启动的新 action；完整性与持久化由来源保证。
     * 仅隔离非 fatal 监控异常，业务错误、空结果和取消保持原样。
     *
     * @param spec      来源本地的不可变配置
     * @param input     本次实际业务输入
     * @param operation 原响应式操作，在订阅时调用
     * @since 1.3.3
     * @see ObservationSpec
     */
    default <I, O> Mono<O> observeMono(ObservationSpec<I, O> spec, I input,
                                      Function<? super I, ? extends Mono<O>> operation) {
        return ObservationSupport.observe(this, spec, input, operation);
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

    /**
     * 记录器接口,用于记录操作执行等信息
     *
     * <pre>{@code
     * ActionRecorder<Void> recorder= monitor
     *   .recorder()
     *   .action("execute");
     *
     * // .....
     *
     *
     * //完成
     * recorder.complete();
     * }</pre>
     *
     * @return Recorder
     * @see Recorder
     * @see org.jetlinks.core.monitor.recorder.ActionRecorder
     */
    default Recorder recorder() {
        return Recorder.noop();
    }
}
