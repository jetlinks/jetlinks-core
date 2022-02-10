package org.jetlinks.core.message.function;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonThingMessageReply;
import org.jetlinks.core.message.MessageType;


/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class DefaultFunctionInvokeMessageReply extends CommonThingMessageReply<DefaultFunctionInvokeMessageReply>
        implements ThingFunctionInvokeMessageReply {

    private String functionId;

    private Object output;

    public DefaultFunctionInvokeMessageReply() {
    }

    public MessageType getMessageType() {
        return MessageType.INVOKE_FUNCTION_REPLY;
    }

    public static DefaultFunctionInvokeMessageReply create() {
        DefaultFunctionInvokeMessageReply reply = new DefaultFunctionInvokeMessageReply();
        reply.setTimestamp(System.currentTimeMillis());
        return reply;
    }

    @Override
    public DefaultFunctionInvokeMessageReply functionId(String id) {
        this.functionId = id;
        return this;
    }

    public DefaultFunctionInvokeMessageReply success() {
        this.setSuccess(true);
        return this;
    }

    public DefaultFunctionInvokeMessageReply success(Object output) {
        return success()
                .output(output);
    }

    public DefaultFunctionInvokeMessageReply output(Object output) {
        this.setOutput(output);
        return this;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        this.functionId = jsonObject.getString("functionId");
        this.output = jsonObject.get("output");
    }

    public static DefaultFunctionInvokeMessageReply success(String thingType,
                                                            String thingId,
                                                            String functionId,
                                                            String messageId,
                                                            Object output) {
        DefaultFunctionInvokeMessageReply reply = new DefaultFunctionInvokeMessageReply();

        reply.setFunctionId(functionId);
        reply.setOutput(output);
        reply.success();
        reply.setThingType(thingType);
        reply.setThingId(thingId);
        reply.setMessageId(messageId);

        return reply;
    }

    public static DefaultFunctionInvokeMessageReply error(String thingType,
                                                          String thingId,
                                                          String functionId,
                                                          String messageId,
                                                          String message) {
        DefaultFunctionInvokeMessageReply reply = new DefaultFunctionInvokeMessageReply();

        reply.setFunctionId(functionId);
        reply.setMessage(message);
        reply.setSuccess(false);
        reply.setThingType(thingType);
        reply.setThingId(thingId);
        reply.setMessageId(messageId);

        return reply;
    }
}
