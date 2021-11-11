package org.jetlinks.core.message.function;

import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.ThingMessageReply;
import org.jetlinks.core.metadata.FunctionMetadata;

import javax.annotation.Nullable;


/**
 * 物功能调用回复,用于对物模型功能调用进行响应
 *
 * @author zhouhao
 * @since 1.1.9
 * @see ThingFunctionInvokeMessage
 */

public interface ThingFunctionInvokeMessageReply extends ThingMessageReply {

    /**
     * functionId 对应物模型功能ID
     *
     * @return functionId
     * @see FunctionMetadata#getId()
     */
    @Nullable
    String getFunctionId();

    /**
     * 功能调用响应结果,根据物模型的不同而不同.
     *
     * @return 响应结果
     * @see FunctionMetadata#getOutput()
     */
    Object getOutput();

    /**
     * 设置功能的输出值
     *
     * @param output output
     * @return this
     */
    ThingFunctionInvokeMessageReply output(Object output);

    default MessageType getMessageType() {
        return MessageType.INVOKE_FUNCTION_REPLY;
    }


}
