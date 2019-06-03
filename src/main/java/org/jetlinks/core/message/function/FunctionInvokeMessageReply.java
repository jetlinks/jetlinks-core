package org.jetlinks.core.message.function;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.core.message.CommonDeviceMessageReply;


/**
 * @author zhouhao
 * @since 1.0.0
 */
@Getter
@Setter
public class FunctionInvokeMessageReply extends CommonDeviceMessageReply<FunctionInvokeMessageReply> {

    private String functionId;

    private Object output;

    public static FunctionInvokeMessageReply create() {
        return new FunctionInvokeMessageReply();
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        this.functionId=jsonObject.getString("functionId");
        this.output=jsonObject.get("output");

    }
}
