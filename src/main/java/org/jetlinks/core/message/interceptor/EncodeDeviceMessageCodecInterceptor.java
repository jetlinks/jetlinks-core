package org.jetlinks.core.message.interceptor;

import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MessageEncodeContext;

public interface EncodeDeviceMessageCodecInterceptor extends DeviceMessageCodecInterceptor {

    void preEncode(MessageEncodeContext context);

    EncodedMessage postEncode(MessageEncodeContext context, EncodedMessage message);

}
