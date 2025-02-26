package org.jetlinks.core.monitor.logger;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;

@AllArgsConstructor
public class Slf4jLogger implements Slf4jLoggerAdapter {
    private final org.slf4j.Logger logger;

    @Override
    public Logger getLogger() {
        return logger;
    }

}
