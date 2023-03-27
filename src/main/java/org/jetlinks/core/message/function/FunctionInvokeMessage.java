package org.jetlinks.core.message.function;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.RepayableDeviceMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class FunctionInvokeMessage extends CommonDeviceMessage<FunctionInvokeMessage>
        implements RepayableDeviceMessage<FunctionInvokeMessageReply>,
        ThingFunctionInvokeMessage<FunctionInvokeMessageReply> {

    private String functionId;

    public FunctionInvokeMessage() {

    }

    private List<FunctionParameter> inputs = new ArrayList<>();

    @Override
    public FunctionInvokeMessage functionId(String id) {
        this.functionId = id;
        return this;
    }

    public FunctionInvokeMessage addInput(FunctionParameter parameter) {
        inputs.add(parameter);
        return this;
    }

    @Override
    @SuppressWarnings("all")
    public void fromJson(JSONObject jsonObject) {
        this.functionId = jsonObject.getString("functionId");

        Object inputs = jsonObject.get("inputs");
        //处理以Map形式传入参数的场景
        if (inputs instanceof Map) {

            super.fromJson(new JSONObject(
                    Maps.filterKeys(jsonObject, key -> !"inputs".equals(key)
                    )));

            Map<String, Object> inputMap = (Map<String, Object>) inputs;
            inputMap.forEach(this::addInput);

            return;
        }
        super.fromJson(jsonObject);

    }

    @Override
    public FunctionInvokeMessageReply newReply() {
        FunctionInvokeMessageReply reply = new FunctionInvokeMessageReply().from(this);
        reply.setFunctionId(this.functionId);
        return reply;
    }
}
