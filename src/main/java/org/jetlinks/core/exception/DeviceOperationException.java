package org.jetlinks.core.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetlinks.core.enums.ErrorCode;

@AllArgsConstructor
public class DeviceOperationException extends RuntimeException {

    @Getter
    private ErrorCode code;

    public DeviceOperationException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.code = errorCode;
    }

    @Override
    public String getMessage() {
        return code.getText();
    }
}
