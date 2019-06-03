package org.jetlinks.core.message.function;

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

}
