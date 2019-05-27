package org.jetlinks.core.message.codec;


/**
 * @author zhouhao
 * @since 1.0.0
 */
public interface FromDeviceMessageContext extends MessageDecodeContext {

    void sendToDevice(EncodedMessage message);

    void disconnect();
}
