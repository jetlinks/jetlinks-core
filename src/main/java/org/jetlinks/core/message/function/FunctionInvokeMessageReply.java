package org.jetlinks.core.message.function;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;
import org.jetlinks.core.message.MessageType;


/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class FunctionInvokeMessageReply extends CommonDeviceMessageReply<FunctionInvokeMessageReply> {

    private String functionId;

    private Object output;

    public FunctionInvokeMessageReply() {
    }

    public MessageType getMessageType() {
        return MessageType.INVOKE_FUNCTION_REPLY;
    }

    public static FunctionInvokeMessageReply create() {
        FunctionInvokeMessageReply reply = new FunctionInvokeMessageReply();
        reply.setTimestamp(System.currentTimeMillis());
        return reply;
    }

    public FunctionInvokeMessageReply success() {
        this.setSuccess(true);
        return this;
    }

    public FunctionInvokeMessageReply success(Object output) {
        return success()
                .output(output);
    }

    public FunctionInvokeMessageReply output(Object output) {
        this.setOutput(output);
        return this;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        this.functionId = jsonObject.getString("functionId");
        this.output = jsonObject.get("output");
    }

    public static FunctionInvokeMessageReply success(String deviceId,
                                                     String functionId,
                                                     String messageId,
                                                     Object output){
        FunctionInvokeMessageReply reply = new FunctionInvokeMessageReply();

        reply.setFunctionId(functionId);
        reply.setOutput(output);
        reply.success();
        reply.setDeviceId(deviceId);
        reply.setMessageId(messageId);

        return reply;
    }

    public static FunctionInvokeMessageReply error(String deviceId,
                                                     String functionId,
                                                     String messageId,
                                                     String message){
        FunctionInvokeMessageReply reply = new FunctionInvokeMessageReply();

        reply.setFunctionId(functionId);
        reply.setMessage(message);
        reply.setSuccess(false);
        reply.setDeviceId(deviceId);
        reply.setMessageId(messageId);

        return reply;
    }
}
