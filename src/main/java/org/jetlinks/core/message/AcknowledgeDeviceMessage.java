package org.jetlinks.core.message;

/**
 * 应答消息,通常用于发送非可回复消息后的应答
 *
 * @author zhouhao
 * @since 1.1.5
 */
public class AcknowledgeDeviceMessage extends CommonDeviceMessageReply<AcknowledgeDeviceMessage> {

    @Override
    public MessageType getMessageType() {
        return MessageType.ACKNOWLEDGE;
    }
}
