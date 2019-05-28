package org.jetlinks.core.message;

/**
 * 支持回复的消息
 *
 * @author zhouhao
 * @see org.jetlinks.core.message.property.ReadPropertyMessage
 * @see org.jetlinks.core.message.property.WritePropertyMessage
 * @see org.jetlinks.core.message.function.FunctionInvokeMessage
 * @since 1.0.0
 */
public interface RepayableDeviceMessage<R extends DeviceMessageReply> extends DeviceMessage {

    /**
     * 新建一个回复对象
     *
     * @return 回复对象
     * @see org.jetlinks.core.message.property.ReadPropertyMessageReply
     * @see org.jetlinks.core.message.property.WritePropertyMessageReply
     * @see org.jetlinks.core.message.function.FunctionInvokeMessageReply
     */
    R newReply();

}
