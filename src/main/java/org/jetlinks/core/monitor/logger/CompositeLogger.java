package org.jetlinks.core.monitor.logger;

import lombok.AllArgsConstructor;
import org.slf4j.event.Level;

@AllArgsConstructor
class CompositeLogger implements Logger {
    private final Logger[] loggers;

    @Override
    public boolean isEnabled(Level level) {
        for (Logger logger : loggers) {
            if (logger.isEnabled(level)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void log(Level level, String message, Object... args) {
        for (Logger logger : loggers) {
            if (logger.isEnabled(level)) {
                logger.log(level, message, args);
            }
        }
    }
}
