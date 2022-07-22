package org.jetlinks.core.message.function;

import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.ThingMessageReply;
import org.jetlinks.core.metadata.FunctionMetadata;
import org.jetlinks.core.utils.SerializeUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
 * 物功能调用回复,用于对物模型功能调用进行响应
 *
 * @author zhouhao
 * @see ThingFunctionInvokeMessage
 * @since 1.1.9
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

    ThingFunctionInvokeMessageReply functionId(String functionId);

    default MessageType getMessageType() {
        return MessageType.INVOKE_FUNCTION_REPLY;
    }


    @Override
    default void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        ThingMessageReply.super.readExternal(in);
        functionId(SerializeUtils.readNullableUTF(in));
        output(SerializeUtils.readObject(in));
    }

    @Override
    default void writeExternal(ObjectOutput out) throws IOException {
        ThingMessageReply.super.writeExternal(out);
        SerializeUtils.writeNullableUTF(getFunctionId(), out);
        SerializeUtils.writeObject(getOutput(), out);
    }
}
