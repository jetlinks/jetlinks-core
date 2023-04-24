package org.jetlinks.core.monitor.logger;

import org.slf4j.event.Level;

class NoopLogger implements Logger {
    static final NoopLogger INSTANCE = new NoopLogger();


    @Override
    public boolean isEnabled(Level level) {
        return false;
    }

    @Override
    public void log(Level level, String message, Object... args) {

    }
}
