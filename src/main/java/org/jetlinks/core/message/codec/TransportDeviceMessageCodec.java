package org.jetlinks.core.message.codec;

import org.jetlinks.core.message.DeviceMessage;

/**
 * @since 1.0
 **/
public interface TransportDeviceMessageCodec {

    Transport getSupportTransport();

    EncodedMessage encode(MessageEncodeContext context);

    DeviceMessage decode(MessageDecodeContext context);
}
