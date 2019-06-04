package org.jetlinks.core.message.interceptor;

import org.jetlinks.core.message.codec.EncodedMessage;
import org.jetlinks.core.message.codec.MessageEncodeContext;

public interface EncodeDeviceMessageInterceptor extends DeviceMessageInterceptor {

    void preEncode(MessageEncodeContext context);

    EncodedMessage postEncode(MessageEncodeContext context, EncodedMessage message);

}
