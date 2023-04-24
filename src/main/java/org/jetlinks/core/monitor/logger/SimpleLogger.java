package org.jetlinks.core.monitor.logger;

import org.slf4j.event.Level;

@Deprecated
public abstract class SimpleLogger implements Logger {

    @Override
    public void log(Level level, String message, Object... args) {
        handleLog(level, message, args);
    }

    protected abstract void handleLog(Level level, String message, Object... args);
}
