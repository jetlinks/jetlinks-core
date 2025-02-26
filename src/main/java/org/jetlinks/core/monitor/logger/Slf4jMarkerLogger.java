package org.jetlinks.core.monitor.logger;

import org.slf4j.Marker;
import org.slf4j.event.Level;

public interface Slf4jMarkerLogger extends Logger {
    org.slf4j.Logger getLogger();

    Marker getMarker();

    @Override
    default boolean isEnabled(Level level) {
        switch (level) {
            case TRACE:
                return getLogger().isTraceEnabled(getMarker());
            case DEBUG:
                return getLogger().isDebugEnabled(getMarker());
            case INFO:
                return getLogger().isInfoEnabled(getMarker());
            case WARN:
                return getLogger().isWarnEnabled(getMarker());
            case ERROR:
                return getLogger().isErrorEnabled(getMarker());
        }
        return false;
    }

    @Override
    default void log(Level level, String message, Object... args) {
        switch (level) {
            case TRACE:
                getLogger().trace(getMarker(), message, args);
                return;
            case DEBUG:
                getLogger().debug(getMarker(), message, args);
                return;
            case INFO:
                getLogger().info(getMarker(), message, args);
                return;
            case WARN:
                getLogger().warn(getMarker(), message, args);
                return;
            case ERROR:
                getLogger().error(getMarker(), message, args);
        }
    }

}
