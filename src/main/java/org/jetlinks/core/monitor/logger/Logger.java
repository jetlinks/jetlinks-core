package org.jetlinks.core.monitor.logger;

import org.slf4j.event.Level;

import java.util.function.Supplier;

/**
 * 日志接口,用法和slf4j相同.
 *
 * <pre>{@code
 *
 *  logger().debug("收到Kafka报文:{}",message);
 *
 * }</pre>
 *
 * @author zhouhao
 * @since 1.2.1
 */
public interface Logger {

    /**
     * 组合多个日志接口,在相关api被调用时,每一个接口的对应api都会被调用.
     *
     * @param loggers 要被组合的日志接口
     * @return 日志接口
     */
    static Logger composite(Logger... loggers) {
        return new CompositeLogger(loggers);
    }

    /**
     * 创建一个延迟加载的日志接口,用于解决日志接口延后加载时无法获取等场景.
     *
     * @param lazy 日志提供商
     * @return 日志接口
     */
    static Logger lazy(Supplier<Logger> lazy) {
        return new ProxyLogger(lazy);
    }

    /**
     * @return 什么也不做的Logger
     */
    static Logger noop() {
        return NoopLogger.INSTANCE;
    }

    /**
     * 判断是否启用TRACE级别的日志
     *
     * @return 是否启用
     * @see Level#TRACE
     */
    default boolean isTraceEnabled() {
        return isEnabled(Level.TRACE);
    }

    /**
     * 判断是否启用DEBUG级别的日志
     *
     * @return 是否启用
     * @see Level#DEBUG
     */
    default boolean isDebugEnabled() {
        return isEnabled(Level.DEBUG);
    }

    /**
     * 判断是否启用INFO级别的日志
     *
     * @return 是否启用
     * @see Level#INFO
     */
    default boolean isInfoEnabled() {
        return isEnabled(Level.INFO);
    }

    /**
     * 判断是否启用WARN级别的日志
     *
     * @return 是否启用
     * @see Level#WARN
     */
    default boolean isWarnEnabled() {
        return isEnabled(Level.WARN);
    }

    /**
     * 判断是否启用ERROR级别的日志
     *
     * @return 是否启用
     * @see Level#ERROR
     */
    default boolean isErrorEnabled() {
        return isEnabled(Level.ERROR);
    }

    /**
     * 打印跟踪日志
     * 用法和{@link org.slf4j.Logger#trace(String, Object...)}一致
     *
     * @param message 日志内容
     * @param args    模版参数
     */
    default void trace(String message, Object... args) {
        log(Level.TRACE, message, args);
    }

    /**
     * 打印Debug日志
     * 用法和{@link org.slf4j.Logger#debug(String, Object...)}一致
     *
     * @param message 日志内容
     * @param args    模版参数
     */
    default void debug(String message, Object... args) {
        log(Level.DEBUG, message, args);
    }

    /**
     * 打印info日志
     * 用法和{@link org.slf4j.Logger#info(String, Object...)}一致
     *
     * @param message 日志内容
     * @param args    模版参数
     */
    default void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    /**
     * 打印warn日志
     * 用法和{@link org.slf4j.Logger#warn(String, Object...)}一致
     *
     * @param message 日志内容
     * @param args    模版参数
     */
    default void warn(String message, Object... args) {
        log(Level.WARN, message, args);
    }

    /**
     * 打印error日志
     * 用法和{@link org.slf4j.Logger#error(String, Object...)}一致
     *
     * @param message 日志内容
     * @param args    模版参数
     */
    default void error(String message, Object... args) {
        log(Level.ERROR, message, args);
    }

    /**
     * 判断是否启用对应级别的日志
     *
     * @return 是否启用
     * @see Level
     */
    default boolean isEnabled(Level level) {
        return true;
    }

    /**
     * 打印日志
     *
     * @param level   日志级别
     * @param message 日志内容
     * @param args    模版参数
     */
    void log(Level level, String message, Object... args);


    /**
     * 转为Slf4j Logger
     *
     * @return slf4j Logger
     */
    default org.slf4j.Logger slf4j() {
        return new BridgeLoggerSlf4j(this);
    }
}
