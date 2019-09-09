package org.jetlinks.core.message.codec;

public interface ToDeviceMessageContext extends MessageEncodeContext{
    void sendToDevice(EncodedMessage message);

    void disconnect();
}
