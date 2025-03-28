package org.jetlinks.core.collector.exception;

import lombok.Getter;
import org.hswebframework.web.exception.I18nSupportException;

@Getter
public abstract class CollectorException extends I18nSupportException.NoStackTrace {

    private final String collectorId;

    public CollectorException(String collectorId,
                              String messageOrI18nCode,
                              Throwable cause,
                              Object... args) {
        super(messageOrI18nCode, cause, args);
        this.collectorId = collectorId;
    }

    abstract int getCode();
}
