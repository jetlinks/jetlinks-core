package org.jetlinks.core.message.function;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessage;
import org.jetlinks.core.message.RepayableDeviceMessage;

import java.util.ArrayList;
import java.util.List;

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
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        this.functionId = jsonObject.getString("functionId");
    }

    @Override
    public FunctionInvokeMessageReply newReply() {
        FunctionInvokeMessageReply reply = new FunctionInvokeMessageReply().from(this);
        reply.setFunctionId(this.functionId);
        return reply;
    }
}
