package org.jetlinks.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.hswebframework.web.exception.ValidationException;
import org.jetlinks.core.exception.DeviceOperationException;

import java.net.SocketTimeoutException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

/**
 * @author bsetfeng
 * @author zhouhao
 * @version 1.0
 **/
@Getter
@AllArgsConstructor
public enum ErrorCode {
    /* 设备消息相关*/
    REQUEST_HANDLING("error.code.request_handling"),
    CLIENT_OFFLINE("error.code.client_offline"),
    CONNECTION_LOST("error.code.connection_lost"),
    NO_REPLY("error.code.no_reply"),
    TIME_OUT("error.code.time_out"),
    SYSTEM_ERROR("error.code.system_error"),
    UNSUPPORTED_MESSAGE("error.code.unsupported_message"),
    PARAMETER_ERROR("error.code.parameter_error"),
    PARAMETER_UNDEFINED("error.code.parameter_undefined"),
    FUNCTION_UNDEFINED("error.code.function_undefined"),
    PROPERTY_UNDEFINED("error.code.property_undefined"),
    UNKNOWN_PARENT_DEVICE("error.code.unknown_parent_device"),
    CYCLIC_DEPENDENCE("error.code.cyclic_dependence"),
    SERVER_NOT_AVAILABLE("error.code.server_not_available"),
    UNKNOWN("error.code.unknown"),
    SYSTEM_BUSY("error.code.system_busy"),
    ;

    private final String text;

    public static Optional<ErrorCode> of(String code) {
        if (code == null) {
            return Optional.empty();
        }
        for (ErrorCode value : values()) {
            if (value.name().equalsIgnoreCase(code)) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public static ErrorCode of(Throwable e) {
        if (e instanceof DeviceOperationException) {
            return ((DeviceOperationException) e).getCode();
        }
        // 参数错误
        else if (e instanceof IllegalArgumentException
            || e instanceof ValidationException
            || e instanceof javax.validation.ValidationException
            || e instanceof NullPointerException
            || e instanceof IndexOutOfBoundsException) {
            return ErrorCode.PARAMETER_ERROR;
        }
        // 不支持
        else if (e instanceof UnsupportedOperationException) {
            return ErrorCode.UNSUPPORTED_MESSAGE;
        }
        // 超时
        else if (e instanceof TimeoutException
            || e instanceof SocketTimeoutException
            || e instanceof io.netty.handler.timeout.TimeoutException) {
            return ErrorCode.TIME_OUT;
        } else {
            return ErrorCode.SYSTEM_ERROR;
        }
    }

}
