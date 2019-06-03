package org.jetlinks.core.message.function;

import io.vavr.control.Try;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.message.CommonDeviceMessageReply;


/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class FunctionInvokeMessageReply extends CommonDeviceMessageReply {

    private String functionId;

    private Object output;

    public static FunctionInvokeMessageReply failure(ErrorCode errorCode) {
        FunctionInvokeMessageReply reply = new FunctionInvokeMessageReply();
        reply.error(errorCode);
        reply.setTimestamp(System.currentTimeMillis());

        return reply;
    }

    public static Try<FunctionInvokeMessageReply> failureTry(ErrorCode errorCode) {
        return Try.success(failure(errorCode));
    }

    public static Try<FunctionInvokeMessageReply> failureTry(String code, String message) {
        return Try.success(failure(code, message));
    }

    public static FunctionInvokeMessageReply failure(String code, String message) {

        FunctionInvokeMessageReply reply = new FunctionInvokeMessageReply();
        reply.setCode(code);
        reply.setSuccess(false);
        reply.setMessage(message);
        reply.setTimestamp(System.currentTimeMillis());

        return reply;
    }

    public FunctionInvokeMessageReply messageId(String messageId){
        this.setMessageId(messageId);
        return this;
    }
}
