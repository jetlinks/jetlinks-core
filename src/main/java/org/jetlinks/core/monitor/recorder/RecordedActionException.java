package org.jetlinks.core.monitor.recorder;

import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * 由已有 {@link ActionRecord} 回放出的错误信息.
 *
 * @author zhouhao
 * @since 1.3.2
 */
@Getter
public class RecordedActionException extends RuntimeException {

    private final String errorType;
    private final String errorDetail;

    public RecordedActionException(ActionRecord record) {
        this(record == null ? null : record.getErrorType(),
             record == null ? null : record.getErrorDetail());
    }

    public RecordedActionException(String errorType, String errorDetail) {
        super(createMessage(errorType, errorDetail));
        this.errorType = errorType;
        this.errorDetail = errorDetail;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    private static String createMessage(String errorType, String errorDetail) {
        if (StringUtils.hasText(errorType) && StringUtils.hasText(errorDetail)) {
            return errorType + ": " + errorDetail;
        }
        if (StringUtils.hasText(errorType)) {
            return errorType;
        }
        return errorDetail;
    }
}
