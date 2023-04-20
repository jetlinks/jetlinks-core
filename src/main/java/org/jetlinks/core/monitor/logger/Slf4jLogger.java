package org.jetlinks.core.monitor.logger;

import lombok.AllArgsConstructor;
import org.slf4j.event.Level;

@AllArgsConstructor
public class Slf4jLogger implements Logger {
    private final org.slf4j.Logger logger;

    @Override
    public boolean isEnabled(Level level) {
        switch (level) {
            case TRACE:
                return logger.isTraceEnabled();
            case DEBUG:
                return logger.isDebugEnabled();
            case INFO:
                return logger.isInfoEnabled();
            case WARN:
                return logger.isWarnEnabled();
            case ERROR:
                return logger.isErrorEnabled();
        }
        return false;
    }

    @Override
    public void log(Level level, String message, Object... args) {
        switch (level) {
            case TRACE:
                logger.trace(message, args);
                return;
            case DEBUG:
                logger.debug(message, args);
                return;
            case INFO:
                logger.info(message, args);
                return;
            case WARN:
                logger.warn(message, args);
                return;
            case ERROR:
                logger.error(message, args);
                return;
        }
    }
}
