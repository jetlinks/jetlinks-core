package org.jetlinks.core.monitor.logger;

import org.slf4j.event.Level;

import java.util.function.Supplier;

public interface Logger {

    static Logger composite(Logger... loggers) {
        return new CompositeLogger(loggers);
    }

    static Logger lazy(Supplier<Logger> lazy) {
        return new ProxyLogger(lazy);
    }

    default boolean isTraceEnabled() {
        return isEnabled(Level.TRACE);
    }

    default boolean isDebugEnabled() {
        return isEnabled(Level.DEBUG);
    }

    default boolean isInfoEnabled() {
        return isEnabled(Level.INFO);
    }

    default boolean isWarnEnabled() {
        return isEnabled(Level.WARN);
    }

    default boolean isErrorEnabled() {
        return isEnabled(Level.ERROR);
    }

    default void trace(String message, Object... args) {
        log(Level.TRACE, message, args);
    }

    default void debug(String message, Object... args) {
        log(Level.DEBUG, message, args);
    }

    default void info(String message, Object... args) {
        log(Level.INFO, message, args);
    }

    default void warn(String message, Object... args) {
        log(Level.WARN, message, args);
    }

    default void error(String message, Object... args) {
        log(Level.ERROR, message, args);
    }

    default boolean isEnabled(Level level) {
        return true;
    }

    void log(Level level, String message, Object... args);

}
