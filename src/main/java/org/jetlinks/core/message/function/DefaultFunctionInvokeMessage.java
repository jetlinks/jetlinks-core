package org.jetlinks.core.message.function;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonThingMessage;
import org.jetlinks.core.message.MessageType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class DefaultFunctionInvokeMessage extends CommonThingMessage<DefaultFunctionInvokeMessage>
        implements ThingFunctionInvokeMessage<DefaultFunctionInvokeMessageReply> {

    private String functionId;

    public DefaultFunctionInvokeMessage() {

    }

    private List<FunctionParameter> inputs = new ArrayList<>();

    @Override
    public MessageType getMessageType() {
        return ThingFunctionInvokeMessage.super.getMessageType();
    }

    public DefaultFunctionInvokeMessage addInput(FunctionParameter parameter) {
        inputs.add(parameter);
        return this;
    }

    @Override
    public DefaultFunctionInvokeMessage addHeader(String header, Object value) {
        super.addHeader(header, value);
        return this;
    }

    @Override
    public DefaultFunctionInvokeMessage removeHeader(String header) {
        super.removeHeader(header);
        return this;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        this.functionId = jsonObject.getString("functionId");
    }

    @Override
    public DefaultFunctionInvokeMessageReply newReply() {
        DefaultFunctionInvokeMessageReply reply = new DefaultFunctionInvokeMessageReply().from(this);
        reply.setFunctionId(this.functionId);
        return reply;
    }
}
