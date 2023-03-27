package org.jetlinks.core.message.function;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonThingMessage;
import org.jetlinks.core.message.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    public DefaultFunctionInvokeMessage functionId(String id) {
        this.functionId=id;
        return this;
    }

    @Override
    @SuppressWarnings("all")
    public void fromJson(JSONObject jsonObject) {
        Object inputs = jsonObject.get("inputs");
        //处理以Map形式传入参数的场景
        if (inputs instanceof Map) {

            super.fromJson(new JSONObject(Maps.filterKeys(jsonObject, key -> !"inputs".equals(key))));

            Map<String, Object> inputMap = (Map<String, Object>) inputs;
            inputMap.forEach(this::addInput);

            return;
        }
        super.fromJson(jsonObject);
    }

    @Override
    public DefaultFunctionInvokeMessageReply newReply() {
        DefaultFunctionInvokeMessageReply reply = new DefaultFunctionInvokeMessageReply().from(this);
        reply.setFunctionId(this.functionId);
        return reply;
    }
}
