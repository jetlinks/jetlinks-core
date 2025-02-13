package org.jetlinks.core.collector.exception;

import lombok.Getter;
import org.hswebframework.web.exception.I18nSupportException;

@Getter
public class CollectorException extends I18nSupportException.NoStackTrace {

    private final String collectorId;

    public CollectorException(String collectorId,
                              String code,
                              Throwable cause,
                              Object... args) {
        super(code, cause, args);
        this.collectorId = collectorId;
    }
}
