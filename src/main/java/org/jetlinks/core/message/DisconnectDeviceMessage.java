package org.jetlinks.core.message;

public class DisconnectDeviceMessage extends CommonDeviceMessage implements RepayableDeviceMessage<DisconnectDeviceMessageReply> {

    @Override
    public DisconnectDeviceMessageReply newReply() {
        return new DisconnectDeviceMessageReply().from(this);
    }

    public MessageType getMessageType() {
        return MessageType.DISCONNECT;
    }

}
