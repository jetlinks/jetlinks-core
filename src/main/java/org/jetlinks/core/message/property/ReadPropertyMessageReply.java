package org.jetlinks.core.message.property;

import io.vavr.control.Try;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.message.CommonDeviceMessageReply;

import java.util.Map;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class ReadPropertyMessageReply extends CommonDeviceMessageReply {

    private Map<String, Object> properties;

    public static ReadPropertyMessageReply failure(ErrorCode errorCode) {
        ReadPropertyMessageReply reply = new ReadPropertyMessageReply();
        reply.error(errorCode);
        return reply;
    }

    public static Try<ReadPropertyMessageReply> failureTry(ErrorCode errorCode) {
        return Try.success(failure(errorCode));
    }

    public static Try<ReadPropertyMessageReply> failureTry(String code, String message) {
        return Try.success(failure(code, message));
    }

    public static ReadPropertyMessageReply failure(String code, String message) {

        ReadPropertyMessageReply reply = new ReadPropertyMessageReply();
        reply.setCode(code);
        reply.setSuccess(false);
        reply.setMessage(message);

        return reply;
    }
}
