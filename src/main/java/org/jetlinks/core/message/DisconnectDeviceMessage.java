package org.jetlinks.core.message;

public class DisconnectDeviceMessage extends CommonDeviceMessage<DisconnectDeviceMessage> implements RepayableDeviceMessage<DisconnectDeviceMessageReply> {

    @Override
    public DisconnectDeviceMessageReply newReply() {
        return new DisconnectDeviceMessageReply().from(this);
    }

    public MessageType getMessageType() {
        return MessageType.DISCONNECT;
    }

    @Override
    public MessageType getReplyType() {
        return MessageType.DISCONNECT_REPLY;
    }
}
