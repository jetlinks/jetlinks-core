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
public class FunctionInvokeMessage extends CommonDeviceMessage
        implements RepayableDeviceMessage<FunctionInvokeMessageReply> {

    private String functionId;

    private Boolean async;

    private List<FunctionParameter> inputs = new ArrayList<>();

    public FunctionInvokeMessage addInput(String name, Object value) {
        inputs.add(new FunctionParameter(name, value));
        return this;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        this.functionId = jsonObject.getString("functionId");
    }

    @Override
    public FunctionInvokeMessageReply newReply() {
        return new FunctionInvokeMessageReply();
    }
}
