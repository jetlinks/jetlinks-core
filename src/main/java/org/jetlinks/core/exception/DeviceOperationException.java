package org.jetlinks.core.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.enums.ErrorCode;

@AllArgsConstructor
public class DeviceOperationException extends RuntimeException {

    @Getter
    private ErrorCode code;

    private String message;

    public DeviceOperationException(ErrorCode errorCode) {
        this(errorCode, errorCode.getText());
    }

    public DeviceOperationException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.code = errorCode;
        this.message = cause.getMessage();
    }

    @Override
    public String getMessage() {
        return message == null ? code.getText() : message;
    }
}
