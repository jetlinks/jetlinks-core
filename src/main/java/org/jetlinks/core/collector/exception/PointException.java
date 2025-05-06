package org.jetlinks.core.collector.exception;

import lombok.Getter;
import org.hswebframework.web.exception.I18nSupportException;

@Getter
public class PointException extends I18nSupportException.NoStackTrace {

    private final String pointId;

    public PointException(String pointId,
                          String code,
                          Object... args) {
        super(code, args);
        this.pointId = pointId;
    }

    public PointException(String channelId,
                          String code,
                          Throwable cause,
                          Object... args) {
        super(code, cause, args);
        this.pointId = channelId;
    }

}
