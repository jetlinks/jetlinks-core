package org.jetlinks.core.monitor.logger;

import org.slf4j.event.Level;

public interface Slf4jLoggerAdapter extends Logger {

    org.slf4j.Logger getLogger();

    @Override
    default boolean isEnabled(Level level) {
        switch (level) {
            case TRACE:
                return getLogger().isTraceEnabled();
            case DEBUG:
                return getLogger().isDebugEnabled();
            case INFO:
                return getLogger().isInfoEnabled();
            case WARN:
                return getLogger().isWarnEnabled();
            case ERROR:
                return getLogger().isErrorEnabled();
        }
        return false;
    }

    @Override
    default void log(Level level, String message, Object... args) {
        switch (level) {
            case TRACE:
                getLogger().trace(message, args);
                return;
            case DEBUG:
                getLogger().debug(message, args);
                return;
            case INFO:
                getLogger().info(message, args);
                return;
            case WARN:
                getLogger().warn(message, args);
                return;
            case ERROR:
                getLogger().error(message, args);
        }
    }

}
