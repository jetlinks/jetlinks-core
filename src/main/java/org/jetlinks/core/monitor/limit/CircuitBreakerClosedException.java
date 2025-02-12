package org.jetlinks.core.monitor.limit;

import org.hswebframework.web.exception.BusinessException;

public class CircuitBreakerClosedException extends BusinessException {


    public CircuitBreakerClosedException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
