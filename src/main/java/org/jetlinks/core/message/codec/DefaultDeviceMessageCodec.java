package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.DeviceMessage;

import java.util.HashMap;
import java.util.Map;

/**
 * @author bsetfeng
 * @author zhouhao
 * @since 1.0
 **/
public class DefaultDeviceMessageCodec implements DeviceMessageCodec {

    private Map<Transport, TransportDeviceMessageCodec> messageCodec = new HashMap<>();

    public void register(TransportDeviceMessageCodec codec) {
        messageCodec.put(codec.getSupportTransport(), codec);
    }

    @Override
    public EncodedMessage encode(Transport transport, MessageEncodeContext context) {
        return messageCodec.get(transport).encode(context);
    }

    @Override
    public DeviceMessage decode(Transport transport, MessageDecodeContext message) {
        return messageCodec.get(transport).decode(message);
    }
}
