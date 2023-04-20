package org.jetlinks.core.monitor.logger;

import lombok.AllArgsConstructor;
import org.slf4j.event.Level;

import java.util.function.Supplier;

@AllArgsConstructor
class ProxyLogger implements Logger {

    private final Supplier<Logger> lazyRef;

    @Override
    public boolean isEnabled(Level level) {
        return lazyRef.get().isEnabled(level);
    }

    @Override
    public void log(Level level, String message, Object... args) {
        lazyRef.get().log(level, message, args);
    }
}
