package org.jetlinks.core.monitor.limit;

public class DoNotCircuitBreakerException extends RuntimeException{

    public DoNotCircuitBreakerException(Throwable cause) {
        super(cause);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
